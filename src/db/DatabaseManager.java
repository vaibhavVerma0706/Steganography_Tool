package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    // ── Change PASSWORD to your actual MySQL password ──
    private static final String URL      = "jdbc:mysql://localhost:3306/steganography_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "India@1947"; // ← change this!

    // ── Load driver once when class is first used ──
    static {
        try {
            // Explicitly load the MySQL driver
            // This fixes "No suitable driver found" error
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL Driver loaded successfully.");
        } catch (ClassNotFoundException ex) {
            System.err.println("❌ MySQL Driver not found: " + ex.getMessage());
            System.err.println("   → Make sure mysql-connector-j jar is added to project.");
        }
    }

    // ──────────────────────────────────────────────────────
    // getConnection()
    // ──────────────────────────────────────────────────────
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // ──────────────────────────────────────────────────────
    // testConnection()
    // ──────────────────────────────────────────────────────
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException ex) {
            System.err.println("DB connection failed: " + ex.getMessage());
            return false;
        }
    }
}