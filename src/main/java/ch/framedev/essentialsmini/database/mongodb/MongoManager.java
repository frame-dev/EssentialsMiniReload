package ch.framedev.essentialsmini.database.mongodb;

import ch.framedev.essentialsmini.main.Main;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.List;

public class MongoManager {

    private final String databasestring;
    private final String username;
    private final String password;
    private final String hostname;
    private final int port;
    private MongoClient client;
    private MongoDatabase database;

    public MongoManager() {
        // Safely retrieve config values with null checks
        this.databasestring = Main.getInstance().getConfig().getString("MongoDB.Database", "database");
        this.username = Main.getInstance().getConfig().getString("MongoDB.User", "username");
        this.password = Main.getInstance().getConfig().getString("MongoDB.Password");
        this.hostname = Main.getInstance().getConfig().getString("MongoDB.Host", "localhost");
        this.port = Main.getInstance().getConfig().getInt("MongoDB.Port", 27017);
    }

    public void connectLocalHost() {
        if (hostname == null || hostname.isEmpty()) {
            throw new IllegalStateException("MongoDB hostname is not configured");
        }

        this.client = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(List.of(new ServerAddress(hostname, port))))
                        .build());
        this.database = this.client.getDatabase(databasestring);
    }

    public void connect() {
        if (hostname == null || hostname.isEmpty()) {
            throw new IllegalStateException("MongoDB hostname is not configured");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("MongoDB username is not configured");
        }
        if (databasestring == null || databasestring.isEmpty()) {
            throw new IllegalStateException("MongoDB database name is not configured");
        }

        MongoCredential credential;
        if (password != null && !password.isEmpty()) {
            credential = MongoCredential.createCredential(username, databasestring, password.toCharArray());
        } else {
            credential = MongoCredential.createCredential(username, databasestring, new char[0]);
        }

        this.client = MongoClients.create(
                MongoClientSettings.builder()
                        .credential(credential)
                        .applyToClusterSettings(builder ->
                                builder.hosts(List.of(new ServerAddress(hostname, port))))
                        .build());
        this.database = this.client.getDatabase(databasestring);
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database is not initialized. Call connect() or connectLocalHost() first.");
        }
        return database;
    }

    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
            database = null;
        }
    }

    public boolean isConnected() {
        return client != null && database != null;
    }
}