package ch.framedev.essentialsmini.database.mongodb;

/*
 * ch.framedev.essentialsmini.database
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 07.03.2025 23:14
 */

import ch.framedev.essentialsmini.main.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bukkit.OfflinePlayer;

import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;

import java.util.*;

@SuppressWarnings("unused")
public class BackendManagerBanMute {

    private static volatile BackendManagerBanMute instance;
    private final String COLLECTION = "essentialsmini_banmute";
    private final Main plugin;

    private BackendManagerBanMute(Main plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        ensureCollectionExists();
    }

    public static BackendManagerBanMute getInstance(Main plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (instance == null) {
            synchronized (BackendManagerBanMute.class) {
                if (instance == null) {
                    instance = new BackendManagerBanMute(plugin);
                }
            }
        }
        return instance;
    }

    private void ensureCollectionExists() {
        try {
            if (plugin.getDatabaseManager() == null || plugin.getDatabaseManager().getMongoManager() == null) {
                plugin.getLogger4J().error("DatabaseManager or MongoManager is not initialized");
                return;
            }
            if (plugin.getDatabaseManager().getMongoManager().getDatabase() == null) {
                plugin.getLogger4J().error("MongoDB database is not initialized");
                return;
            }

            if (!plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .listCollectionNames().into(new ArrayList<>()).contains(COLLECTION)) {
                plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(COLLECTION);
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error ensuring collection exists", e);
        }
    }

    public void createUser(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot create user: player is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", uuid);
            map.put("username", player.getName() != null ? player.getName() : "Unknown");

            plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .getCollection(COLLECTION)
                    .replaceOne(new Document("uuid", uuid), new Document(map), new ReplaceOptions().upsert(true));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error creating user in MongoDB", e);
        }
    }

    public boolean existsUser(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot check user existence: player is null");
            return false;
        }

        try {
            String uuid = player.getUniqueId().toString();
            return plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .getCollection(COLLECTION)
                    .find(new Document("uuid", uuid))
                    .first() != null;
        } catch (Exception e) {
            plugin.getLogger4J().error("Error checking user existence in MongoDB", e);
            return false;
        }
    }

    public void removeUser(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot remove user: player is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .getCollection(COLLECTION)
                    .deleteOne(new Document("uuid", uuid));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error removing user from MongoDB", e);
        }
    }

    public void setTempBan(OfflinePlayer player, String reason, String date) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot set temp ban: player is null");
            return;
        }
        if (reason == null) {
            reason = "No reason specified";
        }
        if (date == null) {
            plugin.getLogger4J().error("Cannot set temp ban: date is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            Document banData = new Document()
                    .append("reason", reason)
                    .append("expiresAt", date);

            plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .getCollection(COLLECTION)
                    .updateOne(new Document("uuid", uuid),
                            new Document("$set", new Document("tempban", banData)),
                            new UpdateOptions().upsert(true));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error setting temp ban in MongoDB", e);
        }
    }

    public void removeTempBan(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot remove temp ban: player is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            plugin.getDatabaseManager().getMongoManager().getDatabase()
                   .getCollection(COLLECTION)
                   .updateOne(new Document("uuid", uuid),
                            new Document("$unset", new Document("tempban", "")));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error removing temp ban from MongoDB", e);
        }
    }

    public Optional<Document> getTempBan(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot get temp ban: player is null");
            return Optional.empty();
        }

        try {
            String uuid = player.getUniqueId().toString();
            return Optional.ofNullable(plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .getCollection(COLLECTION)
                    .find(new Document("uuid", uuid))
                    .map(doc -> doc.get("tempban", Document.class)).first());
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting temp ban from MongoDB", e);
            return Optional.empty();
        }
    }

    public boolean isTempBan(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        return getTempBan(player).isPresent();
    }

    public void setPermBan(OfflinePlayer player, String reason) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot set perm ban: player is null");
            return;
        }
        if (reason == null) {
            reason = "No reason specified";
        }

        try {
            String uuid = player.getUniqueId().toString();
            plugin.getDatabaseManager().getMongoManager().getDatabase()
                   .getCollection(COLLECTION)
                   .updateOne(new Document("uuid", uuid),
                            new Document("$set", new Document("permban", reason)),
                            new UpdateOptions().upsert(true));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error setting perm ban in MongoDB", e);
        }
    }

    public void removePermBan(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot remove perm ban: player is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            plugin.getDatabaseManager().getMongoManager().getDatabase()
                   .getCollection(COLLECTION)
                   .updateOne(new Document("uuid", uuid),
                            new Document("$unset", new Document("permban", "")));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error removing perm ban from MongoDB", e);
        }
    }

    public Optional<String> getPermBan(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot get perm ban: player is null");
            return Optional.empty();
        }

        try {
            String uuid = player.getUniqueId().toString();
            return Optional.ofNullable(plugin.getDatabaseManager().getMongoManager().getDatabase()
                   .getCollection(COLLECTION)
                   .find(new Document("uuid", uuid))
                   .map(doc -> doc.getString("permban")).first());
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting perm ban from MongoDB", e);
            return Optional.empty();
        }
    }

    public boolean isPermBan(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        return getPermBan(player).isPresent();
    }


    public List<String> getTempBannedUsers() {
        List<String> bannedUsers = new ArrayList<>();

        try {
            // Get the collection
            MongoCollection<Document> collection = plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(COLLECTION);
            if (collection == null) {
                plugin.getLogger4J().error("Collection is null in getTempBannedUsers");
                return bannedUsers;
            }

            // Query to find users who have a temp ban set
            Document query = new Document("tempban", new Document("$exists", true));

            try (MongoCursor<Document> cursor = collection.find(query).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    if (doc != null) {
                        String username = doc.getString("username");
                        if (username != null && !username.isEmpty()) {
                            bannedUsers.add(username);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting temp banned users from MongoDB", e);
        }

        return bannedUsers;
    }

    public void setTempMute(OfflinePlayer player, String reason, String date) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot set temp mute: player is null");
            return;
        }
        if (reason == null) {
            reason = "No reason specified";
        }
        if (date == null) {
            plugin.getLogger4J().error("Cannot set temp mute: date is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            Document banData = new Document()
                   .append("reason", reason)
                   .append("expiresAt", date);

            plugin.getDatabaseManager().getMongoManager().getDatabase()
                   .getCollection(COLLECTION)
                   .updateOne(new Document("uuid", uuid),
                            new Document("$set", new Document("tempmute", banData)),
                            new UpdateOptions().upsert(true));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error setting temp mute in MongoDB", e);
        }
    }

    public void removeTempMute(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot remove temp mute: player is null");
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            plugin.getDatabaseManager().getMongoManager().getDatabase()
                   .getCollection(COLLECTION)
                   .updateOne(new Document("uuid", uuid),
                            new Document("$unset", new Document("tempmute", "")));
        } catch (Exception e) {
            plugin.getLogger4J().error("Error removing temp mute from MongoDB", e);
        }
    }

    public Optional<Document> getTempMute(OfflinePlayer player) {
        if (player == null) {
            plugin.getLogger4J().error("Cannot get temp mute: player is null");
            return Optional.empty();
        }

        try {
            String uuid = player.getUniqueId().toString();
            return Optional.ofNullable(plugin.getDatabaseManager().getMongoManager().getDatabase()
                    .getCollection(COLLECTION)
                    .find(new Document("uuid", uuid))
                    .map(doc -> doc.get("tempmute", Document.class)).first());
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting temp mute from MongoDB", e);
            return Optional.empty();
        }
    }

    public boolean isTempMute(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        return getTempMute(player).isPresent();
    }

    public List<String> getAllBannedPlayers() {
        List<String> bannedUsers = new ArrayList<>();

        try {
            // Get the collection
            MongoCollection<Document> collection = plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(COLLECTION);
            if (collection == null) {
                plugin.getLogger4J().error("Collection is null in getAllBannedPlayers");
                return bannedUsers;
            }

            // Query to find users who have a perm ban set
            Document query = new Document("permban", new Document("$exists", true));

            try (MongoCursor<Document> cursor = collection.find(query).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    if (doc != null) {
                        String username = doc.getString("username");
                        if (username != null && !username.isEmpty()) {
                            bannedUsers.add(username);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting all banned players from MongoDB", e);
        }

        return bannedUsers;
    }
}