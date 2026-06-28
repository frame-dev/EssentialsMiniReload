package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * This Plugin was Created by FrameDev
 * Package : ch.framedev.essentialsmini.managers
 * ClassName FileManager
 * Date: 20.03.21
 * Project: FrameEconomy
 * Copyrighted by FrameDev
 */

public class MoneyFileManager {

    private final File file;
    private final FileConfiguration cfg;

    public MoneyFileManager() {
        this.file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    private void saveFile() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                Main.getInstance().getLogger4J().error("Could not create money directory: " + parent.getAbsolutePath());
                return;
            }
            cfg.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    private void loadFile() {
        if (!file.exists()) {
            return;
        }
        try {
            cfg.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    public void setMoney(OfflinePlayer player, double amount) {
        String storageKey = getStorageKey(player);
        if (storageKey == null) return;
        loadFile();
        cfg.set(storageKey, amount);
        saveFile();
    }

    public double getMoney(OfflinePlayer player) {
        String storageKey = getStorageKey(player);
        if (storageKey == null) return 0.0;
        loadFile();
        return cfg.getDouble(storageKey);
    }

    private String getStorageKey(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        if (Bukkit.getServer().getOnlineMode()) {
            return player.getUniqueId().toString();
        }
        String playerName = player.getName();
        return playerName == null || playerName.isBlank() ? null : playerName;
    }

    public void addMoney(OfflinePlayer player, double amount) {
        double money = getMoney(player);
        money += amount;
        setMoney(player, money);
    }

    public void removeMoney(OfflinePlayer player, double amount) {
        double money = getMoney(player);
        money -= amount;
        setMoney(player, money);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return getMoney(player) >= amount;
    }
}
