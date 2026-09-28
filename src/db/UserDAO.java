package db;

import model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // ──────────────────────────────────────────────────────
    // hashPassword()
    // Converts plain text password to SHA-256 hash string
    // Example: "hello" → "2cf24dba5fb0a30e26e83b2ac5b9e29e..."
    // ──────────────────────────────────────────────────────
    private static String hashPassword(String password) {
        try {
            MessageDigest md     = MessageDigest.getInstance("SHA-256");
            byte[]        hashed = md.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb     = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 not available: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // register()
    // Creates a new user account in the database
    // Returns:
    //   "ok"       = registered successfully
    //   "taken"    = username already exists
    //   "error"    = database error
    // ──────────────────────────────────────────────────────
    public static String register(String username, String password) {
        if (username == null || username.trim().isEmpty()) return "error";
        if (password == null || password.isEmpty())        return "error";

        if (usernameExists(username.trim())) return "taken";

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ps.setString(2, hashPassword(password));
            ps.executeUpdate();
            return "ok";

        } catch (SQLException ex) {
            System.err.println("Register error: " + ex.getMessage());
            return "error";
        }
    }

    // ──────────────────────────────────────────────────────
    // login()
    // Validates username + password
    // Returns User object if correct, null if wrong
    // ──────────────────────────────────────────────────────
    public static User login(String username, String password) {
        if (username == null || password == null) return null;

        String sql = "SELECT id, username, password, created_at "
                + "FROM users WHERE username = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash  = rs.getString("password");
                String enteredHash = hashPassword(password);

                if (storedHash.equals(enteredHash)) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("created_at")
                    );
                }
            }
            return null; // wrong credentials

        } catch (SQLException ex) {
            System.err.println("Login error: " + ex.getMessage());
            return null;
        }
    }

    // ──────────────────────────────────────────────────────
    // usernameExists()
    // Returns true if username is already registered
    // ──────────────────────────────────────────────────────
    private static boolean usernameExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException ex) {
            System.err.println("Username check error: " + ex.getMessage());
            return false;
        }
    }
}