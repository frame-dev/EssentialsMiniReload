package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.commands.playercommands.BanCMD;
import ch.framedev.essentialsmini.commands.playercommands.MuteCMD;
import ch.framedev.essentialsmini.commands.playercommands.TempBanCMD;
import ch.framedev.essentialsmini.database.SQL;
import ch.framedev.essentialsmini.main.Main;
import org.apache.log4j.Level;
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

    public CompletableFuture<Void> setTempMute(OfflinePlayer player, String reason, String date) {
        ensureTableExists();
        String playerName = player.getName();
        CompletableFuture<Void> future = new CompletableFuture<>();

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
        return future;
    }

    public void removeTempMute(OfflinePlayer player) {
        String playerName = player.getName();
        if (playerName == null) return;

        ensureTableExists();

        String tempMute = String.valueOf(SQL.get(table, "TempMute","Player", playerName));
        if (SQL.exists(table, "Player", playerName) && tempMute != null && !tempMute.equalsIgnoreCase(" ")) {
            SQL.updateData(table, "TempMute", " ", "Player = ?", playerName);
            SQL.updateData(table, "TempMuteReason", " ", "Player = ?", playerName);
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
        ensureTableExists();
        String playerName = player.getName();
        Map<String, String> tempMute = new HashMap<>();

        CompletableFuture<Map<String, String>> resultFuture = new CompletableFuture<>();
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
                            }
                            else {
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
        return resultFuture;
    }

    public boolean isTempMute(OfflinePlayer player) {
        ensureTableExists();
        String playerName = player.getName();

        if (SQL.exists(table, "Player", playerName)) {
            String muteDate = (String) SQL.get(table, "TempMute", "Player", playerName);
            return muteDate != null && !muteDate.trim().isEmpty();
        }
        return false;
    }

    public void setTempBan(OfflinePlayer player, TempBanCMD.Ban reason, String date) {
        setTempBan(player, reason.getReason(), date);
    }

    public void setTempBan(OfflinePlayer player, String reason, String date) {
        ensureTableExists();
        String playerName = player.getName();

        if (SQL.exists(table, "Player", playerName)) {
            SQL.updateData(table, "TempBan", date, "Player = ?", playerName);
            SQL.updateData(table, "TempBanReason", reason, "Player = ?", playerName);
        } else {
            SQL.insertData(table, new String[]{playerName, date, reason}, "Player", "TempBan", "TempBanReason");
        }
    }

    public void removeTempBan(OfflinePlayer player) {
        String playerName = player.getName();
        if (playerName == null) return;

        ensureTableExists();

        if (SQL.exists(table, "Player", playerName)) {
            Bukkit.getServer().getBanList(BanList.Type.NAME).pardon(playerName);
            SQL.updateData(table, "TempBan", " ", "Player = ?", playerName);
            SQL.updateData(table, "TempBanReason", " ", "Player = ?", playerName);
        }
    }

    public Map<String, String> getTempBan(OfflinePlayer player) {
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
        ensureTableExists();
        String playerName = player.getName();

        if (SQL.exists(table, "Player", playerName)) {
            String banDate = (String) SQL.get(table, "TempBan", "Player", playerName);
            return banDate != null && !banDate.trim().isEmpty();
        }
        return false;
    }

    public void setPermBan(OfflinePlayer player, BanCMD.BanType reason, boolean permaBan) {
        setPermBan(player, reason.getReason(), permaBan);
    }

    public void setPermBan(OfflinePlayer player, String reason, boolean permaBan) {
        ensureTableExists();
        String playerName = player.getName();

        if (SQL.exists(table, "Player", playerName)) {
            SQL.updateData(table, "Ban", String.valueOf(permaBan), "Player = ?", playerName);
            SQL.updateData(table, "BanReason", reason, "Player = ?", playerName);
        } else {
            SQL.insertData(table, new String[]{playerName, String.valueOf(permaBan), reason}, "Player", "Ban", "BanReason");
        }
    }

    public boolean isPermBan(OfflinePlayer player) {
        ensureTableExists();
        String playerName = player.getName();

        if (SQL.exists(table, "Player", playerName)) {
            String ban = (String) SQL.get(table, "Ban", "Player", playerName);
            return Boolean.parseBoolean(ban);
        }
        return false;
    }

    public String getPermBanReason(OfflinePlayer player) {
        ensureTableExists();
        String playerName = player.getName();

        if (SQL.exists(table, "Player", playerName)) {
            return (String) SQL.get(table, "BanReason", "Player", playerName);
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

        return playerNames;
    }
}