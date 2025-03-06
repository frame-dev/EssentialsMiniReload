package ch.framedev.essentialsmini.database;



/*
 * ch.framedev.essentialsmini.database
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 06.03.2025 12:49
 */

import ch.framedev.essentialsmini.api.VaultAPI;
import ch.framedev.essentialsmini.main.Main;

public class DatabaseManager {

    private final Main plugin;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        // MySQL Database
        if (plugin.isMysql()) {
            new MySQL();
        }

        // SQLite Database
        if (plugin.isSQL()) {
            new SQLite(plugin.getConfig().getString("SQLite.Path", "path"), plugin.getConfig().getString("SQLite.FileName", "database"));
        }

        if(plugin.isMongoDB()) {
            VaultAPI.init();
        }
    }

    public BackendManager getBackendManager() {
        return plugin.getMongoDBUtils().getBackendManager();
    }

    public MongoManager getMongoManager() {
        return plugin.getMongoDBUtils().getMongoManager();
    }
}
