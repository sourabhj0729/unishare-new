package unishare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Campus-wide contribution rankings and achievement titles. */
public final class HallOfFameFrame extends JFrame {

    private static final String[] TITLES = {
        "👑 Campus Legend",
        "💎 Grandmaster",
        "🏆 Master",
        "⚡ Elite Contributor",
        "🌟 Rising Star",
        "🤝 Community Hero",
        "📚 Knowledge Guardian"
    };

    private final Models.User currentUser;
    private final HallOfFameDAO dao = new HallOfFameDAO();
    private final JLabel status = Ui.label("Loading campus contributions...", Font.PLAIN, 13f, Ui.MUTED);

    public HallOfFameFrame(Models.User currentUser) {
        super("UniShare - Hall of Fame");
        this.currentUser = currentUser;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1080, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setContentPane(buildShell());
        loadLeaderboard();
    }

    private JPanel buildShell() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        heading.add(Ui.label("🏆 Hall of Fame", Font.BOLD, 29f, Ui.TEXT));
        heading.add(Ui.label("Celebrating the students who make campus sharing better.",
                Font.PLAIN, 14f, Ui.MUTED));
        heading.add(javax.swing.Box.createVerticalStrut(12));
        heading.add(status);

        root.add(heading, BorderLayout.NORTH);
        return root;
    }

    private void loadLeaderboard() {
        try {
            List<HallOfFameDAO.Entry> entries = dao.leaderboard();
            JPanel content = new JPanel(new BorderLayout(0, 16));
            content.setOpaque(false);
            content.add(buildPodium(entries), BorderLayout.NORTH);
            content.add(buildRankingTable(entries), BorderLayout.CENTER);

            JPanel rules = new JPanel(new GridLayout(2, 3, 10, 6));
            rules.setOpaque(false);
            rules.add(rule("+10", "Resource listed"));
            rules.add(rule("+20", "Successful lending"));
            rules.add(rule("+15", "On-time return"));
            rules.add(rule("+5", "Late return"));
            rules.add(rule("+5", "Book / notes bonus"));
            rules.add(rule("Top 7", "Earn the seven titles"));

            JPanel lower = new JPanel(new BorderLayout(0, 8));
            lower.setOpaque(false);
            lower.add(content, BorderLayout.CENTER);
            lower.add(rules, BorderLayout.SOUTH);

            setContentPane(lower);
            lower.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
            lower.setBackground(Ui.BG);
            revalidate();
            repaint();
        } catch (SQLException e) {
            status.setForeground(Ui.DANGER);
            status.setText("Could not load the Hall of Fame: " + e.getMessage());
        }
    }

    private JPanel buildPodium(List<HallOfFameDAO.Entry> entries) {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setOpaque(false);
        for (int i = 0; i < 3; i++) {
            if (i < entries.size()) {
                HallOfFameDAO.Entry e = entries.get(i);
                Ui.Card card = new Ui.Card(16, Ui.CARD);
                card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
                card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
                card.add(Ui.label("#" + (i + 1) + "  " + titleFor(i),
                        Font.BOLD, 14f, Ui.PRIMARY));
                card.add(javax.swing.Box.createVerticalStrut(8));
                card.add(Ui.label(e.fullName, Font.BOLD, 18f, Ui.TEXT));
                card.add(Ui.label("@" + e.userId, Font.PLAIN, 12f, Ui.MUTED));
                card.add(javax.swing.Box.createVerticalStrut(8));
                card.add(Ui.label(e.points + " points", Font.BOLD, 20f, Ui.SUCCESS));
                panel.add(card);
            } else {
                panel.add(new JPanel());
            }
        }
        return panel;
    }

    private JScrollPane buildRankingTable(List<HallOfFameDAO.Entry> entries) {
        String[] columns = {"Rank", "Student", "Department", "Title", "Resources", "Successful lends",
                            "On-time returns", "Late returns", "Points"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        int currentRank = -1;
        int currentPoints = 0;
        for (int i = 0; i < entries.size(); i++) {
            HallOfFameDAO.Entry e = entries.get(i);
            if (e.userId.equals(currentUser.userId)) {
                currentRank = i + 1;
                currentPoints = e.points;
            }
            model.addRow(new Object[] {
                i + 1, e.fullName + "  (@" + e.userId + ")", e.department,
                titleFor(i), e.resourcesShared, e.successfulLends,
                e.onTimeReturns, e.lateReturns, e.points
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(Ui.font(Font.PLAIN, 12f));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().setFont(Ui.font(Font.BOLD, 12f));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                boolean mine = entries.get(row).userId.equals(currentUser.userId);
                setBackground(mine ? Ui.PRIMARY_SOFT : (row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB)));
                setForeground(column == 8 ? Ui.SUCCESS : Ui.TEXT);
                setFont(Ui.font(mine || column == 8 ? Font.BOLD : Font.PLAIN, 12f));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        scroll.setPreferredSize(new Dimension(950, 330));
        status.setText(currentRank < 0
                ? "Your profile is not present in the current ranking."
                : "Your standing: #" + currentRank + "  •  " + currentPoints + " points  •  "
                    + titleFor(currentRank - 1));
        status.setForeground(Ui.PRIMARY);
        return scroll;
    }

    private JPanel rule(String points, String description) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        p.setOpaque(false);
        p.add(Ui.label(points, Font.BOLD, 13f, Ui.PRIMARY));
        p.add(Ui.label(description, Font.PLAIN, 12f, Ui.MUTED));
        return p;
    }

    private static String titleFor(int zeroBasedRank) {
        return zeroBasedRank >= 0 && zeroBasedRank < TITLES.length
                ? TITLES[zeroBasedRank] : "Contributor";
    }
}
