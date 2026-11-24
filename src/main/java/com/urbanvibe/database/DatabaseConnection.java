package com.urbanvibe.database;

import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.utility.FileManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    public static final String CONFIG_PATH = "config/config.properties";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/urbanvibe_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String url = DEFAULT_URL;
    private static String user = "root";
    private static String password = "";
    private static Properties config = new Properties();

    private DatabaseConnection() { }

    public static synchronized void loadConfig() {
        config = FileManager.readProperties(CONFIG_PATH);
        url = pick("URBANVIBE_DB_URL", "db.url", DEFAULT_URL);
        user = pick("URBANVIBE_DB_USER", "db.user", "root");
        password = pick("URBANVIBE_DB_PASSWORD", "db.password", "");
    }

    private static String pick(String envName, String propName, String defaultValue) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv;
        }
        return config.getProperty(propName, defaultValue);
    }

    public static String getSetting(String envName, String propName, String defaultValue) {
        return pick(envName, propName, defaultValue);
    }

    public static Connection getConnection() throws DatabaseOperationException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseOperationException(
                    "Cannot connect to the database (" + e.getMessage() + "). Is MySQL running?", e);
        }
    }

    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con.isValid(3);
        } catch (DatabaseOperationException | SQLException e) {
            return false;
        }
    }

    public static void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static boolean isConstraintViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}