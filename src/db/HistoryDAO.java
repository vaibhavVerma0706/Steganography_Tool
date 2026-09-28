package db;

import model.HistoryRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryDAO {

    // ──────────────────────────────────────────────────────
    // saveRecord()
    // Saves one encode or decode operation to database
    // Called automatically after every successful encode/decode
    // ──────────────────────────────────────────────────────
    public static void saveRecord(int userId, String action,
                                  String imageName, long imageSize,
                                  String hiddenType, String hiddenName) {
        String sql = "INSERT INTO encode_history "
                + "(user_id, action, image_name, image_size, hidden_type, hidden_name) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setInt(1,    userId);
            ps.setString(2, action);
            ps.setString(3, imageName  != null ? imageName  : "");
            ps.setLong(4,   imageSize);
            ps.setString(5, hiddenType != null ? hiddenType : "");
            ps.setString(6, hiddenName != null ? hiddenName : "");
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("Save history error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // getHistory()
    // Returns all history records for one user
    // Sorted by newest first
    // ──────────────────────────────────────────────────────
    public static List<HistoryRecord> getHistory(int userId) {
        List<HistoryRecord> list = new ArrayList<>();

        String sql = "SELECT id, user_id, action, image_name, image_size, "
                + "hidden_type, hidden_name, timestamp "
                + "FROM encode_history "
                + "WHERE user_id = ? "
                + "ORDER BY timestamp DESC";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new HistoryRecord(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("action"),
                        rs.getString("image_name"),
                        rs.getLong("image_size"),
                        rs.getString("hidden_type"),
                        rs.getString("hidden_name"),
                        rs.getString("timestamp")
                ));
            }

        } catch (SQLException ex) {
            System.err.println("Get history error: " + ex.getMessage());
        }
        return list;
    }

    // ──────────────────────────────────────────────────────
    // deleteRecord()
    // Deletes one specific history record by its ID
    // ──────────────────────────────────────────────────────
    public static void deleteRecord(int recordId) {
        String sql = "DELETE FROM encode_history WHERE id = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Delete record error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // clearAllHistory()
    // Deletes ALL history records for one user
    // ──────────────────────────────────────────────────────
    public static void clearAllHistory(int userId) {
        String sql = "DELETE FROM encode_history WHERE user_id = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Clear history error: " + ex.getMessage());
        }
    }
}