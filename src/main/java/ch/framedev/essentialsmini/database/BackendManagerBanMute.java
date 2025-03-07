package ch.framedev.essentialsmini.database;

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
import java.util.stream.Collectors;

public class BackendManagerBanMute {

    private static BackendManagerBanMute instance;
    private final String COLLECTION = "essentialsmini_banmute";
    private final Main plugin;

    private BackendManagerBanMute(Main plugin) {
        this.plugin = plugin;
        ensureCollectionExists();
    }

    public static BackendManagerBanMute getInstance(Main plugin) {
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
        if (!plugin.getDatabaseManager().getMongoManager().getDatabase()
                .listCollectionNames().into(new ArrayList<>()).contains(COLLECTION)) {
            plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(COLLECTION);
        }
    }

    public void createUser(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("username", player.getName());

        plugin.getDatabaseManager().getMongoManager().getDatabase()
                .getCollection(COLLECTION)
                .replaceOne(new Document("uuid", uuid), new Document(map), new ReplaceOptions().upsert(true));
    }

    public boolean existsUser(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return plugin.getDatabaseManager().getMongoManager().getDatabase()
                .getCollection(COLLECTION)
                .find(new Document("uuid", uuid))
                .first() != null;
    }

    public void removeUser(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        plugin.getDatabaseManager().getMongoManager().getDatabase()
                .getCollection(COLLECTION)
                .deleteOne(new Document("uuid", uuid));
    }

    public void setTempBan(OfflinePlayer player, String reason, String date) {
        String uuid = player.getUniqueId().toString();
        Document banData = new Document()
                .append("reason", reason)
                .append("expiresAt", date);

        plugin.getDatabaseManager().getMongoManager().getDatabase()
                .getCollection(COLLECTION)
                .updateOne(new Document("uuid", uuid),
                        new Document("$set", new Document("tempban", banData)),
                        new UpdateOptions().upsert(true));
    }

    public void removeTempBan(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        plugin.getDatabaseManager().getMongoManager().getDatabase()
               .getCollection(COLLECTION)
               .updateOne(new Document("uuid", uuid),
                        new Document("$unset", new Document("tempban", "")));
    }

    public Optional<Document> getTempBan(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return Optional.ofNullable(plugin.getDatabaseManager().getMongoManager().getDatabase()
                .getCollection(COLLECTION)
                .find(new Document("uuid", uuid))
                .map(doc -> doc.get("tempban", Document.class)).first());
    }

    public boolean isTempBan(OfflinePlayer player) {
        return getTempBan(player).isPresent();
    }

    public void setPermBan(OfflinePlayer player, String reason) {
        String uuid = player.getUniqueId().toString();
        plugin.getDatabaseManager().getMongoManager().getDatabase()
               .getCollection(COLLECTION)
               .updateOne(new Document("uuid", uuid),
                        new Document("$set", new Document("permban", reason)),
                        new UpdateOptions().upsert(true));
    }

    public void removePermBan(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        plugin.getDatabaseManager().getMongoManager().getDatabase()
               .getCollection(COLLECTION)
               .updateOne(new Document("uuid", uuid),
                        new Document("$unset", new Document("permban", "")));
    }

    public Optional<String> getPermBan(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return Optional.ofNullable(plugin.getDatabaseManager().getMongoManager().getDatabase()
               .getCollection(COLLECTION)
               .find(new Document("uuid", uuid))
               .map(doc -> doc.getString("permban")).first());
    }

    public boolean isPermBan(OfflinePlayer player) {
        return getPermBan(player).isPresent();
    }


    public List<String> getTempBannedUsers() {
        List<String> bannedUsers = new ArrayList<>();

        // Get the collection
        MongoCollection<Document> collection = plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(COLLECTION);

        // Query to find users who have a temp ban set
        Document query = new Document("tempban", new Document("$exists", true));

        try (MongoCursor<Document> cursor = collection.find(query).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String username = doc.getString("username"); // Assuming "username" is stored in the document
                bannedUsers.add(username);
            }
        }

        return bannedUsers;
    }

    public void setTempMute(OfflinePlayer player, String reason, String date) {
        String uuid = player.getUniqueId().toString();
        Document banData = new Document()
               .append("reason", reason)
               .append("expiresAt", date);

        plugin.getDatabaseManager().getMongoManager().getDatabase()
               .getCollection(COLLECTION)
               .updateOne(new Document("uuid", uuid),
                        new Document("$set", new Document("tempmute", banData)),
                        new UpdateOptions().upsert(true));
    }

    public void removeTempMute(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        plugin.getDatabaseManager().getMongoManager().getDatabase()
               .getCollection(COLLECTION)
               .updateOne(new Document("uuid", uuid),
                        new Document("$unset", new Document("tempmute", "")));
    }

    public Optional<Document> getTempMute(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return Optional.ofNullable(plugin.getDatabaseManager().getMongoManager().getDatabase()
                .getCollection(COLLECTION)
                .find(new Document("uuid", uuid))
                .map(doc -> doc.get("tempmute", Document.class)).first());
    }

    public boolean isTempMute(OfflinePlayer player) {
        return getTempMute(player).isPresent();
    }
}