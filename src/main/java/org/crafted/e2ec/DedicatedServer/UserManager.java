package org.crafted.e2ec.DedicatedServer;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserManager {

    private Connection conn;

    public UserManager(String dbPath) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initialize();
    }

    private void initialize() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE,
                    password_hash TEXT,
                    permission_level INTEGER,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """);
        }
    }

    public boolean register(String username, String password, int level) throws Exception {
        String hash = sha256(password);
        String sql = "INSERT INTO users(username, password_hash, permission_level) VALUES(?,?,?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setInt(3, level);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // username already exists
        }
    }

    public User login(String username, String password) throws Exception {
        String hash = sha256(password);
        String sql = "SELECT * FROM users WHERE username=? AND password_hash=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hash);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int level = rs.getInt("permission_level");
                return new User(username, level);
            } else {
                return null; // invalid credentials
            }
        }
    }

    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static class User {
        public String username;
        public int permissionLevel;
        public User(String username, int level) {
            this.username = username;
            this.permissionLevel = level;
        }
    }
}
