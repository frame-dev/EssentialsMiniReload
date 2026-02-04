package ch.framedev.essentialsmini.api;

import ch.framedev.essentialsmini.database.mongodb.BackendManager;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.MoneyFileManager;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.log4j.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Plugin was Created by FrameDev
 * Package: de.framedev.essentialsmini.api
 * Date: 22.11.2020
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
@SuppressWarnings({"deprecation"})
public class VaultAPI extends AbstractEconomy {
    
    private static BackendManager BACKEND_MANAGER;

    public VaultAPI() {
    }

    public static void init() {
        try {
            if (Main.getInstance() != null && Main.getInstance().getDatabaseManager() != null) {
                BACKEND_MANAGER = Main.getInstance().getDatabaseManager().getBackendManager();
            }
        } catch (Exception e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to initialize BackendManager", e);
        }
    }

    /**
     * Ensure BACKEND_MANAGER is available before using it
     * @return true if the backend is available, false otherwise
     */
    private static boolean notEnsureBackend() {
        if (BACKEND_MANAGER == null) {
            try {
                if (Main.getInstance() != null && Main.getInstance().getDatabaseManager() != null) {
                    init();
                }
            } catch (Exception e) {
                Main.getInstance().getLogger4J().log(Level.WARN, "Backend not available", e);
            }
        }
        return BACKEND_MANAGER == null;
    }

    @SuppressWarnings("unused")
    public interface Callback<T> {
        T onSuccess(T object);
        void onError(Exception exception);
    }

    /**
     * Safely get an OfflinePlayer by name with null checks
     * @param playerName the player name
     * @return OfflinePlayer or null if invalid
     */
    private OfflinePlayer getOfflinePlayerSafe(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }
        try {
            return Bukkit.getOfflinePlayer(playerName);
        } catch (Exception e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to get OfflinePlayer for: " + playerName, e);
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return Main.getInstance().isEnabled();
    }

    @Override
    public String getName() {
        return Main.getInstance().getDescription().getName();
    }

    @Override
    public boolean hasBankSupport() {
        return Main.getInstance().getConfig().getBoolean("Bank");
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @SuppressWarnings("unused")
    public double formatToDouble(double amount) {
        return Double.parseDouble(format(amount));
    }

    @Override
    public String currencyNamePlural() {
        return Main.getInstance().getCurrencySymbolMulti();
    }

    @Override
    public String currencyNameSingular() {
        return Main.getInstance().getCurrencySymbol();
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return hasAccount(player.getName());
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return depositPlayer(player.getName(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdrawPlayer(player.getName(), amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return createPlayerAccount(player.getName());
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return has(player.getName(), amount);
    }

    @Override
    public boolean hasAccount(String s) {
        if (s == null || s.trim().isEmpty()) {
            return false;
        }

        OfflinePlayer player = getOfflinePlayerSafe(s);
        if (player == null) {
            return false;
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            return new MySQLManager().hasAccount(player);
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return false;
            }
            return BACKEND_MANAGER.exists(player, "money", "essentialsmini_data");
        }

        // File-based storage
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (Bukkit.getServer().getOnlineMode()) {
            return cfg.getStringList("accounts").contains(player.getUniqueId().toString());
        } else {
            return cfg.getStringList("accounts").contains(player.getName());
        }
    }

    @Override
    public boolean hasAccount(String s, String s1) {
        return hasAccount(s);
    }

    @Override
    public double getBalance(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return 0.0;
        }

        OfflinePlayer player = getOfflinePlayerSafe(playerName);
        if (player == null) {
            return 0.0;
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            return new MySQLManager().getMoney(player);
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return 0.0;
            }
            if (BACKEND_MANAGER.exists(player, "money", "essentialsmini_data")) {
                Object moneyObj = BACKEND_MANAGER.get(player, "money", "essentialsmini_data");
                if (moneyObj instanceof Number) {
                    return ((Number) moneyObj).doubleValue();
                }
            }
            return 0.0;
        }

        // File-based storage
        return new MoneyFileManager().getMoney(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getBalance(player.getName());
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player.getName());
    }

    @Override
    public double getBalance(String s, String s1) {
        return getBalance(s);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return !(getBalance(playerName) < amount);
    }

    @Override
    public boolean has(String s, String s1, double v) {
        return has(s, v);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid player name!");
        }

        if (amount < 0.0D) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative!");
        }

        if (!hasAccount(playerName)) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "The Player does not have an Account!");
        }

        double balance = getBalance(playerName);
        double minBalance = Main.getInstance().getConfig().getDouble("Economy.MinBalance");

        if (balance <= minBalance) {
            return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds!");
        }

        double newBalance = balance - amount;
        if (newBalance < minBalance) {
            return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Would fall below minimum balance!");
        }

        OfflinePlayer player = getOfflinePlayerSafe(playerName);
        if (player == null) {
            return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Failed to get player!");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().removeMoney(player, amount);
        } else if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            if (BACKEND_MANAGER.exists(player, "money", "essentialsmini_data")) {
                BACKEND_MANAGER.updateUser(player, "money", newBalance, "essentialsmini_data");
            }
        } else {
            new MoneyFileManager().removeMoney(player, amount);
        }

        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, String s1, double v) {
        return withdrawPlayer(s, v);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid player name!");
        }

        if (!hasAccount(playerName)) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "The Player does not have an Account!");
        }

        if (amount < 0.0D) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative!");
        }

        double balance = getBalance(playerName);
        double newBalance = balance + amount;
        double maxBalance = Main.getInstance().getConfig().getDouble("Economy.MaxBalance");

        if (newBalance > maxBalance) {
            return new EconomyResponse(maxBalance, balance, EconomyResponse.ResponseType.FAILURE, "Would exceed maximum balance!");
        }

        OfflinePlayer player = getOfflinePlayerSafe(playerName);
        if (player == null) {
            return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Failed to get player!");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().addMoney(player, amount);
        } else if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            if (BACKEND_MANAGER.exists(player, "money", "essentialsmini_data")) {
                BACKEND_MANAGER.updateUser(player, "money", newBalance, "essentialsmini_data");
            }
        } else {
            new MoneyFileManager().addMoney(player, amount);
        }

        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        return depositPlayer(s, v);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return createBank(name, player.getName());
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (player == null || player.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid player name!");
        }

        OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
        if (offlinePlayer == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Failed to get player!");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().createBank(offlinePlayer, name);
        } else if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateUser(offlinePlayer, "bankowner", offlinePlayer.getUniqueId().toString(), "essentialsmini_data");
            BACKEND_MANAGER.updateUser(offlinePlayer, "bankname", name, "essentialsmini_data");
        } else {
            File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("Banks." + name + ".Owner", player);
            cfg.set("Banks." + name + ".balance", 0.0);
            save(file, cfg);
        }

        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            if (new MySQLManager().removeBank(name)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
            } else {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Error while Deleting Bank!");
            }
        } else {
            File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            if (!cfg.contains("Banks." + name)) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
            }

            cfg.set("Banks." + name, null);
            save(file, cfg);
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
        }
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            double balance = new MySQLManager().getBankMoney(name);
            return new EconomyResponse(balance, balance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }

            Object obj = BACKEND_MANAGER.getObject("bankname", name, "bank", "essentialsmini_data");
            if (obj instanceof Number) {
                double val = ((Number) obj).doubleValue();
                return new EconomyResponse(val, val, EconomyResponse.ResponseType.SUCCESS, "");
            }
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
        }

        // File-based storage
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (!cfg.contains("Banks." + name)) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }

        if (cfg.contains("Banks." + name + ".balance")) {
            double balance = cfg.getDouble("Banks." + name + ".balance");
            return new EconomyResponse(balance, balance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        if (bankBalance(name).amount < amount) {
            return new EconomyResponse(amount, bankBalance(name).amount, EconomyResponse.ResponseType.FAILURE, "Not enough Money!");
        }
        return new EconomyResponse(amount, bankBalance(name).amount, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (amount < 0.0D) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative!");
        }

        double balance = bankBalance(name).amount;

        // Check if bank has sufficient funds
        if (!bankHas(name, amount).transactionSuccess()) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Not enough Money");
        }

        double newBalance = balance - amount;

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().setBankMoney(name, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateData("bankname", name, "bank", newBalance, "essentialsmini_data");
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        // File-based storage
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("Banks." + name + ".balance", newBalance);
        save(file, cfg);
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (amount < 0.0D) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative!");
        }

        double balance = bankBalance(name).balance;
        double newBalance = balance + amount;

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().setBankMoney(name, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateData("bankname", name, "bank", newBalance, "essentialsmini_data");
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        // File-based storage
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("Banks." + name + ".balance", newBalance);
        save(file, cfg);
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String player) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid bank name");
        }

        if (player == null || player.trim().isEmpty()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid player name");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
            if (offlinePlayer == null) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Failed to get player");
            }

            if (!new MySQLManager().isBankOwner(name, offlinePlayer)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }
        } else if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Backend not available");
            }

            OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
            if (offlinePlayer == null) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Failed to get player");
            }

            Object ownerObj = BACKEND_MANAGER.get(offlinePlayer, "bankowner", "essentialsmini_data");
            if (!(ownerObj instanceof String owner)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }

            if (!owner.equalsIgnoreCase(offlinePlayer.getUniqueId().toString())) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }
        } else {
            File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            if (!cfg.contains("Banks." + name + ".Owner")) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist");
            }

            String owner = cfg.getString("Banks." + name + ".Owner");
            if (!player.equalsIgnoreCase(owner)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }
        }

        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse isBankMember(String name, String player) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid bank name");
        }

        if (player == null || player.trim().isEmpty()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid player name");
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
            if (offlinePlayer == null) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Failed to get player");
            }

            if (!new MySQLManager().isBankMember(name, offlinePlayer)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        } else if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Backend not available");
            }

            List<String> members = Main.getInstance().getVaultManager().getBankMembers(name);
            if (members == null || !members.contains(player)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        } else {
            File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            if (!cfg.contains("Banks." + name + ".members")) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank has no members");
            }

            List<String> players = cfg.getStringList("Banks." + name + ".members");
            if (!players.contains(player)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        }

        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public List<String> getBanks() {
        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            return new MySQLManager().getBanks();
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                return new ArrayList<>();
            }

            List<String> data = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOnlinePlayers()) {
                if (offlinePlayer == null) continue;

                try {
                    Object ownerObj = BACKEND_MANAGER.get(offlinePlayer, "bankowner", "essentialsmini_data");
                    if (ownerObj instanceof String owner) {
                        if (owner.equalsIgnoreCase(offlinePlayer.getUniqueId().toString())) {
                            Object bankNameObj = BACKEND_MANAGER.get(offlinePlayer, "bankname", "essentialsmini_data");
                            if (bankNameObj instanceof String bankName) {
                                data.add(bankName);
                            }
                        }
                    }
                } catch (Exception e) {
                    Main.getInstance().getLogger4J().log(Level.WARN, "Error getting bank for player: " + offlinePlayer.getName(), e);
                }
            }
            return data;
        }

        // File-based storage
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<String> banks = new ArrayList<>();
        ConfigurationSection cs = cfg.getConfigurationSection("Banks");
        if (cs != null) {
            for (String s : cs.getKeys(false)) {
                if (s != null && !s.trim().isEmpty()) {
                    banks.add(s);
                }
            }
        }
        return banks;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Attempted to create account with invalid player name");
            return false;
        }

        if (hasAccount(playerName)) {
            return false;
        }

        OfflinePlayer player = getOfflinePlayerSafe(playerName);
        if (player == null) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to get OfflinePlayer for: " + playerName);
            return false;
        }

        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            new MySQLManager().createAccount(player);
            Main.getInstance().getLogger4J().info("[Economy] Created Account for " + playerName);
            return true;
        }

        if (Main.getInstance().isMongoDB()) {
            if (notEnsureBackend()) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Backend not available for account creation");
                return false;
            }
            BACKEND_MANAGER.createUser(player, "essentialsmini_data", new BackendManager.Callback<>() {
                @Override
                public void onResult(Void result) {
                    Main.getInstance().getLogger4J().info("[Economy] Created Account for " + playerName);
                }

                @Override
                public void onError(Exception exception) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to create account for " + playerName, exception);
                }
            });
            return true;
        }

        // File-based storage
        File file = new File(Main.getInstance().getDataFolder() + "/money", "eco.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<String> accounts = cfg.getStringList("accounts");

        if (Bukkit.getServer().getOnlineMode()) {
            accounts.add(player.getUniqueId().toString());
        } else {
            accounts.add(player.getName());
        }

        cfg.set("accounts", accounts);
        save(file, cfg);
        Main.getInstance().getLogger4J().info("[Economy] Created Account for " + playerName);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    public void save(File file, FileConfiguration cfg) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error saving", e);
        }
    }

}
