package ch.framedev.essentialsmini.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NameTagChanger {

    public static Map<String,String> getSkin(String skinName) {
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
            e.printStackTrace();
        }
        return Map.of("value", value, "signature", signature);
    }

    public static String getResponse(String _url) {
        try {
            URL url = new URL(_url);
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            return IOUtils.toString(in, encoding);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void changeNameAndSkin(Player target, String newName, String value, String signature) {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        UUID uuid = target.getUniqueId();
        int entityId = target.getEntityId();

        try {
            // Step 1: Remove from tab list
            PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            remove.getUUIDLists().write(0, List.of(uuid));
            sendToAllExcept(target, remove);

            // Step 2: Build spoofed profile
            WrappedGameProfile profile = new WrappedGameProfile(uuid, newName);
            profile.getProperties().put("textures", new WrappedSignedProperty("textures", value, signature));

            // Step 3: Add back to tab list
            PlayerInfoData infoData = new PlayerInfoData(
                    profile,
                    0,
                    NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(newName)
            );

            PacketContainer add = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
            add.getPlayerInfoActions().write(0, Set.of(PlayerInfoAction.ADD_PLAYER));
            add.getPlayerInfoDataLists().write(1, List.of(infoData));
            sendToAllExcept(target, add);

            // Step 4: Destroy entity
            PacketContainer destroy = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntegerArrays().write(0, new int[]{entityId});
            sendToAllExcept(target, destroy);

            // Step 5: Respawn entity
            PacketContainer spawn = pm.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            spawn.getIntegers().write(0, entityId);
            spawn.getUUIDs().write(0, uuid);
            spawn.getDoubles().write(0, target.getLocation().getX());
            spawn.getDoubles().write(1, target.getLocation().getY());
            spawn.getDoubles().write(2, target.getLocation().getZ());
            spawn.getBytes().write(0, (byte) (target.getLocation().getYaw() * 256 / 360));
            spawn.getBytes().write(1, (byte) (target.getLocation().getPitch() * 256 / 360));
            spawn.getDataWatcherModifier().write(0, WrappedDataWatcher.getEntityWatcher(target));

            sendToAllExcept(target, spawn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendToAllExcept(Player excluded, PacketContainer packet) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(excluded)) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(online, packet);
            }
        }
    }
}