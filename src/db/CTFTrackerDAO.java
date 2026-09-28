package db;

import model.CTFChallenge;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CTFTrackerDAO {

    // ──────────────────────────────────────────────────────
    // addChallenge()
    // Adds a new CTF challenge to track
    // Returns true if saved successfully
    // ──────────────────────────────────────────────────────
    public static boolean addChallenge(int userId,
                                       String name,
                                       String imageName) {
        String sql = "INSERT INTO ctf_challenges "
                + "(user_id, name, image_name, status) "
                + "VALUES (?, ?, ?, 'UNSOLVED')";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setInt(1,    userId);
            ps.setString(2, name      != null ? name      : "");
            ps.setString(3, imageName != null ? imageName : "");
            ps.executeUpdate();
            return true;

        } catch (SQLException ex) {
            System.err.println("Add challenge error: " + ex.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────────────
    // getChallenges()
    // Returns all CTF challenges for one user
    // Sorted by newest first
    // ──────────────────────────────────────────────────────
    public static List<CTFChallenge> getChallenges(int userId) {
        List<CTFChallenge> list = new ArrayList<>();

        String sql = "SELECT id, user_id, name, image_name, status, "
                + "flag_found, notes, created_at, solved_at "
                + "FROM ctf_challenges "
                + "WHERE user_id = ? "
                + "ORDER BY created_at DESC";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new CTFChallenge(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("image_name"),
                        rs.getString("status"),
                        rs.getString("flag_found"),
                        rs.getString("notes"),
                        rs.getString("created_at"),
                        rs.getString("solved_at")
                ));
            }

        } catch (SQLException ex) {
            System.err.println("Get challenges error: " + ex.getMessage());
        }
        return list;
    }

    // ──────────────────────────────────────────────────────
    // updateStatus()
    // Changes challenge status: UNSOLVED / IN PROGRESS / SOLVED
    // Automatically sets solved_at when status = SOLVED
    // ──────────────────────────────────────────────────────
    public static void updateStatus(int challengeId, String status) {
        String sql;
        if ("SOLVED".equals(status)) {
            sql = "UPDATE ctf_challenges "
                    + "SET status = ?, solved_at = NOW() "
                    + "WHERE id = ?";
        } else {
            sql = "UPDATE ctf_challenges "
                    + "SET status = ? "
                    + "WHERE id = ?";
        }

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2,    challengeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Update status error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // saveFlag()
    // Saves the found flag and marks challenge as SOLVED
    // ──────────────────────────────────────────────────────
    public static void saveFlag(int challengeId, String flag) {
        String sql = "UPDATE ctf_challenges "
                + "SET flag_found = ?, status = 'SOLVED', solved_at = NOW() "
                + "WHERE id = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setString(1, flag != null ? flag : "");
            ps.setInt(2,    challengeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Save flag error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // saveNotes()
    // Updates the notes field for a challenge
    // ──────────────────────────────────────────────────────
    public static void saveNotes(int challengeId, String notes) {
        String sql = "UPDATE ctf_challenges SET notes = ? WHERE id = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setString(1, notes != null ? notes : "");
            ps.setInt(2,    challengeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Save notes error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // deleteChallenge()
    // Permanently removes a challenge from database
    // ──────────────────────────────────────────────────────
    public static void deleteChallenge(int challengeId) {
        String sql = "DELETE FROM ctf_challenges WHERE id = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {
            ps.setInt(1, challengeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Delete challenge error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // getStats()
    // Returns int[3]: [total, solved, unsolved]
    // ──────────────────────────────────────────────────────
    public static int[] getStats(int userId) {
        int[] stats = {0, 0, 0};

        String sql = "SELECT "
                + "COUNT(*) AS total, "
                + "SUM(CASE WHEN status = 'SOLVED' THEN 1 ELSE 0 END) AS solved "
                + "FROM ctf_challenges WHERE user_id = ?";

        try (Connection       con = DatabaseManager.getConnection();
             PreparedStatement ps  = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                stats[0] = rs.getInt("total");
                stats[1] = rs.getInt("solved");
                stats[2] = stats[0] - stats[1];
            }

        } catch (SQLException ex) {
            System.err.println("Get stats error: " + ex.getMessage());
        }
        return stats;
    }
}