package ch.framedev.essentialsmini.database;

import ch.framedev.essentialsmini.main.Main;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;

public class MongoManager {

    String databasestring = Main.getInstance().getConfig().getString("MongoDB.Database");
    String username = Main.getInstance().getConfig().getString("MongoDB.User");
    String password = Main.getInstance().getConfig().getString("MongoDB.Password");
    private String hostname = Main.getInstance().getConfig().getString("MongoDB.Host");
    private int port = Main.getInstance().getConfig().getInt("MongoDB.Port");
    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> players;

    public MongoManager() {

    }

    public void connectLocalHost() {
        this.client = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(hostname, port))))
                        .build());
        this.database = this.client.getDatabase(databasestring);
    }

    public void connect() {
        MongoCredential credential = MongoCredential.createCredential(username, databasestring, password.toCharArray());
        this.client = MongoClients.create(
                MongoClientSettings.builder()
                        .credential(credential)
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(hostname, port))))
                        .build());
        this.database = this.client.getDatabase(databasestring);
    }


    public MongoClient getClient() {
        return client;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}