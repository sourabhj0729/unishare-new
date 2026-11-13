package unishare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Community Hall of Fame with live rankings from UniShare database activity. */
public class CommunityFrame extends JFrame {
    private final CommunityDAO dao = new CommunityDAO();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = Ui.label("Rankings are calculated from actual platform activity.",
            Font.PLAIN, 12f, Ui.MUTED);

    public CommunityFrame() {
        super("UniShare - Community Hall of Fame");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 590);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        titles.add(Ui.label("Community Hall of Fame", Font.BOLD, 25f, Ui.TEXT));
        titles.add(Ui.label("Celebrate students who make campus sharing better.",
                Font.PLAIN, 13f, Ui.MUTED));
        heading.add(titles, BorderLayout.WEST);
        Ui.Button refresh = new Ui.Button("Refresh rankings", Ui.Button.Kind.PRIMARY);
        refresh.addActionListener(e -> load());
        heading.add(refresh, BorderLayout.EAST);

        tabs.setFont(Ui.font(Font.BOLD, 13f));
        root.add(heading, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        load();
    }

    private void load() {
        try {
            tabs.removeAll();
            tabs.addTab("Top Contributors", table(
                    new String[]{"Rank", "User ID", "Student", "Resources Shared", "Title"},
                    dao.topContributors(), 2));
            tabs.addTab("Top Lenders", table(
                    new String[]{"Rank", "User ID", "Student", "Successful Loans", "Title"},
                    dao.topLenders(), 2));
            tabs.addTab("Reliable Borrowers", table(
                    new String[]{"Rank", "User ID", "Student", "On-time Returns", "Returned Items", "Reliability", "Title"},
                    dao.reliableBorrowers(), 3));
            status.setText("Live rankings • Top 20 students • Updated just now");
        } catch (SQLException ex) {
            status.setText("Could not load community rankings.");
            JOptionPane.showMessageDialog(this, "Unable to load Hall of Fame:\n" + ex.getMessage(),
                    "UniShare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JScrollPane table(String[] columns, List<Object[]> data, int scoreIndex) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        int rank = 1;
        for (Object[] row : data) {
            Object[] display = new Object[row.length + 1];
            display[0] = rank++;
            System.arraycopy(row, 0, display, 1, row.length);
            model.addRow(display);
        }
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFont(Ui.font(Font.PLAIN, 13f));
        table.setForeground(Ui.TEXT);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(Ui.PRIMARY_SOFT);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(Ui.font(Font.BOLD, 12f));
        table.getTableHeader().setBackground(new Color(0xF9FAFB));
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        if (data.isEmpty()) {
            status.setText("No activity yet in this category. Be the first to earn a place!");
        }
        return scroll;
    }
}
