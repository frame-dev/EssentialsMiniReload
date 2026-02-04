package ch.framedev.essentialsmini.database.mysqlsqlite;

import ch.framedev.essentialsmini.main.Main;
import org.apache.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class SQL {

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Callback interface for asynchronous operations.
     *
     * @param <T> The type of the result.
     */
    public interface Callback<T> {
        /**
         * Called when the operation is successful.
         *
         * @param value The result of the operation.
         */
        void accept(T value);

        /**
         * Called when an error occurs during the operation.
         *
         * @param t The Throwable that represents the error.
         */
        void onError(Throwable t);
    }

    public static void createTable(String tableName, String... columns) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }
        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Columns must not be null or empty");
        }

        String columnDefinition = String.join(",", columns);
        String sql;

        if (Main.getInstance().isMysql()) {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnDefinition + ", Numbers INT AUTO_INCREMENT PRIMARY KEY, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " + columnDefinition + ", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
        }

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to create table: " + tableName, e);
        }
    }

    public static void createTableAsync(String tableName, Callback<Boolean> callback, String... columns) {
        if (tableName == null || tableName.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (columns == null || columns.length == 0) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Columns must not be null or empty"));
            }
            return;
        }

        executor.execute(() -> {
            String columnDefinition = String.join(",", columns);
            String sql;

            if (Main.getInstance().isMysql()) {
                sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnDefinition + ", Numbers INT AUTO_INCREMENT PRIMARY KEY, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
            } else {
                sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " + columnDefinition + ", created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
            }

            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
                if (callback != null) {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static void insertData(String table, Object[] data, String... columns) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }
        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Columns must not be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (columns.length != data.length) {
            throw new IllegalArgumentException("Number of columns (" + columns.length + ") and data elements (" + data.length + ") must match.");
        }

        // Join the column names with commas
        String columnNames = String.join(",", columns);
        // Create a string of placeholders (e.g., "?, ?")
        String placeholders = String.join(",", Collections.nCopies(columns.length, "?"));
        // Construct the SQL insert statement
        String sql = "INSERT INTO " + table + " (" + columnNames + ") VALUES (" + placeholders + ")";

        // Try-with-resources ensures that resources are closed automatically
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Set the values for each placeholder
            for (int i = 0; i < data.length; i++) {
                stmt.setObject(i + 1, data[i]);
            }
            // Execute the statement
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log any SQL exceptions at the ERROR level
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to insert data into table: " + table, e);
        }
    }

    public static void insertDataAsync(String table, Callback<Boolean> callback, Object[] data, String... columns) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (columns == null || columns.length == 0) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Columns must not be null or empty"));
            }
            return;
        }
        if (data == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Data must not be null"));
            }
            return;
        }
        if (columns.length != data.length) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Number of columns (" + columns.length + ") and data elements (" + data.length + ") must match."));
            }
            return;
        }

        // Join the column names with commas
        String columnNames = String.join(",", columns);
        // Create a string of placeholders (e.g., "?, ?")
        String placeholders = String.join(",", Collections.nCopies(columns.length, "?"));
        // Construct the SQL insert statement
        String sql = "INSERT INTO " + table + " (" + columnNames + ") VALUES (" + placeholders + ")";

        executor.execute(() -> {
            // Try-with-resources ensures that resources are closed automatically
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Set the values for each placeholder
                for (int i = 0; i < data.length; i++) {
                    stmt.setObject(i + 1, data[i]);
                }
                // Execute the statement
                int update = stmt.executeUpdate();
                if (callback != null) {
                    callback.accept(update > 0);
                }
            } catch (SQLException e) {
                // Log any SQL exceptions at the ERROR level
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static void updateData(String table, String selected, @Nullable Object data, String whereClause, String... whereParams) {
        // Validate table name
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }

        // Validate selected column name
        if (selected == null || selected.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected column must not be null or empty");
        }

        // Validate WHERE clause
        if (whereClause == null || whereClause.trim().isEmpty()) {
            throw new IllegalArgumentException("WHERE clause must not be null or empty");
        }

        // Count placeholders in WHERE clause
        int placeholderCount = countPlaceholders(whereClause);
        if (placeholderCount != whereParams.length) {
            throw new IllegalArgumentException("Number of WHERE parameters does not match number of placeholders");
        }

        String sql = "UPDATE " + table + " SET " + selected + " = ? WHERE " + whereClause;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Set the data to update
            if (data != null) {
                stmt.setObject(1, data);
            } else {
                stmt.setNull(1, Types.NULL);
            }

            // Set the parameters for the WHERE clause starting at index 2
            for (int i = 0; i < placeholderCount; i++) {
                stmt.setString(i + 2, whereParams[i]);
            }

            // Execute the update
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                Main.getInstance().getLogger4J().log(Level.WARN, "No rows updated in table " + table + " with condition: " + whereClause);
            } else {
                Main.getInstance().getLogger4J().log(Level.INFO, "Updated " + affectedRows + " rows in table " + table + " with condition: " + whereClause);
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to execute update on table " + table + " with condition: " + whereClause, e);
        }
    }

    public static void updateDataAsync(String table, Callback<Boolean> callback, String selected, @Nullable Object data, String whereClause, String... whereParams) {
        // Validate table name
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }

        // Validate selected column name
        if (selected == null || selected.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected column must not be null or empty");
        }

        // Validate WHERE clause
        if (whereClause == null || whereClause.trim().isEmpty()) {
            throw new IllegalArgumentException("WHERE clause must not be null or empty");
        }

        // Count placeholders in WHERE clause
        int placeholderCount = countPlaceholders(whereClause);
        if (placeholderCount != whereParams.length) {
            throw new IllegalArgumentException("Number of WHERE parameters does not match number of placeholders");
        }

        String sql = "UPDATE " + table + " SET " + selected + " = ? WHERE " + whereClause;

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Set the data to update
                if (data != null) {
                    stmt.setObject(1, data);
                } else {
                    stmt.setNull(1, Types.NULL);
                }

                // Set the parameters for the WHERE clause starting at index 2
                for (int i = 0; i < placeholderCount; i++) {
                    stmt.setString(i + 2, whereParams[i]);
                }

                // Execute the update
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    callback.accept(false);
                } else {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                callback.onError(e);
            }
        });
    }

    public static void updateDataAsync(String table, Callback<Boolean> callback, Map<String, Object> selectedMap, String whereClause, String... whereParams) {
        // Validate table name
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }

        // Validate selectedMap
        if (selectedMap == null || selectedMap.isEmpty()) {
            throw new IllegalArgumentException("Selected map must not be null or empty");
        }

        // Validate WHERE clause
        if (whereClause == null || whereClause.trim().isEmpty()) {
            throw new IllegalArgumentException("WHERE clause must not be null or empty");
        }

        // Count placeholders in WHERE clause
        int placeholderCount = countPlaceholders(whereClause);
        if (placeholderCount != whereParams.length) {
            throw new IllegalArgumentException("Number of WHERE parameters does not match number of placeholders");
        }

        // Dynamically construct the SQL SET clause
        StringBuilder setClause = new StringBuilder();
        for (String column : selectedMap.keySet()) {
            if (!setClause.isEmpty()) {
                setClause.append(", ");
            }
            setClause.append(column).append(" = ?");
        }

        String sql = "UPDATE " + table + " SET " + setClause + " WHERE " + whereClause;

        // Execute the update asynchronously
        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramIndex = 1; // Start parameter index for PreparedStatement

                // Set values for the SET clause
                for (Object value : selectedMap.values()) {
                    stmt.setObject(paramIndex++, value);
                }

                // Set parameters for the WHERE clause
                for (String param : whereParams) {
                    stmt.setString(paramIndex++, param);
                }

                // Execute the update
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    callback.accept(false);
                } else {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to execute update on table " + table + " with condition: " + whereClause, e);
                callback.onError(e);
            }
        });
    }

    /**
     * Counts the number of ? placeholders in the given SQL clause.
     *
     * @param clause The SQL clause to inspect.
     * @return The number of ? placeholders.
     */
    private static int countPlaceholders(String clause) {
        return (int) clause.chars().filter(ch -> ch == '?').count();
    }


    public static void deleteDataInTable(String table, String where) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }
        if (where == null || where.trim().isEmpty()) {
            throw new IllegalArgumentException("WHERE clause must not be null or empty");
        }

        String sql = "DELETE FROM " + table + " WHERE " + where;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int affectedRows = stmt.executeUpdate();
            Main.getInstance().getLogger4J().log(Level.INFO, "Deleted " + affectedRows + " rows from table: " + table);
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to delete data from table: " + table, e);
        }
    }

    public static void deleteDataInTableAsync(String table, String where, Callback<Boolean> callback) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (where == null || where.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("WHERE clause must not be null or empty"));
            }
            return;
        }

        String sql = "DELETE FROM " + table + " WHERE " + where;

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                int update = stmt.executeUpdate();
                if (callback != null) {
                    callback.accept(update > 0);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static boolean exists(String table, String column, String data) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name must not be null or empty");
        }
        if (data == null) {
            return false;
        }

        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ? LIMIT 1";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to check existence in table: " + table, e);
            return false;
        }
    }

    public static void existsAsync(String table, String column, String data, Callback<Boolean> callback) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (column == null || column.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Column name must not be null or empty"));
            }
            return;
        }

        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ? LIMIT 1";

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, data);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (callback != null) {
                        callback.accept(rs.next());
                    }
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static Object get(String table, String selected, String column, String data) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }
        if (selected == null || selected.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected column must not be null or empty");
        }
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name must not be null or empty");
        }

        String sql = "SELECT " + selected + " FROM " + table + " WHERE " + column + " = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(selected);
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to get data from table: " + table, e);
        }
        return null;
    }

    public static void getAsync(String table, String selected, String column, String data, Callback<Object> callback) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (selected == null || selected.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Selected column must not be null or empty"));
            }
            return;
        }
        if (column == null || column.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Column name must not be null or empty"));
            }
            return;
        }

        String sql = "SELECT " + selected + " FROM " + table + " WHERE " + column + " = ?";

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, data);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && callback != null) {
                        callback.accept(rs.getObject(selected));
                    }
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static void getAsync(String table, String[] selected, String column, String data, Callback<List<Map<String, Object>>> callback) {
        // Validate input parameters
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (selected == null || selected.length == 0) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Selected columns must not be null or empty"));
            }
            return;
        }
        if (column == null || column.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Column name must not be null or empty"));
            }
            return;
        }
        if (data == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Data must not be null"));
            }
            return;
        }

        // Build the selected columns string
        String selectedColumns = String.join(", ", selected);

        // Build the SQL query
        String sql = "SELECT " + selectedColumns + " FROM " + table + " WHERE " + column + " = ?";

        // Execute the query asynchronously
        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, data);

                List<Map<String, Object>> results = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (String s : selected) {
                            row.put(s, rs.getObject(s));
                        }
                        results.add(row);
                    }
                    if (callback != null) {
                        callback.accept(results);
                    }
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static void deleteTable(String table) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }

        String sql = "DROP TABLE IF EXISTS " + table;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            Main.getInstance().getLogger4J().log(Level.INFO, "Dropped table: " + table);
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to drop table: " + table, e);
        }
    }

    public static void deleteTableAsync(String table, Callback<Boolean> callback) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }

        String sql = "DROP TABLE IF EXISTS " + table;

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
                if (callback != null) {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static boolean isTableExists(String table) {
        if (table == null || table.trim().isEmpty()) {
            return false;
        }

        String sql = Main.getInstance().isMysql() ?
                "SHOW TABLES LIKE ?" :
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, table);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to check table existence: " + table, e);
            return false;
        }
    }

    public static void isTableExistsAsync(String table, Callback<Boolean> callback) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.accept(false);
            }
            return;
        }

        String sql = Main.getInstance().isMysql() ?
                "SHOW TABLES LIKE ?" :
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?";

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, table);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (callback != null) {
                        callback.accept(rs.next());
                    }
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static <T> T get(String table, String selected, String column, String data, Class<T> type) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be null or empty");
        }
        if (selected == null || selected.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected column must not be null or empty");
        }
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name must not be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type class must not be null");
        }

        String sql = "SELECT " + selected + " FROM " + table + " WHERE " + column + " = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return type.cast(rs.getObject(selected));
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to get typed data from table: " + table, e);
        } catch (ClassCastException e) {
            Main.getInstance().getLogger4J().log(Level.ERROR, "Failed to cast result to type: " + type.getName(), e);
        }
        return null;
    }

    public static <T> void get(String table, String selected, String column, String data, Class<T> type, Callback<T> callback) {
        if (table == null || table.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Table name must not be null or empty"));
            }
            return;
        }
        if (selected == null || selected.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Selected column must not be null or empty"));
            }
            return;
        }
        if (column == null || column.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Column name must not be null or empty"));
            }
            return;
        }
        if (type == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Type class must not be null"));
            }
            return;
        }

        String sql = "SELECT " + selected + " FROM " + table + " WHERE " + column + " = ?";

        executor.execute(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, data);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && callback != null) {
                        callback.accept(type.cast(rs.getObject(selected)));
                    }
                }
            } catch (SQLException | ClassCastException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }


    public static Connection getConnection() throws SQLException {
        return Main.getInstance().isMysql() ? MySQL.getConnection() : SQLite.connect();
    }

    public static void getConnectionAsync(Callback<Connection> callback) {
        if (callback == null) {
            return;
        }

        executor.execute(() -> {
            try {
                callback.accept(Main.getInstance().isMysql() ? MySQL.getConnection() : SQLite.connect());
            } catch (SQLException e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Shuts down the executor service gracefully.
     * Should be called when the plugin is disabled to release resources.
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    Main.getInstance().getLogger4J().log(Level.ERROR, "Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}