package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.api.MySQLManager;
import ch.framedev.essentialsmini.api.VaultAPI;
import ch.framedev.essentialsmini.main.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VaultManager {

    private final Economy eco;

    public VaultManager(Main plugin) {
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            if(!file.getParentFile().mkdirs())
                System.err.println("Could not create directory");
            try {
                if(!file.createNewFile())
                    System.err.println("Could not create new file");
            } catch (IOException e) {
                plugin.getLogger4J().error(e.getMessage(), e);
            }
        }
        if (Bukkit.getServer().getOnlineMode()) {
            if (!cfg.contains("accounts")) {
                ArrayList<String> accounts = new ArrayList<>();
                accounts.add("14555508-6819-4434-aa6a-e5ce1509ea35");
                cfg.set("accounts", accounts);
                try {
                    cfg.save(file);
                } catch (IOException e) {
                    plugin.getLogger4J().error(e.getMessage(), e);
                }
                plugin.getLogger4J().info("Economy Accounts created!");
            }
        } else {
            if (!cfg.contains("accounts")) {
                ArrayList<String> accounts = new ArrayList<>();
                accounts.add("sambakuchen");
                cfg.set("accounts", accounts);
                try {
                    cfg.save(file);
                } catch (IOException e) {
                    plugin.getLogger4J().error(e.getMessage(), e);
                }
                plugin.getLogger4J().info("Economy Accounts created!");
            }
        }
        plugin.getServer().getServicesManager().register(Economy.class, new VaultAPI(), plugin, ServicePriority.High);
        eco = new VaultAPI();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!eco.hasAccount(player))
                eco.createPlayerAccount(player);
        }
        if(plugin.isMongoDB()) {
            VaultAPI.init();
        }

        Bukkit.getConsoleSender().sendMessage(plugin.getPrefix() + "§aVaultManager Loaded and Enabled!");
    }

    public List<String> getBanks() {
        return Main.getInstance().getVaultManager().getEco().getBanks();
    }

    public List<String> getAccounts() {
        return Main.getInstance().getVaultManager().getAccounts();
    }

    private final File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
    private final FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

    /**
     * Add a User to the Bank
     *
     * @param bankName the Bank name
     * @param player   the OfflinePlayer
     */
    @SuppressWarnings("unchecked")
    public void addBankMember(String bankName, OfflinePlayer player) {
        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().addBankMember(bankName, player);
        } else if (Main.getInstance().isMongoDB()) {
            List<String> users = (List<String>) Main.getInstance().getDatabaseManager().getBackendManager().getObject("bankname", bankName, "bankmembers", "essentialsmini_data");
            if (!users.contains(player.getName()))
                users.add(player.getName());
            Main.getInstance().getDatabaseManager().getBackendManager().updateUser(player, "bankname", bankName, "essentialsmini_data");
            Main.getInstance().getDatabaseManager().getBackendManager().updateUser(player, "bankmembers", users, "essentialsmini_data");
            Main.getInstance().getDatabaseManager().getBackendManager().updateData("bankname", bankName, "bankmembers", users, "essentialsmini_data");
        } else {
            try {
                cfg.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                Main.getInstance().getLogger4J().error(e.getMessage(), e);
            }
            if (!cfg.contains("Banks." + bankName + ".members")) {
                List<String> players = new ArrayList<>();
                players.add(player.getName());
                cfg.set("Banks." + bankName + ".members", players);
            } else {
                List<String> players = cfg.getStringList("Banks." + bankName + ".members");
                if (!players.contains(player.getName()))
                    players.add(player.getName());
                cfg.set("Banks." + bankName + ".members", players);
            }
            try {
                cfg.save(file);
            } catch (IOException e) {
                Main.getInstance().getLogger4J().error(e.getMessage(), e);
            }
        }
    }

    /**
     * Removing a User from the Bank
     *
     * @param bankName the BankName
     * @param player   the OfflinePlayer
     */
    @SuppressWarnings("unchecked")
    public void removeBankMember(String bankName, OfflinePlayer player) {
        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().removeBankMember(bankName, player);
        } else if (Main.getInstance().isMongoDB()) {
            List<String> users = (List<String>) Main.getInstance().getDatabaseManager().getBackendManager().getObject("bankname", bankName, "bankmembers", "essentialsmini_data");
            users.remove(player.getName());
            Main.getInstance().getDatabaseManager().getBackendManager().updateUser(player, "bankname", "", "essentialsmini_data");
            Main.getInstance().getDatabaseManager().getBackendManager().updateUser(player, "bankmembers", users, "essentialsmini_data");
            Main.getInstance().getDatabaseManager().getBackendManager().updateData("bankname", bankName, "bankmembers", users, "essentialsmini_data");
        } else {
            try {
                cfg.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                Main.getInstance().getLogger4J().error(e.getMessage(), e);
            }
            if (cfg.contains("Banks." + bankName + ".members")) {
                List<String> players = cfg.getStringList("Banks." + bankName + ".members");
                players.remove(player.getName());
                cfg.set("Banks." + bankName + ".members", players);
            }
            try {
                cfg.save(file);
            } catch (IOException e) {
                Main.getInstance().getLogger4J().error(e.getMessage(), e);
            }
        }
    }

    /**
     * Return all BankMembers if the Bank exists and have BankMembers!
     *
     * @param bankName the BankName
     * @return all BankMembers from the Bank
     */
    @SuppressWarnings("unchecked")
    public List<String> getBankMembers(String bankName) {
        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            return new MySQLManager().getBankMembers(bankName);
        } else if (Main.getInstance().isMongoDB()) {
            return (List<String>) Main.getInstance().getDatabaseManager().getBackendManager().getObject("bankname", bankName, "bankmembers", "essentialsmini_data");
        } else {
            return cfg.getStringList("Banks." + bankName + ".members");
        }
    }

    public Economy getEconomy() {
        return eco;
    }

    public Economy getEco() {
        return eco;
    }
}
