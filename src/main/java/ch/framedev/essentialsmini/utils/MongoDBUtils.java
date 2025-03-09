package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.database.mongodb.BackendManager;
import ch.framedev.essentialsmini.database.mongodb.MongoManager;
import ch.framedev.essentialsmini.main.Main;

import java.util.logging.Level;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.utils
 * ClassName MongoDbUtils
 * Date: 06.04.21
 * Project: Unknown
 * Copyrighted by FrameDev
 */

public class MongoDBUtils {

    private boolean mongoDb = false;
    private MongoManager mongoManager;
    private BackendManager backendManager;

    public MongoDBUtils(Main plugin) {
        /* MongoDB */
        if (plugin.getConfig().getBoolean("MongoDB.Boolean") || plugin.getConfig().getBoolean("MongoDB.LocalHost")) {
            this.mongoDb = true;
        }
        if (plugin.getConfig().getBoolean("MongoDB.Boolean") || plugin.getConfig().getBoolean("MongoDB.LocalHost")) {
            if (plugin.getConfig().getBoolean("MongoDB.LocalHost")) {
                this.mongoManager = new MongoManager();
                this.mongoManager.connectLocalHost();
                Main.getInstance().getLogger().log(Level.INFO, "MongoDB Enabled");
            }
            if (plugin.getConfig().getBoolean("MongoDB.Boolean")) {
                this.mongoManager = new MongoManager();
                this.mongoManager.connect();
                Main.getInstance().getLogger().log(Level.INFO, "MongoDB Enabled");
            }
            if (plugin.getConfig().getBoolean("MongoDB.LocalHost")) {
                this.backendManager = new BackendManager(Main.getInstance());
            }
            if (plugin.getConfig().getBoolean("MongoDB.Boolean")) {
                this.backendManager = new BackendManager(Main.getInstance());
            }
        }
    }

    public BackendManager getBackendManager() {
        return backendManager;
    }

    public MongoManager getMongoManager() {
        return mongoManager;
    }

    public boolean isMongoDb() {
        return mongoDb;
    }
}
