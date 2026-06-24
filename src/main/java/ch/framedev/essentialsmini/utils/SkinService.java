package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.commands.playercommands.VanishCMD;
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

    private static final long TAB_READD_DELAY_TICKS = 2L;
    private static final long ENTITY_RESPAWN_DELAY_TICKS = 2L;
    private static final long ENTITY_SHOW_DELAY_TICKS = 1L;
    private static final String VANISH_SEE_PERMISSION = "essentialsmini.vanish.see";

    private final Plugin plugin;
    private final ProtocolManager pm;
    private final Map<UUID, ProfileOverride> overrides = new ConcurrentHashMap<>();
    private final Map<UUID, WrappedGameProfile> originalProfiles = new ConcurrentHashMap<>();

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
                    WrappedGameProfile repl = createProfile(profileId, ov.profileName(), ov.textures(), old);

                    list.set(i, createPlayerInfoData(
                            d,
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
        captureOriginalProfile(player);
        WrappedSignedProperty textures = value == null || signature == null
                ? null
                : new WrappedSignedProperty("textures", value, signature);
        overrides.put(player.getUniqueId(), new ProfileOverride(profileName, textures));
        pushSkinRefresh(player, profileName, textures, null);
    }

    public void clear(Player player) {
        overrides.remove(player.getUniqueId());
        WrappedGameProfile originalProfile = originalProfiles.remove(player.getUniqueId());
        if (originalProfile == null) {
            originalProfile = readCurrentProfile(player);
        }
        pushSkinRefresh(player, player.getName(), getTextures(originalProfile), originalProfile);
    }

    private void pushSkinRefresh(Player target, String profileName, WrappedSignedProperty textures, WrappedGameProfile baseProfile) {
        try {
            PacketContainer remove = createRemovePacket(target);
            PacketContainer add = createAddPacket(target, profileName, textures, baseProfile);
            sendRemovePacket(target, remove);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline()) return;
                sendAddPacket(target, add);
                target.updateInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> refreshEntityForViewers(target), ENTITY_RESPAWN_DELAY_TICKS);
            }, TAB_READD_DELAY_TICKS);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinRefresh] " + e.getMessage());
        }
    }

    private PacketContainer createAddPacket(Player target, String profileName, WrappedSignedProperty textures,
                                            WrappedGameProfile baseProfile) {
        PacketContainer add = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        writeAddActions(add);

        WrappedGameProfile profile = createProfile(target.getUniqueId(), profileName, textures, baseProfile);

        if (add.getPlayerInfoDataLists().size() > 0) {
            add.getPlayerInfoDataLists().write(0, Collections.singletonList(createPlayerInfoData(
                    null,
                    target.getUniqueId(),
                    profile,
                    target.getPing(),
                    true,
                    EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                    WrappedChatComponent.fromText(profileName)
            )));
        } else {
            plugin.getLogger().warning("[SkinRefresh] ADD_PLAYER has no PlayerInfoData list on this mapping.");
        }
        return add;
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
                    null,
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

    private PlayerInfoData createPlayerInfoData(PlayerInfoData source, UUID profileId, WrappedGameProfile profile, int latency,
                                                boolean listed, EnumWrappers.NativeGameMode gameMode,
                                                WrappedChatComponent displayName) {
        if (source != null) {
            try {
                return new PlayerInfoData(profileId, latency, listed, gameMode, profile, displayName,
                        source.isShowHat(), source.getListOrder(), source.getRemoteChatSessionData());
            } catch (Throwable ignored) {
                // Older ProtocolLib constructors are handled below.
            }
        }

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

    private void captureOriginalProfile(Player player) {
        originalProfiles.computeIfAbsent(player.getUniqueId(), ignored -> {
            WrappedGameProfile profile = readCurrentProfile(player);
            return profile == null ? new WrappedGameProfile(player.getUniqueId(), player.getName()) : profile;
        });
    }

    private WrappedGameProfile readCurrentProfile(Player player) {
        try {
            return cloneProfile(WrappedGameProfile.fromPlayer(player), player.getUniqueId(), player.getName());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private WrappedGameProfile createProfile(UUID profileId, String profileName, WrappedSignedProperty textures,
                                             WrappedGameProfile baseProfile) {
        WrappedGameProfile profile = new WrappedGameProfile(profileId, profileName);
        if (baseProfile != null) {
            for (WrappedSignedProperty prop : baseProfile.getProperties().values()) {
                if (prop != null && !"textures".equalsIgnoreCase(prop.getName())) {
                    profile.getProperties().put(prop.getName(), prop);
                }
            }
        }
        if (textures != null) {
            profile.getProperties().put("textures", textures);
        }
        return profile;
    }

    private WrappedGameProfile cloneProfile(WrappedGameProfile source, UUID profileId, String fallbackName) {
        String name = source == null || source.getName() == null ? fallbackName : source.getName();
        WrappedGameProfile copy = new WrappedGameProfile(profileId, name);
        if (source != null) {
            for (WrappedSignedProperty prop : source.getProperties().values()) {
                if (prop != null) {
                    copy.getProperties().put(prop.getName(), prop);
                }
            }
        }
        return copy;
    }

    private WrappedSignedProperty getTextures(WrappedGameProfile profile) {
        if (profile == null) {
            return null;
        }
        for (WrappedSignedProperty prop : profile.getProperties().get("textures")) {
            if (prop != null) {
                return prop;
            }
        }
        return null;
    }

    private void sendRemovePacket(Player target, PacketContainer packet) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendPacket(viewer, packet);
        }
    }

    private void sendAddPacket(Player target, PacketContainer packet) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target) || viewer.canSee(target)) {
                sendPacket(viewer, packet);
            }
        }
    }

    private void sendPacket(Player viewer, PacketContainer packet) {
        try {
            pm.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinRefresh] Could not send packet to " + viewer.getName() + ": " + e.getMessage());
        }
    }

    private void refreshEntityForViewers(Player target) {
        if (!target.isOnline()) {
            return;
        }

        List<Player> visibleViewers = new ArrayList<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target) && viewer.canSee(target)) {
                visibleViewers.add(viewer);
                viewer.hidePlayer(plugin, target);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isOnline()) {
                return;
            }
            for (Player viewer : visibleViewers) {
                if (viewer.isOnline() && shouldRestoreVisibility(viewer, target)) {
                    viewer.showPlayer(plugin, target);
                }
            }
        }, ENTITY_SHOW_DELAY_TICKS);
    }

    private boolean shouldRestoreVisibility(Player viewer, Player target) {
        return !VanishCMD.hided.contains(target.getName()) || viewer.hasPermission(VANISH_SEE_PERMISSION);
    }

    private record ProfileOverride(String profileName, WrappedSignedProperty textures) {
    }
}
