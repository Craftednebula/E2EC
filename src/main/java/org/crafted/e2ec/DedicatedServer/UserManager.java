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
        // connect to SQLite database
        // input : path to database file
        // output: none
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initialize();
    }
    
    private void initialize() throws SQLException {
        // sql witchcraft
        // create users table if not exists
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
    public User getUser(String username) {
        // get user by username on the database
        // input: username string
        // output: User object or null if not found
        String sql = "SELECT username, permission_level FROM users WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;

            return new User(
                rs.getString("username"),
                rs.getInt("permission_level")
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    public boolean setPermissionLevel(String username, int newLevel) {
        // set a user's permission level in the database
        // input: username and new permission level
        // output: true if successful, false if not
        String sql = "UPDATE users SET permission_level = ? WHERE username = ?";

         try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newLevel);
            stmt.setString(2, username);

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean register(String username, String password, int level) throws Exception {
        // more sql witchcraft
        // register a new user in the database
        // input: username, password, and permission level
        // output: true if successful, false if username already exists
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
        // login a user by checking credentials in the database
        // input: username and password
        // output: User object if successful, null if invalid credentials
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
        // hash a string using SHA-256
        // input: string to hash
        // output: hashed string in hex format
        // why is this here? i dont think this is used at all
        // also why is it here instead of in a utils class?
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static class User {
        // simple user data class
        //:thumbs-up:
        public String username;
        public int permissionLevel;
        public User(String username, int level) {
            this.username = username;
            this.permissionLevel = level;
        }
    }
}
