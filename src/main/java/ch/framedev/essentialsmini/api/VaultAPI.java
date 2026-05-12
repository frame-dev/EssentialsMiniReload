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

    private static final String COLLECTION = "essentialsmini_data";
    private static final String MONEY_FILE_NAME = "eco.yml";
    private static final String ACCOUNTS_PATH = "accounts";
    private static final String BANKS_PATH = "Banks.";
    private static final String BANK_OWNER_PATH = ".Owner";
    private static final String BANK_BALANCE_PATH = ".balance";
    private static final String BANK_MEMBERS_PATH = ".members";

    private static BackendManager BACKEND_MANAGER;

    private final Main plugin;
    private final File moneyFile;
    private MySQLManager mySQLManager;
    private MoneyFileManager moneyFileManager;

    public VaultAPI() {
        this.plugin = Main.getInstance();
        this.moneyFile = new File(plugin.getDataFolder() + "/money", MONEY_FILE_NAME);
    }

    public static void init() {
        try {
            if (Main.getInstance() != null && Main.getInstance().getDatabaseManager() != null) {
                BACKEND_MANAGER = Main.getInstance().getDatabaseManager().getBackendManager();
            }
        } catch (Exception e) {
            log(Level.ERROR, "Failed to initialize BackendManager", e);
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
                log(Level.WARN, "Backend not available", e);
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
            plugin.getLogger4J().log(Level.ERROR, "Failed to get OfflinePlayer for: " + playerName, e);
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public String getName() {
        return plugin.getDescription().getName();
    }

    @Override
    public boolean hasBankSupport() {
        return plugin.getConfig().getBoolean("Bank");
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
        return plugin.getCurrencySymbolMulti();
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getCurrencySymbol();
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        return hasAccount(player.getName());
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return failure(0.0D, 0.0D, "Invalid player!");
        }
        return depositPlayer(player.getName(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return failure(0.0D, 0.0D, "Invalid player!");
        }
        return withdrawPlayer(player.getName(), amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        return createPlayerAccount(player.getName());
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (player == null) {
            return false;
        }
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

        if (usesSqlStorage()) {
            return getMySQLManager().hasAccount(player);
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return false;
            }
            return BACKEND_MANAGER.exists(player, "money", COLLECTION);
        }

        FileConfiguration cfg = loadMoneyConfig();

        if (Bukkit.getServer().getOnlineMode()) {
            return cfg.getStringList(ACCOUNTS_PATH).contains(player.getUniqueId().toString());
        } else {
            return cfg.getStringList(ACCOUNTS_PATH).contains(player.getName());
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

        if (usesSqlStorage()) {
            return getMySQLManager().getMoney(player);
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return 0.0;
            }
            if (BACKEND_MANAGER.exists(player, "money", COLLECTION)) {
                Object moneyObj = BACKEND_MANAGER.get(player, "money", COLLECTION);
                if (moneyObj instanceof Number) {
                    return ((Number) moneyObj).doubleValue();
                }
            }
            return 0.0;
        }

        return getMoneyFileManager().getMoney(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) {
            return 0.0D;
        }
        return getBalance(player.getName());
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        if (player == null) {
            return 0.0D;
        }
        return getBalance(player.getName());
    }

    @Override
    public double getBalance(String s, String s1) {
        return getBalance(s);
    }

    @Override
    public boolean has(String playerName, double amount) {
        if (amount < 0.0D) {
            return false;
        }
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
        double minBalance = plugin.getConfig().getDouble("Economy.MinBalance");

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

        if (usesSqlStorage()) {
            getMySQLManager().removeMoney(player, amount);
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            if (BACKEND_MANAGER.exists(player, "money", COLLECTION)) {
                BACKEND_MANAGER.updateUser(player, "money", newBalance, COLLECTION);
            }
        } else {
            getMoneyFileManager().removeMoney(player, amount);
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
        double maxBalance = plugin.getConfig().getDouble("Economy.MaxBalance");

        if (newBalance > maxBalance) {
            return new EconomyResponse(maxBalance, balance, EconomyResponse.ResponseType.FAILURE, "Would exceed maximum balance!");
        }

        OfflinePlayer player = getOfflinePlayerSafe(playerName);
        if (player == null) {
            return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Failed to get player!");
        }

        if (usesSqlStorage()) {
            getMySQLManager().addMoney(player, amount);
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            if (BACKEND_MANAGER.exists(player, "money", COLLECTION)) {
                BACKEND_MANAGER.updateUser(player, "money", newBalance, COLLECTION);
            }
        } else {
            getMoneyFileManager().addMoney(player, amount);
        }

        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        return depositPlayer(s, v);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        if (player == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid player!");
        }
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

        if (usesSqlStorage()) {
            getMySQLManager().createBank(offlinePlayer, name);
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateUser(offlinePlayer, "bankowner", offlinePlayer.getUniqueId().toString(), COLLECTION);
            BACKEND_MANAGER.updateUser(offlinePlayer, "bankname", name, COLLECTION);
        } else {
            FileConfiguration cfg = loadMoneyConfig();
            cfg.set(getBankPath(name) + BANK_OWNER_PATH, player);
            cfg.set(getBankPath(name) + BANK_BALANCE_PATH, 0.0);
            save(cfg);
        }

        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (usesSqlStorage()) {
            if (getMySQLManager().removeBank(name)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
            } else {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Error while Deleting Bank!");
            }
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            boolean deleted = false;
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                Object bankName = BACKEND_MANAGER.get(offlinePlayer, "bankname", COLLECTION);
                if (bankName instanceof String storedName && storedName.equalsIgnoreCase(name)) {
                    BACKEND_MANAGER.updateUser(offlinePlayer, "bankname", "", COLLECTION);
                    BACKEND_MANAGER.updateUser(offlinePlayer, "bankowner", "", COLLECTION);
                    BACKEND_MANAGER.updateUser(offlinePlayer, "bankmembers", new ArrayList<String>(), COLLECTION);
                    BACKEND_MANAGER.updateUser(offlinePlayer, "bank", 0.0D, COLLECTION);
                    deleted = true;
                }
            }
            return deleted
                    ? new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "")
                    : new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        } else {
            FileConfiguration cfg = loadMoneyConfig();

            if (!cfg.contains(getBankPath(name))) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
            }

            cfg.set(getBankPath(name), null);
            save(cfg);
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
        }
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (usesSqlStorage()) {
            double balance = getMySQLManager().getBankMoney(name);
            return new EconomyResponse(balance, balance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }

            Object obj = BACKEND_MANAGER.getObject("bankname", name, "bank", COLLECTION);
            if (obj instanceof Number) {
                double val = ((Number) obj).doubleValue();
                return new EconomyResponse(val, val, EconomyResponse.ResponseType.SUCCESS, "");
            }
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
        }

        FileConfiguration cfg = loadMoneyConfig();

        if (!cfg.contains(getBankPath(name))) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }

        if (cfg.contains(getBankPath(name) + BANK_BALANCE_PATH)) {
            double balance = cfg.getDouble(getBankPath(name) + BANK_BALANCE_PATH);
            return new EconomyResponse(balance, balance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        EconomyResponse balance = bankBalance(name);
        if (!balance.transactionSuccess()) {
            return balance;
        }
        if (balance.amount < amount) {
            return new EconomyResponse(amount, balance.amount, EconomyResponse.ResponseType.FAILURE, "Not enough Money!");
        }
        return new EconomyResponse(amount, balance.amount, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        if (name == null || name.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (amount < 0.0D) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative!");
        }

        EconomyResponse balanceResponse = bankBalance(name);
        if (!balanceResponse.transactionSuccess()) {
            return balanceResponse;
        }
        double balance = balanceResponse.amount;

        if (balance < amount) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Not enough Money");
        }

        double newBalance = balance - amount;

        if (usesSqlStorage()) {
            getMySQLManager().setBankMoney(name, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateData("bankname", name, "bank", newBalance, COLLECTION);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        FileConfiguration cfg = loadMoneyConfig();
        if (!cfg.contains(getBankPath(name))) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }
        cfg.set(getBankPath(name) + BANK_BALANCE_PATH, newBalance);
        save(cfg);
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

        EconomyResponse balanceResponse = bankBalance(name);
        if (!balanceResponse.transactionSuccess()) {
            return balanceResponse;
        }
        double balance = balanceResponse.balance;
        double newBalance = balance + amount;

        if (usesSqlStorage()) {
            getMySQLManager().setBankMoney(name, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateData("bankname", name, "bank", newBalance, COLLECTION);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        FileConfiguration cfg = loadMoneyConfig();
        if (!cfg.contains(getBankPath(name))) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }
        cfg.set(getBankPath(name) + BANK_BALANCE_PATH, newBalance);
        save(cfg);
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

        if (usesSqlStorage()) {
            OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
            if (offlinePlayer == null) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Failed to get player");
            }

            if (!getMySQLManager().isBankOwner(name, offlinePlayer)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Backend not available");
            }

            OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
            if (offlinePlayer == null) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Failed to get player");
            }

            Object bankNameObj = BACKEND_MANAGER.get(offlinePlayer, "bankname", COLLECTION);
            if (!(bankNameObj instanceof String bankName) || !bankName.equalsIgnoreCase(name)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }

            Object ownerObj = BACKEND_MANAGER.get(offlinePlayer, "bankowner", COLLECTION);
            if (!(ownerObj instanceof String owner)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }

            if (!owner.equalsIgnoreCase(offlinePlayer.getUniqueId().toString())) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }
        } else {
            FileConfiguration cfg = loadMoneyConfig();

            if (!cfg.contains(getBankPath(name) + BANK_OWNER_PATH)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist");
            }

            String owner = cfg.getString(getBankPath(name) + BANK_OWNER_PATH);
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

        if (usesSqlStorage()) {
            OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
            if (offlinePlayer == null) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Failed to get player");
            }

            if (!getMySQLManager().isBankMember(name, offlinePlayer)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Backend not available");
            }

            List<String> members = plugin.getVaultManager().getBankMembers(name);
            if (members == null || !members.contains(player)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        } else {
            FileConfiguration cfg = loadMoneyConfig();

            if (!cfg.contains(getBankPath(name) + BANK_MEMBERS_PATH)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank has no members");
            }

            List<String> players = cfg.getStringList(getBankPath(name) + BANK_MEMBERS_PATH);
            if (!players.contains(player)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        }

        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public List<String> getBanks() {
        if (usesSqlStorage()) {
            return getMySQLManager().getBanks();
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new ArrayList<>();
            }

            List<String> data = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (offlinePlayer == null) continue;

                try {
                    Object ownerObj = BACKEND_MANAGER.get(offlinePlayer, "bankowner", COLLECTION);
                    if (ownerObj instanceof String owner) {
                        if (owner.equalsIgnoreCase(offlinePlayer.getUniqueId().toString())) {
                            Object bankNameObj = BACKEND_MANAGER.get(offlinePlayer, "bankname", COLLECTION);
                            if (bankNameObj instanceof String bankName) {
                                data.add(bankName);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger4J().log(Level.WARN, "Error getting bank for player: " + offlinePlayer.getName(), e);
                }
            }
            return data;
        }

        FileConfiguration cfg = loadMoneyConfig();
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
            plugin.getLogger4J().log(Level.WARN, "Attempted to create account with invalid player name");
            return false;
        }

        if (hasAccount(playerName)) {
            return false;
        }

        OfflinePlayer player = getOfflinePlayerSafe(playerName);
        if (player == null) {
            plugin.getLogger4J().log(Level.ERROR, "Failed to get OfflinePlayer for: " + playerName);
            return false;
        }

        if (usesSqlStorage()) {
            getMySQLManager().createAccount(player);
            plugin.getLogger4J().info("[Economy] Created Account for " + playerName);
            return true;
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                plugin.getLogger4J().log(Level.ERROR, "Backend not available for account creation");
                return false;
            }
            BACKEND_MANAGER.createUser(player, COLLECTION, new BackendManager.Callback<>() {
                @Override
                public void onResult(Void result) {
                    plugin.getLogger4J().info("[Economy] Created Account for " + playerName);
                }

                @Override
                public void onError(Exception exception) {
                    plugin.getLogger4J().log(Level.ERROR, "Failed to create account for " + playerName, exception);
                }
            });
            return true;
        }

        FileConfiguration cfg = loadMoneyConfig();
        List<String> accounts = cfg.getStringList(ACCOUNTS_PATH);

        if (Bukkit.getServer().getOnlineMode()) {
            accounts.add(player.getUniqueId().toString());
        } else {
            accounts.add(player.getName());
        }

        cfg.set(ACCOUNTS_PATH, accounts);
        save(cfg);
        plugin.getLogger4J().info("[Economy] Created Account for " + playerName);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    private boolean usesSqlStorage() {
        return plugin.isMysql() || plugin.isSQL();
    }

    private boolean usesMongoStorage() {
        return plugin.isMongoDB();
    }

    private MySQLManager getMySQLManager() {
        if (mySQLManager == null) {
            mySQLManager = new MySQLManager();
        }
        return mySQLManager;
    }

    private MoneyFileManager getMoneyFileManager() {
        if (moneyFileManager == null) {
            moneyFileManager = new MoneyFileManager();
        }
        return moneyFileManager;
    }

    private FileConfiguration loadMoneyConfig() {
        return YamlConfiguration.loadConfiguration(moneyFile);
    }

    private String getBankPath(String name) {
        return BANKS_PATH + name;
    }

    private EconomyResponse failure(double amount, double balance, String error) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, error);
    }

    private void save(FileConfiguration cfg) {
        save(moneyFile, cfg);
    }

    public void save(File file, FileConfiguration cfg) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger4J().log(Level.ERROR, "Error saving", e);
        }
    }

    private static void log(Level level, String message, Throwable throwable) {
        Main instance = Main.getInstance();
        if (instance != null && instance.getLogger4J() != null) {
            instance.getLogger4J().log(level, message, throwable);
        }
    }

}
