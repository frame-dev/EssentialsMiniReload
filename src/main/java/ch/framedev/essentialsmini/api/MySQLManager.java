package ch.framedev.essentialsmini.api;

import ch.framedev.essentialsmini.database.mysqlsqlite.MySQL;
import ch.framedev.essentialsmini.database.mysqlsqlite.SQL;
import ch.framedev.essentialsmini.database.mysqlsqlite.SQLite;
import ch.framedev.essentialsmini.main.Main;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This Plugin was Created by FrameDev
 * Package: de.framedev.essentialsmini.api
 * Date: 23.11.2020
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
@SuppressWarnings("deprecation")
public class MySQLManager {

    protected final String tableName = "essentialsmini_eco";
    private static boolean runOnce;

    public MySQLManager() {
        if (!runOnce) {
            Bukkit.getConsoleSender().sendMessage(Main.getInstance().getPrefix() + "§aMySQL for Vault Enabled!");
            runOnce = true;
        }
    }

    protected boolean isOnlineMode() {
        return Bukkit.getServer().getOnlineMode();
    }

    /**
     * Ensures the economy table exists
     */
    private void ensureTableExists() {
        if (!SQL.isTableExists(tableName)) {
            SQL.createTable(tableName,
                "Player TEXT(256)",
                "Name TEXT(255)",
                "Money TEXT",
                "BankBalance DOUBLE",
                "BankName TEXT",
                "BankOwner TEXT",
                "BankMembers TEXT");
        }
    }

    /**
     * set the Money in the Database
     *
     * @param player the Player
     * @param amount Money amount
     */
    protected void setMoney(OfflinePlayer player, double amount) {
        if (player == null || player.getName() == null) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Attempted to set money for null player");
            return;
        }

        ensureTableExists();

        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();

        if (SQL.exists(tableName, "Player", playerId)) {
            SQL.updateData(tableName, "Money", String.valueOf(amount), "Player = ?", playerId);
        } else {
            if (isOnlineMode()) {
                SQL.insertData(tableName,
                    new String[]{player.getUniqueId().toString(), player.getName(), String.valueOf(amount)},
                    "Player", "Name", "Money");
            } else {
                SQL.insertData(tableName,
                    new String[]{player.getName(), String.valueOf(amount)},
                    "Player", "Money");
            }
        }
    }

    /**
     * @param player the Player
     * @return the Money from the selected Player
     */
    protected double getMoney(OfflinePlayer player) {
        if (player == null || player.getName() == null) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Attempted to get money for null player");
            return 0.0D;
        }

        ensureTableExists();

        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();

        if (SQL.exists(tableName, "Player", playerId)) {
            Object moneyObj = SQL.get(tableName, "Money", "Player", playerId);
            if (moneyObj != null) {
                try {
                    String moneyStr = moneyObj.toString();
                    return Double.parseDouble(moneyStr);
                } catch (NumberFormatException e) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Invalid money value for player " + player.getName(), e);
                    return 0.0D;
                }
            }
        }

        return 0.0D;
    }

    protected void addMoney(OfflinePlayer player, double amount) {
        double money = getMoney(player);
        money += amount;
        setMoney(player, money);
    }

    protected void removeMoney(OfflinePlayer player, double amount) {
        double money = getMoney(player);
        money -= amount;
        setMoney(player, money);
    }

    /**
     * @param player   the BankOwner
     * @param bankName the BankName
     */
    protected void createBank(OfflinePlayer player, String bankName) {
        if (player == null || player.getName() == null || bankName == null || bankName.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Invalid parameters for createBank");
            return;
        }

        ensureTableExists();

        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();

        if (SQL.exists(tableName, "Player", playerId)) {
            Object existingBank = SQL.get(tableName, "BankName", "Player", playerId);
            if (existingBank == null || existingBank.toString().trim().isEmpty()) {
                SQL.updateData(tableName, "BankName", bankName, "Player = ?", playerId);
                SQL.updateData(tableName, "BankOwner", playerId, "Player = ?", playerId);
            }
        } else {
            SQL.insertData(tableName, new String[]{playerId, bankName, playerId}, "Player", "BankName", "BankOwner");
        }
    }


    /**
     * set the Bank Money
     *
     * @param name   the BankName
     * @param amount amount to adding to the Bank
     */
    protected void setBankMoney(String name, double amount) {
        if (name == null || name.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Invalid bank name for setBankMoney");
            return;
        }

        List<String> players = new ArrayList<>();
        String query = "SELECT Player FROM " + tableName + " WHERE BankName = ?";

        try {
            if (Main.getInstance().isMysql()) {
                try (java.sql.PreparedStatement stmt = MySQL.getConnection().prepareStatement(query)) {
                    stmt.setString(1, name);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        while (resultSet.next()) {
                            players.add(resultSet.getString("Player"));
                        }
                    }
                }
            } else if (Main.getInstance().isSQL()) {
                try (Connection conn = Objects.requireNonNull(SQLite.connect());
                     java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, name);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        while (resultSet.next()) {
                            players.add(resultSet.getString("Player"));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error in setBankMoney for bank: " + name, ex);
            return;
        }

        // Update all bank members' balance
        for (String player : players) {
            SQL.updateData(tableName, "BankBalance", String.valueOf(amount), "Player = ?", player);
        }
    }

    /**
     * @param name the BankName
     * @return Amount of the Bank
     */
    protected double getBankMoney(String name) {
        if (name == null || name.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Invalid bank name for getBankMoney");
            return 0.0;
        }

        String query = "SELECT BankBalance FROM " + tableName + " WHERE BankName = ? LIMIT 1";

        try {
            if (Main.getInstance().isMysql()) {
                try (java.sql.PreparedStatement stmt = MySQL.getConnection().prepareStatement(query)) {
                    stmt.setString(1, name);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getDouble("BankBalance");
                        }
                    }
                }
            } else if (Main.getInstance().isSQL()) {
                try (Connection conn = Objects.requireNonNull(SQLite.connect());
                     java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, name);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getDouble("BankBalance");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error in getBankMoney for bank: " + name, ex);
        }
        return 0.0;
    }

    @SuppressWarnings("unused")
    protected void addBankMoney(String name, double amount) {
        double money = getBankMoney(name);
        money += amount;
        setBankMoney(name, money);
    }

    @SuppressWarnings("unused")
    protected void removeBankMoney(String name, double amount) {
        double money = getBankMoney(name);
        money -= amount;
        setBankMoney(name, money);
    }

    /**
     * @param name   the BankName
     * @param player the Player
     * @return if the user is the BankOwner
     */
    protected boolean isBankOwner(String name, OfflinePlayer player) {
        if (name == null || name.trim().isEmpty() || player == null || player.getName() == null) {
            return false;
        }

        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();
        String query = "SELECT BankName, BankOwner FROM " + tableName + " WHERE Player = ?";

        try {
            if (Main.getInstance().isMysql()) {
                try (java.sql.PreparedStatement stmt = MySQL.getConnection().prepareStatement(query)) {
                    stmt.setString(1, playerId);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        if (resultSet.next()) {
                            String bank = resultSet.getString("BankName");
                            String owner = resultSet.getString("BankOwner");
                            return bank != null && owner != null &&
                                   bank.equalsIgnoreCase(name) && owner.equalsIgnoreCase(playerId);
                        }
                    }
                }
            } else if (Main.getInstance().isSQL()) {
                try (Connection conn = Objects.requireNonNull(SQLite.connect());
                     java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, playerId);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        if (resultSet.next()) {
                            String bank = resultSet.getString("BankName");
                            String owner = resultSet.getString("BankOwner");
                            return bank != null && owner != null &&
                                   bank.equalsIgnoreCase(name) && owner.equalsIgnoreCase(playerId);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error in isBankOwner for bank: " + name, ex);
        }
        return false;
    }

    /**
     * Adding user to the Bank as Member
     *
     * @param bankName the BankName
     * @param player   the Player
     */
    public void addBankMember(String bankName, OfflinePlayer player) {
        if (bankName == null || bankName.trim().isEmpty() || player == null || player.getName() == null) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Invalid parameters for addBankMember");
            return;
        }

        if (!SQL.isTableExists(tableName) || !SQL.exists(tableName, "BankName", bankName)) {
            return;
        }

        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();
        Object membersObj = SQL.get(tableName, "BankMembers", "BankName", bankName);
        Object ownerObj = SQL.get(tableName, "BankOwner", "BankName", bankName);

        List<String> players;
        if (membersObj != null) {
            Type type = new TypeToken<List<String>>() {}.getType();
            players = new Gson().fromJson(membersObj.toString(), type);
            if (players == null) {
                players = new ArrayList<>();
            }
        } else {
            players = new ArrayList<>();
        }

        if (!players.contains(player.getName())) {
            players.add(player.getName());
        }

        // Update player's bank info
        SQL.updateData(tableName, "BankOwner", ownerObj, "Player = ?", playerId);
        SQL.updateData(tableName, "BankName", bankName, "Player = ?", playerId);
        SQL.updateData(tableName, "BankMembers", new Gson().toJson(players), "BankName = ?", bankName);
    }

    /**
     * @param bankName the BankName
     * @param player   the Player
     * @return if the Player is a BankMember
     */
    public boolean isBankMember(String bankName, OfflinePlayer player) {
        if (SQL.isTableExists(tableName)) {
            if (SQL.exists(tableName, "BankName", bankName)) {
                if (SQL.get(tableName, "BankMembers", "BankName", bankName) != null) {
                    Type type = new TypeToken<List<String>>() {
                    }.getType();
                    List<String> players = new Gson().fromJson((String) SQL.get(tableName, "BankMembers", "BankName", bankName), type);
                    if (players != null) {
                        return players.contains(player.getName());
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param bankName the Bank Name
     * @param player   the Player
     */
    public void removeBankMember(String bankName, OfflinePlayer player) {
        if (bankName == null || bankName.trim().isEmpty() || player == null || player.getName() == null) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Invalid parameters for removeBankMember");
            return;
        }

        if (!SQL.isTableExists(tableName) || !SQL.exists(tableName, "BankName", bankName)) {
            return;
        }

        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();
        Object membersObj = SQL.get(tableName, "BankMembers", "BankName", bankName);

        if (membersObj == null) {
            return;
        }

        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> members = new Gson().fromJson(membersObj.toString(), type);

        if (members == null) {
            members = new ArrayList<>();
        } else {
            members.remove(player.getName());
        }

        // Clear player's bank info
        SQL.updateData(tableName, "BankOwner", null, "Player = ?", playerId);
        SQL.updateData(tableName, "BankName", null, "Player = ?", playerId);
        SQL.updateData(tableName, "BankBalance", null, "Player = ?", playerId);
        SQL.updateData(tableName, "BankMembers", null, "Player = ?", playerId);

        // Get all players in this bank and update their member list
        List<String> bankPlayers = new ArrayList<>();
        String query = "SELECT Player FROM " + tableName + " WHERE BankName = ?";

        try {
            if (Main.getInstance().isMysql()) {
                try (java.sql.PreparedStatement stmt = MySQL.getConnection().prepareStatement(query)) {
                    stmt.setString(1, bankName);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        while (resultSet.next()) {
                            bankPlayers.add(resultSet.getString("Player"));
                        }
                    }
                }
            } else if (Main.getInstance().isSQL()) {
                try (Connection conn = Objects.requireNonNull(SQLite.connect());
                     java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, bankName);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        while (resultSet.next()) {
                            bankPlayers.add(resultSet.getString("Player"));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error in removeBankMember", ex);
        }

        // Update member list for all bank members
        String membersJson = new Gson().toJson(members);
        for (String p : bankPlayers) {
            SQL.updateData(tableName, "BankMembers", membersJson, "Player = ?", p);
        }
    }

    /**
     * Create an Account for Vault
     *
     * @param player the Player
     */
    protected void createAccount(OfflinePlayer player) {
        if (!SQL.isTableExists("essentialsmini_accounts")) {
            SQL.createTable("essentialsmini_accounts", "name TEXT(255)", "uuid VARCHAR(2003)");
        }
        if (isOnlineMode()) {
            if (!SQL.exists("essentialsmini_accounts", "uuid", "" + player.getUniqueId())) {
                SQL.insertData(
                        "essentialsmini_accounts",
                        new String[]{player.getName(), player.getUniqueId().toString()},
                        "name", "uuid"
                );
            }
        } else {
            if (!SQL.exists("essentialsmini_accounts", "name", player.getName())) {
                SQL.insertData(
                        "essentialsmini_accounts",
                        new String[]{player.getName()},
                        "name"
                );
            }
        }
    }

    /**
     * @param player the Player
     * @return if the Player has an Account or not
     */
    protected boolean hasAccount(OfflinePlayer player) {
        if (SQL.isTableExists("essentialsmini_accounts")) {
            if (isOnlineMode()) {
                return SQL.exists("essentialsmini_accounts", "uuid", "" + player.getUniqueId());
            } else {
                return SQL.exists("essentialsmini_accounts", "name", player.getName());
            }
        }
        return false;
    }

    /**
     * @param bankName the Bank
     * @return all BankMembers
     */
    public List<String> getBankMembers(String bankName) {
        if (SQL.isTableExists(tableName)) {
            if (SQL.exists(tableName, "BankName", bankName)) {
                if (SQL.get(tableName, "BankMembers", "BankName", bankName) != null) {
                    Type type = new TypeToken<List<String>>() {
                    }.getType();
                    return new Gson().fromJson((String) SQL.get(tableName, "BankMembers", "BankName", bankName), type);
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * @return all Banks
     */
    protected List<String> getBanks() {
        List<String> banks = new ArrayList<>();
        if (SQL.isTableExists(tableName)) {
            try {
                if (Main.getInstance().isMysql()) {
                    ResultSet resultSet;
                    try (Statement statement = MySQL.getConnection().createStatement()) {
                        resultSet = statement.executeQuery("SELECT DISTINCT BankName FROM " + tableName + " WHERE BankName IS NOT NULL");
                        while (resultSet.next()) {
                            banks.add(resultSet.getString("BankName"));
                        }
                    }
                } else if (Main.getInstance().isSQL()) {
                    try (Connection conn = Objects.requireNonNull(SQLite.connect()); Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT DISTINCT BankName FROM " + tableName + " WHERE BankName IS NOT NULL")) {
                        while (resultSet.next()) {
                            banks.add(resultSet.getString("BankName"));
                        }
                    }
                }
            } catch (Exception ex) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
            }
        }
        return banks;
    }

    /**
     * Delete bank
     *
     * @param bankName the Bank name for delete
     * @return return if success or not
     */
    public boolean removeBank(String bankName) {
        if (bankName == null || bankName.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.WARN, "Invalid bank name for removeBank");
            return false;
        }

        if (!SQL.isTableExists(tableName) || !getBanks().contains(bankName)) {
            return false;
        }

        List<String> members = getBankMembers(bankName);

        try {
            // Find and clear bank owner
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player == null || player.getName() == null) continue;

                if (isBankOwner(bankName, player)) {
                    String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();
                    clearBankData(playerId);
                }
            }

            // Clear bank data for all members
            if (members != null && !members.isEmpty()) {
                for (String memberName : members) {
                    OfflinePlayer member = Bukkit.getOfflinePlayer(memberName);
                    if (member.getName() == null) continue;

                    removeBankMember(bankName, member);
                    String memberId = isOnlineMode() ? member.getUniqueId().toString() : member.getName();
                    clearBankData(memberId);
                }
            }

            return true;
        } catch (Exception ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error removing bank: " + bankName, ex);
            return false;
        }
    }

    /**
     * Helper method to clear bank data for a player
     * @param playerId the player identifier (UUID or name)
     */
    private void clearBankData(String playerId) {
        SQL.updateData(tableName, "BankOwner", null, "Player = ?", playerId);
        SQL.updateData(tableName, "BankName", null, "Player = ?", playerId);
        SQL.updateData(tableName, "BankBalance", null, "Player = ?", playerId);
        SQL.updateData(tableName, "BankMembers", null, "Player = ?", playerId);
    }
}
