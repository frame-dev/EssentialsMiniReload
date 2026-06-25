package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.commands.playercommands.VanishCMD;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.*;
import com.google.common.collect.Multimap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings({"deprecation", "null"})
public final class SkinService {

    private static final long TAB_READD_DELAY_TICKS = 2L;
    private static final long ENTITY_RESPAWN_DELAY_TICKS = 2L;
    private static final long ENTITY_SHOW_DELAY_TICKS = 1L;
    private static final String VANISH_SEE_PERMISSION = "essentialsmini.vanish.see";
    private static final String TEXTURES_PROPERTY = "textures";

    private final Plugin plugin;
    private final ProtocolManager pm;
    private final Map<UUID, ProfileOverride> overrides = new ConcurrentHashMap<>();
    private final Map<UUID, WrappedGameProfile> originalProfiles = new ConcurrentHashMap<>();

    public SkinService(Plugin plugin) {
        this.plugin = plugin;
        this.pm = ProtocolLibrary.getProtocolManager();
        debug("SkinService initialized with ProtocolLib " + plugin.getServer().getPluginManager().getPlugin("ProtocolLib").getDescription().getVersion());
    }

    public void start() {
        debug("Registering PLAYER_INFO listener for skin profile rewrites");
        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            @Override public void onPacketSending(PacketEvent event) {
                PacketContainer p = event.getPacket();
                if (!containsProfileData(p)) {
                    return;
                }

                List<PlayerInfoData> list;
                try { list = p.getPlayerInfoDataLists().read(0); }
                catch (Throwable t) { return; }
                if (list == null || list.isEmpty()) {
                    return;
                }

                boolean changed = false;
                List<PlayerInfoData> fallbackList = null;
                for (int i = 0; i < list.size(); i++) {
                    PlayerInfoData d = list.get(i);
                    WrappedGameProfile old = getProfile(d);
                    if (old == null) continue;

                    UUID profileId = getProfileId(d, old);
                    if (profileId == null) continue;

                    ProfileOverride ov = overrides.get(profileId);
                    if (ov == null) continue;

                    WrappedGameProfile repl = createProfile(profileId, ov.profileName(), ov.textures(), old);
                    debug("Rewriting outgoing PLAYER_INFO profile for " + shortId(profileId)
                            + " viewer=" + event.getPlayer().getName()
                            + " profileName='" + ov.profileName() + "' textures=" + describeTextures(ov.textures()));

                    PlayerInfoData replacement = createPlayerInfoData(
                            d,
                            profileId,
                            repl,
                            d.getLatency(),
                            d.isListed(),
                            d.getGameMode(),
                            d.getDisplayName()
                    );
                    try {
                        list.set(i, replacement);
                    } catch (Throwable ignored) {
                        if (fallbackList == null) {
                            fallbackList = new ArrayList<>(list);
                        }
                        fallbackList.set(i, replacement);
                    }
                    changed = true;
                }
                if (changed && fallbackList != null) {
                    boolean written = writePlayerInfoDataList(p, fallbackList);
                    debug("PLAYER_INFO listener wrote fallback list=" + written + " viewer=" + event.getPlayer().getName());
                }
            }
        });
    }

    public void apply(Player player, String value, String signature) {
        apply(player, player.getName(), value, signature);
    }

    public void apply(Player player, String profileName, String value, String signature) {
        debug("Applying skin override target=" + player.getName()
                + " uuid=" + shortId(player.getUniqueId())
                + " profileName='" + profileName + "' valueLength=" + length(value)
                + ", signatureLength=" + length(signature));
        captureOriginalProfile(player);
        TextureProperty textures = value == null || signature == null
                ? null
                : new TextureProperty(value, signature);
        overrides.put(player.getUniqueId(), new ProfileOverride(profileName, textures));
        boolean profileUpdated = applyProfileTextures(player, profileName, textures);
        debug("Profile update path completed for " + player.getName() + " success=" + profileUpdated);
        pushSkinRefresh(player, profileName, textures, null);
    }

    public void clear(Player player) {
        debug("Clearing skin override target=" + player.getName() + " uuid=" + shortId(player.getUniqueId()));
        overrides.remove(player.getUniqueId());
        WrappedGameProfile originalProfile = originalProfiles.remove(player.getUniqueId());
        if (originalProfile == null) {
            originalProfile = readCurrentProfile(player);
            debug("No stored original profile for " + player.getName() + "; read current profile=" + (originalProfile != null));
        }
        TextureProperty textures = getTextures(originalProfile);
        boolean profileUpdated = applyProfileTextures(player, player.getName(), textures);
        debug("Profile reset path completed for " + player.getName() + " success=" + profileUpdated);
        pushSkinRefresh(player, player.getName(), textures, originalProfile);
    }

    private void pushSkinRefresh(Player target, String profileName, TextureProperty textures, WrappedGameProfile baseProfile) {
        try {
            PacketContainer remove = createRemovePacket(target);
            PacketContainer add = createAddPacket(target, profileName, textures, baseProfile);
            if (remove != null) {
                sendRemovePacket(target, remove);
            } else {
                debug("No remove packet could be built for " + target.getName());
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline()) return;
                if (add != null) {
                    sendAddPacket(target, add);
                    target.updateInventory();
                } else {
                    debug("No add packet could be built for " + target.getName());
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> refreshEntityForViewers(target), ENTITY_RESPAWN_DELAY_TICKS);
            }, TAB_READD_DELAY_TICKS);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinRefresh] " + e.getMessage());
            debug("pushSkinRefresh failed for " + target.getName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private PacketContainer createAddPacket(Player target, String profileName, TextureProperty textures,
                                            WrappedGameProfile baseProfile) {
        WrappedGameProfile profile = createProfile(target.getUniqueId(), profileName, textures, baseProfile);
        List<PlayerInfoData> data = Collections.singletonList(createPlayerInfoData(
                null,
                target.getUniqueId(),
                profile,
                target.getPing(),
                true,
                EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                WrappedChatComponent.fromText(profileName)
        ));

        PacketContainer nativeAdd = createNativeAddPacket(target);
        if (nativeAdd != null) {
            debug("Using native ClientboundPlayerInfoUpdatePacket ADD_PLAYER for " + target.getName());
            return nativeAdd;
        }

        PacketContainer add = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        if (!writeAddActions(add)) {
            debug("Could not write ADD_PLAYER actions through ProtocolLib for " + target.getName());
            return null;
        }

        if (add.getPlayerInfoDataLists().size() > 0) {
            boolean written = writePlayerInfoDataList(add, data);
            debug("Using ProtocolLib PLAYER_INFO ADD_PLAYER for " + target.getName() + " dataWritten=" + written);
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
                debug("Using PLAYER_INFO_REMOVE packet for " + target.getName());
                return remove;
            }
        } catch (Throwable ignored) {
            // Older ProtocolLib/Minecraft mappings use PLAYER_INFO with REMOVE_PLAYER.
        }

        PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        if (!writeRemoveAction(remove)) {
            debug("Could not write REMOVE_PLAYER action through ProtocolLib for " + target.getName());
            return null;
        }

        if (remove.getUUIDLists().size() > 0) {
            remove.getUUIDLists().write(0, Collections.singletonList(target.getUniqueId()));
            debug("Using legacy PLAYER_INFO REMOVE_PLAYER UUID list for " + target.getName());
        } else if (remove.getPlayerInfoDataLists().size() > 0) {
            boolean written = writePlayerInfoDataList(remove, Collections.singletonList(createPlayerInfoData(
                    null,
                    target.getUniqueId(),
                    new WrappedGameProfile(target.getUniqueId(), target.getName()),
                    0,
                    true,
                    EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                    null
            )));
            debug("Using legacy PLAYER_INFO REMOVE_PLAYER data list for " + target.getName() + " dataWritten=" + written);
        }
        return remove;
    }

    private boolean containsProfileData(PacketContainer packet) {
        try {
            if (packet.getPlayerInfoActions().size() > 0) {
                Set<EnumWrappers.PlayerInfoAction> actions = packet.getPlayerInfoActions().read(0);
                return actions != null && actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            }
        } catch (Throwable ignored) {
            // Fall back to the legacy single-action field below.
        }

        try {
            EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
            return action == EnumWrappers.PlayerInfoAction.ADD_PLAYER;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean writeAddActions(PacketContainer packet) {
        try {
            if (packet.getPlayerInfoActions().size() > 0) {
                packet.getPlayerInfoActions().write(0, EnumSet.of(
                        EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                        EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                        EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
                        EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
                        EnumWrappers.PlayerInfoAction.UPDATE_LISTED
                ));
                return true;
            }
        } catch (Throwable ignored) {
            // Fall back to legacy single-action packets.
        }

        try {
            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean writeRemoveAction(PacketContainer packet) {
        try {
            if (packet.getPlayerInfoActions().size() > 0) {
                packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }

        try {
            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private PacketContainer createNativeAddPacket(Player target) {
        try {
            Object handle = target.getClass().getMethod("getHandle").invoke(target);
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = Class.forName(packetClass.getName() + "$Action");
            Object addAction = enumValue(actionClass, "ADD_PLAYER");
            if (addAction == null) {
                return null;
            }

            Object singleActionPacket = createNativeSingleActionPacket(packetClass, actionClass, addAction, handle);
            if (singleActionPacket != null) {
                debug("Built native single-action player info packet for " + target.getName());
                return PacketContainer.fromPacket(singleActionPacket);
            }

            EnumSet<?> actions = createNativeActionSet(actionClass,
                    "ADD_PLAYER",
                    "UPDATE_DISPLAY_NAME",
                    "UPDATE_GAME_MODE",
                    "UPDATE_LATENCY",
                    "UPDATE_LISTED");
            if (actions == null) {
                return null;
            }

            for (java.lang.reflect.Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 2
                        && EnumSet.class.isAssignableFrom(parameters[0])
                        && Collection.class.isAssignableFrom(parameters[1])) {
                    return PacketContainer.fromPacket(constructor.newInstance(actions, Collections.singletonList(handle)));
                }
            }
        } catch (Throwable t) {
            debug("Native player info packet build failed for " + target.getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        }
        debug("No supported native player info packet constructor found for " + target.getName());
        return null;
    }

    private Object createNativeSingleActionPacket(Class<?> packetClass, Class<?> actionClass, Object action, Object handle) {
        for (java.lang.reflect.Constructor<?> constructor : packetClass.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 2 || !parameters[0].isAssignableFrom(actionClass)) {
                continue;
            }
            if (!parameters[1].isAssignableFrom(handle.getClass())) {
                continue;
            }
            try {
                return constructor.newInstance(action, handle);
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private EnumSet<?> createNativeActionSet(Class<?> actionClass, String... actionNames) {
        try {
            Class<? extends Enum> enumClass = actionClass.asSubclass(Enum.class);
            EnumSet actions = EnumSet.noneOf(enumClass);
            for (String actionName : actionNames) {
                Object action = enumValue(actionClass, actionName);
                if (action instanceof Enum<?> enumAction) {
                    actions.add(enumAction);
                }
            }
            return actions;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object enumValue(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean writePlayerInfoDataList(PacketContainer packet, List<PlayerInfoData> data) {
        if (packet.getPlayerInfoDataLists().size() <= 0) {
            return false;
        }

        try {
            List<PlayerInfoData> current = packet.getPlayerInfoDataLists().read(0);
            if (current != null) {
                if (current.size() == data.size()) {
                    for (int i = 0; i < data.size(); i++) {
                        current.set(i, data.get(i));
                    }
                    return true;
                }

                current.clear();
                current.addAll(data);
                return true;
            }
        } catch (Throwable ignored) {
            // Some mappings return immutable lists; try the ProtocolLib writer as a fallback.
        }

        try {
            packet.getPlayerInfoDataLists().write(0, data);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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

    private WrappedGameProfile getProfile(PlayerInfoData data) {
        if (data == null) {
            return null;
        }
        try {
            return data.getProfile();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private UUID getProfileId(PlayerInfoData data, WrappedGameProfile profile) {
        if (data == null) {
            return null;
        }
        try {
            UUID profileId = data.getProfileId();
            if (profileId != null) {
                return profileId;
            }
        } catch (Throwable ignored) {
            // Older ProtocolLib versions only expose the profile UUID.
        }
        return profile == null ? null : profile.getUUID();
    }

    private void captureOriginalProfile(Player player) {
        originalProfiles.computeIfAbsent(player.getUniqueId(), ignored -> {
            WrappedGameProfile profile = readCurrentProfile(player);
            debug("Captured original profile for " + player.getName()
                    + " profileFound=" + (profile != null)
                    + " textures=" + describeTextures(getTextures(profile)));
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

    private boolean applyProfileTextures(Player player, String profileName, TextureProperty textures) {
        if (applyPaperPlayerProfile(player, profileName, textures)) {
            return true;
        }

        Object gameProfile = getPlayerGameProfile(player);
        Object propertyMap = getGameProfileProperties(gameProfile);
        if (propertyMap == null) {
            debug("Live profile texture write skipped for " + player.getName() + ": no GameProfile property map");
            return replacePlayerGameProfile(player, profileName, textures);
        }

        boolean removed = removeNativeProperties(propertyMap, TEXTURES_PROPERTY);
        if (textures != null) {
            try {
                boolean inserted = putNativeProperty(propertyMap, TEXTURES_PROPERTY, createNativeTextureProperty(textures));
                debug("Live GameProfile texture mutation for " + player.getName()
                        + " removed=" + removed
                        + ", inserted=" + inserted
                        + ", propertyCount=" + countProperties(propertyMap, TEXTURES_PROPERTY)
                        + " " + describeTextures(textures));
                if (inserted) {
                    return true;
                }
            } catch (ReflectiveOperationException e) {
                // If Mojang authlib changes, the packet listener fallback can still try to carry the texture.
                debug("Live GameProfile texture write failed for " + player.getName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            return replacePlayerGameProfile(player, profileName, textures);
        } else {
            debug("Removed live GameProfile textures for " + player.getName()
                    + " propertyCount=" + countProperties(propertyMap, TEXTURES_PROPERTY));
            return removed;
        }
    }

    private boolean applyPaperPlayerProfile(Player player, String profileName, TextureProperty textures) {
        Object profile;
        try {
            profile = player.getClass().getMethod("getPlayerProfile").invoke(player);
        } catch (Throwable ignored) {
            debug("Paper/Purpur PlayerProfile API not available for " + player.getName());
            return false;
        }

        if (profile == null) {
            debug("Paper/Purpur PlayerProfile API returned null for " + player.getName());
            return false;
        }

        try {
            Object workingProfile = cloneObject(profile);
            if (workingProfile == null) {
                workingProfile = profile;
            }

            invokeMethod(workingProfile, "setName", profileName);
            if (textures == null) {
                invokeMethod(workingProfile, "removeProperty", TEXTURES_PROPERTY);
            } else {
                Object property = createPaperProfileProperty(textures);
                if (property == null) {
                    debug("Paper/Purpur ProfileProperty class not available for " + player.getName());
                    return false;
                }
                invokeMethod(workingProfile, "removeProperty", TEXTURES_PROPERTY);
                if (!invokeMethod(workingProfile, "setProperty", property)) {
                    debug("Paper/Purpur PlayerProfile#setProperty failed for " + player.getName());
                    return false;
                }
            }

            if (!invokePlayerProfileSetter(player, workingProfile)) {
                debug("Paper/Purpur Player#setPlayerProfile failed for " + player.getName());
                return false;
            }

            debug("Applied skin through Paper/Purpur PlayerProfile API for " + player.getName()
                    + " profileName='" + profileName + "' " + describeTextures(textures));
            return true;
        } catch (Throwable t) {
            debug("Paper/Purpur PlayerProfile API failed for " + player.getName() + ": "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private Object createPaperProfileProperty(TextureProperty textures) {
        try {
            Class<?> propertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            for (Constructor<?> constructor : propertyClass.getDeclaredConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 3
                        && parameters[0] == String.class
                        && parameters[1] == String.class
                        && parameters[2] == String.class) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(TEXTURES_PROPERTY, textures.value(), textures.signature());
                }
            }
        } catch (Throwable ignored) {
            // Paper profile API is optional.
        }
        return null;
    }

    private boolean invokePlayerProfileSetter(Player player, Object profile) {
        for (Method method : player.getClass().getMethods()) {
            if (!method.getName().equals("setPlayerProfile") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            if (!parameter.isAssignableFrom(profile.getClass())) {
                continue;
            }
            try {
                method.invoke(player, profile);
                return true;
            } catch (Throwable ignored) {
                // Try another overload.
            }
        }
        return false;
    }

    private Object cloneObject(Object target) {
        Object clone = invokeNoArg(target, "clone");
        return clone == null ? target : clone;
    }

    private boolean replacePlayerGameProfile(Player player, String profileName, TextureProperty textures) {
        Object handle;
        try {
            handle = player.getClass().getMethod("getHandle").invoke(player);
        } catch (Throwable t) {
            debug("NMS GameProfile replacement skipped for " + player.getName() + ": no handle - "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }

        Object replacement = createNativeGameProfile(player.getUniqueId(), profileName, textures);
        if (replacement == null) {
            debug("NMS GameProfile replacement skipped for " + player.getName() + ": replacement profile could not be built");
            return false;
        }

        Class<?> replacementClass = replacement.getClass();
        for (Class<?> type = handle.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().isAssignableFrom(replacementClass)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    field.set(handle, replacement);
                    debug("Replaced NMS GameProfile field '" + field.getName() + "' on "
                            + handle.getClass().getName() + " for " + player.getName());
                    return true;
                } catch (Throwable t) {
                    debug("Could not replace NMS GameProfile field '" + field.getName() + "' on "
                            + handle.getClass().getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        }

        debug("No replaceable NMS GameProfile field found for " + player.getName()
                + " handleClass=" + handle.getClass().getName());
        return false;
    }

    private Object createNativeGameProfile(UUID profileId, String profileName, TextureProperty textures) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(profileId, profileName);
            Object propertyMap = getGameProfileProperties(gameProfile);
            if (textures != null) {
                boolean inserted = putNativeProperty(propertyMap, TEXTURES_PROPERTY, createNativeTextureProperty(textures));
                debug("Built replacement GameProfile profileName='" + profileName + "' inserted=" + inserted
                        + ", propertyCount=" + countProperties(propertyMap, TEXTURES_PROPERTY));
            }
            return gameProfile;
        } catch (Throwable t) {
            debug("Could not build replacement GameProfile profileName='" + profileName + "': "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        }
    }

    private Object getPlayerGameProfile(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object profile = handle.getClass().getMethod("getGameProfile").invoke(handle);
            if (getGameProfileProperties(profile) != null) {
                debug("Resolved GameProfile for " + player.getName() + " through NMS handle");
                return profile;
            }
            debug("NMS GameProfile for " + player.getName() + " did not expose a property map; trying Bukkit getProfile()");
        } catch (Throwable t) {
            debug("Could not resolve NMS GameProfile for " + player.getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        try {
            Object profile = player.getClass().getMethod("getProfile").invoke(player);
            if (getGameProfileProperties(profile) != null) {
                debug("Resolved GameProfile for " + player.getName() + " through Bukkit getProfile()");
                return profile;
            }
            debug("Bukkit getProfile() for " + player.getName() + " did not expose Mojang properties");
        } catch (Throwable ignored) {
            debug("Could not resolve Bukkit profile for " + player.getName() + ": " + ignored.getClass().getSimpleName() + ": " + ignored.getMessage());
        }
        return null;
    }

    private WrappedGameProfile createProfile(UUID profileId, String profileName, TextureProperty textures,
                                             WrappedGameProfile baseProfile) {
        WrappedGameProfile reflectedProfile = createProfileReflectively(profileId, profileName, textures, baseProfile);
        if (reflectedProfile != null) {
            debug("Created reflected WrappedGameProfile profileName='" + profileName + "' textures=" + describeTextures(textures));
            return reflectedProfile;
        }

        WrappedGameProfile profile = new WrappedGameProfile(profileId, profileName);
        if (textures != null) {
            try {
                profile.getProperties().put(TEXTURES_PROPERTY, new WrappedSignedProperty(TEXTURES_PROPERTY, textures.value(), textures.signature()));
                debug("Created ProtocolLib WrappedGameProfile profileName='" + profileName + "' textures=" + describeTextures(textures));
            } catch (Throwable t) {
                // Some ProtocolLib builds cannot access GameProfile properties directly. The fallback still updates the profile name.
                debug("ProtocolLib WrappedGameProfile texture write failed profileName='" + profileName + "': " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        } else {
            debug("Created WrappedGameProfile profileName='" + profileName + "' without textures");
        }
        return profile;
    }

    private WrappedGameProfile cloneProfile(WrappedGameProfile source, UUID profileId, String fallbackName) {
        String name = source == null || source.getName() == null ? fallbackName : source.getName();
        WrappedGameProfile copy = new WrappedGameProfile(profileId, name);
        TextureProperty textures = getTextures(source);
        if (textures != null) {
            return createProfile(profileId, name, textures, null);
        }
        return copy;
    }

    private TextureProperty getTextures(WrappedGameProfile profile) {
        if (profile == null) {
            return null;
        }

        Object propertyMap = getNativeProperties(profile);
        if (propertyMap != null) {
            for (Object property : getNativePropertyValues(propertyMap)) {
                if (property == null || !TEXTURES_PROPERTY.equalsIgnoreCase(getNativePropertyName(property))) {
                    continue;
                }
                String value = getNativePropertyValue(property);
                String signature = getNativePropertySignature(property);
                if (value != null) {
                    return new TextureProperty(value, signature);
                }
            }
        }

        try {
            for (WrappedSignedProperty prop : profile.getProperties().get(TEXTURES_PROPERTY)) {
                if (prop != null) {
                    return new TextureProperty(prop.getValue(), prop.getSignature());
                }
            }
        } catch (Throwable ignored) {
            // Direct ProtocolLib property access is not available on every 1.21 mapping.
        }
        return null;
    }

    private WrappedGameProfile createProfileReflectively(UUID profileId, String profileName, TextureProperty textures,
                                                        WrappedGameProfile baseProfile) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(profileId, profileName);
            Object propertyMap = getGameProfileProperties(gameProfile);
            copyNativeProperties(baseProfile, propertyMap);
            if (textures != null) {
                boolean inserted = putNativeProperty(propertyMap, TEXTURES_PROPERTY, createNativeTextureProperty(textures));
                debug("Reflected GameProfile texture mutation profileName='" + profileName
                        + "' inserted=" + inserted
                        + ", propertyCount=" + countProperties(propertyMap, TEXTURES_PROPERTY));
            }
            return WrappedGameProfile.fromHandle(gameProfile);
        } catch (Throwable t) {
            debug("Reflective GameProfile creation failed profileName='" + profileName + "': " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        }
    }

    private Object createNativeTextureProperty(TextureProperty textures) throws ReflectiveOperationException {
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Object property = instantiateNativeTextureProperty(propertyClass, textures);
        if (property != null) {
            return property;
        }
        throw new ReflectiveOperationException("No supported Property constructor found");
    }

    private Object instantiateNativeTextureProperty(Class<?> propertyClass, TextureProperty textures) {
        for (Constructor<?> constructor : propertyClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            try {
                constructor.setAccessible(true);
                if (parameters.length == 2
                        && parameters[0] == String.class
                        && parameters[1] == String.class
                        && (textures.signature() == null || textures.signature().isBlank())) {
                    return constructor.newInstance(TEXTURES_PROPERTY, textures.value());
                }
                if (parameters.length == 3
                        && parameters[0] == String.class
                        && parameters[1] == String.class
                        && parameters[2] == String.class) {
                    return constructor.newInstance(TEXTURES_PROPERTY, textures.value(), textures.signature());
                }
            } catch (Throwable ignored) {
                // Try the next constructor.
            }
        }
        return null;
    }

    private void copyNativeProperties(WrappedGameProfile source, Object targetPropertyMap) {
        Object sourcePropertyMap = getNativeProperties(source);
        if (sourcePropertyMap == null || targetPropertyMap == null) {
            return;
        }

        for (Object property : getNativePropertyValues(sourcePropertyMap)) {
            String name = getNativePropertyName(property);
            if (name == null || TEXTURES_PROPERTY.equalsIgnoreCase(name)) {
                continue;
            }
            putNativeProperty(targetPropertyMap, name, property);
        }
    }

    private Object getNativeProperties(WrappedGameProfile profile) {
        if (profile == null || profile.getHandle() == null) {
            return null;
        }
        return getGameProfileProperties(profile.getHandle());
    }

    private Object getGameProfileProperties(Object gameProfile) {
        if (gameProfile == null) {
            return null;
        }

        Object properties = invokeNoArg(gameProfile, "getProperties", "properties");
        if (properties != null) {
            debug("Resolved property map from " + gameProfile.getClass().getName() + " using accessor");
            return properties;
        }

        for (Class<?> type = gameProfile.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!looksLikePropertyMapField(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(gameProfile);
                    if (value != null) {
                        debug("Resolved property map from " + gameProfile.getClass().getName() + " field '" + field.getName() + "'");
                        return value;
                    }
                } catch (Throwable ignored) {
                    // Try the next field.
                }
            }
        }
        debug("Could not find property map on " + gameProfile.getClass().getName());
        return null;
    }

    private Collection<?> getNativePropertyValues(Object propertyMap) {
        if (propertyMap == null) {
            return Collections.emptyList();
        }
        Object values = invokeNoArg(propertyMap, "values");
        if (values instanceof Collection<?> collection) {
            return collection;
        }
        Object entries = invokeNoArg(propertyMap, "entries");
        if (entries instanceof Collection<?> collection) {
            List<Object> collected = new ArrayList<>();
            for (Object entry : collection) {
                Object value = invokeNoArg(entry, "getValue");
                if (value != null) {
                    collected.add(value);
                }
            }
            return collected;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean putNativeProperty(Object propertyMap, String name, Object property) {
        if (propertyMap == null || name == null || property == null) {
            return false;
        }
        if (propertyMap instanceof Multimap multimap) {
            try {
                boolean inserted = multimap.put(name, property);
                debug("Inserted native property '" + name + "' through Guava Multimap on " + propertyMap.getClass().getName()
                        + " result=" + inserted);
                return inserted;
            } catch (UnsupportedOperationException e) {
                debug("Guava Multimap insert is unsupported on " + propertyMap.getClass().getName() + "; trying reflective methods");
            }
        }
        if (putIntoBackingPropertyMap(propertyMap, name, property)) {
            return true;
        }
        if (invokeMethod(propertyMap, "put", name, property)) {
            debug("Inserted native property '" + name + "' through put on " + propertyMap.getClass().getName());
            return true;
        }
        if (invokeMethod(propertyMap, "putAll", name, Collections.singleton(property))) {
            debug("Inserted native property '" + name + "' through putAll on " + propertyMap.getClass().getName());
            return true;
        }
        debug("Could not insert native property '" + name + "' into " + propertyMap.getClass().getName());
        return false;
    }

    @SuppressWarnings("rawtypes")
    private boolean removeNativeProperties(Object propertyMap, String name) {
        if (propertyMap == null || name == null) {
            return false;
        }
        if (propertyMap instanceof Multimap multimap) {
            try {
                int removed = multimap.removeAll(name).size();
                debug("Removed native properties '" + name + "' through Guava Multimap on " + propertyMap.getClass().getName()
                        + " count=" + removed);
                return true;
            } catch (UnsupportedOperationException e) {
                debug("Guava Multimap removeAll is unsupported on " + propertyMap.getClass().getName() + "; trying reflective methods");
            }
        }
        if (removeFromBackingPropertyMap(propertyMap, name)) {
            return true;
        }
        if (invokeMethod(propertyMap, "removeAll", name)) {
            debug("Removed native properties '" + name + "' through removeAll on " + propertyMap.getClass().getName());
            return true;
        }
        if (invokeMethod(propertyMap, "remove", name)) {
            debug("Removed native properties '" + name + "' through remove on " + propertyMap.getClass().getName());
            return true;
        }
        debug("Could not remove native properties '" + name + "' from " + propertyMap.getClass().getName());
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean putIntoBackingPropertyMap(Object propertyMap, String name, Object property) {
        Object backing = getBackingPropertyMap(propertyMap);
        if (backing instanceof Multimap multimap) {
            try {
                boolean inserted = multimap.put(name, property);
                debug("Inserted native property '" + name + "' through backing Multimap on "
                        + propertyMap.getClass().getName() + " result=" + inserted);
                return inserted;
            } catch (Throwable t) {
                debug("Backing Multimap insert failed on " + propertyMap.getClass().getName()
                        + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    private boolean removeFromBackingPropertyMap(Object propertyMap, String name) {
        Object backing = getBackingPropertyMap(propertyMap);
        if (backing instanceof Multimap multimap) {
            try {
                int removed = multimap.removeAll(name).size();
                debug("Removed native properties '" + name + "' through backing Multimap on "
                        + propertyMap.getClass().getName() + " count=" + removed);
                return true;
            } catch (Throwable t) {
                debug("Backing Multimap remove failed on " + propertyMap.getClass().getName()
                        + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
        return false;
    }

    private Object getBackingPropertyMap(Object propertyMap) {
        if (propertyMap == null) {
            return null;
        }
        for (Class<?> type = propertyMap.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Multimap.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object backing = field.get(propertyMap);
                    if (backing != null && backing != propertyMap) {
                        debug("Resolved backing Multimap field '" + field.getName() + "' on "
                                + propertyMap.getClass().getName() + " -> " + backing.getClass().getName());
                        return backing;
                    }
                } catch (Throwable t) {
                    debug("Could not access backing Multimap field '" + field.getName() + "' on "
                            + propertyMap.getClass().getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        }
        return null;
    }

    private String getNativePropertyName(Object property) {
        return invokeStringMethod(property, "name", "getName");
    }

    private String getNativePropertyValue(Object property) {
        return invokeStringMethod(property, "value", "getValue");
    }

    private String getNativePropertySignature(Object property) {
        return invokeStringMethod(property, "signature", "getSignature");
    }

    private String invokeStringMethod(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String methodName : methodNames) {
            Object value = invokeNoArg(target, methodName);
            if (value instanceof String text) {
                return text;
            }
        }
        return null;
    }

    private Object invokeNoArg(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String methodName : methodNames) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (Throwable ignored) {
                    // Try declared methods below.
                }
            }
            for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                        continue;
                    }
                    try {
                        method.setAccessible(true);
                        return method.invoke(target);
                    } catch (Throwable ignored) {
                        // Try the next method.
                    }
                }
            }
        }
        return null;
    }

    private boolean invokeMethod(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(target, args);
                return true;
            } catch (Throwable ignored) {
                // Try declared methods below.
            }
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(target, args);
                    return true;
                } catch (Throwable ignored) {
                    // Try the next overload.
                }
            }
        }
        return false;
    }

    private boolean looksLikePropertyMapField(Field field) {
        String fieldName = field.getName().toLowerCase(Locale.ROOT);
        String typeName = field.getType().getName().toLowerCase(Locale.ROOT);
        return fieldName.contains("properties")
                || fieldName.contains("property")
                || typeName.contains("propertymap");
    }

    private void sendRemovePacket(Player target, PacketContainer packet) {
        int sent = 0;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                sendPacket(viewer, packet);
                sent++;
            }
        }
        debug("Sent remove packet for " + target.getName() + " to " + sent + " viewers");
        if (sent == 0) {
            debug("No external viewers online for " + target.getName() + "; the target player may need to relog to see their own skin.");
        }
    }

    private void sendAddPacket(Player target, PacketContainer packet) {
        int sent = 0;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target) && viewer.canSee(target)) {
                sendPacket(viewer, packet);
                sent++;
            }
        }
        debug("Sent add packet for " + target.getName() + " to " + sent + " viewers");
        if (sent == 0) {
            debug("No external viewers received the skin refresh for " + target.getName() + ".");
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
        debug("Entity hide/show refresh for " + target.getName() + " viewers=" + visibleViewers.size());

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

    private void debug(String message) {
        if (isSkinDebug()) {
            plugin.getLogger().info("[SkinDebug] " + message);
        }
    }

    private boolean isSkinDebug() {
        if (plugin instanceof JavaPlugin javaPlugin) {
            return javaPlugin.getConfig().getBoolean("skinDebug", false)
                    || javaPlugin.getConfig().getBoolean("debug", false);
        }
        return false;
    }

    private String describeTextures(TextureProperty textures) {
        if (textures == null) {
            return "textures=null";
        }
        return "valueLength=" + length(textures.value()) + ", signatureLength=" + length(textures.signature());
    }

    private int length(String value) {
        return value == null ? -1 : value.length();
    }

    private String shortId(UUID uuid) {
        if (uuid == null) {
            return "null";
        }
        String text = uuid.toString();
        return text.length() <= 8 ? text : text.substring(0, 8);
    }

    private int countProperties(Object propertyMap, String name) {
        if (propertyMap == null || name == null) {
            return -1;
        }
        int count = 0;
        for (Object property : getNativePropertyValues(propertyMap)) {
            if (name.equalsIgnoreCase(getNativePropertyName(property))) {
                count++;
            }
        }
        return count;
    }

    private record TextureProperty(String value, String signature) {
    }

    private record ProfileOverride(String profileName, TextureProperty textures) {
    }
}
