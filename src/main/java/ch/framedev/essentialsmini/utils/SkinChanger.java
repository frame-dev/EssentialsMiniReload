package ch.framedev.essentialsmini.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class SkinChanger {

    public record Textures(String value, String signature) {}

    // From a Minecraft username -> resolve UUID -> get textures
    public static CompletableFuture<Textures> fetchByUsername(Plugin plugin, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1) Get UUID from Mojang API
                URL u1 = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                HttpURLConnection c1 = (HttpURLConnection) u1.openConnection();
                c1.setConnectTimeout(5000);
                c1.setReadTimeout(5000);
                if (c1.getResponseCode() != 200) throw new RuntimeException("No such user");
                JsonObject j1 = JsonParser.parseReader(new InputStreamReader(c1.getInputStream())).getAsJsonObject();
                String id = j1.get("id").getAsString(); // no dashes

                // 2) Session server profile (signed texture property)
                URL u2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
                HttpURLConnection c2 = (HttpURLConnection) u2.openConnection();
                c2.setConnectTimeout(5000);
                c2.setReadTimeout(5000);
                if (c2.getResponseCode() != 200) throw new RuntimeException("Session profile fetch failed");
                JsonObject j2 = JsonParser.parseReader(new InputStreamReader(c2.getInputStream())).getAsJsonObject();
                var props = j2.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = props.get("value").getAsString();
                String sig   = props.get("signature").getAsString();
                return new Textures(value, sig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }
}