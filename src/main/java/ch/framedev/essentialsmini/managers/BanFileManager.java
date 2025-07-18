package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BanFileManager {

    public static final File file = new File(Main.getInstance().getDataFolder(), "Banned.yml");
    public static final FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

    public static void saveCfg() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    public static boolean isBanned(String playerName) {
        return cfg.getBoolean("Ban." + playerName + ".isBanned");
    }

    public static String getBannedReason(String playerName) {
        if (cfg.getBoolean("Ban." + playerName + ".isBanned")) {
            return cfg.getString("Ban." + playerName + ".reason");
        }
        return "";
    }

    public static void banPlayer(String playerName, String reason) {
        if (cfg.getBoolean("Ban." + playerName + ".isBanned")) {
            Bukkit.getConsoleSender().sendMessage(playerName + " ist schon gebannt!");
        } else {
            cfg.set("Ban." + playerName + ".isBanned", true);
            cfg.set("Ban." + playerName + ".reason", reason);
            saveCfg();
            if (!file.exists()) {
                try {
                    if (!file.mkdir())
                        System.err.println("File cannot be created!");
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void unBanPlayer(String playerName) {
        if (!cfg.getBoolean("Ban." + playerName + ".isBanned")) {
            Bukkit.getConsoleSender().sendMessage("Ban." + playerName + " ist nicht gebannt!");
        } else {
            cfg.set("Ban." + playerName + ".isBanned", false);
            saveCfg();
        }
    }

    public List<String> getAllBannedPlayers() {
        return cfg.getStringList("Ban");
    }

}
