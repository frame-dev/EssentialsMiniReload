package ch.framedev.essentialsmini.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class SkinService {

    private final Plugin plugin;
    private final ProtocolManager pm;
    // store the skin override (value+signature) for each player uuid
    private final Map<UUID, WrappedSignedProperty> overrides = new ConcurrentHashMap<>();

    public SkinService(Plugin plugin) {
        this.plugin = plugin;
        this.pm = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            @Override public void onPacketSending(PacketEvent event) {
                PacketContainer p = event.getPacket();
                EnumWrappers.PlayerInfoAction action;
                try { action = p.getPlayerInfoAction().read(0); } catch (Throwable t) { return; }

                switch (action) {
                    case ADD_PLAYER:
                    case UPDATE_DISPLAY_NAME:
                    case UPDATE_GAME_MODE:
                    case UPDATE_LATENCY:
                        break;
                    default:
                        return; // e.g., REMOVE_PLAYER -> may be UUID list; don’t touch
                }

                List<PlayerInfoData> list;
                try { list = new ArrayList<>(p.getPlayerInfoDataLists().read(0)); }
                catch (Throwable t) { return; }

                boolean changed = false;
                for (int i = 0; i < list.size(); i++) {
                    PlayerInfoData d = list.get(i);
                    WrappedSignedProperty ov = overrides.get(d.getProfile().getUUID());
                    if (ov == null) continue;

                    WrappedGameProfile old = d.getProfile();
                    WrappedGameProfile repl = new WrappedGameProfile(old.getUUID(), old.getName());
                    for (WrappedSignedProperty prop : old.getProperties().values())
                        if (!"textures".equalsIgnoreCase(Objects.requireNonNull(prop).getName()))
                            repl.getProperties().put(prop.getName(), prop);
                    repl.getProperties().put("textures", ov);

                    list.set(i, new PlayerInfoData(repl, d.getLatency(), d.getGameMode(), d.getDisplayName()));
                    changed = true;
                }
                if (changed) p.getPlayerInfoDataLists().write(0, list);
            }
        });
    }

    public void apply(Player player, String value, String signature) {
        overrides.put(player.getUniqueId(), new WrappedSignedProperty("textures", value, signature));
        pushTabRefreshLegacy(player, value, signature);
    }

    public void clear(Player player) {
        overrides.remove(player.getUniqueId());
        pushTabRefreshLegacy(player, null, null);
    }

    private void pushTabRefreshLegacy(Player target, String maybeValue, String maybeSig) {
        try {
            // ----- REMOVE -----
            PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
            remove.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

            // On newer mappings REMOVE uses UUID list; older used PlayerInfoData list.
            if (remove.getUUIDLists().size() > 0) {
                remove.getUUIDLists().write(0, Collections.singletonList(target.getUniqueId()));
            } else if (remove.getPlayerInfoDataLists().size() > 0) {
                remove.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(
                        new WrappedGameProfile(target.getUniqueId(), target.getName()),
                        0,
                        EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                        null
                )));
            } // else: nothing to write (very rare), but avoid crashing.

            // ----- ADD (must be PlayerInfoData list) -----
            PacketContainer add = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
            add.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

            WrappedGameProfile profile = new WrappedGameProfile(target.getUniqueId(), target.getName());
            if (maybeValue != null && maybeSig != null) {
                profile.getProperties().put("textures", new WrappedSignedProperty("textures", maybeValue, maybeSig));
            }

            if (add.getPlayerInfoDataLists().size() > 0) {
                add.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(
                        profile,
                        0,
                        EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                        null
                )));
            } else {
                // Fallback: extremely old mappings — send nothing rather than crashing
                plugin.getLogger().warning("[SkinRefresh] ADD_PLAYER has no PlayerInfoData list on this mapping.");
            }

            // Send to everyone (others first, then self)
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(target)) {
                    pm.sendServerPacket(viewer, remove);
                    pm.sendServerPacket(viewer, add);
                }
            }
            pm.sendServerPacket(target, remove);
            pm.sendServerPacket(target, add);

        } catch (Exception e) {
            plugin.getLogger().warning("[SkinRefresh] " + e.getMessage());
        }
    }
}