package unishare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Community Hall of Fame with live rankings from UniShare database activity. */
public class CommunityFrame extends JFrame {
    private static final Color TABLE_ALT = new Color(0xF8FAFF);
    private static final Color HEADER_BG = new Color(0xEEF2FF);
    private static final Color HEADER_FG = new Color(0x312E81);

    private final CommunityDAO dao = new CommunityDAO();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = Ui.label("Rankings are calculated from actual platform activity.",
            Font.PLAIN, 12f, Ui.MUTED);

    public CommunityFrame() {
        super("UniShare - Community Hall of Fame");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));

        JPanel heading = new JPanel(new BorderLayout(18, 0));
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = Ui.label("Community Hall of Fame", Font.BOLD, 27f, Ui.TEXT);
        JLabel subtitle = Ui.label("Recognising the students who make sharing on campus better.",
                Font.PLAIN, 14f, Ui.MUTED);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        titles.add(title);
        titles.add(subtitle);
        heading.add(titles, BorderLayout.WEST);

        Ui.Button refresh = new Ui.Button("Refresh rankings", Ui.Button.Kind.PRIMARY);
        refresh.setPreferredSize(new Dimension(166, 44));
        refresh.setToolTipText("Reload the latest community activity");
        refresh.addActionListener(e -> load());
        heading.add(refresh, BorderLayout.EAST);

        tabs.setFont(Ui.font(Font.BOLD, 14f));
        tabs.setForeground(Ui.TEXT);
        tabs.setBackground(Ui.CARD);
        tabs.setOpaque(true);
        tabs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.BORDER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        status.setHorizontalAlignment(SwingConstants.LEFT);
        footer.add(status, BorderLayout.WEST);
        JLabel note = Ui.label("Rankings update when you press Refresh.",
                Font.PLAIN, 11f, Ui.MUTED);
        footer.add(note, BorderLayout.EAST);

        root.add(heading, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        load();
    }

    private void load() {
        try {
            tabs.removeAll();
            tabs.addTab("  Top Contributors  ", table(
                    new String[]{"Rank", "User ID", "Student", "Resources Shared", "Achievement"},
                    dao.topContributors()));
            tabs.addTab("  Top Lenders  ", table(
                    new String[]{"Rank", "User ID", "Student", "Successful Loans", "Achievement"},
                    dao.topLenders()));
            tabs.addTab("  Reliable Borrowers  ", table(
                    new String[]{"Rank", "User ID", "Student", "On-time Returns", "Returned Items", "Reliability", "Achievement"},
                    dao.reliableBorrowers()));
            if (tabs.getTabCount() > 0) tabs.setSelectedIndex(0);
            status.setText("  Live rankings  •  Top 20 students  •  Updated just now");
        } catch (SQLException ex) {
            status.setText("Could not load community rankings.");
            JOptionPane.showMessageDialog(this, "Unable to load Hall of Fame:\n" + ex.getMessage(),
                    "UniShare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JScrollPane table(String[] columns, List<Object[]> data) {
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

        JTable table = new JTable(model) {
            @Override public java.awt.Component prepareRenderer(
                    javax.swing.table.TableCellRenderer renderer, int row, int column) {
                java.awt.Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_ALT);
                    c.setForeground(Ui.TEXT);
                }
                return c;
            }
        };
        table.setRowHeight(44);
        table.setFont(Ui.font(Font.PLAIN, 14f));
        table.setForeground(Ui.TEXT);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(Ui.PRIMARY_SOFT);
        table.setSelectionForeground(Ui.TEXT);
        table.setGridColor(new Color(0xE9ECF5));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(Ui.font(Font.BOLD, 13f));
        header.setBackground(HEADER_BG);
        header.setForeground(HEADER_FG);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(130);
        }
        if (table.getColumnModel().getColumnCount() > 2) {
            table.getColumnModel().getColumn(2).setPreferredWidth(190);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        if (data.isEmpty()) {
            status.setText("No activity yet in this category. Be the first to earn a place!");
        }
        return scroll;
    }
}
