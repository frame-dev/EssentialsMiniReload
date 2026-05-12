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

@SuppressWarnings("unused")
public class VaultManager {

    private static final String ECO_FILE_NAME = "eco.yml";
    private static final String ACCOUNTS_PATH = "accounts";
    private static final String BANKS_PATH = "Banks.";
    private static final String BANK_MEMBERS_PATH = ".members";
    private static final String DEFAULT_ONLINE_ACCOUNT = "14555508-6819-4434-aa6a-e5ce1509ea35";
    private static final String DEFAULT_OFFLINE_ACCOUNT = "sambakuchen";

    private final Main plugin;
    private final Economy eco;
    private final File file;
    private final FileConfiguration cfg;
    private MySQLManager mySQLManager;

    public VaultManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder() + "/money", ECO_FILE_NAME);
        this.cfg = YamlConfiguration.loadConfiguration(file);

        createStorageFile();
        createDefaultAccounts();

        if (plugin.isMongoDB()) {
            VaultAPI.init();
        }

        VaultAPI vaultAPI = new VaultAPI();
        plugin.getServer().getServicesManager().register(Economy.class, vaultAPI, plugin, ServicePriority.High);
        eco = vaultAPI;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!eco.hasAccount(player)) {
                eco.createPlayerAccount(player);
            }
        }

        Bukkit.getConsoleSender().sendMessage(plugin.getPrefix() + "§aVaultManager Loaded and Enabled!");
    }

    public List<String> getBanks() {
        return eco.getBanks();
    }

    public List<String> getAccounts() {
        loadConfig();
        return new ArrayList<>(cfg.getStringList(ACCOUNTS_PATH));
    }

    /**
     * Add a User to the Bank
     *
     * @param bankName the Bank name
     * @param player   the OfflinePlayer
     */
    @SuppressWarnings("unchecked")
    public void addBankMember(String bankName, OfflinePlayer player) {
        String playerName = getPlayerName(player);
        if (isInvalidBankRequest(bankName, playerName)) {
            return;
        }

        if (plugin.isMysql() || plugin.isSQL()) {
            getMySQLManager().addBankMember(bankName, player);
        } else if (plugin.isMongoDB()) {
            List<String> users = (List<String>) plugin.getDatabaseManager().getBackendManager().getObject("bankname", bankName, "bankmembers", "essentialsmini_data");
            if (users == null) {
                users = new ArrayList<>();
            }
            if (!users.contains(playerName)) {
                users.add(playerName);
            }
            plugin.getDatabaseManager().getBackendManager().updateUser(player, "bankname", bankName, "essentialsmini_data");
            plugin.getDatabaseManager().getBackendManager().updateUser(player, "bankmembers", users, "essentialsmini_data");
            plugin.getDatabaseManager().getBackendManager().updateData("bankname", bankName, "bankmembers", users, "essentialsmini_data");
        } else {
            loadConfig();
            List<String> players = cfg.getStringList(getBankMembersPath(bankName));
            if (!players.contains(playerName)) {
                players.add(playerName);
            }
            cfg.set(getBankMembersPath(bankName), players);
            saveConfig();
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
        String playerName = getPlayerName(player);
        if (isInvalidBankRequest(bankName, playerName)) {
            return;
        }

        if (plugin.isMysql() || plugin.isSQL()) {
            getMySQLManager().removeBankMember(bankName, player);
        } else if (plugin.isMongoDB()) {
            List<String> users = (List<String>) plugin.getDatabaseManager().getBackendManager().getObject("bankname", bankName, "bankmembers", "essentialsmini_data");
            if (users != null) {
                users.remove(playerName);
            }
            plugin.getDatabaseManager().getBackendManager().updateUser(player, "bankname", "", "essentialsmini_data");
            plugin.getDatabaseManager().getBackendManager().updateUser(player, "bankmembers", users, "essentialsmini_data");
            plugin.getDatabaseManager().getBackendManager().updateData("bankname", bankName, "bankmembers", users, "essentialsmini_data");
        } else {
            loadConfig();
            if (cfg.contains(getBankMembersPath(bankName))) {
                List<String> players = cfg.getStringList(getBankMembersPath(bankName));
                players.remove(playerName);
                cfg.set(getBankMembersPath(bankName), players);
                saveConfig();
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
        if (bankName == null || bankName.isBlank()) {
            return new ArrayList<>();
        }

        if (plugin.isMysql() || plugin.isSQL()) {
            return getMySQLManager().getBankMembers(bankName);
        } else if (plugin.isMongoDB()) {
            List<String> users = (List<String>) plugin.getDatabaseManager().getBackendManager().getObject("bankname", bankName, "bankmembers", "essentialsmini_data");
            return users == null ? new ArrayList<>() : new ArrayList<>(users);
        } else {
            loadConfig();
            return new ArrayList<>(cfg.getStringList(getBankMembersPath(bankName)));
        }
    }

    public Economy getEconomy() {
        return eco;
    }

    public Economy getEco() {
        return eco;
    }

    private void createStorageFile() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger4J().error("Could not create directory: " + parent.getAbsolutePath());
            return;
        }

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger4J().error("Could not create file: " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                plugin.getLogger4J().error(e.getMessage(), e);
            }
        }
    }

    private void createDefaultAccounts() {
        loadConfig();
        if (cfg.contains(ACCOUNTS_PATH)) {
            return;
        }

        List<String> accounts = new ArrayList<>();
        accounts.add(Bukkit.getServer().getOnlineMode() ? DEFAULT_ONLINE_ACCOUNT : DEFAULT_OFFLINE_ACCOUNT);
        cfg.set(ACCOUNTS_PATH, accounts);
        saveConfig();
        plugin.getLogger4J().info("Economy Accounts created!");
    }

    private void loadConfig() {
        try {
            cfg.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger4J().error(e.getMessage(), e);
        }
    }

    private void saveConfig() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger4J().error(e.getMessage(), e);
        }
    }

    private boolean isInvalidBankRequest(String bankName, String playerName) {
        if (bankName == null || bankName.isBlank()) {
            plugin.getLogger4J().warn("Bank name cannot be empty.");
            return true;
        }
        if (playerName == null || playerName.isBlank()) {
            plugin.getLogger4J().warn("Player name cannot be empty.");
            return true;
        }
        return false;
    }

    private String getPlayerName(OfflinePlayer player) {
        return player == null ? null : player.getName();
    }

    private MySQLManager getMySQLManager() {
        if (mySQLManager == null) {
            mySQLManager = new MySQLManager();
        }
        return mySQLManager;
    }

    private String getBankMembersPath(String bankName) {
        return BANKS_PATH + bankName + BANK_MEMBERS_PATH;
    }
}
