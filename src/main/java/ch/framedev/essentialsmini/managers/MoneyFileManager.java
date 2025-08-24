package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
            cfg.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    public void setMoney(OfflinePlayer player, double amount) {
        if(player.getName() == null) return;
        if (Bukkit.getServer().getOnlineMode()) {
            cfg.set(player.getUniqueId().toString(), amount);
        } else {
            cfg.set(player.getName(), amount);
        }
        saveFile();
    }

    public double getMoney(OfflinePlayer player) {
        if(player.getName() == null) return 0.0;
        if (Bukkit.getServer().getOnlineMode()) {
            return cfg.getDouble(player.getUniqueId().toString());
        } else {
            return cfg.getDouble(player.getName());
        }
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
