package ch.framedev.essentialsmini.database.mongodb;

import ch.framedev.essentialsmini.main.Main;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertOneOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.database
 * Date: 07.03.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

@SuppressWarnings("unused")
public record BackendManager(Main plugin) {
    public interface Callback<T> {
        void onResult(T result);

        void onError(Exception exception);
    }

    public enum DATA {
        NAME("name"),
        MONEY("money"),
        BANKNAME("bankname"),
        BANKMEMBERS("bankmembers"),
        BANKOWNER("bankowner"),
        BANK("bank"),
        CREATEDATE("createDate"),
        LASTLOGIN("lastLogin"),
        LASTLOGOUT("lastLogout");

        private final String name;

        DATA(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Creating the Document of the User
     *
     * @param player     the Player
     * @param collection Collection in the Database
     * @param callback   Callback for result
     */
    public void createUser(OfflinePlayer player, String collection, Callback<Void> callback) {
        if (player == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Player cannot be null"));
            }
            return;
        }
        if (collection == null || collection.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Collection name cannot be null or empty"));
            }
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                Document result = collections.find(new Document("uuid", uuid)).first();
                if (result == null) {
                    Document dc = (new Document("uuid", uuid))
                            .append("name", player.getName() != null ? player.getName() : "Unknown")
                            .append("money", 0.0)
                            .append("bankname", "")
                            .append("bankmembers", new ArrayList<String>())
                            .append("bankowner", "")
                            .append("bank", 0.0)
                            .append("createDate", System.currentTimeMillis())
                            .append("lastLogin", 0L)
                            .append("lastLogout", 0L);
                    collections.insertOne(dc, (new InsertOneOptions()).bypassDocumentValidation(false));
                }
                if (callback != null) {
                    callback.onResult(null);
                }
            } else {
                this.plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(collection);
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                Document result = collections.find(new Document("uuid", uuid)).first();
                if (result == null) {
                    Document dc = (new Document("uuid", uuid))
                            .append("name", player.getName() != null ? player.getName() : "Unknown")
                            .append("money", 0.0)
                            .append("bankname", "")
                            .append("bankmembers", new ArrayList<String>())
                            .append("bankowner", "")
                            .append("bank", 0.0)
                            .append("createDate", System.currentTimeMillis())
                            .append("lastLogin", 0L)
                            .append("lastLogout", 0L);
                    collections.insertOne(dc, (new InsertOneOptions()).bypassDocumentValidation(false));
                }
                if (callback != null) {
                    callback.onResult(null);
                }
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    /**
     *
     * @param where      from the Database Document
     * @param data       Data in where
     * @param selected   the Selected key in your Database
     * @param collection the Collection in your Database
     * @return data from Database
     */
    public Object getObject(String where, Object data, String selected, String collection) {
        if (where == null || where.isEmpty()) {
            return null;
        }
        if (selected == null || selected.isEmpty()) {
            return null;
        }
        if (collection == null || collection.isEmpty()) {
            return null;
        }

        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return null;
                }
                Document document = collections.find(new Document(where, data)).first();
                if (document != null) {
                    return document.get(selected);
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting object from MongoDB", e);
        }
        return null;
    }

    public void updateData(String where, Object data, String selected, Object dataSelected, String collection) {
        if (where == null || where.isEmpty() || selected == null || selected.isEmpty() || collection == null || collection.isEmpty()) {
            return;
        }

        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document(where, data)).first();
                if (document != null) {
                    Document document1 = new Document(selected, dataSelected);
                    Document document2 = new Document("$set", document1);
                    if (document.get(where) != null) {
                        collections.updateOne(document, document2);
                    } else {
                        document.put(selected, dataSelected);
                        Document foundDoc = collections.find(new Document(where, data)).first();
                        if (foundDoc != null) {
                            collections.updateOne(foundDoc, document);
                        }
                    }
                }
            } else {
                this.plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(collection);
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document(where, data)).first();
                if (document != null) {
                    Document document1 = new Document(selected, dataSelected);
                    Document document2 = new Document("$set", document1);
                    collections.updateOne(document, document2);
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error updating data in MongoDB", e);
        }
    }

    public void updateUser(OfflinePlayer player, String where, Object data, String collection) {
        if (player == null || where == null || where.isEmpty() || collection == null || collection.isEmpty()) {
            return;
        }

        try {
            if (existsCollection(collection)) {
                String uuid = player.getUniqueId().toString();
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                Document document = collections.find(new Document("uuid", uuid)).first();
                if (document != null) {
                    Document document1 = new Document(where, data);
                    Document document2 = new Document("$set", document1);
                    if (document.get(where) != null) {
                        collections.updateOne(document, document2);
                    } else {
                        document.put(where, data);
                        Document foundDoc = collections.find(new Document("uuid", uuid)).first();
                        if (foundDoc != null) {
                            collections.updateOne(foundDoc, document);
                        }
                    }
                }
            } else {
                String uuid = player.getUniqueId().toString();
                this.plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(collection);
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document("uuid", uuid)).first();
                if (document != null) {
                    Document document1 = new Document(where, data);
                    Document document2 = new Document("$set", document1);
                    collections.updateOne(document, document2);
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error updating user in MongoDB", e);
        }
    }

    public boolean exists(String where, Object data, String whereSelected, String collection) {
        if (where == null || where.isEmpty() || whereSelected == null || whereSelected.isEmpty() || collection == null || collection.isEmpty()) {
            return false;
        }

        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return false;
                }
                Document document = collections.find(new Document(where, data)).first();
                if (document != null) {
                    return document.get(whereSelected) != null;
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error checking existence in MongoDB", e);
        }
        return false;
    }

    public boolean exists(OfflinePlayer player, String where, String collection) {
        if (player == null || where == null || where.isEmpty() || collection == null || collection.isEmpty()) {
            return false;
        }

        try {
            if (existsCollection(collection)) {
                String uuid = player.getUniqueId().toString();
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return false;
                }
                Document document = collections.find(new Document("uuid", uuid)).first();
                if (document != null) {
                    return document.get(where) != null;
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error checking player existence in MongoDB", e);
        }
        return false;
    }

    public void insertData(OfflinePlayer player, String where, Object data, String collection) {
        if (player == null || where == null || where.isEmpty() || collection == null || collection.isEmpty()) {
            return;
        }

        try {
            if (existsCollection(collection)) {
                String uuid = player.getUniqueId().toString();
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document("uuid", uuid)).first();
                if (document != null) {
                    collections.updateOne(new Document("uuid", uuid),
                            new Document("$set", new Document(where, data)));
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error inserting data in MongoDB", e);
        }
    }

    public void insertData(String where, Object data, String newKey, Object newValue, String collection) {
        if (where == null || where.isEmpty() || newKey == null || newKey.isEmpty() || collection == null || collection.isEmpty()) {
            return;
        }

        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document(where, data)).first();
                if (document != null) {
                    collections.updateOne(new Document(where, data),
                            new Document("$set", new Document(newKey, newValue)));
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error inserting data in MongoDB", e);
        }
    }

    public void deleteUser(OfflinePlayer player, String collection) {
        if (player == null || collection == null || collection.isEmpty()) {
            return;
        }

        try {
            String uuid = player.getUniqueId().toString();
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document("uuid", uuid)).first();
                if (document != null) {
                    collections.deleteOne(document);
                }
            } else {
                this.plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(collection);
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return;
                }
                Document document = collections.find(new Document("uuid", uuid)).first();
                if (document != null) {
                    collections.deleteOne(document);
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error deleting user in MongoDB", e);
        }
    }

    public Object get(OfflinePlayer player, String where, String collection) {
        if (player == null || where == null || where.isEmpty() || collection == null || collection.isEmpty()) {
            return null;
        }

        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> mongoCollection = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (mongoCollection == null) {
                    return null;
                }
                String str = player.getUniqueId().toString();
                Document document1 = mongoCollection.find(new Document("uuid", str)).first();
                if (document1 != null) {
                    return document1.get(where);
                }
                return null;
            }
            this.plugin.getDatabaseManager().getMongoManager().getDatabase().createCollection(collection);
            MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
            if (collections == null) {
                return null;
            }
            String uuid = player.getUniqueId().toString();
            collections.insertOne(new Document());
            Document document = collections.find(new Document("uuid", uuid)).first();
            if (document != null) {
                return document.get(where);
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting data from MongoDB", e);
        }
        return null;
    }


    @SuppressWarnings("ConstantValue")
    public boolean existsCollection(String collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        try {
            MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
            return collections != null;
        } catch (Exception e) {
            plugin.getLogger4J().error("Error checking collection existence in MongoDB", e);
            return false;
        }
    }


    public ArrayList<OfflinePlayer> getOfflinePlayers(String collection) {
        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<OfflinePlayer> players = new ArrayList<>();
        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return players;
                }
                collections.find(new Document("offline", true)).forEach(document -> {
                    if (document != null && document.getString("uuid") != null) {
                        try {
                            UUID uuid = UUID.fromString(document.getString("uuid"));
                            players.add(Bukkit.getOfflinePlayer(uuid));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger4J().error("Invalid UUID in document", e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting offline players from MongoDB", e);
        }
        return players;
    }

    public List<Object> getList(String where, Object data, String selected, String collection) {
        if (where == null || where.isEmpty() || selected == null || selected.isEmpty() || collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Object> items = new ArrayList<>();
        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return items;
                }
                collections.find(new Document(where, data)).forEach(document -> {
                    if (document != null) {
                        Object value = document.get(selected);
                        if (value != null) {
                            items.add(value);
                        }
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting list from MongoDB", e);
        }
        return items;
    }

    public List<Document> getAllDocuments(String collection) {
        List<Document> list = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return list;
        }

        try {
            if (existsCollection(collection)) {
                MongoCollection<Document> collections = this.plugin.getDatabaseManager().getMongoManager().getDatabase().getCollection(collection);
                if (collections == null) {
                    return list;
                }
                FindIterable<Document> find = collections.find();
                if (find != null) {
                    for (Document document : find) {
                        if (document != null) {
                            list.add(document);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger4J().error("Error getting all documents from MongoDB", e);
        }
        return list;
    }
}