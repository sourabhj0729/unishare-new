package unishare;

import java.awt.BorderLayout;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Buy-and-sell marketplace embedded in the existing UniShare desktop application. */
public final class MarketplaceFrame extends JFrame {
    private final Models.User me;
    private final MarketplaceDAO dao=new MarketplaceDAO();
    private final List<MarketplaceDAO.Product> products=new ArrayList<>();
    private final Map<Integer,Integer> cart=new LinkedHashMap<>();
    private final DefaultTableModel model=new DefaultTableModel(
        new String[]{"ID","Product","Category","Seller","Price (INR)","Stock"},0) {
        @Override public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=new JTable(model);
    private final JTextField search=new JTextField(22);
    private final JComboBox<String> category=new JComboBox<>();
    private final JLabel status=Ui.label("Your campus store",Font.PLAIN,12f,Ui.MUTED);

    public MarketplaceFrame(Models.User me) {
        super("UniShare — Campus Marketplace");
        this.me=me;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050,650);
        setMinimumSize(new Dimension(850,550));
        setLocationRelativeTo(null);
        build();
        reload();
    }

    private void build() {
        JPanel root=new JPanel(new BorderLayout(12,12));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(22,24,20,24));
        JPanel top=new JPanel(new BorderLayout(8,8)); top.setOpaque(false);
        JPanel heading=new JPanel(new BorderLayout()); heading.setOpaque(false);
        JPanel labels=new JPanel(); labels.setOpaque(false); labels.setLayout(new javax.swing.BoxLayout(labels,javax.swing.BoxLayout.Y_AXIS));
        labels.add(Ui.label("Campus Marketplace",Font.BOLD,25f,Ui.TEXT));
        labels.add(Ui.label("Buy and sell items with other students",Font.PLAIN,13f,Ui.MUTED));
        heading.add(labels,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false);
        Ui.Button sell=new Ui.Button("＋ Sell an item",Ui.Button.Kind.PRIMARY);
        Ui.Button add=new Ui.Button("Add to cart",Ui.Button.Kind.SECONDARY);
        Ui.Button checkout=new Ui.Button("Checkout",Ui.Button.Kind.PRIMARY);
        Ui.Button orders=new Ui.Button("My orders",Ui.Button.Kind.GHOST);
        sell.addActionListener(e->sellItem()); add.addActionListener(e->addToCart());
        checkout.addActionListener(e->checkout()); orders.addActionListener(e->showOrders());
        actions.add(sell); actions.add(add); actions.add(checkout); actions.add(orders);
        heading.add(actions,BorderLayout.EAST);
        top.add(heading,BorderLayout.NORTH);
        JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); filters.setOpaque(false);
        search.setFont(Ui.font(Font.PLAIN,13f)); Ui.height(search,38);
        category.addItem("All categories"); Ui.style(category); Ui.height(category,38);
        Ui.Button find=new Ui.Button("Search",Ui.Button.Kind.SECONDARY);
        find.addActionListener(e->reload()); search.addActionListener(e->reload());
        category.addActionListener(e->{if(category.getSelectedItem()!=null) reload();});
        filters.add(search); filters.add(category); filters.add(find);
        top.add(filters,BorderLayout.SOUTH);
        table.setRowHeight(36); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true); table.setFont(Ui.font(Font.PLAIN,13f));
        table.getTableHeader().setFont(Ui.font(Font.BOLD,12f));
        JScrollPane scroll=new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        JPanel bottom=new JPanel(new BorderLayout()); bottom.setOpaque(false);
        bottom.add(status,BorderLayout.WEST);
        root.add(top,BorderLayout.NORTH); root.add(scroll,BorderLayout.CENTER); root.add(bottom,BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void reload() {
        try {
            Object selected=category.getSelectedItem();
            List<String> cats=dao.categories();
            category.removeAllItems(); category.addItem("All categories");
            for(String c:cats) category.addItem(c);
            if(selected!=null) category.setSelectedItem(selected);
            products.clear();
            products.addAll(dao.products(search.getText(),(String)category.getSelectedItem()));
            model.setRowCount(0);
            for(MarketplaceDAO.Product p:products)
                model.addRow(new Object[]{p.id,p.title,p.category,p.sellerName,p.price.toPlainString(),p.stock});
            status.setText(products.size()+" products available   •   "+cart.values().stream().mapToInt(Integer::intValue).sum()+" items in cart");
        } catch(SQLException ex) { error("Could not load marketplace: "+ex.getMessage()); }
    }

    private MarketplaceDAO.Product selected() {
        int row=table.getSelectedRow();
        return row<0 || row>=products.size()?null:products.get(row);
    }

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
        status.setText(cart.values().stream().mapToInt(Integer::intValue).sum()+" items in cart");
    }

    private void sellItem() {
        JTextField title=new JTextField(); JTextField cat=new JTextField("Books");
        JTextArea desc=new JTextArea(4,24); JTextField price=new JTextField(); JTextField stock=new JTextField("1");
        JPanel form=new JPanel(new java.awt.GridLayout(0,1,5,5));
        form.add(new JLabel("Product name"));form.add(title);form.add(new JLabel("Category"));form.add(cat);
        form.add(new JLabel("Description"));form.add(new JScrollPane(desc));form.add(new JLabel("Price (INR)"));form.add(price);
        form.add(new JLabel("Quantity available"));form.add(stock);
        int choice=JOptionPane.showConfirmDialog(this,form,"Create campus listing",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(choice!=JOptionPane.OK_OPTION)return;
        try {
            String t=title.getText().trim(), c=cat.getText().trim(), d=desc.getText().trim();
            BigDecimal amount=new BigDecimal(price.getText().trim()); int qty=Integer.parseInt(stock.getText().trim());
            if(t.isEmpty()||t.length()>120||c.isEmpty()||c.length()>50||d.length()>500)throw new IllegalArgumentException("Check product name, category and description lengths.");
            if(amount.compareTo(BigDecimal.ZERO)<=0||amount.scale()>2||amount.compareTo(new BigDecimal("9999999.99"))>0)throw new IllegalArgumentException("Price must be positive and have at most 2 decimal places.");
            if(qty<1||qty>10000)throw new IllegalArgumentException("Quantity must be between 1 and 10000.");
            dao.createProduct(t,c,d,amount,qty,me.userId);
            JOptionPane.showMessageDialog(this,"Your product is now listed.","Listing created",JOptionPane.INFORMATION_MESSAGE);
            reload();
        } catch(NumberFormatException ex){error("Enter a valid price and whole-number stock quantity.");}
          catch(Exception ex){error(ex.getMessage());}
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
