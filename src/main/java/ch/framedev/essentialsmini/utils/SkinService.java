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


@SuppressWarnings({"deprecation", "null"})
public final class SkinService {

    private final Plugin plugin;
    private final ProtocolManager pm;
    private final Map<UUID, ProfileOverride> overrides = new ConcurrentHashMap<>();

    public SkinService(Plugin plugin) {
        this.plugin = plugin;
        this.pm = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            @Override public void onPacketSending(PacketEvent event) {
                PacketContainer p = event.getPacket();
                if (!containsProfileData(p)) {
                    return;
                }

                List<PlayerInfoData> list;
                try { list = new ArrayList<>(p.getPlayerInfoDataLists().read(0)); }
                catch (Throwable t) { return; }

                boolean changed = false;
                for (int i = 0; i < list.size(); i++) {
                    PlayerInfoData d = list.get(i);
                    UUID profileId = getProfileId(d);
                    if (profileId == null || d.getProfile() == null) continue;

                    ProfileOverride ov = overrides.get(profileId);
                    if (ov == null) continue;

                    WrappedGameProfile old = d.getProfile();
                    WrappedGameProfile repl = new WrappedGameProfile(profileId, ov.profileName());
                    for (WrappedSignedProperty prop : old.getProperties().values()) {
                        if (!"textures".equalsIgnoreCase(Objects.requireNonNull(prop).getName())) {
                            repl.getProperties().put(prop.getName(), prop);
                        }
                    }
                    if (ov.textures() != null) {
                        repl.getProperties().put("textures", ov.textures());
                    }

                    list.set(i, createPlayerInfoData(
                            profileId,
                            repl,
                            d.getLatency(),
                            d.isListed(),
                            d.getGameMode(),
                            d.getDisplayName()
                    ));
                    changed = true;
                }
                if (changed) p.getPlayerInfoDataLists().write(0, list);
            }
        });
    }

    public void apply(Player player, String value, String signature) {
        apply(player, player.getName(), value, signature);
    }

    public void apply(Player player, String profileName, String value, String signature) {
        WrappedSignedProperty textures = value == null || signature == null
                ? null
                : new WrappedSignedProperty("textures", value, signature);
        overrides.put(player.getUniqueId(), new ProfileOverride(profileName, textures));
        pushTabRefreshLegacy(player, profileName, textures);
    }

    public void clear(Player player) {
        overrides.remove(player.getUniqueId());
        pushTabRefreshLegacy(player, player.getName(), null);
    }

    private void pushTabRefreshLegacy(Player target, String profileName, WrappedSignedProperty textures) {
        try {
            // ----- REMOVE -----
            PacketContainer remove = createRemovePacket(target);

            // ----- ADD (must be PlayerInfoData list) -----
            PacketContainer add = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
            writeAddActions(add);

            WrappedGameProfile profile = new WrappedGameProfile(target.getUniqueId(), profileName);
            if (textures != null) {
                profile.getProperties().put("textures", textures);
            }

            if (add.getPlayerInfoDataLists().size() > 0) {
                add.getPlayerInfoDataLists().write(0, Collections.singletonList(createPlayerInfoData(
                        target.getUniqueId(),
                        profile,
                        0,
                        true,
                        EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                        WrappedChatComponent.fromText(profileName)
                )));
            } else {
                // Fallback: extremely old mappings - send nothing rather than crashing.
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

    private PacketContainer createRemovePacket(Player target) {
        try {
            PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            if (remove.getUUIDLists().size() > 0) {
                remove.getUUIDLists().write(0, Collections.singletonList(target.getUniqueId()));
                return remove;
            }
        } catch (Throwable ignored) {
            // Older ProtocolLib/Minecraft mappings use PLAYER_INFO with REMOVE_PLAYER.
        }

        PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        remove.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

        if (remove.getUUIDLists().size() > 0) {
            remove.getUUIDLists().write(0, Collections.singletonList(target.getUniqueId()));
        } else if (remove.getPlayerInfoDataLists().size() > 0) {
            remove.getPlayerInfoDataLists().write(0, Collections.singletonList(createPlayerInfoData(
                    target.getUniqueId(),
                    new WrappedGameProfile(target.getUniqueId(), target.getName()),
                    0,
                    true,
                    EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                    null
            )));
        }
        return remove;
    }

    private boolean containsProfileData(PacketContainer packet) {
        try {
            if (packet.getPlayerInfoActions().size() > 0) {
                Set<EnumWrappers.PlayerInfoAction> actions = packet.getPlayerInfoActions().read(0);
                return actions == null || actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER)
                        || actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
                        || actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE)
                        || actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY)
                        || actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_LISTED);
            }
        } catch (Throwable ignored) {
            // Fall back to the legacy single-action field below.
        }

        try {
            EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
            return action == EnumWrappers.PlayerInfoAction.ADD_PLAYER
                    || action == EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
                    || action == EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE
                    || action == EnumWrappers.PlayerInfoAction.UPDATE_LATENCY;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void writeAddActions(PacketContainer packet) {
        try {
            if (packet.getPlayerInfoActions().size() > 0) {
                packet.getPlayerInfoActions().write(0, EnumSet.of(
                        EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                        EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                        EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
                        EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
                        EnumWrappers.PlayerInfoAction.UPDATE_LISTED
                ));
                return;
            }
        } catch (Throwable ignored) {
            // Fall back to legacy single-action packets.
        }

        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
    }

    private PlayerInfoData createPlayerInfoData(UUID profileId, WrappedGameProfile profile, int latency,
                                                boolean listed, EnumWrappers.NativeGameMode gameMode,
                                                WrappedChatComponent displayName) {
        try {
            return new PlayerInfoData(profileId, latency, listed, gameMode, profile, displayName);
        } catch (Throwable ignored) {
            return new PlayerInfoData(profile, latency, gameMode, displayName);
        }
    }

    private UUID getProfileId(PlayerInfoData data) {
        try {
            UUID profileId = data.getProfileId();
            if (profileId != null) {
                return profileId;
            }
        } catch (Throwable ignored) {
            // Older ProtocolLib versions only expose the profile UUID.
        }
        return data.getProfile() == null ? null : data.getProfile().getUUID();
    }

    private record ProfileOverride(String profileName, WrappedSignedProperty textures) {
    }
}
