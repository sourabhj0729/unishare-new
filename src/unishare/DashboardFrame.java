package unishare;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/** Main window: sidebar navigation, stats, searchable resource table and actions. */
public class DashboardFrame extends JFrame {

    private enum View {
        ALL("Browse Resources", "Everything shared on campus right now"),
        MINE("My Resources", "Items you have listed for others to borrow"),
        BORROWED("Borrowed by Me", "Items currently issued to you");

        final String title;
        final String subtitle;

        View(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private interface DbAction {
        void run() throws Exception;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String ALL_CATS = "All categories";

    private final Models.User me;
    private final ResourceDAO dao = new ResourceDAO();
    private final List<Models.Resource> rows = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private final List<Ui.Button> navButtons = new ArrayList<>();
    private final List<Ui.Button> allNavButtons = new ArrayList<>();
    private CardLayout pageLayout;
    private JPanel pageHost;
    private MarketplaceFrame marketplacePage;
    private Ui.Button activeNavButton;
    private View view = View.ALL;
    private boolean loadingFilters;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Item", "Category", "Owner", "Status", "Issued to", "Issued date", "Due date"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable table;
    private Ui.HintField search;
    private JComboBox<String> categoryBox;
    private JComboBox<String> statusBox;
    private JLabel titleLbl;
    private JLabel subtitleLbl;
    private JLabel countLbl;
    private JLabel flashLbl;
    private JPanel tableHost;
    private Ui.Button borrowBtn, returnBtn, editBtn, deleteBtn, historyBtn;
    private final JLabel[] statValues = new JLabel[4];
    private Timer debounce;
    private Timer flashTimer;

    public DashboardFrame(Models.User me) {
        super("Unishare");
        this.me = me;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 680));
        setSize(1280, 780);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTopBar(), BorderLayout.NORTH);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.add(buildSidebar(), BorderLayout.WEST);
        pageLayout = new CardLayout();
        pageHost = new JPanel(pageLayout);
        pageHost.add(buildHomePage(), "home");
        pageHost.add(buildMain(), "resources");
        marketplacePage = new MarketplaceFrame(me);
        pageHost.add(marketplacePage, "marketplace");
        pageHost.add(buildPlaceholderPage("My Cart", "Your shopping cart will be available in the next marketplace phase."), "cart");
        pageHost.add(buildPlaceholderPage("Wishlist", "Products you save will appear here in the next marketplace phase."), "wishlist");
        pageHost.add(buildPlaceholderPage("My Orders", "Your purchase history and order tracking will appear here."), "orders");
        pageHost.add(buildPlaceholderPage("My Listings", "Manage your active and inactive product listings here."), "listings");
        pageHost.add(buildPlaceholderPage("Seller Dashboard", "Manage sales, incoming orders and inventory from one place."), "seller");
        pageHost.add(buildPlaceholderPage("My Account", "Your account settings and marketplace profile will appear here."), "account");
        pageHost.add(buildPlaceholderPage("Admin Dashboard", "Platform administration and moderation tools will appear here."), "admin");
        workspace.add(pageHost, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);

        setView(View.ALL);
        showPage("home", homeButtonRef());
    }

    // ---------------------------------------------------------------- top bar

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(18, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.BORDER),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)));
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.add(Ui.label("UniShare", Font.BOLD, 22f, new Color(0x123C3A)));
        brand.add(Ui.label("CAMPUS MARKETPLACE & RESOURCE HUB", Font.BOLD, 9f, Ui.PRIMARY));
        bar.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 2));
        right.setOpaque(false);
        JLabel campus = Ui.label("Your campus. Your community.", Font.PLAIN, 12f, Ui.MUTED);
        JLabel user = Ui.label(me.fullName, Font.BOLD, 13f, Ui.TEXT);
        right.add(campus);
        right.add(new JLabel("  "));
        right.add(user);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------- sidebar

    private JComponent buildSidebar() {
        JPanel p = new JPanel();
        p.setBackground(Ui.SIDEBAR);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(30, 18, 22, 18));
        p.setPreferredSize(new Dimension(250, 0));

        p.add(Box.createVerticalStrut(18));

        Ui.Button home = addNavigationButton(p, "Home", () -> showPage("home", homeButtonRef()), false);
        p.add(Box.createVerticalStrut(18));
        addSectionLabel(p, "MARKETPLACE");
        addNavigationButton(p, "Shop", () -> showPage("marketplace", findNavButton("Shop")), false);
        addNavigationButton(p, "My Cart", () -> showPage("cart", findNavButton("My Cart")), false);
        addNavigationButton(p, "Wishlist", () -> showPage("wishlist", findNavButton("Wishlist")), false);
        addNavigationButton(p, "My Orders", () -> showPage("orders", findNavButton("My Orders")), false);
        p.add(Box.createVerticalStrut(12));
        addSectionLabel(p, "SELLER");
        addNavigationButton(p, "Sell an Item", () -> showPage("marketplace", findNavButton("Sell an Item")), false);
        addNavigationButton(p, "My Listings", () -> showPage("listings", findNavButton("My Listings")), false);
        addNavigationButton(p, "Seller Dashboard", () -> showPage("seller", findNavButton("Seller Dashboard")), false);
        p.add(Box.createVerticalStrut(12));
        addSectionLabel(p, "BORROW & SHARE");
        for (View v : View.values()) {
            Ui.Button b = addNavigationButton(p, v.title, () -> setView(v), true);
            navButtons.add(b);
        }
        addNavigationButton(p, "My Account", () -> showPage("account", findNavButton("My Account")), false);
        Ui.Button admin = addNavigationButton(p, "Admin Dashboard", () -> showPage("admin", findNavButton("Admin Dashboard")), false);
        admin.setToolTipText("Admin tools will be enabled after role-based access is implemented");

        Ui.Button hallOfFame = addNavigationButton(p, "Community Hall of Fame", () -> {
            CommunityFrame frame = new CommunityFrame();
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);
            frame.toFront();
            frame.requestFocus();
            frame.setAlwaysOnTop(false);
        }, false);
        hallOfFame.setToolTipText("View top contributors, lenders and reliable borrowers");

        p.add(Box.createVerticalGlue());

        JPanel user = new JPanel(new BorderLayout(12, 0));
        user.setOpaque(false);
        user.setAlignmentX(0f);
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        final String initials = me.initials();
        JComponent avatar = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Ui.PRIMARY);
                g2.fillOval(0, 0, 44, 44);
                g2.setColor(Color.WHITE);
                g2.setFont(Ui.font(Font.BOLD, 15f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (44 - fm.stringWidth(initials)) / 2,
                        (44 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(44, 44));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Box.createVerticalGlue());
        names.add(Ui.label(me.fullName, Font.BOLD, 13f, Color.WHITE));
        names.add(Ui.label("@" + me.userId, Font.PLAIN, 12f, new Color(0xA5B4FC)));
        names.add(Box.createVerticalGlue());
        user.add(avatar, BorderLayout.WEST);
        user.add(names, BorderLayout.CENTER);
        p.add(user);
        p.add(Box.createVerticalStrut(16));

        Ui.Button logout = new Ui.Button("Log out", Ui.Button.Kind.GHOST);
        logout.setAlignmentX(0f);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        p.add(logout);
        JScrollPane sidebarScroll = new JScrollPane(p);
        sidebarScroll.setPreferredSize(new Dimension(250, 0));
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.getViewport().setBackground(Ui.SIDEBAR);
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);
        return sidebarScroll;
    }

    private Ui.Button addNavigationButton(JPanel sidebar, String text, Runnable action, boolean resourceNav) {
        Ui.Button button = new Ui.Button(text, Ui.Button.Kind.NAV);
        button.setAlignmentX(0f);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.addActionListener(e -> action.run());
        allNavButtons.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));
        return button;
    }

    private void addSectionLabel(JPanel sidebar, String text) {
        JLabel label = Ui.label(text, Font.BOLD, 10f, new Color(0x77D9C9));
        label.setBorder(BorderFactory.createEmptyBorder(4, 5, 7, 0));
        label.setAlignmentX(0f);
        sidebar.add(label);
    }

    private Ui.Button homeButtonRef() {
        return allNavButtons.isEmpty() ? null : allNavButtons.get(0);
    }

    private void showPage(String key, Ui.Button button) {
        pageLayout.show(pageHost, key);
        if (button != null) {
            if (activeNavButton != null && activeNavButton != button) activeNavButton.setActive(false);
            activeNavButton = button;
            activeNavButton.setActive(true);
        }
    }

    private JComponent buildHomePage() {
        JPanel page = new JPanel(new BorderLayout(0, 22));
        page.setBackground(Ui.BG);
        page.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));

        JPanel hero = new JPanel(new BorderLayout(24, 0));
        hero.setBackground(new Color(0xE7F7F3));
        hero.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 30));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrow = Ui.label("WELCOME TO UNISHARE", Font.BOLD, 10f, Ui.PRIMARY);
        JLabel headline = Ui.label("Buy. Sell. Connect.", Font.BOLD, 31f, new Color(0x123C3A));
        JLabel secondLine = Ui.label("Grow together.", Font.BOLD, 31f, Ui.PRIMARY);
        JLabel description = Ui.label("Your campus hub for books, electronics, essentials and shared resources.", Font.PLAIN, 14f, new Color(0x47615E));
        eyebrow.setAlignmentX(0f); headline.setAlignmentX(0f); secondLine.setAlignmentX(0f); description.setAlignmentX(0f);
        copy.add(eyebrow); copy.add(Box.createVerticalStrut(12)); copy.add(headline); copy.add(secondLine);
        copy.add(Box.createVerticalStrut(10)); copy.add(description); copy.add(Box.createVerticalStrut(18));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        Ui.Button explore = new Ui.Button("Explore items", Ui.Button.Kind.PRIMARY);
        Ui.Button sell = new Ui.Button("Sell something", Ui.Button.Kind.SECONDARY);
        explore.addActionListener(e -> showPage("marketplace", findNavButton("Shop")));
        sell.addActionListener(e -> showPage("marketplace", findNavButton("Sell an Item")));
        buttons.add(explore); buttons.add(sell);
        copy.add(buttons);
        JPanel art = new JPanel(new GridBagLayout());
        art.setOpaque(false);
        Ui.Card artCard = new Ui.Card(22, Color.WHITE);
        artCard.setLayout(new BoxLayout(artCard, BoxLayout.Y_AXIS));
        artCard.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        JLabel icon = Ui.label("↗", Font.BOLD, 52f, Ui.PRIMARY);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel artTitle = Ui.label("Same campus.", Font.BOLD, 20f, new Color(0x123C3A));
        JLabel artSubtitle = Ui.label("More possibilities.", Font.BOLD, 17f, Ui.PRIMARY);
        artTitle.setAlignmentX(Component.CENTER_ALIGNMENT); artSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        artCard.add(icon); artCard.add(Box.createVerticalStrut(6)); artCard.add(artTitle); artCard.add(artSubtitle);
        art.add(artCard);
        hero.add(copy, BorderLayout.CENTER);
        hero.add(art, BorderLayout.EAST);

        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.add(Ui.label("Explore UniShare", Font.BOLD, 20f, Ui.TEXT), BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(homeActionCard("Shop on campus", "Discover useful items listed by other students.", "Browse products",
                () -> showPage("marketplace", findNavButton("Shop"))));
        cards.add(homeActionCard("Sell an item", "Give your unused items a second life.", "Create listing",
                () -> showPage("marketplace", findNavButton("Sell an Item"))));
        cards.add(homeActionCard("Borrow & share", "Find resources shared by your campus community.", "Browse resources",
                () -> setView(View.ALL)));
        section.add(cards, BorderLayout.CENTER);
        page.add(hero, BorderLayout.NORTH);
        page.add(section, BorderLayout.CENTER);
        return page;
    }

    private JComponent homeActionCard(String title, String description, String actionText, Runnable action) {
        Ui.Card card = new Ui.Card(18, Ui.CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel heading = Ui.label(title, Font.BOLD, 19f, Ui.TEXT);
        JLabel detail = Ui.label(description, Font.PLAIN, 13f, Ui.MUTED);
        heading.setAlignmentX(0f);
        detail.setAlignmentX(0f);
        card.add(heading);
        card.add(Box.createVerticalStrut(9));
        card.add(detail);
        card.add(Box.createVerticalGlue());
        Ui.Button button = new Ui.Button(actionText, Ui.Button.Kind.PRIMARY);
        button.setAlignmentX(0f);
        button.addActionListener(e -> action.run());
        card.add(button);
        return card;
    }

    private Ui.Button findNavButton(String text) {
        for (Ui.Button button : allNavButtons) if (text.equals(button.getText())) return button;
        return null;
    }

    private JComponent buildPlaceholderPage(String title, String description) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(Ui.BG);
        Ui.Card card = new Ui.Card(18, Ui.CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(34, 42, 34, 42));
        JLabel heading = Ui.label(title, Font.BOLD, 25f, Ui.TEXT);
        JLabel detail = Ui.label(description, Font.PLAIN, 14f, Ui.MUTED);
        heading.setAlignmentX(0f);
        detail.setAlignmentX(0f);
        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(detail);
        card.add(Box.createVerticalStrut(22));
        Ui.Button shop = new Ui.Button("Continue to Shop", Ui.Button.Kind.PRIMARY);
        shop.setAlignmentX(0f);
        shop.addActionListener(e -> showPage("marketplace", findNavButton("Shop")));
        card.add(shop);
        page.add(card);
        return page;
    }

    // -------------------------------------------------------------- main area

    private JComponent buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(Ui.BG);
        main.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(0f);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titleLbl = Ui.label(" ", Font.BOLD, 26f, Ui.TEXT);
        subtitleLbl = Ui.label(" ", Font.PLAIN, 14f, Ui.MUTED);
        titles.add(titleLbl);
        titles.add(subtitleLbl);
        header.add(titles, BorderLayout.WEST);
        Ui.Button addBtn = new Ui.Button("+  Add Resource", Ui.Button.Kind.PRIMARY);
        addBtn.addActionListener(e -> onAdd());
        JPanel addWrap = new JPanel(new GridBagLayout());
        addWrap.setOpaque(false);
        addWrap.add(addBtn);
        header.add(addWrap, BorderLayout.EAST);
        top.add(header);
        top.add(Box.createVerticalStrut(20));

        // stats
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(0f);
        stats.add(statCard("Total resources", 0, Ui.TEXT));
        stats.add(statCard("Available now", 1, Ui.SUCCESS));
        stats.add(statCard("Currently issued", 2, Ui.WARN));
        stats.add(statCard("Listed by me", 3, Ui.PRIMARY));
        top.add(stats);

        main.add(top, BorderLayout.NORTH);
        main.add(buildTableCard(), BorderLayout.CENTER);
        return main;
    }

    private JComponent statCard(String caption, int idx, Color accent) {
        Ui.Card c = new Ui.Card(16, Ui.CARD);
        c.setLayout(new BorderLayout(0, 4));
        c.setBorder(BorderFactory.createEmptyBorder(16, 20, 19, 20));
        c.add(Ui.label(caption, Font.PLAIN, 13f, Ui.MUTED), BorderLayout.NORTH);
        statValues[idx] = Ui.label("0", Font.BOLD, 30f, accent);
        c.add(statValues[idx], BorderLayout.CENTER);
        return c;
    }

    private JComponent buildTableCard() {
        Ui.Card card = new Ui.Card(16, Ui.CARD);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 21, 18));

        // toolbar
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        search = new Ui.HintField("Search by item, description or category...");
        Ui.height(search, 42);
        debounce = new Timer(250, e -> reload());
        debounce.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });

        categoryBox = new JComboBox<>(new String[]{ALL_CATS});
        statusBox = new JComboBox<>(new String[]{"All status", "Available", "Issued"});
        Ui.style(categoryBox);
        Ui.style(statusBox);
        categoryBox.setPreferredSize(new Dimension(180, 42));
        statusBox.setPreferredSize(new Dimension(140, 42));
        categoryBox.addActionListener(e -> { if (!loadingFilters) reload(); });
        statusBox.addActionListener(e -> { if (!loadingFilters) reload(); });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);
        filters.add(categoryBox);
        filters.add(statusBox);
        bar.add(search, BorderLayout.CENTER);
        bar.add(filters, BorderLayout.EAST);
        card.add(bar, BorderLayout.NORTH);

        // table
        table = new JTable(model);
        table.setRowHeight(46);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Ui.font(Font.PLAIN, 13f));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(Ui.PRIMARY_SOFT);
        table.setSelectionForeground(Ui.TEXT);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActions();
        });

        JTableHeader th = table.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 40));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                     boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setBackground(new Color(0xF9FAFB));
                setForeground(Ui.MUTED);
                setFont(Ui.font(Font.BOLD, 12f));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.BORDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                setHorizontalAlignment(SwingConstants.LEFT);
                return this;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                     boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setBackground(sel ? Ui.PRIMARY_SOFT : Color.WHITE);
                setForeground(c == 1 ? Ui.TEXT : new Color(0x374151));
                setFont(Ui.font(c == 1 ? Font.BOLD : Font.PLAIN, 13f));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
        table.getColumnModel().getColumn(4).setCellRenderer(new PillRenderer());

        int[] widths = {45, 220, 125, 145, 105, 145, 110, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JLabel empty = Ui.label("No resources match. Adjust the filters or add a new one!",
                Font.PLAIN, 14f, Ui.MUTED);
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        empty.setOpaque(true);
        empty.setBackground(Color.WHITE);
        empty.setBorder(BorderFactory.createLineBorder(Ui.BORDER));

        tableHost = new JPanel(new CardLayout());
        tableHost.setOpaque(false);
        tableHost.add(scroll, "table");
        tableHost.add(empty, "empty");
        card.add(tableHost, BorderLayout.CENTER);

        // actions
        JPanel actions = new JPanel(new BorderLayout(14, 0));
        actions.setOpaque(false);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        borrowBtn = new Ui.Button("Borrow", Ui.Button.Kind.PRIMARY);
        returnBtn = new Ui.Button("Return", Ui.Button.Kind.SECONDARY);
        editBtn = new Ui.Button("Edit", Ui.Button.Kind.SECONDARY);
        deleteBtn = new Ui.Button("Delete", Ui.Button.Kind.DANGER);
        historyBtn = new Ui.Button("Borrowing History", Ui.Button.Kind.GHOST);
        borrowBtn.setToolTipText("Borrow the selected item (must be available and not your own)");
        returnBtn.setToolTipText("Mark the selected item as returned");
        editBtn.setToolTipText("Edit an item you listed");
        deleteBtn.setToolTipText("Delete an item you listed");
        borrowBtn.addActionListener(e -> onBorrow());
        returnBtn.addActionListener(e -> onReturn());
        editBtn.addActionListener(e -> onEdit());
        deleteBtn.addActionListener(e -> onDelete());
        historyBtn.addActionListener(e -> showBorrowingHistory());
        btns.add(historyBtn);
        btns.add(borrowBtn);
        btns.add(returnBtn);
        btns.add(editBtn);
        btns.add(deleteBtn);
        flashLbl = Ui.label(" ", Font.BOLD, 13f, Ui.SUCCESS);
        countLbl = Ui.label("0 items", Font.PLAIN, 13f, Ui.MUTED);
        actions.add(btns, BorderLayout.WEST);
        actions.add(flashLbl, BorderLayout.CENTER);
        actions.add(countLbl, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);

        updateActions();
        return card;
    }

    // ------------------------------------------------------------------ logic

    private void setView(View v) {
        view = v;
        View[] all = View.values();
        for (int i = 0; i < all.length; i++) navButtons.get(i).setActive(all[i] == v);
        Ui.Button selectedButton = navButtons.get(v.ordinal());
        if (activeNavButton != null && activeNavButton != selectedButton) activeNavButton.setActive(false);
        activeNavButton = selectedButton;
        pageLayout.show(pageHost, "resources");
        titleLbl.setText(v.title);
        subtitleLbl.setText(v.subtitle);
        loadCategories();
        reload();
    }

    private void loadCategories() {
        try {
            Object keep = categoryBox.getSelectedItem();
            categoryNames.clear();
            categoryNames.addAll(dao.categories());
            loadingFilters = true;
            categoryBox.removeAllItems();
            categoryBox.addItem(ALL_CATS);
            for (String c : categoryNames) categoryBox.addItem(c);
            if (keep != null) categoryBox.setSelectedItem(keep);
            if (categoryBox.getSelectedItem() == null) categoryBox.setSelectedIndex(0);
            loadingFilters = false;
        } catch (SQLException e) {
            loadingFilters = false;
            showError(e);
        }
    }

    private void reload() {
        try {
            String kw = search.getText().trim();
            String cat = categoryBox.getSelectedIndex() <= 0 ? null : (String) categoryBox.getSelectedItem();
            String st = statusBox.getSelectedIndex() <= 0 ? null : (String) statusBox.getSelectedItem();
            String owner = view == View.MINE ? me.userId : null;
            String borrower = view == View.BORROWED ? me.userId : null;

            rows.clear();
            rows.addAll(dao.search(kw, cat, st, owner, borrower));
            model.setRowCount(0);
            for (Models.Resource r : rows) {
                model.addRow(new Object[]{
                        r.id, r.name, r.category, r.ownerName,
                        r.available ? "Available" : "Issued",
                        r.available || r.issuedToName == null ? "-" : r.issuedToName,
                        r.issuedDate == null ? "-" : FMT.format(r.issuedDate),
                        r.dueDate == null ? "-" : FMT.format(r.dueDate)});
            }

            int[] s = dao.stats(me.userId);
            for (int i = 0; i < 4; i++) statValues[i].setText(String.valueOf(s[i]));
            countLbl.setText(rows.size() + (rows.size() == 1 ? " item" : " items"));
            ((CardLayout) tableHost.getLayout()).show(tableHost, rows.isEmpty() ? "empty" : "table");
            updateActions();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private Models.Resource selected() {
        int r = table.getSelectedRow();
        return r < 0 || r >= rows.size() ? null : rows.get(r);
    }

    private void updateActions() {
        Models.Resource r = selected();
        boolean has = r != null;
        boolean mine = has && r.ownerId.equals(me.userId);
        borrowBtn.setEnabled(has && r.available && !mine);
        returnBtn.setEnabled(has && !r.available && (mine || me.userId.equals(r.issuedToId)));
        editBtn.setEnabled(mine);
        deleteBtn.setEnabled(mine);
    }

    private void act(DbAction a, String okMsg) {
        try {
            a.run();
            loadCategories();
            reload();
            flash(okMsg, Ui.SUCCESS);
        } catch (Exception e) {
            flash(e.getMessage() == null ? "Something went wrong" : e.getMessage(), Ui.DANGER);
        }
    }

    private void onAdd() {
        Models.Resource r = new ResourceDialog(this, me, null, categoryNames).showDialog();
        if (r != null) act(() -> dao.add(r), "Resource added");
    }

    private void onEdit() {
        Models.Resource sel = selected();
        if (sel == null) return;
        Models.Resource upd = new ResourceDialog(this, me, sel, categoryNames).showDialog();
        if (upd != null) act(() -> dao.update(upd), "Changes saved");
    }

    private void onBorrow() {
        Models.Resource sel = selected();
        if (sel == null) return;
        LocalDate borrowedOn = LocalDate.now();
        LocalDate dueDate = borrowedOn.plusDays(7);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Borrow this resource?\nReturn it by: " + FMT.format(dueDate)
                        + " (7-day borrowing period)",
                "Confirm borrowing", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        act(() -> {
            if (!dao.issue(sel.id, me.userId, borrowedOn, dueDate)) {
                throw new IllegalStateException("Someone just borrowed this item.");
            }
        }, "You borrowed \"" + sel.name + "\". Due in 7 days.");
    }

    private void onReturn() {
        Models.Resource sel = selected();
        if (sel == null) return;
        act(() -> dao.returnItem(sel.id), "\"" + sel.name + "\" marked as returned");
    }

    private void showBorrowingHistory() {
        try {
            List<Models.BorrowRecord> history = dao.borrowingHistory(me.userId);
            String[] columns = {"Resource", "Activity", "Other student", "Borrowed on", "Due date", "Returned on", "Status"};
            DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (Models.BorrowRecord record : history) {
                boolean borrowedByMe = me.userId.equals(record.borrowerId);
                historyModel.addRow(new Object[]{
                        record.resourceName,
                        borrowedByMe ? "Borrowed" : "Lent",
                        borrowedByMe ? record.ownerName : record.borrowerName,
                        FMT.format(record.borrowedOn),
                        FMT.format(record.dueDate),
                        record.returnedOn == null ? "-" : FMT.format(record.returnedOn),
                        record.status()
                });
            }
            JTable historyTable = new JTable(historyModel);
            historyTable.setRowHeight(32);
            historyTable.setAutoCreateRowSorter(true);
            JScrollPane scroll = new JScrollPane(historyTable);
            scroll.setPreferredSize(new Dimension(850, 360));
            if (history.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No borrowing history yet.",
                        "Borrowing History", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, scroll, "Borrowing History", JOptionPane.PLAIN_MESSAGE);
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void onDelete() {
        Models.Resource sel = selected();
        if (sel == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete \"" + sel.name + "\" permanently?", "Delete resource",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) act(() -> dao.delete(sel.id), "Resource deleted");
    }

    private void flash(String msg, Color c) {
        flashLbl.setForeground(c);
        flashLbl.setText(msg);
        if (flashTimer != null) flashTimer.stop();
        flashTimer = new Timer(3500, e -> flashLbl.setText(" "));
        flashTimer.setRepeats(false);
        flashTimer.start();
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + e.getMessage(),
                "Unishare", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------- status pill cell

    private static class PillRenderer extends JLabel implements TableCellRenderer {
        private Color rowBg = Color.WHITE;
        private Color pill = Ui.SUCCESS_BG;

        PillRenderer() {
            setOpaque(false);
            setFont(Ui.font(Font.BOLD, 12f));
        }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean foc, int row, int col) {
            boolean ok = "Available".equals(v);
            setText(String.valueOf(v));
            setForeground(ok ? Ui.SUCCESS : Ui.WARN);
            pill = ok ? Ui.SUCCESS_BG : Ui.WARN_BG;
            rowBg = sel ? Ui.PRIMARY_SOFT : Color.WHITE;
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(rowBg);
            g2.fillRect(0, 0, w, h);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(getText()) + 24, th = 24;
            int x = 10, y = (h - th) / 2;
            g2.setColor(pill);
            g2.fillRoundRect(x, y, tw, th, th, th);
            g2.setColor(getForeground());
            g2.drawString(getText(), x + 12, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
