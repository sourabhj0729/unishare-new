package unishare;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Buy-and-sell marketplace embedded in the existing UniShare desktop application. */
public final class MarketplaceFrame extends JPanel {
    private final Models.User me;
    private final MarketplaceDAO dao=new MarketplaceDAO();
    private final List<MarketplaceDAO.Product> products=new ArrayList<>();
    private final Map<Integer,Integer> cart=new LinkedHashMap<>();
    private final JPanel productsGrid = new JPanel(new GridLayout(0, 3, 18, 18));
    private MarketplaceDAO.Product selectedProduct;
    private Ui.Button cartButton;
    private final JTextField search=new JTextField(22);
    private final JComboBox<String> category=new JComboBox<>();
    private final JLabel status=Ui.label("Your campus store",Font.PLAIN,12f,Ui.MUTED);
    private boolean loadingCategories;

    public MarketplaceFrame(Models.User me) {
        this.me=me;
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        build();
        reload();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 22, 30));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.label("UNISHARE  /  CAMPUS MARKETPLACE", Font.BOLD, 10f, Ui.PRIMARY));
        titles.add(Box.createVerticalStrut(7));
        titles.add(Ui.label("Find something useful.", Font.BOLD, 29f, Ui.TEXT));
        titles.add(Box.createVerticalStrut(4));
        titles.add(Ui.label("Buy, sell and discover items shared by students on your campus.", Font.PLAIN, 14f, Ui.MUTED));
        heading.add(titles, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 8));
        actions.setOpaque(false);
        Ui.Button sell = new Ui.Button("+  Sell an item", Ui.Button.Kind.PRIMARY);
        cartButton = new Ui.Button("Cart (" + cartCount() + ")", Ui.Button.Kind.SECONDARY);
        Ui.Button orders = new Ui.Button("My orders", Ui.Button.Kind.SECONDARY);
        sell.addActionListener(e -> sellItem());
        cartButton.addActionListener(e -> showCartSummary());
        orders.addActionListener(e -> showOrders());
        actions.add(cartButton); actions.add(orders); actions.add(sell);
        heading.add(actions, BorderLayout.EAST);

        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Ui.BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        search.setFont(Ui.font(Font.PLAIN, 14f));
        search.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        search.setToolTipText("Search by product, category or seller");
        Ui.height(search, 40);
        category.addItem("All categories"); Ui.style(category); Ui.height(category, 40);
        category.setPreferredSize(new Dimension(180, 40));
        Ui.Button find = new Ui.Button("Search", Ui.Button.Kind.PRIMARY);
        find.addActionListener(e -> reload()); search.addActionListener(e -> reload());
        category.addActionListener(e -> { if (!loadingCategories && category.getSelectedItem() != null) reload(); });
        searchPanel.add(search, BorderLayout.CENTER);
        JPanel rightFilters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightFilters.setOpaque(false); rightFilters.add(category); rightFilters.add(find);
        searchPanel.add(rightFilters, BorderLayout.EAST);

        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.add(Ui.label("Recently listed", Font.BOLD, 19f, Ui.TEXT), BorderLayout.WEST);
        section.add(status, BorderLayout.EAST);
        productsGrid.setOpaque(false);
        productsGrid.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        JScrollPane scroll = new JScrollPane(productsGrid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Ui.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false); center.add(section, BorderLayout.NORTH); center.add(scroll, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false); content.add(searchPanel, BorderLayout.NORTH); content.add(center, BorderLayout.CENTER);
        root.add(heading, BorderLayout.NORTH); root.add(content, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    private int cartCount() {
        return cart.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void showCartSummary() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty. Add a product to get started.", "My Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder summary = new StringBuilder("Items in your cart:\n\n");
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer,Integer> entry : cart.entrySet()) {
            for (MarketplaceDAO.Product p : products) if (p.id == entry.getKey()) {
                summary.append(p.title).append("  × ").append(entry.getValue()).append("  —  ₹")
                    .append(p.price.multiply(BigDecimal.valueOf(entry.getValue())).toPlainString()).append("\\n");
                total = total.add(p.price.multiply(BigDecimal.valueOf(entry.getValue())));
            }
        }
        summary.append("\nSubtotal: ₹").append(total.toPlainString());
        Object[] options = {"Continue shopping", "Checkout"};
        int choice = JOptionPane.showOptionDialog(this, summary.toString(), "My Cart",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 1) checkout();
    }

    private JComponent productCard(MarketplaceDAO.Product product) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xE3EAEB)),
            BorderFactory.createEmptyBorder(0, 0, 12, 0)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel artwork = new JPanel(new BorderLayout());
        artwork.setPreferredSize(new Dimension(220, 132));
        artwork.setBackground(categoryColor(product.category));
        JLabel categoryLabel = Ui.label(product.category.toUpperCase(), Font.BOLD, 11f, Color.WHITE);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 0));
        artwork.add(categoryLabel, BorderLayout.NORTH);
        JLabel symbol = Ui.label(categorySymbol(product.category), Font.BOLD, 42f, Color.WHITE);
        symbol.setHorizontalAlignment(SwingConstants.CENTER);
        artwork.add(symbol, BorderLayout.CENTER);

        JPanel details = new JPanel();
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 14));
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel price = Ui.label("₹" + product.price.toPlainString(), Font.BOLD, 21f, Ui.TEXT);
        JLabel name = Ui.label(product.title, Font.BOLD, 14f, Ui.TEXT);
        JLabel meta = Ui.label(product.category + "  ·  " + product.stock + " available", Font.PLAIN, 11f, Ui.MUTED);
        JLabel seller = Ui.label("Listed by " + product.sellerName, Font.PLAIN, 12f, Ui.MUTED);
        price.setAlignmentX(0f); name.setAlignmentX(0f); meta.setAlignmentX(0f); seller.setAlignmentX(0f);
        details.add(price); details.add(Box.createVerticalStrut(6)); details.add(name);
        details.add(Box.createVerticalStrut(5)); details.add(meta);
        details.add(Box.createVerticalStrut(7)); details.add(seller);
        details.add(Box.createVerticalStrut(12));
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false);
        row.add(Ui.label("●  Campus listing", Font.BOLD, 10f, Ui.SUCCESS), BorderLayout.WEST);
        Ui.Button add = new Ui.Button("Add to cart", Ui.Button.Kind.PRIMARY);
        add.addActionListener(e -> { selectedProduct = product; addToCart(); });
        row.add(add, BorderLayout.EAST); details.add(row);
        card.add(artwork, BorderLayout.NORTH); card.add(details, BorderLayout.CENTER);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getSource() == card) { selectedProduct = product; showProductDetails(product); }
            }
        });
        return card;
    }

    private Color categoryColor(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (c.contains("book")) return new Color(0xB77945);
        if (c.contains("elect")) return new Color(0x477F8B);
        if (c.contains("sport")) return new Color(0x4F8B70);
        if (c.contains("hostel") || c.contains("furn")) return new Color(0x8873A8);
        if (c.contains("station")) return new Color(0xC28A46);
        return new Color(0x398E82);
    }

    private String categorySymbol(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (c.contains("book")) return "Aa";
        if (c.contains("elect")) return "⌁";
        if (c.contains("sport")) return "↗";
        if (c.contains("hostel") || c.contains("furn")) return "⌂";
        if (c.contains("station")) return "✎";
        return "U";
    }

    private void showProductDetails(MarketplaceDAO.Product p) {
        String detail = p.title + "\n\n" + p.description + "\n\nCategory: " + p.category
            + "\nSeller: " + p.sellerName + "\nPrice: ₹" + p.price.toPlainString()
            + "\\nAvailable: " + p.stock;
        Object[] options = {"Close", "Add to cart"};
        int choice = JOptionPane.showOptionDialog(this, detail, "Product details",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 1) addToCart();
    }

    private void reload() {
        try {
            Object selected = category.getSelectedItem();
            List<String> cats = dao.categories();
            loadingCategories = true;
            category.removeAllItems(); category.addItem("All categories");
            for (String name : cats) category.addItem(name);
            if (selected != null) category.setSelectedItem(selected);
            loadingCategories = false;
            products.clear();
            products.addAll(dao.products(search.getText(), (String) category.getSelectedItem()));
            productsGrid.removeAll();
            productsGrid.setLayout(new GridLayout(0, 3, 18, 18));
            for (MarketplaceDAO.Product p : products) productsGrid.add(productCard(p));
            if (products.isEmpty()) {
                productsGrid.setLayout(new BorderLayout());
                JPanel empty = new JPanel();
                empty.setOpaque(false); empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
                JLabel title = Ui.label("No products found", Font.BOLD, 20f, Ui.TEXT);
                JLabel note = Ui.label("Try another search or be the first student to list this item.", Font.PLAIN, 13f, Ui.MUTED);
                title.setAlignmentX(0f); note.setAlignmentX(0f);
                empty.add(title); empty.add(Box.createVerticalStrut(8)); empty.add(note);
                productsGrid.add(empty, BorderLayout.CENTER);
            }
            status.setText(products.size() + " items  ·  " + cartCount() + " in cart");
            productsGrid.revalidate(); productsGrid.repaint();
        } catch (SQLException ex) {
            loadingCategories = false;
            error("Could not load marketplace: " + ex.getMessage());
        }
    }

    private MarketplaceDAO.Product selected() { return selectedProduct; }

    private void addToCart() {
        MarketplaceDAO.Product p=selected();
        if(p==null){error("Select a product first.");return;}
        if(p.sellerId.equals(me.userId)){error("You cannot buy your own listing.");return;}
        int qty=1;
        String raw=JOptionPane.showInputDialog(this,"Quantity (available: "+p.stock+")","1");
        if(raw==null)return;
        try{qty=Integer.parseInt(raw.trim());}catch(NumberFormatException ex){error("Enter a whole-number quantity.");return;}
        if(qty<1 || qty>p.stock){error("Quantity must be between 1 and "+p.stock+".");return;}
        cart.put(p.id,cart.getOrDefault(p.id,0)+qty);
        status.setText(products.size() + " items  ·  " + cartCount() + " in cart");
        if (cartButton != null) cartButton.setText("Cart (" + cartCount() + ")");
        JOptionPane.showMessageDialog(this, p.title + " added to your cart.", "Added to cart", JOptionPane.INFORMATION_MESSAGE);
    }

    private void sellItem() {
        JTextField title = new JTextField();
        JTextField cat = new JTextField("Books");
        JTextArea desc = new JTextArea(3, 20);
        JTextField price = new JTextField();
        JTextField stock = new JTextField("1");

        styleMarketplaceField(title);
        styleMarketplaceField(cat);
        styleMarketplaceField(price);
        styleMarketplaceField(stock);
        desc.setFont(Ui.font(Font.PLAIN, 13f));
        desc.setForeground(Ui.TEXT);
        desc.setBackground(Color.WHITE);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Create campus listing", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel();
        header.setBackground(Ui.SIDEBAR);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        JLabel heading = Ui.label("Sell an item", Font.BOLD, 21f, Color.WHITE);
        JLabel subheading = Ui.label("Add a product for students on your campus", Font.PLAIN, 12f, new Color(0xC7D2FE));
        heading.setAlignmentX(0f);
        subheading.setAlignmentX(0f);
        header.add(heading);
        header.add(javax.swing.Box.createVerticalStrut(5));
        header.add(subheading);

        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        form.add(marketplaceField("Product name", title, "e.g. Engineering Mathematics textbook"));
        form.add(javax.swing.Box.createVerticalStrut(13));
        form.add(marketplaceField("Category", cat, "e.g. Books, Electronics, Stationery"));
        form.add(javax.swing.Box.createVerticalStrut(13));

        JScrollPane descriptionScroll = new JScrollPane(desc);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        descriptionScroll.setPreferredSize(new Dimension(100, 78));
        descriptionScroll.setMinimumSize(new Dimension(100, 78));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        form.add(marketplaceField("Description", descriptionScroll, "Condition, details, or pickup notes"));
        form.add(javax.swing.Box.createVerticalStrut(13));

        JPanel priceStock = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        priceStock.setOpaque(false);
        priceStock.add(marketplaceField("Price (₹)", price, "e.g. 450"));
        priceStock.add(marketplaceField("Available quantity", stock, "e.g. 1"));
        priceStock.setAlignmentX(0f);
        priceStock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        form.add(priceStock);

        JLabel hint = Ui.label("Your listing will be visible to other registered students.", Font.PLAIN, 11f, Ui.MUTED);
        hint.setAlignmentX(0f);
        form.add(javax.swing.Box.createVerticalStrut(12));
        form.add(hint);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(new Color(0xF9FAFB));
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.BORDER),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        Ui.Button cancel = new Ui.Button("Cancel", Ui.Button.Kind.SECONDARY);
        Ui.Button publish = new Ui.Button("Publish listing", Ui.Button.Kind.PRIMARY);
        cancel.addActionListener(e -> dialog.dispose());
        publish.addActionListener(e -> {
            try {
                String t = title.getText().trim();
                String categoryName = cat.getText().trim();
                String description = desc.getText().trim();
                BigDecimal amount = new BigDecimal(price.getText().trim());
                int quantity = Integer.parseInt(stock.getText().trim());

                if (t.isEmpty() || t.length() > 120 || categoryName.isEmpty() || categoryName.length() > 50 || description.length() > 500)
                    throw new IllegalArgumentException("Check the product name, category and description lengths.");
                if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2 || amount.compareTo(new BigDecimal("9999999.99")) > 0)
                    throw new IllegalArgumentException("Enter a positive price with at most 2 decimal places.");
                if (quantity < 1 || quantity > 10000)
                    throw new IllegalArgumentException("Quantity must be between 1 and 10000.");

                dao.createProduct(t, categoryName, description, amount, quantity, me.userId);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Your product is now listed.", "Listing published", JOptionPane.INFORMATION_MESSAGE);
                reload();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Enter a valid price and whole-number quantity.", "Check your details", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Could not publish listing", JOptionPane.WARNING_MESSAGE);
            }
        });
        footer.add(cancel);
        footer.add(publish);

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setSize(470, 540);
        dialog.setMinimumSize(new Dimension(470, 540));
        dialog.setMaximumSize(new Dimension(470, 540));
        dialog.setResizable(false);
        dialog.getRootPane().setDefaultButton(publish);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void styleMarketplaceField(JTextField field) {
        Ui.style(field);
        field.setFont(Ui.font(Font.PLAIN, 13f));
        Ui.height(field, 38);
    }

    private JPanel marketplaceField(String caption, JComponent input, String placeholderHint) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new javax.swing.BoxLayout(group, javax.swing.BoxLayout.Y_AXIS));
        JLabel label = Ui.label(caption, Font.BOLD, 12f, new Color(0x374151));
        label.setAlignmentX(0f);
        input.setAlignmentX(0f);
        group.add(label);
        group.add(javax.swing.Box.createVerticalStrut(6));
        group.add(input);
        group.setAlignmentX(0f);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, input instanceof JScrollPane ? 104 : 64));
        return group;
    }

    private void checkout() {
        if(cart.isEmpty()){error("Your cart is empty. Select a product and add it first.");return;}
        String pickup=JOptionPane.showInputDialog(this,"Enter a campus pickup location (for example, Main Gate):","Campus pickup");
        if(pickup==null)return;
        if(pickup.trim().isEmpty()){error("Pickup location is required.");return;}
        try {
            long id=dao.checkout(me.userId,cart,pickup);
            cart.clear();
            JOptionPane.showMessageDialog(this,"Order #"+id+" placed successfully. Payment is arranged directly with the seller at pickup; no online payment is processed.","Order placed",JOptionPane.INFORMATION_MESSAGE);
            reload();
        } catch(Exception ex){error("Checkout failed: "+ex.getMessage());reload();}
    }

    private void showOrders() {
        try {
            List<String[]> rows=dao.myOrders(me.userId);
            if(rows.isEmpty()){JOptionPane.showMessageDialog(this,"You have not placed any marketplace orders yet.");return;}
            DefaultTableModel m=new DefaultTableModel(new String[]{"Order","Date","Status","Total (INR)","Pickup","Items"},0) {
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            for(String[] row:rows)m.addRow(row);
            JTable t=new JTable(m);t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);t.setRowHeight(30);
            t.getColumnModel().getColumn(5).setPreferredWidth(320);
            JScrollPane pane=new JScrollPane(t);pane.setPreferredSize(new Dimension(900,300));
            JOptionPane.showMessageDialog(this,pane,"My marketplace orders",JOptionPane.PLAIN_MESSAGE);
        } catch(SQLException ex){error("Could not load orders: "+ex.getMessage());}
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this,message,"Campus Marketplace",JOptionPane.WARNING_MESSAGE);
    }
}
