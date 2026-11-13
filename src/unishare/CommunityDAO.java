package unishare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only community rankings calculated from existing UniShare records. */
public final class CommunityDAO {
    public List<Object[]> topContributors() throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, COUNT(r.id) AS score "
                + "FROM users u JOIN resources r ON r.owner_id = u.user_id "
                + "GROUP BY u.user_id, u.full_name "
                + "ORDER BY score DESC, u.full_name ASC LIMIT 20";
        return query(sql, "score");
    }

    public List<Object[]> topLenders() throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, COUNT(h.id) AS score "
                + "FROM users u JOIN borrowing_history h ON h.owner_id = u.user_id "
                + "GROUP BY u.user_id, u.full_name "
                + "ORDER BY score DESC, u.full_name ASC LIMIT 20";
        return query(sql, "score");
    }

    public List<Object[]> reliableBorrowers() throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, "
                + "SUM(CASE WHEN h.returned_on IS NOT NULL AND h.returned_on <= h.due_date THEN 1 ELSE 0 END) AS on_time, "
                + "SUM(CASE WHEN h.returned_on IS NOT NULL THEN 1 ELSE 0 END) AS returned "
                + "FROM users u JOIN borrowing_history h ON h.borrower_id = u.user_id "
                + "GROUP BY u.user_id, u.full_name "
                + "HAVING returned > 0 "
                + "ORDER BY (on_time / returned) DESC, returned DESC, u.full_name ASC LIMIT 20";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int onTime = rs.getInt("on_time");
                int returned = rs.getInt("returned");
                int percent = returned == 0 ? 0 : Math.round(onTime * 100f / returned);
                rows.add(new Object[]{rs.getString("user_id"), rs.getString("full_name"),
                        onTime, returned, percent + "%", reliabilityTier(percent, returned)});
            }
        }
        return rows;
    }

    private List<Object[]> query(String sql, String scoreColumn) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int score = rs.getInt(scoreColumn);
                rows.add(new Object[]{rs.getString("user_id"), rs.getString("full_name"),
                        score, tier(score, score)});
            }
        }
        return rows;
    }

    private String reliabilityTier(int percent, int returned) {
        if (returned >= 20 && percent >= 95) return "Grandmaster";
        if (returned >= 10 && percent >= 90) return "Master";
        if (returned >= 5 && percent >= 80) return "Expert";
        return "Rising Star";
    }

    private String tier(int score, int activity) {
        if (activity >= 20 && score >= 20) return "Grandmaster";
        if (activity >= 10 && score >= 10) return "Master";
        if (activity >= 5 && score >= 5) return "Expert";
        if (activity >= 1) return "Rising Star";
        return "Newcomer";
    }
}
