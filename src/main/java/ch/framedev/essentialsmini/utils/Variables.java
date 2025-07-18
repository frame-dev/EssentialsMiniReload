package ch.framedev.essentialsmini.utils;


/*
 * de.framedev.essentialsmini.utils
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 25.08.2020 20:09
 */

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

public class Variables {

    transient private final Main instance;
    private final String prefix;
    private final String onlyPlayer;
    private final String permissionBase;
    private final List<String> authors;
    private final String version;
    private final boolean onlineMode;
    private final boolean jsonFormat;
    private String playerNameNotOnline;
    private String playerNotOnline;

    private final List<OfflinePlayer> mutedPlayers;

    public static final String TP_MESSAGES = "TpaMessages";
    public static final String MONEY_MESSAGE = "Money";
    public static final String WARP_MESSAGE = "Warp";
    public static final String EXPERIENCE = "Experience";
    public static final String BANK = "Bank";

    public Variables() {
        this.instance = Main.getInstance();
        this.prefix = instance.getPrefix();
        this.onlyPlayer = instance.getOnlyPlayer();
        this.permissionBase = instance.getPermissionBase();
        this.authors = instance.getDescription().getAuthors();
        this.version = instance.getDescription().getVersion();
        this.onlineMode = instance.getConfig().getBoolean("OnlineMode");
        this.jsonFormat = instance.getConfig().getBoolean("JsonFormat");
        this.playerNameNotOnline = instance.getLanguageConfig(null).getString("PlayerNameNotOnline");
        this.playerNotOnline = instance.getLanguageConfig(null).getString("PlayerNotOnline");
        this.mutedPlayers = new ArrayList<>();
    }

    public String getPlayerNotOnline() {
        playerNotOnline = ReplaceCharConfig.replaceParagraph(playerNotOnline);
        return playerNotOnline;
    }

    public String getPlayerNameNotOnline(String playerName) {
        if (playerNameNotOnline.contains("&"))
            playerNameNotOnline = playerNameNotOnline.replace('&', '§');
        if (playerNameNotOnline.contains("%Player%"))
            playerNameNotOnline = playerNameNotOnline.replace("%Player%", playerName);
        return playerNameNotOnline;
    }

    public boolean isJsonFormat() {
        return jsonFormat;
    }

    public String getWrongArgs(String cmdName) {
        return instance.getWrongArgs(cmdName);
    }

    public String getVersion() {
        return version;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getPermissionBase() {
        return permissionBase;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getOnlyPlayer() {
        return onlyPlayer;
    }

    public List<OfflinePlayer> getMutedPlayers() {
        return mutedPlayers;
    }
}



