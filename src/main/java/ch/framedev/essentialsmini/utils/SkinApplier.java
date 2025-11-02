package ch.framedev.essentialsmini.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class SkinApplier {

    public static void applyAndRefresh(Plugin plugin, Player player, SkinChanger.Textures textures) {
        try {
            // Get CraftPlayer#getProfile() without version string
            Method getProfile = player.getClass().getMethod("getProfile");
            GameProfile profile = (GameProfile) getProfile.invoke(player);

            // Replace textures property
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", textures.value(), textures.signature()));

            // Refresh: simple & reliable — hide/show to every viewer incl. self
            Bukkit.getOnlinePlayers().forEach(viewer -> {
                if (viewer.equals(player)) return; // handle others first
                viewer.hidePlayer(plugin, player);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getOnlinePlayers().forEach(viewer -> {
                    if (viewer.equals(player)) return;
                    viewer.showPlayer(plugin, player);
                });

                // Nudge self view too (briefly hide/show self)
                player.hidePlayer(plugin, player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.showPlayer(plugin, player), 2L);
            }, 2L);

        } catch (Exception e) {
            throw new RuntimeException("Failed to apply skin", e);
        }
    }
}