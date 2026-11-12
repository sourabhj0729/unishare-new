package unishare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Database access for user accounts. */
public final class UserDAO {

    public boolean exists(String userId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void register(String userId, String fullName, String department, String password)
            throws SQLException {
        if (exists(userId)) {
            throw new IllegalArgumentException("That user ID is already taken.");
        }
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash(password, salt);
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(user_id, full_name, department, password_hash, salt) "
                             + "VALUES (?,?,?,?,?)")) {
            ps.setString(1, userId);
            ps.setString(2, fullName);
            ps.setString(3, department);
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.executeUpdate();
        }
    }

    /** Returns the user if the ID/password pair is valid, otherwise null. */
    public Models.User authenticate(String userId, String password) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT full_name, department, password_hash, salt FROM users WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                if (!PasswordUtil.matches(password, rs.getString("salt"), rs.getString("password_hash"))) {
                    return null;
                }
                return new Models.User(userId, rs.getString("full_name"), rs.getString("department"));
            }
        }
    }
}
