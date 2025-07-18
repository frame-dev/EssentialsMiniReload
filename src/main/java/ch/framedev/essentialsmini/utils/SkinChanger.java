package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.main.Main;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Stream;

public class SkinChanger {

    public void changeSkin(Player player, String skinName) {
        String value = null;
        String signature = null;

        try {
            JSONParser parser = new JSONParser();

            Object obj = parser.parse(getResponse("https://api.mojang.com/users/profiles/minecraft/" + skinName));
            JSONObject json = (JSONObject) obj;
            String uuid = (String) json.get("id");

            Object obj2 = parser.parse(getResponse("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false"));
            JSONObject json2 = (JSONObject) obj2;
            JSONObject propsObj = (JSONObject) ((JSONArray) json2.get("properties")).get(0);
            value = (String) propsObj.get("value");
            signature = (String) propsObj.get("signature");
        } catch (ParseException e) {
            Main.getInstance().getLogger4J().error("Failed to parse skin data for " + skinName, e);
        }

        if (value == null || signature == null) {
            Bukkit.getLogger().warning("Could not fetch skin data for " + skinName);
            return;
        }

        try {
            Object handle = Reflection.getHandle(player);
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");

            Field profileField = Reflection.findFirstFieldOfType(handle.getClass(), gameProfileClass);
            Object profile = Reflection.getField(profileField, handle);

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Class<?> propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");

            Object propertyMap = Reflection.callMethod(
                    Reflection.makeMethod(gameProfileClass, "getProperties"), profile);

            Method removeMethod = Reflection.findMethod(propertyMapClass, "removeAll", Object.class);
            if (removeMethod == null) throw new RuntimeException("Could not find remove() method in PropertyMap");
            Reflection.callMethod(removeMethod, propertyMap, "textures");

            Object newProperty = Reflection.callConstructor(
                    Reflection.makeConstructor(propertyClass, String.class, String.class, String.class),
                    "textures", value, signature);

            Reflection.callMethod(
                    Reflection.makeMethod(propertyMapClass, "put", Object.class, Object.class),
                    propertyMap, "textures", newProperty);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                sendPackets(player, online);
            }
        } catch (Exception e) {
            Main.getInstance().getLogger4J().error("Failed to change skin for " + player.getName(), e);
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void sendPackets(Player target, Player viewer) throws Exception {
        Object handle = Reflection.getHandle(target);

        Class<?> playerInfoPacketClass = Reflection.getAnyNmsClass("PacketPlayOutPlayerInfo");
        boolean modern = false;

        if (playerInfoPacketClass == null) {
            playerInfoPacketClass = Reflection.getAnyNmsClass("ClientboundPlayerInfoUpdatePacket");
            modern = true;
        }

        if (playerInfoPacketClass == null) {
            throw new RuntimeException("Could not resolve player info packet class");
        }

        Class<?> enumClass = Stream.of(playerInfoPacketClass.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equalsIgnoreCase("EnumPlayerInfoAction") ||
                             c.getSimpleName().equalsIgnoreCase("Action"))
                .findFirst().orElseThrow(() -> new RuntimeException("Enum class not found in packet"));

        Enum<?> addAction = (Enum<?>) Stream.of(enumClass.getEnumConstants())
                .filter(e -> e.toString().equalsIgnoreCase("ADD_PLAYER"))
                .findFirst().orElseThrow(() -> new RuntimeException("ADD_PLAYER not found"));

        Enum<?> removeAction = (Enum<?>) Stream.of(enumClass.getEnumConstants())
                .filter(e -> e.toString().equalsIgnoreCase("REMOVE_PLAYER") ||
                             e.toString().equalsIgnoreCase("UPDATE_LISTED"))
                .findFirst().get();

        Object removePacket = null, addPacket = null;

        if (!modern) {
            // Legacy version: PacketPlayOutPlayerInfo(EnumPlayerInfoAction, EntityPlayer)
            Constructor<?> constructor = Reflection.makeConstructor(playerInfoPacketClass, enumClass, Reflection.getAnyNmsClass("EntityPlayer"));
            removePacket = Reflection.callConstructor(constructor, removeAction, handle);
            addPacket = Reflection.callConstructor(constructor, addAction, handle);
        } else {
            Bukkit.getLogger().warning("PlayerInfo packets are unsupported on 1.20.2+ via reflection. Skipping.");
        }

        Object destroyPacket;

        Class<?> destroyPacketClass = null;

// Try modern class first (1.20.2+)
        try {
            destroyPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
        } catch (ClassNotFoundException ignored) {}

// Try legacy fallback (1.8 - 1.19)
        if (destroyPacketClass == null) {
            try {
                String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                destroyPacketClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityDestroy");
            } catch (ClassNotFoundException ignored) {}
        }

        if (destroyPacketClass == null) {
            throw new RuntimeException("Destroy packet class not found.");
        }

// Find matching constructor with int[] or varargs
        Constructor<?> destroyConstructor = getConstructor(destroyPacketClass);

// Create the packet with the entity ID
        destroyPacket = destroyConstructor.newInstance((Object) new int[]{target.getEntityId()});

        Class<?> spawnPacketClass = Reflection.getAnyNmsClass("PacketPlayOutNamedEntitySpawn");
        if (spawnPacketClass == null) {
            spawnPacketClass = Reflection.getAnyNmsClass("ClientboundAddPlayerPacket");
        }
        if (spawnPacketClass == null) {
            Bukkit.getLogger().warning("Spawn packet is not available in this version. Using destroy-only fallback.");
        }
        Object spawnPacket = null;

        if (spawnPacketClass != null) {
            Constructor<?> spawnConstructor = Reflection.makeConstructor(spawnPacketClass, handle.getClass());
            if (spawnConstructor != null) {
                spawnPacket = Reflection.callConstructor(spawnConstructor, handle);
            }
        }

        if (spawnPacket != null) sendPacket(viewer, spawnPacket);

        if (removePacket != null) sendPacket(viewer, removePacket);
        if (addPacket != null) sendPacket(viewer, addPacket);
        sendPacket(viewer, destroyPacket);
    }

    private static @NotNull Constructor<?> getConstructor(Class<?> destroyPacketClass) {
        Constructor<?> destroyConstructor = null;
        for (Constructor<?> ctor : destroyPacketClass.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && params[0].isArray() && params[0].getComponentType() == int.class) {
                destroyConstructor = ctor;
                break;
            }
        }

        if (destroyConstructor == null) {
            throw new RuntimeException("Destroy packet constructor not found.");
        }
        return destroyConstructor;
    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object handle = Reflection.getHandle(player);
            Object connection = null;

// Scan fields and pick the one that's a packet listener / connection
            for (Field field : handle.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(handle);
                    if (value != null && (value.getClass().getName().toLowerCase().contains("connection") || value.getClass().getSimpleName().toLowerCase().contains("listenerimpl"))) {
                        connection = value;
                        break;
                    }
                } catch (IllegalAccessException ignored) {}
            }

            if (connection == null) {
                throw new RuntimeException("Could not find connection field in ServerPlayer (1.20.4+).");
            }

// Find and call sendPacket method
            Class<?> packetClass = Reflection.getAnyNmsClass("Packet");
            Method sendPacket = Reflection.makeMethod(connection.getClass(), "sendPacket", packetClass);

            if (sendPacket == null) {
                throw new RuntimeException("Could not find sendPacket method in connection class: " + connection.getClass().getName());
            }

            Reflection.callMethod(sendPacket, connection, packet);
        } catch (Exception e) {
            Main.getInstance().getLogger4J().error("Failed to send packet to " + player.getName(), e);
            throw new RuntimeException("Failed to send packet to player: " + player.getName(), e);
        }
    }

    @SuppressWarnings("unused")
    public void debugSpawnPacketClasses() {
        String[] names = {
                "PacketPlayOutNamedEntitySpawn",
                "ClientboundAddPlayerPacket",
                "ClientboundPlayerInfoUpdatePacket",
                "ClientboundPlayerInfoRemovePacket",
                "ClientboundPlayerInfoPacket"
        };
        for (String name : names) {
            Class<?> cls = Reflection.getAnyNmsClass(name);
            Bukkit.getLogger().info("[Debug] Class lookup for " + name + ": " + (cls != null ? "FOUND" : "NOT FOUND"));
        }
    }

    public String getResponse(String _url) {
        try {
            URL url = new URL(_url);
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            return IOUtils.toString(in, encoding);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error("Failed to fetch data from " + _url, e);
        }
        return null;
    }
}