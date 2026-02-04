package ch.framedev.essentialsmini.database.mysqlsqlite;

import ch.framedev.essentialsmini.main.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.Level;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;


public class MySQL {

    private static String host;
    private static String user;
    private static String password;
    private static String database;
    private static String port;
    private static volatile HikariDataSource hikariDataSource;
    private static final Object lock = new Object();
    private static boolean initialized = false;

    public MySQL() {
        initialize();
    }

    /**
     * Initialize MySQL connection parameters from config
     */
    private static void initialize() {
        if (initialized) {
            return;
        }

        try {
            FileConfiguration cfg = Main.getInstance().getConfig();
            if (cfg == null) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Config is null, cannot initialize MySQL");
                return;
            }

            host = cfg.getString("MySQL.Host", "localhost");
            user = cfg.getString("MySQL.User", "root");
            password = cfg.getString("MySQL.Password", "");
            database = cfg.getString("MySQL.Database", "database");
            port = cfg.getString("MySQL.Port", "3306");

            // Validate required fields
            if (host == null || host.trim().isEmpty()) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL host is not configured");
                host = "localhost";
            }
            if (user == null || user.trim().isEmpty()) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL user is not configured");
                user = "root";
            }
            if (database == null || database.trim().isEmpty()) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL database is not configured");
                database = "database";
            }
            if (port == null || port.trim().isEmpty()) {
                Main.getInstance().getLogger4J().log(Level.WARN, "MySQL port is not configured, using default 3306");
                port = "3306";
            }

            initialized = true;
            Main.getInstance().getLogger4J().log(Level.INFO, "MySQL configuration initialized successfully");
        } catch (Exception e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to initialize MySQL configuration", e);
        }
    }

    /**
     * Get a connection from the pool. Thread-safe with double-checked locking.
     *
     * @return A database connection
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        if (hikariDataSource == null) {
            synchronized (lock) {
                if (hikariDataSource == null) {
                    connect();
                }
            }
        }

        if (hikariDataSource == null || hikariDataSource.isClosed()) {
            throw new SQLException("HikariCP datasource is not available");
        }

        return hikariDataSource.getConnection();
    }

    /**
     * Establish connection pool. Thread-safe.
     */
    public static void connect() {
        synchronized (lock) {
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                Main.getInstance().getLogger4J().log(Level.WARN, "MySQL connection pool already exists");
                return;
            }

            // Ensure configuration is initialized
            if (!initialized) {
                initialize();
            }

            // Validate configuration before connecting
            if (!validateConfiguration()) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL configuration validation failed, cannot connect");
                return;
            }

            try {
                HikariConfig config = new HikariConfig();

                // Build JDBC URL with null checks
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

                config.setJdbcUrl(jdbcUrl);
                config.setUsername(user);
                config.setPassword(password != null ? password : "");

                // Connection pool settings
                config.setIdleTimeout(600000); // 10 minutes
                config.setMaxLifetime(1800000); // 30 minutes
                config.setConnectionTimeout(30000); // 30 seconds
                config.setMaximumPoolSize(15);
                config.setMinimumIdle(5);

                // Additional performance settings
                config.setLeakDetectionThreshold(60000); // 60 seconds - detect connection leaks
                config.setConnectionTestQuery("SELECT 1");
                config.setPoolName("EssentialsMini-MySQL-Pool");

                // Additional HikariCP properties for better performance
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");

                hikariDataSource = new HikariDataSource(config);
                Main.getInstance().getLogger4J().log(Level.INFO, "MySQL connection pool established successfully");
            } catch (Exception e) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to establish MySQL connection pool", e);
                hikariDataSource = null;
            }
        }
    }

    /**
     * Validate MySQL configuration
     *
     * @return true if configuration is valid
     */
    private static boolean validateConfiguration() {
        if (host == null || host.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL host is null or empty");
            return false;
        }
        if (user == null || user.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL user is null or empty");
            return false;
        }
        if (database == null || database.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL database is null or empty");
            return false;
        }
        if (port == null || port.trim().isEmpty()) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL port is null or empty");
            return false;
        }

        // Validate port is a number
        try {
            int portNum = Integer.parseInt(port);
            if (portNum < 1 || portNum > 65535) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL port is out of valid range (1-65535): " + portNum);
                return false;
            }
        } catch (NumberFormatException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "MySQL port is not a valid number: " + port);
            return false;
        }

        return true;
    }

    /**
     * Close the connection pool gracefully. Thread-safe.
     */
    public static void close() {
        synchronized (lock) {
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                try {
                    hikariDataSource.close();
                    Main.getInstance().getLogger4J().log(Level.INFO, "MySQL connection pool closed successfully");
                } catch (Exception e) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Error closing MySQL connection pool", e);
                } finally {
                    hikariDataSource = null;
                    initialized = false;
                }
            }
        }
    }

    /**
     * Check if the connection pool is active
     *
     * @return true if pool is active and not closed
     */
    public static boolean isConnected() {
        return hikariDataSource != null && !hikariDataSource.isClosed();
    }

    /**
     * Get connection pool statistics for monitoring
     *
     * @return Statistics string, or error message if pool is not available
     */
    public static String getPoolStats() {
        if (hikariDataSource == null || hikariDataSource.isClosed()) {
            return "Connection pool is not active";
        }

        try {
            return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
                    hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                    hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                    hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                    hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        } catch (Exception e) {
            return "Error getting pool stats: " + e.getMessage();
        }
    }
}