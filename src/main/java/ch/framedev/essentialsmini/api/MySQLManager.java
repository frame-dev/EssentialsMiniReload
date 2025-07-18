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
import java.math.BigDecimal;
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
     * set the Money in the Database
     *
     * @param player the Player
     * @param amount Money amount
     */
    protected void setMoney(OfflinePlayer player, double amount) {
        if (isOnlineMode()) {
            if (SQL.isTableExists(tableName)) {
                if (SQL.exists(tableName, "Player", player.getUniqueId().toString())) {
                    SQL.updateData(tableName, "Money", String.valueOf(amount), "Player = '" + player.getUniqueId() + "'");
                } else {
                    String[] data = {"" + player.getUniqueId(), player.getName(), "" + amount};
                    SQL.insertData(tableName, data, "Player", "Name", "Money");
                }
            } else {
                SQL.createTable(tableName, "Player TEXT(256)", "Name TEXT(255)", "Money TEXT", "BankBalance DOUBLE", "BankName TEXT", "BankOwner TEXT", "BankMembers TEXT");
                // Correct and safe way to insert data using the existing insertData method
                SQL.insertData(
                        tableName,
                        new String[]{player.getUniqueId().toString(), player.getName(), String.valueOf(amount)},
                        "Player", "Name", "Money"
                );
            }
        } else {
            if (SQL.isTableExists(tableName)) {
                if (SQL.exists(tableName, "Player", player.getName())) {
                    SQL.updateData(tableName, "Money", String.valueOf(amount), "Player = '" + player.getName() + "'");
                } else {
                    // Correct and safe way to insert data using the insertData method
                    SQL.insertData(
                            tableName,
                            new String[]{player.getName(), String.valueOf(amount)},
                            "Player", "Money"
                    );
                }
            } else {
                BigDecimal bd = new BigDecimal(amount);
                int packedInt = bd.scaleByPowerOfTen(4).intValue();
                SQL.createTable(tableName, "Player TEXT(256)", "Name TEXT(255)", "Money TEXT", "BankBalance DOUBLE", "BankName TEXT", "BankOwner TEXT", "BankMembers TEXT");
                // Safe insertion of data using the correct method
                SQL.insertData(
                        tableName,
                        new String[]{player.getName(), String.valueOf(packedInt)},
                        "Player", "Money"
                );
            }
        }
    }

    /**
     * @param player the Player
     * @return the Money from the selected Player
     */
    protected double getMoney(OfflinePlayer player) {
        if (isOnlineMode()) {
            if (SQL.isTableExists(tableName)) {
                if (SQL.exists(tableName, "Player", player.getUniqueId().toString())) {
                    if (SQL.get(tableName, "Money", "Player", player.getUniqueId().toString()) != null) {
                        String bd = (String) SQL.get(tableName, "Money", "Player", player.getUniqueId().toString());
                        if (bd != null) {
                            return Double.parseDouble(bd);
                        }
                    }
                }
            } else {
                SQL.createTable(tableName, "Player TEXT(256)", "Name TEXT(255)", "Money TEXT", "BankBalance DOUBLE", "BankName TEXT", "BankOwner TEXT", "BankMembers TEXT");
            }
        } else {
            if (SQL.isTableExists(tableName)) {
                if (SQL.exists(tableName, "Player", player.getName())) {
                    if (SQL.get(tableName, "Money", "Player", player.getName()) != null) {
                        String bd = (String) SQL.get(tableName, "Money", "Player", player.getName());
                        if (bd != null) {
                            return Double.parseDouble(bd);
                        }
                    }
                }
            } else {
                SQL.createTable(tableName, "Player TEXT(256)", "Name TEXT(255)", "Money TEXT", "BankBalance DOUBLE", "BankName TEXT", "BankOwner TEXT", "BankMembers TEXT");
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
        String playerId = isOnlineMode() ? player.getUniqueId().toString() : player.getName();
        boolean tableExists = SQL.isTableExists(tableName);

        if (tableExists && SQL.exists(tableName, "Player", playerId)) {
            if (SQL.get(tableName, "BankName", "Player", playerId) == null) {
                SQL.updateData(tableName, "BankName", bankName, "Player = ?", playerId);
                SQL.updateData(tableName, "BankOwner", playerId, "Player = ?", playerId);
            }
        } else {
            if (!tableExists) {
                SQL.createTable(tableName, "Player TEXT(256)", "Name TEXT(255)", "Money TEXT", "BankBalance DOUBLE", "BankName TEXT", "BankOwner TEXT", "BankMembers TEXT");
            }
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
        int i = 0;
        List<String> players = new ArrayList<>();
        try {
            if (Main.getInstance().isMysql()) {
                ResultSet resultSet;
                try (Statement statement = MySQL.getConnection().createStatement()) {
                    resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName ='" + name + "';");
                    while (resultSet.next()) {
                        i++;
                        players.add(resultSet.getString("Player"));
                    }
                }
            } else if (Main.getInstance().isSQL()) {
                ResultSet resultSet;
                try (Statement statement = Objects.requireNonNull(SQLite.connect()).createStatement()) {
                    resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName ='" + name + "';");
                    while (resultSet.next()) {
                        i++;
                        players.add(resultSet.getString("Player"));
                    }
                }
            }
        } catch (SQLException ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
        }
        if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
            for (int x = 0; x <= i; x++) {
                for (String player : players) {
                    SQL.updateData(tableName, "BankBalance", String.valueOf(amount), "Player = '" + player + "'");
                }
            }
        }
    }

    /**
     * @param name the BankName
     * @return Amount of the Bank
     */
    protected double getBankMoney(String name) {
        try {
            if (Main.getInstance().isMysql()) {
                ResultSet resultSet;
                try (Statement statement = MySQL.getConnection().createStatement()) {
                    resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName ='" + name + "';");
                    if (resultSet.next())
                        return resultSet.getDouble("BankBalance");
                }
            } else if (Main.getInstance().isSQL()) {
                ResultSet resultSet;
                try (Statement statement = Objects.requireNonNull(SQLite.connect()).createStatement()) {
                    resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName ='" + name + "';");
                    if (resultSet.next())
                        return resultSet.getDouble("BankBalance");
                }
            }
        } catch (Exception ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
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
        try {
            if (Main.getInstance().isMysql()) {
                try (Statement statement = MySQL.getConnection().createStatement()) {
                    ResultSet resultSet;
                    if (isOnlineMode()) {
                        resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE Player = '" + player.getUniqueId() + "';");
                        if (resultSet.next())
                            if (resultSet.getString("BankName").equalsIgnoreCase(name) && resultSet.getString("BankOwner").equalsIgnoreCase(player.getUniqueId().toString()))
                                return true;
                    } else {
                        resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE Player = '" + player.getName() + "';");
                        if (resultSet.next())
                            if (resultSet.getString("BankName").equalsIgnoreCase(name) && resultSet.getString("BankOwner").equalsIgnoreCase(player.getName()))
                                return true;
                    }
                }
            } else if (Main.getInstance().isSQL()) {
                try (Statement statement = Objects.requireNonNull(SQLite.connect()).createStatement()) {
                    ResultSet resultSet;
                    if (isOnlineMode()) {
                        resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE Player = '" + player.getUniqueId() + "';");
                        if (resultSet.next()) {
                            if (resultSet.getString("BankName").equalsIgnoreCase(name) && resultSet.getString("BankOwner").equalsIgnoreCase(player.getUniqueId().toString())) {
                                return true;
                            }
                        }
                    } else {
                        resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE Player = '" + player.getName() + "';");
                        if (resultSet.next()) {
                            if (resultSet.getString("BankName").equalsIgnoreCase(name) && resultSet.getString("BankOwner").equalsIgnoreCase(player.getName())) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
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
        if (SQL.isTableExists(tableName)) {
            if (SQL.exists(tableName, "BankName", bankName)) {
                if (SQL.get(tableName, "BankMembers", "BankName", bankName) != null) {
                    Type type = new TypeToken<List<String>>() {
                    }.getType();
                    List<String> players = new Gson().fromJson((String) SQL.get(tableName, "BankMembers", "BankName", bankName), type);
                    if (players != null && !players.contains(player.getName())) players.add(player.getName());
                    if (isOnlineMode()) {
                        SQL.updateData(tableName, "BankOwner", SQL.get(tableName, "BankOwner", "BankName", bankName), "Player = '" + player.getUniqueId() + "'");
                        SQL.updateData(tableName, "BankName", bankName, "Player = '" + player.getUniqueId() + "'");
                    } else {
                        SQL.updateData(tableName, "BankOwner", SQL.get(tableName, "BankOwner", "BankName", bankName), "Player = '" + player.getName() + "'");
                        SQL.updateData(tableName, "BankName", bankName, "Player = '" + player.getName() + "'");
                    }
                    SQL.updateData(tableName, "BankMembers", new Gson().toJson(players), "BankName = '" + bankName + "'");
                } else {
                    List<String> players = new ArrayList<>();
                    players.add(player.getName());
                    if (isOnlineMode()) {
                        SQL.updateData(tableName, "BankOwner", SQL.get(tableName, "BankOwner", "BankName", bankName), "Player = '" + player.getUniqueId() + "'");
                        SQL.updateData(tableName, "BankName", bankName, "Player = '" + player.getUniqueId() + "'");
                    } else {
                        SQL.updateData(tableName, "BankOwner", SQL.get(tableName, "BankOwner", "BankName", bankName), "Player = '" + player.getName() + "'");
                        SQL.updateData(tableName, "BankName", bankName, "Player = '" + player.getName() + "'");
                    }
                    SQL.updateData(tableName, "BankMembers", new Gson().toJson(players), "BankName = '" + bankName + "'");
                }
            }
        }
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
        List<String> pls = new ArrayList<>();
        List<String> members = new ArrayList<>();
        if (SQL.isTableExists(tableName)) {
            if (SQL.exists(tableName, "BankName", bankName)) {
                if (SQL.get(tableName, "BankMembers", "BankName", bankName) != null) {
                    Type type = new TypeToken<List<String>>() {
                    }.getType();
                    List<String> players = new Gson().fromJson((String) SQL.get(tableName, "BankMembers", "BankName", bankName), type);
                    if (players != null) {
                        players.remove(player.getName());
                    }
                    if (isOnlineMode()) {
                        SQL.updateData(tableName, "BankOwner", null, "Player = '" + player.getUniqueId() + "'");
                        SQL.updateData(tableName, "BankName", null, "Player = '" + player.getUniqueId() + "'");
                        SQL.updateData(tableName, "BankBalance", null, "Player = '" + player.getUniqueId() + "'");
                        SQL.updateData(tableName, "BankMembers", null, "Player = '" + player.getUniqueId() + "'");
                    } else {
                        SQL.updateData(tableName, "BankOwner", null, "Player = '" + player.getName() + "'");
                        SQL.updateData(tableName, "BankName", null, "Player = '" + player.getName() + "'");
                        SQL.updateData(tableName, "BankBalance", null, "Player = '" + player.getName() + "'");
                        SQL.updateData(tableName, "BankMembers", null, "Player = '" + player.getName() + "'");
                    }
                    if (players != null) {
                        members.addAll(players);
                    }
                    if (Main.getInstance().isMysql()) {
                        try {
                            ResultSet resultSet;
                            try (Statement statement = MySQL.getConnection().createStatement()) {
                                resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName ='" + bankName + "';");
                                while (resultSet.next()) {
                                    pls.add(resultSet.getString("Player"));
                                }
                            }
                        } catch (Exception ex) {
                            Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
                        }
                    } else if (Main.getInstance().isSQL()) {
                        try {
                            Statement statement = Objects.requireNonNull(SQLite.connect()).createStatement();
                            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName ='" + bankName + "';");
                            while (resultSet.next()) {
                                pls.add(resultSet.getString("Player"));
                            }
                        } catch (Exception ex) {
                            Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
                        }
                    }
                }
            }
            if (Main.getInstance().isMysql() || Main.getInstance().isSQL()) {
                if (SQL.isTableExists(tableName)) {
                    if (SQL.exists(tableName, "BankName", bankName)) {
                        if (SQL.get(tableName, "BankMembers", "BankName", bankName) != null) {
                            for (String players : pls) {
                                SQL.updateData(tableName, "BankMembers", new Gson().toJson(members), "Player = '" + players + "'");
                            }
                        }
                    }
                }
            }
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
        return null;
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
                        resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName IS NOT NULL");
                        while (resultSet.next()) {
                            banks.add(resultSet.getString("BankName"));
                        }
                    }
                } else if (Main.getInstance().isSQL()) {
                    Statement statement = Objects.requireNonNull(SQLite.connect()).createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE BankName IS NOT NULL");
                    while (resultSet.next()) {
                        banks.add(resultSet.getString("BankName"));
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
        if (SQL.isTableExists(tableName)) {
            if (getBanks().contains(bankName)) {
                List<String> members = getBankMembers(bankName);
                try {
                    if (Main.getInstance().isMysql()) {
                        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                            if (isBankOwner(bankName, player))
                                if (Bukkit.getOnlineMode()) {
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankName", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + player.getUniqueId() + "'");
                                } else {
                                    SQL.updateData(tableName, "BankName", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getName() + "'");
                                }
                        }
                        if (members != null) {
                            members.forEach(member -> {
                                removeBankMember(bankName, Bukkit.getOfflinePlayer(member));
                                if (Bukkit.getOnlineMode()) {
                                    SQL.updateData(tableName, "BankName", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                } else {
                                    SQL.updateData(tableName, "BankName", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                }
                            });
                        }
                        return true;
                    } else if (Main.getInstance().isSQL()) {
                        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                            if (isBankOwner(bankName, player))
                                if (Bukkit.getOnlineMode()) {
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankName", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + player.getUniqueId() + "'");
                                } else {
                                    SQL.updateData(tableName, "BankName", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + player.getName() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + player.getName() + "'");
                                }
                        }
                        if (members != null) {
                            members.forEach(member -> {
                                removeBankMember(bankName, Bukkit.getOfflinePlayer(member));
                                if (Bukkit.getOnlineMode()) {
                                    SQL.updateData(tableName, "BankName", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + Bukkit.getOfflinePlayer(member).getUniqueId() + "'");
                                } else {
                                    SQL.updateData(tableName, "BankName", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                    SQL.updateData(tableName, "BankBalance", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                    SQL.updateData(tableName, "BankOwner", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                    SQL.updateData(tableName, "BankMembers", null, "player = '" + Bukkit.getOfflinePlayer(member).getName() + "'");
                                }
                            });
                        }
                        return true;
                    }
                } catch (Exception ex) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Error", ex);
                }
            }
        }
        return false;
    }
}