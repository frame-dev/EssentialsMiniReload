package ch.framedev.essentialsmini.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class SkinChanger {

    public record Textures(String value, String signature) {}

    // From a Minecraft username -> resolve UUID -> get textures
    public static CompletableFuture<Textures> fetchByUsername(Plugin plugin, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                debug(plugin, "Starting Mojang lookup for username='" + username + "'");

                // 1) Get UUID from Mojang API
                URL u1 = new URL("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(username, StandardCharsets.UTF_8));
                HttpURLConnection c1 = (HttpURLConnection) u1.openConnection();
                c1.setConnectTimeout(5000);
                c1.setReadTimeout(5000);
                int profileResponse = c1.getResponseCode();
                debug(plugin, "Mojang profile response for '" + username + "': HTTP " + profileResponse);
                if (profileResponse != 200) throw new RuntimeException("No such user");
                JsonObject j1 = JsonParser.parseReader(new InputStreamReader(c1.getInputStream())).getAsJsonObject();
                String id = j1.get("id").getAsString(); // no dashes
                debug(plugin, "Resolved '" + username + "' to Mojang UUID " + id);

                // 2) Session server profile (signed texture property)
                URL u2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
                HttpURLConnection c2 = (HttpURLConnection) u2.openConnection();
                c2.setConnectTimeout(5000);
                c2.setReadTimeout(5000);
                int sessionResponse = c2.getResponseCode();
                debug(plugin, "Mojang session response for '" + username + "': HTTP " + sessionResponse);
                if (sessionResponse != 200) throw new RuntimeException("Session profile fetch failed");
                JsonObject j2 = JsonParser.parseReader(new InputStreamReader(c2.getInputStream())).getAsJsonObject();
                var props = j2.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = props.get("value").getAsString();
                String sig   = props.get("signature").getAsString();
                debug(plugin, "Fetched signed textures for '" + username + "' valueLength=" + value.length() + ", signatureLength=" + sig.length());
                return new Textures(value, sig);
            } catch (Exception e) {
                debug(plugin, "Mojang lookup failed for '" + username + "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    private static void debug(Plugin plugin, String message) {
        if (!isSkinDebug(plugin)) {
            return;
        }
        plugin.getLogger().info("[SkinDebug] " + message);
    }

    private static boolean isSkinDebug(Plugin plugin) {
        if (plugin instanceof JavaPlugin javaPlugin) {
            return javaPlugin.getConfig().getBoolean("skinDebug", false)
                    || javaPlugin.getConfig().getBoolean("debug", false);
        }
        return false;
    }
}
