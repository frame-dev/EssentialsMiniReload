package ch.framedev.essentialsmini.database.mysqlsqlite;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.mysql
 * Date: 08.03.21
 * Project: FrameEconomy
 * Copyrighted by FrameDev
 */

public class SQLite {

    private static final Logger LOGGER = LogManager.getLogger(SQLite.class);
    private static final Object lock = new Object();

    private static volatile Connection connection;
    private static String fileName;
    private static String path;
    private static boolean initialized = false;
    private static boolean driverLoaded = false;

    /**
     * Constructor to initialize SQLite with path and filename
     *
     * @param path The directory path where the database file will be stored
     * @param fileName The name of the database file (without .db extension)
     */
    public SQLite(String path, String fileName) {
        if (path == null || path.trim().isEmpty()) {
            LOGGER.error("SQLite path cannot be null or empty, using default 'plugins/EssentialsMini'");
            SQLite.path = "plugins/EssentialsMini";
        } else {
            SQLite.path = path;
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            LOGGER.error("SQLite fileName cannot be null or empty, using default 'database'");
            SQLite.fileName = "database";
        } else {
            SQLite.fileName = fileName;
        }

        // Ensure directory exists
        ensureDirectoryExists();

        // Load JDBC driver
        loadDriver();

        initialized = true;
        LOGGER.log(Level.INFO, "SQLite initialized with path: " + SQLite.path + "/" + SQLite.fileName + ".db");
    }

    /**
     * Load SQLite JDBC driver. Only needs to be done once.
     */
    private static void loadDriver() {
        if (driverLoaded) {
            return;
        }

        synchronized (lock) {
            if (driverLoaded) {
                return;
            }

            try {
                Class.forName("org.sqlite.JDBC");
                driverLoaded = true;
                LOGGER.log(Level.INFO, "SQLite JDBC driver loaded successfully");
            } catch (ClassNotFoundException e) {
                LOGGER.error("SQLite JDBC driver not found. Make sure sqlite-jdbc is in your dependencies.", e);
                driverLoaded = false;
            }
        }
    }

    /**
     * Ensure the directory for the database file exists
     */
    private static void ensureDirectoryExists() {
        if (path == null || path.trim().isEmpty()) {
            LOGGER.error("Cannot create directory: path is null or empty");
            return;
        }

        try {
            File directory = new File(path);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    LOGGER.log(Level.INFO, "Created SQLite database directory: " + path);
                } else {
                    LOGGER.error("Failed to create SQLite database directory: " + path);
                }
            }
        } catch (SecurityException e) {
            LOGGER.error("Security exception while creating directory: " + path, e);
        }
    }

    /**
     * Validate configuration before attempting connection
     *
     * @return true if configuration is valid
     */
    private static boolean validateConfiguration() {
        if (!initialized) {
            LOGGER.error("SQLite not initialized. Create an instance first.");
            return false;
        }

        if (path == null || path.trim().isEmpty()) {
            LOGGER.error("SQLite path is null or empty");
            return false;
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            LOGGER.error("SQLite fileName is null or empty");
            return false;
        }

        if (!driverLoaded) {
            LOGGER.error("SQLite JDBC driver not loaded");
            return false;
        }

        return true;
    }

    /**
     * Get a connection to the SQLite database. Thread-safe with connection reuse.
     *
     * @return A database connection, or null if connection fails
     */
    public static Connection connect() {
        // Validate configuration
        if (!validateConfiguration()) {
            LOGGER.error("SQLite configuration validation failed");
            return null;
        }

        synchronized (lock) {
            // Reuse existing connection if valid
            if (connection != null && isConnectionValid()) {
                return connection;
            }

            // Close old connection if it exists but is invalid
            if (connection != null) {
                closeQuietly(connection);
                connection = null;
            }

            // Create new connection
            try {
                String url = "jdbc:sqlite:" + path + "/" + fileName + ".db";

                // Set SQLite connection properties for better performance
                Properties properties = new Properties();
                properties.setProperty("journal_mode", "WAL"); // Write-Ahead Logging for better concurrency
                properties.setProperty("synchronous", "NORMAL"); // Balance between safety and performance
                properties.setProperty("cache_size", "10000"); // Cache size in pages
                properties.setProperty("temp_store", "MEMORY"); // Store temp tables in memory
                properties.setProperty("locking_mode", "NORMAL"); // Normal locking mode

                connection = DriverManager.getConnection(url, properties);

                // Enable foreign keys (disabled by default in SQLite)
                try (var stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }

                LOGGER.log(Level.INFO, "SQLite connection established successfully: " + url);
                return connection;

            } catch (SQLException e) {
                LOGGER.error("Error connecting to SQLite database at: " + path + "/" + fileName + ".db", e);
                connection = null;
                return null;
            }
        }
    }

    /**
     * Check if the connection is valid without throwing exceptions
     *
     * @return true if connection is valid
     */
    private static boolean isConnectionValid() {
        if (connection == null) {
            return false;
        }

        try {
            // Check if connection is closed
            if (connection.isClosed()) {
                return false;
            }

            // Validate with a timeout of 2 seconds
            return connection.isValid(2);

        } catch (SQLException e) {
            LOGGER.warn("Error checking connection validity", e);
            return false;
        }
    }

    /**
     * Close a connection quietly without throwing exceptions
     *
     * @param conn The connection to close
     */
    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.warn("Error closing connection quietly", e);
            }
        }
    }

    /**
     * Close the SQLite connection gracefully. Thread-safe.
     */
    public static void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                        LOGGER.log(Level.INFO, "SQLite connection closed successfully");
                    }
                } catch (SQLException ex) {
                    LOGGER.error("Error closing SQLite connection", ex);
                } finally {
                    connection = null;
                }
            }
        }
    }

    /**
     * Check if SQLite is connected
     *
     * @return true if connected and connection is valid
     */
    public static boolean isConnected() {
        return connection != null && isConnectionValid();
    }

    /**
     * Get the database file path
     *
     * @return The full path to the database file
     */
    public static String getDatabasePath() {
        if (path == null || fileName == null) {
            return null;
        }
        return path + "/" + fileName + ".db";
    }

    /**
     * Check if the database file exists
     *
     * @return true if the database file exists
     */
    public static boolean databaseFileExists() {
        String dbPath = getDatabasePath();
        if (dbPath == null) {
            return false;
        }

        File dbFile = new File(dbPath);
        return dbFile.exists() && dbFile.isFile();
    }

    /**
     * Get database file size in bytes
     *
     * @return File size in bytes, or -1 if file doesn't exist or error occurs
     */
    public static long getDatabaseFileSize() {
        String dbPath = getDatabasePath();
        if (dbPath == null) {
            return -1;
        }

        try {
            File dbFile = new File(dbPath);
            if (dbFile.exists() && dbFile.isFile()) {
                return dbFile.length();
            }
        } catch (SecurityException e) {
            LOGGER.warn("Security exception while getting database file size", e);
        }

        return -1;
    }

    /**
     * Reset the SQLite connection (close and clear state)
     */
    public static void reset() {
        synchronized (lock) {
            close();
            connection = null;
            initialized = false;
            LOGGER.log(Level.INFO, "SQLite connection reset");
        }
    }
}