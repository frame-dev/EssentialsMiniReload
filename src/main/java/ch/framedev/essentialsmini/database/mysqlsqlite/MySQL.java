package ch.framedev.essentialsmini.database.mysqlsqlite;

import ch.framedev.essentialsmini.main.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;


public class MySQL {

    public static String host;
    public static String user;
    public static String password;
    public static String database;
    public static String port;
    public static Connection con;
    private static HikariDataSource hikariDataSource;

    public MySQL() {
        FileConfiguration cfg = Main.getInstance().getConfig();
        host = cfg.getString("MySQL.Host");
        user = cfg.getString("MySQL.User");
        password = cfg.getString("MySQL.Password");
        database = cfg.getString("MySQL.Database");
        port = cfg.getString("MySQL.Port");
    }

    public static Connection getConnection() throws SQLException {
        if (hikariDataSource == null) {
            connect();
        }
        return hikariDataSource.getConnection();
    }

    // connect
    public static void connect() {
        if (hikariDataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(user);
            config.setPassword(password);
            config.setIdleTimeout(600000); // 60 seconds
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(30000); // 30 seconds
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(5);

            hikariDataSource = new HikariDataSource(config);
        }
    }

    public static void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();  // Properly close the pool
        }
    }
}