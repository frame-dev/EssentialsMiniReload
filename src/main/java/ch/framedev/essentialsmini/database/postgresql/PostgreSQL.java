package ch.framedev.essentialsmini.database.postgresql;

import ch.framedev.essentialsmini.main.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class PostgreSQL {

    private final String url;
    private final String user;
    private final String password;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // Driver not present on classpath - log and continue; connection attempts will fail later.
            try {
                Main.getInstance().getLogger4J().warn("PostgreSQL JDBC driver not found on classpath.", e);
            } catch (Throwable t) {
                // avoid hard failure during static init if Main isn't available
            }
        }
    }

    public static class Builder {

        private String url, user, password;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setUser(String user) {
            this.user = user;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public PostgreSQL build() {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(user, "user");
            Objects.requireNonNull(password, "password");
            return new PostgreSQL(url, user, password);
        }
    }

    public PostgreSQL(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private String getJdbcUrl() {
        // Expecting the caller to provide host/port/database part (e.g. "host:5432/database" or "host/database")
        if (url.startsWith("jdbc:")) return url;
        return "jdbc:postgresql://" + url;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(getJdbcUrl(), user, password);
        } catch (SQLException e) {
            try {
                Main.getInstance().getLogger4J().error("Could not connect to PostgreSQL database: " + e.getMessage(), e);
            } catch (Throwable t) {
                // swallow if logger isn't available
            }
            return null;
        }
    }

    public int executeUpdate(String sql, Object... params) {
        Connection conn = getConnection();
        if (conn == null) return -1;
        try (Connection c = conn; PreparedStatement ps = c.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            try {
                Main.getInstance().getLogger4J().error("SQL executeUpdate failed: " + e.getMessage(), e);
            } catch (Throwable t) {
                // ignore
            }
            return -1;
        }
    }

    public void closeQuietly(AutoCloseable ac) {
        if (ac == null) return;
        try {
            ac.close();
        } catch (Exception ignored) {
        }
    }
}