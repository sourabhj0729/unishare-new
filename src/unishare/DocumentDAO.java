package unishare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database access for the shared academic document library. */
public final class DocumentDAO {
    private static final String SELECT =
            "SELECT d.id, d.title, d.description, d.original_file_name, d.stored_file_name, "
            + "d.file_path, d.file_type, d.subject, d.department, d.semester, d.faculty, "
            + "d.uploaded_by, u.full_name AS uploader_name, d.uploaded_at "
            + "FROM documents d JOIN users u ON u.user_id = d.uploaded_by";

    public void add(Document d) throws SQLException {
        String sql = "INSERT INTO documents(title, description, original_file_name, stored_file_name, "
                + "file_path, file_type, subject, department, semester, faculty, uploaded_by) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.title);
            ps.setString(2, d.description);
            ps.setString(3, d.originalFileName);
            ps.setString(4, d.storedFileName);
            ps.setString(5, d.filePath);
            ps.setString(6, d.fileType);
            ps.setString(7, d.subject);
            ps.setString(8, d.department);
            ps.setInt(9, d.semester);
            ps.setString(10, d.faculty);
            ps.setString(11, d.uploadedBy);
            ps.executeUpdate();
        }
    }

    public List<Document> search(String keyword, String department, Integer semester) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (d.title LIKE ? OR d.description LIKE ? OR d.subject LIKE ? "
                    + "OR d.faculty LIKE ? OR d.original_file_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (department != null && !department.equals("All departments")) {
            sql.append(" AND d.department = ?");
            params.add(department);
        }
        if (semester != null) {
            sql.append(" AND d.semester = ?");
            params.add(semester);
        }
        sql.append(" ORDER BY d.uploaded_at DESC, d.id DESC");

        List<Document> documents = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) documents.add(map(rs));
            }
        }
        return documents;
    }

    public List<String> departments() throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT DISTINCT department FROM documents WHERE department IS NOT NULL "
                     + "AND department <> '' ORDER BY department");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) values.add(rs.getString(1));
        }
        return values;
    }

    private Document map(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.id = rs.getInt("id");
        d.title = rs.getString("title");
        d.description = rs.getString("description");
        d.originalFileName = rs.getString("original_file_name");
        d.storedFileName = rs.getString("stored_file_name");
        d.filePath = rs.getString("file_path");
        d.fileType = rs.getString("file_type");
        d.subject = rs.getString("subject");
        d.department = rs.getString("department");
        d.semester = rs.getInt("semester");
        d.faculty = rs.getString("faculty");
        d.uploadedBy = rs.getString("uploaded_by");
        d.uploaderName = rs.getString("uploader_name");
        java.sql.Timestamp timestamp = rs.getTimestamp("uploaded_at");
        d.uploadedAt = timestamp == null ? null : timestamp.toLocalDateTime();
        return d;
    }
}
