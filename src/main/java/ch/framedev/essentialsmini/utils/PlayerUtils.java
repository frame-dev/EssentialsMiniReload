package ch.framedev.essentialsmini.utils;



/*
 * ch.framedev.essentialsmini.utils
 * =============================================
 * This File was Created by FrameDev.
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 03.01.2025 20:35
 */

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PlayerUtils {

    @SuppressWarnings("deprecation")
    // TODO: Require Testing with Geyser
    public static OfflinePlayer getOfflinePlayerByName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty!");
        }

        OfflinePlayer player = null;

        if (Bukkit.getOnlineMode()) {
            if(GeyserManager.isGeyserInstalled()) {
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                        player = offlinePlayer;
                        break;
                    }
                }
            } else {
                UUID uuid = UUIDFetcher.getUUID(playerName);
                if (uuid != null) {
                    player = Bukkit.getOfflinePlayer(uuid);
                }
            }
        }

        // Fallback to name-based lookup in case UUID fetch fails
        if (player == null) {
            player = Bukkit.getOfflinePlayer(playerName);
        }

        return player;
    }
}
