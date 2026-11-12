package unishare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Calculates Hall of Fame scores from verified resource and borrowing history. */
public final class HallOfFameDAO {

    public static final class Entry {
        public final String userId;
        public final String fullName;
        public final String department;
        public final int resourcesShared;
        public final int successfulLends;
        public final int onTimeReturns;
        public final int lateReturns;
        public final int knowledgeShares;
        public final int points;

        Entry(String userId, String fullName, String department, int resourcesShared,
              int successfulLends, int onTimeReturns, int lateReturns,
              int knowledgeShares, int points) {
            this.userId = userId;
            this.fullName = fullName;
            this.department = department == null ? "" : department;
            this.resourcesShared = resourcesShared;
            this.successfulLends = successfulLends;
            this.onTimeReturns = onTimeReturns;
            this.lateReturns = lateReturns;
            this.knowledgeShares = knowledgeShares;
            this.points = points;
        }
    }

    /**
     * Points:
     * 10 per currently listed resource, 20 per completed lending,
     * 15 per on-time return, 5 per late return and 5 bonus per
     * currently listed book or study-notes resource.
     *
     * Aggregate subqueries prevent joins from multiplying counts.
     */
    public List<Entry> leaderboard() throws SQLException {
        String sql =
            "SELECT u.user_id, u.full_name, u.department, " +
            "COALESCE(r.resources_shared, 0) AS resources_shared, " +
            "COALESCE(l.successful_lends, 0) AS successful_lends, " +
            "COALESCE(b.on_time_returns, 0) AS on_time_returns, " +
            "COALESCE(b.late_returns, 0) AS late_returns, " +
            "COALESCE(k.knowledge_shares, 0) AS knowledge_shares, " +
            "(COALESCE(r.resources_shared, 0) * 10 + " +
            " COALESCE(l.successful_lends, 0) * 20 + " +
            " COALESCE(b.on_time_returns, 0) * 15 + " +
            " COALESCE(b.late_returns, 0) * 5 + " +
            " COALESCE(k.knowledge_shares, 0) * 5) AS points " +
            "FROM users u " +
            "LEFT JOIN (SELECT owner_id, COUNT(*) AS resources_shared " +
            "           FROM resources GROUP BY owner_id) r ON r.owner_id = u.user_id " +
            "LEFT JOIN (SELECT owner_id, COUNT(*) AS successful_lends " +
            "           FROM borrowing_history WHERE returned_on IS NOT NULL " +
            "           GROUP BY owner_id) l ON l.owner_id = u.user_id " +
            "LEFT JOIN (SELECT borrower_id, " +
            "           SUM(CASE WHEN returned_on <= due_date THEN 1 ELSE 0 END) AS on_time_returns, " +
            "           SUM(CASE WHEN returned_on > due_date THEN 1 ELSE 0 END) AS late_returns " +
            "           FROM borrowing_history WHERE returned_on IS NOT NULL " +
            "           GROUP BY borrower_id) b ON b.borrower_id = u.user_id " +
            "LEFT JOIN (SELECT r.owner_id, COUNT(*) AS knowledge_shares " +
            "           FROM resources r JOIN categories c ON c.id = r.category_id " +
            "           WHERE LOWER(c.name) IN ('books', 'notes & papers') " +
            "           GROUP BY r.owner_id) k ON k.owner_id = u.user_id " +
            "HAVING points > 0 " +
            "ORDER BY points DESC, successful_lends DESC, on_time_returns DESC, " +
            "resources_shared DESC, u.full_name ASC";

        List<Entry> entries = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entries.add(new Entry(
                    rs.getString("user_id"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getInt("resources_shared"),
                    rs.getInt("successful_lends"),
                    rs.getInt("on_time_returns"),
                    rs.getInt("late_returns"),
                    rs.getInt("knowledge_shares"),
                    rs.getInt("points")
                ));
            }
        }
        return entries;
    }
}
