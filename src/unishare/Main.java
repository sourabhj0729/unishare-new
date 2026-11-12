package unishare;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Unishare - college resource sharing platform. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            try {
                Database.init();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to the MySQL database.\n\n"
                                + "1. Make sure MySQL is running.\n"
                                + "2. Check user/password in db.properties.\n"
                                + "3. Make sure the MySQL Connector/J .jar is in the lib folder.\n\n"
                                + "Details: " + e.getMessage(),
                        "Unishare - Database error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
