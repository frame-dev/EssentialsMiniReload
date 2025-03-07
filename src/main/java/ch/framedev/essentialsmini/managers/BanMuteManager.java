package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.commands.playercommands.BanCMD;
import ch.framedev.essentialsmini.commands.playercommands.MuteCMD;
import ch.framedev.essentialsmini.commands.playercommands.TempBanCMD;
import ch.framedev.essentialsmini.database.BackendManager;
import ch.framedev.essentialsmini.database.BackendManagerBanMute;
import ch.framedev.essentialsmini.database.SQL;
import ch.framedev.essentialsmini.main.Main;
import org.apache.log4j.Level;
import org.bson.Document;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.managers
 * ClassName BanMuteManager
 * Date: 03.06.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class BanMuteManager {

    // Database table for MySQL / SQLite
    private final String table = "essentialsmini_banmute";

    public BanMuteManager() {
        if (Main.getInstance().isMongoDB()) {
            for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if(!BackendManagerBanMute.getInstance(Main.getInstance()).existsUser(player)) {
                    BackendManagerBanMute.getInstance(Main.getInstance()).createUser(player);
                }
            }
        }
    }

    private void ensureTableExists() {
        SQL.isTableExistsAsync(table, new SQL.Callback<>() {
            @Override
            public void accept(Boolean exists) {
                if (!exists) {
                    Main.getInstance().getLogger4J().log(Level.INFO, "Table " + table + " does not exist. Creating...");
                    SQL.createTableAsync(
                            table,
                            new SQL.Callback<>() {
                                @Override
                                public void accept(Boolean created) {
                                    if (created) {
                                        Main.getInstance().getLogger4J().log(Level.INFO, "Successfully created table " + table);
                                    } else {
                                        Main.getInstance().getLogger4J().log(Level.WARN, "Table " + table + " creation failed.");
                                    }
                                }

                                @Override
                                public void onError(Throwable t) {
                                    Main.getInstance().getLogger4J().log(Level.ERROR, "Error while creating table: " + table, t);
                                }
                            },
                            "Player VARCHAR(255)",
                            "TempMute TEXT",
                            "TempMuteReason TEXT",
                            "TempBan TEXT",
                            "TempBanReason TEXT",
                            "Ban TEXT",
                            "BanReason TEXT"
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Error while checking table existence: " + table, t);
            }
        });

    }

    public void setTempMute(OfflinePlayer player, MuteCMD.MuteReason reason, String date) {
        setTempMute(player, reason.getReason(), date).thenRun(() -> {
            Main.getInstance().getLogger4J().log(Level.INFO, "TempMute for " + player.getName() + " set successfully.");
        });
    }

    public boolean isSQL() {
        return Main.getInstance().isMysql() || Main.getInstance().isSQL();
    }

    public CompletableFuture<Void> setTempMute(OfflinePlayer player, String reason, String date) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            SQL.existsAsync(table, "Player", playerName, new SQL.Callback<>() {
                @Override
                public void accept(Boolean exists) {
                    if (exists) {
                        Map<String, Object> selected = new HashMap<>();
                        selected.put("TempMute", date);
                        selected.put("TempMuteReason", reason);
                        SQL.updateDataAsync(table, new SQL.Callback<>() {
                            @Override
                            public void accept(Boolean value) {
                                if (value) {
                                    Main.getInstance().getLogger4J().log(Level.INFO, "TempMute for " + playerName + " set successfully.");
                                } else {
                                    Main.getInstance().getLogger4J().log(Level.WARN, "Failed to set tempmute for " + playerName + ".");
                                }
                                future.complete(null);
                            }

                            @Override
                            public void onError(Throwable t) {
                                Main.getInstance().getLogger4J().log(Level.ERROR, "Error while updating tempmute for " + playerName, t);
                                future.complete(null);
                            }
                        }, selected, "Player = ?", playerName);
                    } else {
                        SQL.insertDataAsync(table, new SQL.Callback<Boolean>() {
                            @Override
                            public void accept(Boolean value) {
                                if (value) {
                                    Main.getInstance().getLogger4J().log(Level.INFO, "TempMute for " + playerName + " set successfully.");
                                } else {
                                    Main.getInstance().getLogger4J().log(Level.WARN, "Failed to set tempmute for " + playerName + ".");
                                }
                                future.complete(null);
                            }

                            @Override
                            public void onError(Throwable t) {
                                Main.getInstance().getLogger4J().log(Level.ERROR, "Error while inserting tempmute for " + playerName, t);
                                future.complete(null);
                            }
                        }, new String[]{playerName, date, reason}, "Player", "TempMute", "TempMuteReason");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Error while checking existence of " + playerName, t);
                    future.complete(null);
                }
            });
        } else if (Main.getInstance().isMongoDB()) {
            // MongoDB implementation here
            BackendManagerBanMute.getInstance(Main.getInstance()).setTempMute(player, reason, date);
            future.complete(null);
        }
        return future;
    }

    public void removeTempMute(OfflinePlayer player) {
        String playerName = player.getName();
        if (playerName == null) return;

        if (isSQL()) {
            ensureTableExists();

            String tempMute = String.valueOf(SQL.get(table, "TempMute", "Player", playerName));
            if (SQL.exists(table, "Player", playerName) && tempMute != null && !tempMute.equalsIgnoreCase(" ")) {
                SQL.updateData(table, "TempMute", " ", "Player = ?", playerName);
                SQL.updateData(table, "TempMuteReason", " ", "Player = ?", playerName);
            }
        } else {
            // MongoDB implementation here
            BackendManagerBanMute.getInstance(Main.getInstance()).removeTempMute(player);
        }
    }

    public CompletableFuture<HashMap<String, String>> getTempMuteAsHash(OfflinePlayer player) {
        HashMap<String, String> hash = new HashMap<>();
        CompletableFuture<HashMap<String, String>> future = new CompletableFuture<>();

        getTempMute(player).thenAccept(stringStringMap -> {
            if (stringStringMap != null) {
                hash.put(stringStringMap.get("TempMute"), stringStringMap.get("TempMuteReason"));
            }
            future.complete(hash); // Complete the future with the populated hash map
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable); // Complete exceptionally on error
            return null;
        });
        return future;
    }


    public CompletableFuture<Map<String, String>> getTempMute(OfflinePlayer player) {
        CompletableFuture<Map<String, String>> resultFuture = new CompletableFuture<>();
        if (isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            SQL.existsAsync(table, "Player", playerName, new SQL.Callback<Boolean>() {

                @Override
                public void accept(Boolean exists) {
                    if (exists) {
                        SQL.getAsync(table, new String[]{"TempMute", "TempMuteReason"}, "Player", playerName, new SQL.Callback<>() {
                            @Override
                            public void accept(List<Map<String, Object>> value) {
                                if (!value.isEmpty()) {
                                    Map<String, Object> row = value.get(0);
                                    Map<String, String> tempMute = new HashMap<>();
                                    tempMute.put("TempMute", row.get("TempMute").toString());
                                    tempMute.put("TempMuteReason", row.get("TempMuteReason").toString());
                                    resultFuture.complete(tempMute);
                                } else {
                                    resultFuture.complete(null);
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                Main.getInstance().getLogger4J().error(t);
                                resultFuture.completeExceptionally(t);
                            }
                        });
                    } else {
                        resultFuture.complete(null);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Main.getInstance().getLogger4J().error(t);
                    resultFuture.completeExceptionally(t);
                }
            });
        } else {
            // MongoDB implementation here
            Optional<Document> tempMute = BackendManagerBanMute.getInstance(Main.getInstance()).getTempMute(player);
            if (tempMute.isPresent()) {
                Map<String, String> tempMuteMap = new HashMap<>();
                tempMuteMap.put("TempMute", tempMute.get().getString("expiresAt"));
                tempMuteMap.put("TempMuteReason", tempMute.get().getString("reason"));
                CompletableFuture.completedFuture(tempMuteMap);
            }
        }
        return resultFuture;
    }

    public boolean isTempMute(OfflinePlayer player) {
        if (isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            if (SQL.exists(table, "Player", playerName)) {
                String muteDate = (String) SQL.get(table, "TempMute", "Player", playerName);
                return muteDate != null && !muteDate.trim().isEmpty();
            }
        } else {
            // MongoDB implementation here
            Optional<Document> tempBan = BackendManagerBanMute.getInstance(Main.getInstance()).getTempBan(player);
            return tempBan.isPresent();
        }
        return false;
    }

    public void setTempBan(OfflinePlayer player, TempBanCMD.Ban reason, String date) {
        setTempBan(player, reason.getReason(), date);
    }

    public void setTempBan(OfflinePlayer player, String reason, String date) {
        if (isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            if (SQL.exists(table, "Player", playerName)) {
                SQL.updateData(table, "TempBan", date, "Player = ?", playerName);
                SQL.updateData(table, "TempBanReason", reason, "Player = ?", playerName);
            } else {
                SQL.insertData(table, new String[]{playerName, date, reason}, "Player", "TempBan", "TempBanReason");
            }
        } else {
            // MongoDB implementation here
            BackendManagerBanMute.getInstance(Main.getInstance()).setTempBan(player, reason, date);
        }
    }

    public void removeTempBan(OfflinePlayer player) {
        if (isSQL()) {
            String playerName = player.getName();
            if (playerName == null) return;

            ensureTableExists();

            if (SQL.exists(table, "Player", playerName)) {
                Bukkit.getServer().getBanList(BanList.Type.NAME).pardon(playerName);
                SQL.updateData(table, "TempBan", " ", "Player = ?", playerName);
                SQL.updateData(table, "TempBanReason", " ", "Player = ?", playerName);
            }
        } else {
            // MongoDB implementation here
            BackendManagerBanMute.getInstance(Main.getInstance()).removeTempBan(player);
        }
    }

    public Map<String, String> getTempBan(OfflinePlayer player) {
        if (isSQL()) {
            ensureTableExists();
            String playerName = player.getName();
            Map<String, String> tempBan = new HashMap<>();

            if (SQL.exists(table, "Player", playerName)) {
                String banDate = SQL.get(table, "TempBan", "Player", playerName, String.class);
                String reason = SQL.get(table, "TempBanReason", "Player", playerName, String.class);
                if (banDate != null && reason != null) {
                    tempBan.put(banDate, reason);
                    return tempBan;
                }
            }
        } else {
            // MongoDB implementation here
            Optional<Document> tempBan = BackendManagerBanMute.getInstance(Main.getInstance()).getTempBan(player);
            if (tempBan.isPresent()) {
                Map<String, String> tempBanMap = new HashMap<>();
                tempBanMap.put("TempBan", tempBan.get().getString("expiresAt"));
                tempBanMap.put("TempBanReason", tempBan.get().getString("reason"));
                return tempBanMap;
            }
        }
        return null;
    }

    public boolean isExpiredTempBan(OfflinePlayer player) {
        ensureTableExists();

        if (isTempBan(player)) {
            Date[] date = {null};
            getTempBan(player).forEach((s, s2) -> {
                try {
                    date[0] = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").parse(s);
                } catch (ParseException e) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to parse date: " + s, e);
                }
            });

            return date[0] != null && date[0].getTime() < System.currentTimeMillis();
        }

        return true;
    }

    public boolean isTempBan(OfflinePlayer player) {
        if(isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            if (SQL.exists(table, "Player", playerName)) {
                String banDate = (String) SQL.get(table, "TempBan", "Player", playerName);
                return banDate != null && !banDate.trim().isEmpty();
            }
        } else {
            // MongoDB implementation here
            Optional<Document> tempBan = BackendManagerBanMute.getInstance(Main.getInstance()).getTempBan(player);
            return tempBan.isPresent();
        }
        return false;
    }

    public void setPermBan(OfflinePlayer player, BanCMD.BanType reason, boolean permaBan) {
        setPermBan(player, reason.getReason(), permaBan);
    }

    public void setPermBan(OfflinePlayer player, String reason, boolean permaBan) {
        if(isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            if (SQL.exists(table, "Player", playerName)) {
                SQL.updateData(table, "Ban", String.valueOf(permaBan), "Player = ?", playerName);
                SQL.updateData(table, "BanReason", reason, "Player = ?", playerName);
            } else {
                SQL.insertData(table, new String[]{playerName, String.valueOf(permaBan), reason}, "Player", "Ban", "BanReason");
            }
        } else {
            // MongoDB implementation here
            if(permaBan)
                BackendManagerBanMute.getInstance(Main.getInstance()).setPermBan(player, reason);
            else
                BackendManagerBanMute.getInstance(Main.getInstance()).removePermBan(player);
        }
    }

    public boolean isPermBan(OfflinePlayer player) {
        if(isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            if (SQL.exists(table, "Player", playerName)) {
                String ban = (String) SQL.get(table, "Ban", "Player", playerName);
                return Boolean.parseBoolean(ban);
            }
        } else {
            // MongoDB implementation here
            return BackendManagerBanMute.getInstance(Main.getInstance()).isPermBan(player);
        }
        return false;
    }

    public String getPermBanReason(OfflinePlayer player) {
        if(isSQL()) {
            ensureTableExists();
            String playerName = player.getName();

            if (SQL.exists(table, "Player", playerName)) {
                return (String) SQL.get(table, "BanReason", "Player", playerName);
            }
        } else {
            // MongoDB implementation here
            return BackendManagerBanMute.getInstance(Main.getInstance()).getPermBan(player).orElse("");
        }
        return "";
    }

    public List<String> getAllBannedPlayers() {
        List<String> playerNames = new ArrayList<>();
        ensureTableExists();

        try (Connection conn = SQL.getConnection(); Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table)) {
            while (resultSet.next()) {
                String playerName = resultSet.getString("Player");
                if (playerName != null && isPermBan(Bukkit.getOfflinePlayer(playerName))) {
                    playerNames.add(playerName);
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to fetch all banned players", e);
        }

        return playerNames;
    }

    public List<String> getAllTempBannedPlayers() {
        List<String> playerNames = new ArrayList<>();
        if(isSQL()) {
            ensureTableExists();

            try (Connection conn = SQL.getConnection(); Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table)) {
                while (resultSet.next()) {
                    String playerName = resultSet.getString("Player");
                    if (playerName != null && isTempBan(Bukkit.getOfflinePlayer(playerName)) && !isExpiredTempBan(Bukkit.getOfflinePlayer(playerName))) {
                        playerNames.add(playerName);
                    }
                }
            } catch (SQLException e) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to fetch all temp banned players", e);
            }
        } else {
            // MongoDB implementation here
            return BackendManagerBanMute.getInstance(Main.getInstance()).getTempBannedUsers();
        }

        return playerNames;
    }
}