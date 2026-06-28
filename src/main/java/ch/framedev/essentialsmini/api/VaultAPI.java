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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private static final String BANKS_SECTION = "Banks";
    private static final String BANK_DISPLAY_NAME_PATH = ".Name";
    private static final String BANK_OWNER_PATH = ".Owner";
    private static final String BANK_OWNER_NAME_PATH = ".OwnerName";
    private static final String BANK_BALANCE_PATH = ".balance";
    private static final String BANK_MEMBERS_PATH = ".members";
    private static final String BANK_NAME_PATTERN = "[A-Za-z0-9_-]{1,32}";

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
            try {
                return Bukkit.getOfflinePlayer(UUID.fromString(playerName));
            } catch (IllegalArgumentException ignored) {
            }
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
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return false;
        }
        return hasAccount(playerLookup);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return failure(0.0D, 0.0D, "Invalid player!");
        }
        return depositPlayer(playerLookup, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return failure(0.0D, 0.0D, "Invalid player!");
        }
        return withdrawPlayer(playerLookup, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return false;
        }
        return createPlayerAccount(playerLookup);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return false;
        }
        return has(playerLookup, amount);
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
        String storageKey = getPlayerAccountLookup(player);
        return storageKey != null && cfg.getStringList(ACCOUNTS_PATH).contains(storageKey);
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
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return 0.0D;
        }
        return getBalance(playerLookup);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        String playerLookup = getPlayerAccountLookup(player);
        if (playerLookup == null) {
            return 0.0D;
        }
        return getBalance(playerLookup);
    }

    @Override
    public double getBalance(String s, String s1) {
        return getBalance(s);
    }

    @Override
    public boolean has(String playerName, double amount) {
        if (!isValidAmount(amount, true)) {
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

        if (!isValidAmount(amount, true)) {
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

        if (!isValidAmount(amount, true)) {
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
        String playerLookup = getPlayerNameOrAccountLookup(player);
        if (playerLookup == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid player!");
        }
        return createBank(name, playerLookup);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        String bankName = normalizeBankName(name);
        if (bankName == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }
        if (!isValidBankName(bankName)) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank names may only contain letters, numbers, underscores, and hyphens.");
        }

        if (player == null || player.trim().isEmpty()) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid player name!");
        }

        OfflinePlayer offlinePlayer = getOfflinePlayerSafe(player);
        if (offlinePlayer == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Failed to get player!");
        }

        if (usesSqlStorage()) {
            getMySQLManager().createBank(offlinePlayer, bankName);
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateUser(offlinePlayer, "bankowner", offlinePlayer.getUniqueId().toString(), COLLECTION);
            BACKEND_MANAGER.updateUser(offlinePlayer, "bankname", bankName, COLLECTION);
        } else {
            FileConfiguration cfg = loadMoneyConfig();
            if (getExistingBankKey(cfg, bankName) != null) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank already exists!");
            }
            String bankPath = getBankPath(bankName);
            cfg.set(bankPath + BANK_DISPLAY_NAME_PATH, bankName);
            cfg.set(bankPath + BANK_OWNER_PATH, getPlayerStorageId(offlinePlayer));
            cfg.set(bankPath + BANK_OWNER_NAME_PATH, player);
            cfg.set(bankPath + BANK_BALANCE_PATH, 0.0);
            cfg.set(bankPath + BANK_MEMBERS_PATH, new ArrayList<String>());
            save(cfg);
        }

        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        String bankName = normalizeBankName(name);
        if (bankName == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (usesSqlStorage()) {
            if (getMySQLManager().removeBank(bankName)) {
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
                Object storedBankName = BACKEND_MANAGER.get(offlinePlayer, "bankname", COLLECTION);
                if (storedBankName instanceof String storedName && storedName.equalsIgnoreCase(bankName)) {
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
            String bankKey = getExistingBankKey(cfg, bankName);

            if (bankKey == null) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
            }

            cfg.set(getBankPath(bankKey), null);
            save(cfg);
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
        }
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        String bankName = normalizeBankName(name);
        if (bankName == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (usesSqlStorage()) {
            double balance = getMySQLManager().getBankMoney(bankName);
            return new EconomyResponse(balance, balance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }

            Object obj = BACKEND_MANAGER.getObject("bankname", bankName, "bank", COLLECTION);
            if (obj instanceof Number) {
                double val = ((Number) obj).doubleValue();
                return new EconomyResponse(val, val, EconomyResponse.ResponseType.SUCCESS, "");
            }
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.SUCCESS, "");
        }

        FileConfiguration cfg = loadMoneyConfig();
        String bankKey = getExistingBankKey(cfg, bankName);

        if (bankKey == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }

        if (cfg.contains(getBankPath(bankKey) + BANK_BALANCE_PATH)) {
            double balance = cfg.getDouble(getBankPath(bankKey) + BANK_BALANCE_PATH);
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
        String bankName = normalizeBankName(name);
        if (bankName == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (!isValidAmount(amount, true)) {
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
            getMySQLManager().setBankMoney(bankName, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateData("bankname", bankName, "bank", newBalance, COLLECTION);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        FileConfiguration cfg = loadMoneyConfig();
        String bankKey = getExistingBankKey(cfg, bankName);
        if (bankKey == null) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }
        cfg.set(getBankPath(bankKey) + BANK_BALANCE_PATH, newBalance);
        save(cfg);
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        String bankName = normalizeBankName(name);
        if (bankName == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Invalid bank name!");
        }

        if (!isValidAmount(amount, true)) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative!");
        }

        EconomyResponse balanceResponse = bankBalance(name);
        if (!balanceResponse.transactionSuccess()) {
            return balanceResponse;
        }
        double balance = balanceResponse.balance;
        double newBalance = balance + amount;

        if (usesSqlStorage()) {
            getMySQLManager().setBankMoney(bankName, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0D, balance, EconomyResponse.ResponseType.FAILURE, "Backend not available!");
            }
            BACKEND_MANAGER.updateData("bankname", bankName, "bank", newBalance, COLLECTION);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }

        FileConfiguration cfg = loadMoneyConfig();
        String bankKey = getExistingBankKey(cfg, bankName);
        if (bankKey == null) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist!");
        }
        cfg.set(getBankPath(bankKey) + BANK_BALANCE_PATH, newBalance);
        save(cfg);
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String player) {
        String bankName = normalizeBankName(name);
        if (bankName == null) {
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

            if (!getMySQLManager().isBankOwner(bankName, offlinePlayer)) {
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
            if (!(bankNameObj instanceof String storedBankName) || !storedBankName.equalsIgnoreCase(bankName)) {
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
            String bankKey = getExistingBankKey(cfg, bankName);

            if (bankKey == null || !cfg.contains(getBankPath(bankKey) + BANK_OWNER_PATH)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank doesn't exist");
            }

            List<String> references = getPlayerReferences(player);
            String owner = cfg.getString(getBankPath(bankKey) + BANK_OWNER_PATH);
            String ownerName = cfg.getString(getBankPath(bankKey) + BANK_OWNER_NAME_PATH);
            if (!matchesAnyReference(owner, references) && !matchesAnyReference(ownerName, references)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't the Owner");
            }
        }

        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse isBankMember(String name, String player) {
        String bankName = normalizeBankName(name);
        if (bankName == null) {
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

            if (!getMySQLManager().isBankMember(bankName, offlinePlayer)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        } else if (usesMongoStorage()) {
            if (notEnsureBackend()) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Backend not available");
            }

            List<String> members = plugin.getVaultManager().getBankMembers(bankName);
            if (members == null || !members.contains(player)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Isn't a member");
            }
        } else {
            FileConfiguration cfg = loadMoneyConfig();
            String bankKey = getExistingBankKey(cfg, bankName);

            if (bankKey == null || !cfg.contains(getBankPath(bankKey) + BANK_MEMBERS_PATH)) {
                return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank has no members");
            }

            List<String> players = cfg.getStringList(getBankPath(bankKey) + BANK_MEMBERS_PATH);
            if (!containsAnyReference(players, getPlayerReferences(player))) {
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
        ConfigurationSection cs = cfg.getConfigurationSection(BANKS_SECTION);
        if (cs != null) {
            for (String s : cs.getKeys(false)) {
                if (s != null && !s.trim().isEmpty()) {
                    banks.add(cfg.getString(getBankPath(s) + BANK_DISPLAY_NAME_PATH, s));
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
        String storageKey = getPlayerAccountLookup(player);
        if (storageKey == null) {
            plugin.getLogger4J().log(Level.ERROR, "Failed to resolve account storage key for: " + playerName);
            return false;
        }

        accounts.add(storageKey);

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

    private String normalizeBankName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isValidBankName(String name) {
        return name != null && name.matches(BANK_NAME_PATTERN);
    }

    private boolean isValidAmount(double amount, boolean allowZero) {
        return Double.isFinite(amount) && (allowZero ? amount >= 0.0D : amount > 0.0D);
    }

    private String getExistingBankKey(FileConfiguration cfg, String name) {
        ConfigurationSection section = cfg.getConfigurationSection(BANKS_SECTION);
        if (section == null || name == null) {
            return null;
        }
        for (String key : section.getKeys(false)) {
            String displayName = cfg.getString(getBankPath(key) + BANK_DISPLAY_NAME_PATH, key);
            if (key.equalsIgnoreCase(name) || displayName.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }

    private String getPlayerStorageId(OfflinePlayer player) {
        if (player == null) {
            return "";
        }
        String storageKey = getPlayerAccountLookup(player);
        return storageKey == null ? "" : storageKey;
    }

    private String getPlayerAccountLookup(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        if (Bukkit.getServer().getOnlineMode()) {
            return player.getUniqueId().toString();
        }
        String playerName = player.getName();
        return playerName == null || playerName.isBlank() ? null : playerName;
    }

    private String getPlayerNameOrAccountLookup(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        String playerName = player.getName();
        return playerName == null || playerName.isBlank() ? getPlayerAccountLookup(player) : playerName;
    }

    private List<String> getPlayerReferences(String playerName) {
        Set<String> references = new LinkedHashSet<>();
        if (playerName != null && !playerName.isBlank()) {
            references.add(playerName);
        }
        OfflinePlayer offlinePlayer = getOfflinePlayerSafe(playerName);
        if (offlinePlayer != null) {
            references.add(offlinePlayer.getUniqueId().toString());
            if (offlinePlayer.getName() != null && !offlinePlayer.getName().isBlank()) {
                references.add(offlinePlayer.getName());
            }
        }
        return new ArrayList<>(references);
    }

    private boolean matchesAnyReference(String value, List<String> references) {
        if (value == null || references == null) {
            return false;
        }
        for (String reference : references) {
            if (reference != null && value.equalsIgnoreCase(reference)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyReference(List<String> values, List<String> references) {
        if (values == null || references == null) {
            return false;
        }
        for (String value : values) {
            if (matchesAnyReference(value, references)) {
                return true;
            }
        }
        return false;
    }

    private EconomyResponse failure(double amount, double balance, String error) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, error);
    }

    private void save(FileConfiguration cfg) {
        save(moneyFile, cfg);
    }

    public void save(File file, FileConfiguration cfg) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger4J().log(Level.ERROR, "Could not create directory: " + parent.getAbsolutePath());
                return;
            }
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
