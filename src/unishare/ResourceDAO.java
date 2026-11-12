package unishare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Database access for resources and categories. */
public final class ResourceDAO {

    private static final String BASE_SELECT =
            "SELECT r.id, r.name, c.name AS category, r.description, r.owner_id, "
                    + "o.full_name AS owner_name, r.available, r.issued_to, "
                    + "i.full_name AS issued_name, r.issued_date, r.due_date "
                    + "FROM resources r "
                    + "JOIN categories c ON c.id = r.category_id "
                    + "JOIN users o ON o.user_id = r.owner_id "
                    + "LEFT JOIN users i ON i.user_id = r.issued_to";

    public List<String> categories() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT name FROM categories ORDER BY name")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    /** Finds a category by name, creating it if it does not exist yet. */
    private int categoryId(Connection c, String name) throws SQLException {
        String n = name.trim();
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM categories WHERE name = ?")) {
            ps.setString(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO categories(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public List<Models.Resource> search(String keyword, String category, String status,
                                        String ownerId, String borrowerId) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (r.name LIKE ? OR r.description LIKE ? OR c.name LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (category != null) {
            sql.append(" AND c.name = ?");
            params.add(category);
        }
        if ("Available".equals(status)) sql.append(" AND r.available = TRUE");
        if ("Issued".equals(status)) sql.append(" AND r.available = FALSE");
        if (ownerId != null) {
            sql.append(" AND r.owner_id = ?");
            params.add(ownerId);
        }
        if (borrowerId != null) {
            sql.append(" AND r.issued_to = ?");
            params.add(borrowerId);
        }
        sql.append(" ORDER BY r.added_on DESC, r.id DESC");

        List<Models.Resource> out = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** Returns {total, available, issued, listedByUser}. */
    public int[] stats(String userId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*), COALESCE(SUM(available),0), COALESCE(SUM(NOT available),0), "
                             + "COALESCE(SUM(owner_id = ?),0) FROM resources")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)};
            }
        }
    }

    public void add(Models.Resource r) throws SQLException {
        try (Connection c = Database.connect()) {
            int cat = categoryId(c, r.category);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO resources(name, category_id, description, owner_id, available, "
                            + "issued_to, issued_date, due_date) VALUES (?,?,?,?,?,?,?,?)")) {
                bind(ps, r, cat);
                ps.executeUpdate();
            }
        }
    }

    public void update(Models.Resource r) throws SQLException {
        try (Connection c = Database.connect()) {
            int cat = categoryId(c, r.category);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE resources SET name=?, category_id=?, description=?, owner_id=?, "
                            + "available=?, issued_to=?, issued_date=?, due_date=? WHERE id=?")) {
                bind(ps, r, cat);
                ps.setInt(9, r.id);
                ps.executeUpdate();
            }
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM resources WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Marks an available item as issued and creates its history record atomically. */
    public boolean issue(int id, String userId, LocalDate date, LocalDate dueDate) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                String resourceName;
                String ownerId;
                try (PreparedStatement find = c.prepareStatement(
                        "SELECT name, owner_id FROM resources WHERE id = ? AND available = TRUE FOR UPDATE")) {
                    find.setInt(1, id);
                    try (ResultSet rs = find.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        resourceName = rs.getString("name");
                        ownerId = rs.getString("owner_id");
                    }
                }
                if (ownerId.equals(userId)) {
                    c.rollback();
                    throw new IllegalArgumentException("You cannot borrow your own resource.");
                }

                try (PreparedStatement update = c.prepareStatement(
                        "UPDATE resources SET available = FALSE, issued_to = ?, issued_date = ?, due_date = ? "
                                + "WHERE id = ? AND available = TRUE")) {
                    update.setString(1, userId);
                    update.setDate(2, java.sql.Date.valueOf(date));
                    update.setDate(3, java.sql.Date.valueOf(dueDate));
                    update.setInt(4, id);
                    if (update.executeUpdate() == 0) {
                        c.rollback();
                        return false;
                    }
                }

                try (PreparedStatement history = c.prepareStatement(
                        "INSERT INTO borrowing_history(resource_id, resource_name, owner_id, borrower_id, "
                                + "borrowed_on, due_date) VALUES (?,?,?,?,?,?)")) {
                    history.setInt(1, id);
                    history.setString(2, resourceName);
                    history.setString(3, ownerId);
                    history.setString(4, userId);
                    history.setDate(5, java.sql.Date.valueOf(date));
                    history.setDate(6, java.sql.Date.valueOf(dueDate));
                    history.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Returns an item and closes its active borrowing-history record atomically. */
    public void returnItem(int id) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement update = c.prepareStatement(
                        "UPDATE resources SET available = TRUE, issued_to = NULL, issued_date = NULL, due_date = NULL "
                                + "WHERE id = ? AND available = FALSE")) {
                    update.setInt(1, id);
                    if (update.executeUpdate() == 0) {
                        c.rollback();
                        throw new IllegalStateException("This resource is not currently issued.");
                    }
                }
                try (PreparedStatement history = c.prepareStatement(
                        "UPDATE borrowing_history SET returned_on = CURDATE() "
                                + "WHERE resource_id = ? AND returned_on IS NULL")) {
                    history.setInt(1, id);
                    history.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Returns all borrowing records for a student, newest first. */
    public List<Models.BorrowRecord> borrowingHistory(String userId) throws SQLException {
        String sql = "SELECT h.id, h.resource_name, h.owner_id, h.borrower_id, "
                + "o.full_name AS owner_name, b.full_name AS borrower_name, "
                + "h.borrowed_on, h.due_date, h.returned_on "
                + "FROM borrowing_history h "
                + "JOIN users o ON o.user_id = h.owner_id "
                + "JOIN users b ON b.user_id = h.borrower_id "
                + "WHERE h.borrower_id = ? OR h.owner_id = ? "
                + "ORDER BY h.borrowed_on DESC, h.id DESC";
        List<Models.BorrowRecord> records = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Models.BorrowRecord record = new Models.BorrowRecord();
                    record.id = rs.getLong("id");
                    record.resourceName = rs.getString("resource_name");
                    record.ownerName = rs.getString("owner_name");
                    record.borrowerName = rs.getString("borrower_name");
                    record.ownerId = rs.getString("owner_id");
                    record.borrowerId = rs.getString("borrower_id");
                    record.borrowedOn = rs.getDate("borrowed_on").toLocalDate();
                    record.dueDate = rs.getDate("due_date").toLocalDate();
                    java.sql.Date returned = rs.getDate("returned_on");
                    record.returnedOn = returned == null ? null : returned.toLocalDate();
                    records.add(record);
                }
            }
        }
        return records;
    }

    private void bind(PreparedStatement ps, Models.Resource r, int categoryId) throws SQLException {
        ps.setString(1, r.name);
        ps.setInt(2, categoryId);
        ps.setString(3, r.description);
        ps.setString(4, r.ownerId);
        ps.setBoolean(5, r.available);
        if (r.available || r.issuedToId == null) {
            ps.setNull(6, Types.VARCHAR);
            ps.setNull(7, Types.DATE);
            ps.setNull(8, Types.DATE);
        } else {
            LocalDate d = r.issuedDate == null ? LocalDate.now() : r.issuedDate;
            ps.setString(6, r.issuedToId);
            ps.setDate(7, java.sql.Date.valueOf(d));
            ps.setDate(8, java.sql.Date.valueOf(r.dueDate == null ? d.plusDays(7) : r.dueDate));
        }
    }

    private Models.Resource map(ResultSet rs) throws SQLException {
        Models.Resource r = new Models.Resource();
        r.id = rs.getInt("id");
        r.name = rs.getString("name");
        r.category = rs.getString("category");
        r.description = rs.getString("description");
        r.ownerId = rs.getString("owner_id");
        r.ownerName = rs.getString("owner_name");
        r.available = rs.getBoolean("available");
        r.issuedToId = rs.getString("issued_to");
        r.issuedToName = rs.getString("issued_name");
        java.sql.Date d = rs.getDate("issued_date");
        r.issuedDate = d == null ? null : d.toLocalDate();
        java.sql.Date due = rs.getDate("due_date");
        r.dueDate = due == null ? null : due.toLocalDate();
        return r;
    }
}
