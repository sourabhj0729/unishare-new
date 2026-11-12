package unishare;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * MySQL connection helper. Reads optional settings from "db.properties"
 * (host, port, name, user, password) and creates the database + tables
 * automatically on first run.
 */
public final class Database {
    private static String host = "localhost";
    private static String port = "3306";
    private static String name = "unishare";
    private static String user = "root";
    private static String password = "";

    private static final String OPTS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Database() {}

    private static void loadConfig() {
        File f = new File("db.properties");
        if (!f.exists()) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        host = p.getProperty("host", host).trim();
        port = p.getProperty("port", port).trim();
        name = p.getProperty("name", name).trim();
        user = p.getProperty("user", user).trim();
        password = p.getProperty("password", password);
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + name + OPTS, user, password);
    }

    /** Creates the database, tables and starter categories if they do not exist. */
    public static void init() throws SQLException {
        loadConfig();

        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + OPTS, user, password);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + name + "` CHARACTER SET utf8mb4");
        }

        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                    + " user_id VARCHAR(30) PRIMARY KEY,"
                    + " full_name VARCHAR(80) NOT NULL,"
                    + " department VARCHAR(60),"
                    + " password_hash VARCHAR(128) NOT NULL,"
                    + " salt VARCHAR(64) NOT NULL,"
                    + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB");

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS categories ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " name VARCHAR(50) NOT NULL UNIQUE"
                    + ") ENGINE=InnoDB");

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS resources ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " name VARCHAR(120) NOT NULL,"
                    + " category_id INT NOT NULL,"
                    + " description VARCHAR(500),"
                    + " owner_id VARCHAR(30) NOT NULL,"
                    + " available BOOLEAN NOT NULL DEFAULT TRUE,"
                    + " issued_to VARCHAR(30) NULL,"
                    + " issued_date DATE NULL,"
                    + " added_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " CONSTRAINT fk_res_cat FOREIGN KEY (category_id) REFERENCES categories(id),"
                    + " CONSTRAINT fk_res_owner FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,"
                    + " CONSTRAINT fk_res_issued FOREIGN KEY (issued_to) REFERENCES users(user_id) ON DELETE SET NULL"
                    + ") ENGINE=InnoDB");

            // Safe migration for databases created by an older UniShare version.
            if (!columnExists(c, "resources", "due_date")) {
                s.executeUpdate("ALTER TABLE resources ADD COLUMN due_date DATE NULL AFTER issued_date");
            }

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS borrowing_history ("
                    + " id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + " resource_id INT NULL,"
                    + " resource_name VARCHAR(120) NOT NULL,"
                    + " owner_id VARCHAR(30) NOT NULL,"
                    + " borrower_id VARCHAR(30) NOT NULL,"
                    + " borrowed_on DATE NOT NULL,"
                    + " due_date DATE NOT NULL,"
                    + " returned_on DATE NULL,"
                    + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " INDEX idx_history_borrower (borrower_id),"
                    + " INDEX idx_history_resource (resource_id),"
                    + " CONSTRAINT fk_history_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL,"
                    + " CONSTRAINT fk_history_owner FOREIGN KEY (owner_id) REFERENCES users(user_id),"
                    + " CONSTRAINT fk_history_borrower FOREIGN KEY (borrower_id) REFERENCES users(user_id)"
                    + ") ENGINE=InnoDB");

            s.executeUpdate(
                    "INSERT IGNORE INTO categories(name) VALUES "
                    + "('Books'),('Lab Equipment'),('Electronics'),('Stationery'),"
                    + "('Sports'),('Notes & Papers'),('Other')");
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (java.sql.ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }

}
