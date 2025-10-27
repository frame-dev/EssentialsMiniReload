package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

/*
 * Runtime-safe NameTagChanger:
 * - Does not depend on ProtocolLib internals
 * - Sets Bukkit display/list names
 * - Forces a refresh for viewers via hide/show (tries both modern and legacy API)
 * - Provides a simple getSkin(...) helper returning texture value/signature
 */
public final class NameTagChanger {

    private NameTagChanger() { /* utility */ }

    /**
     * Change displayed name and (optionally) update skin data via SkinChanger.
     * value/signature may be null; SkinChanger handles skin changes if desired.
     */
    public static void changeNameAndSkin(Player target, String newName, String value, String signature) {
        if (target == null || newName == null) return;

        try {
            target.setDisplayName(newName);
        } catch (Throwable ignored) {
        }

        try {
            target.setCustomName(newName);
        } catch (Throwable ignored) {
        }

        try {
            target.setPlayerListName(truncate(newName, 16));
        } catch (Throwable ignored) {
        }

        // Refresh for all viewers to update nametag/skin without ProtocolLib
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            try {
                // Modern API (requires plugin instance)
                try {
                    viewer.hidePlayer(Main.getInstance(), target);
                    viewer.showPlayer(Main.getInstance(), target);
                } catch (NoSuchMethodError | NoClassDefFoundError e) {
                    // Legacy API fallback
                    viewer.hidePlayer(target);
                    viewer.showPlayer(target);
                }
            } catch (Throwable t) {
                Main.getInstance().getLogger4J().warn("Failed to refresh visibility for viewer " + viewer.getName() + ": " + t.getMessage());
            }
        }

        // If texture data provided, attempt to apply via SkinChanger (safer reflection-based implementation)
        if (value != null && signature != null && !value.isBlank() && !signature.isBlank()) {
            try {
                new SkinChanger().changeSkin(target, null); // SkinChanger will fetch if null or skip; keep call for compatibility
            } catch (Throwable t) {
                Main.getInstance().getLogger4J().error("Failed to apply skin via SkinChanger for " + target.getName(), t);
            }
        }
    }

    /**
     * Fetches skin texture data (value + signature) from Mojang session server.
     * Returns a map with keys "value" and "signature" (empty strings on failure).
     */
    public static Map<String, String> getSkin(String skinName) {
        Map<String, String> result = new HashMap<>();
        result.put("value", "");
        result.put("signature", "");
        if (skinName == null || skinName.isBlank()) return result;

        try {
            SkinChanger sc = new SkinChanger();
            JSONParser parser = new JSONParser();

            String prof = sc.getResponse("https://api.mojang.com/users/profiles/minecraft/" + skinName);
            if (prof == null) return result;
            JSONObject json = (JSONObject) parser.parse(prof);
            String uuid = (String) json.get("id");
            if (uuid == null) return result;

            String session = sc.getResponse("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            if (session == null) return result;
            JSONObject json2 = (JSONObject) parser.parse(session);
            JSONArray props = (JSONArray) json2.get("properties");
            if (props == null || props.isEmpty()) return result;
            JSONObject prop = (JSONObject) props.get(0);

            result.put("value", (String) prop.getOrDefault("value", ""));
            result.put("signature", (String) prop.getOrDefault("signature", ""));
        } catch (ParseException e) {
            Main.getInstance().getLogger4J().error("Failed to parse skin data for " + skinName, e);
        } catch (Throwable t) {
            Main.getInstance().getLogger4J().error("Failed to fetch skin data for " + skinName, t);
        }
        return result;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}