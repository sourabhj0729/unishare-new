package unishare;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JDBC operations for the campus buy-and-sell marketplace. */
public final class MarketplaceDAO {
    public static final class Product {
        public int id;
        public String title, category, description, sellerId, sellerName;
        public BigDecimal price;
        public int stock;
    }

    public List<Product> products(String keyword, String category) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT p.id,p.title,p.category,p.description,p.price,p.stock,p.seller_id,u.full_name " +
            "FROM marketplace_products p JOIN users u ON u.user_id=p.seller_id WHERE p.active=TRUE AND p.stock>0");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (p.title LIKE ? OR p.description LIKE ? OR p.category LIKE ? OR u.full_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            for (int i=0;i<4;i++) args.add(like);
        }
        if (category != null && !category.equals("All categories")) {
            sql.append(" AND p.category=?");
            args.add(category);
        }
        sql.append(" ORDER BY p.created_at DESC,p.id DESC");
        List<Product> out = new ArrayList<>();
        try (Connection c=Database.connect(); PreparedStatement ps=c.prepareStatement(sql.toString())) {
            for(int i=0;i<args.size();i++) ps.setObject(i+1,args.get(i));
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) {
                    Product p=new Product();
                    p.id=rs.getInt("id"); p.title=rs.getString("title");
                    p.category=rs.getString("category"); p.description=rs.getString("description");
                    p.price=rs.getBigDecimal("price"); p.stock=rs.getInt("stock");
                    p.sellerId=rs.getString("seller_id"); p.sellerName=rs.getString("full_name");
                    out.add(p);
                }
            }
        }
        return out;
    }

    public List<String> categories() throws SQLException {
        List<String> out=new ArrayList<>();
        try(Connection c=Database.connect(); Statement s=c.createStatement();
            ResultSet rs=s.executeQuery("SELECT DISTINCT category FROM marketplace_products WHERE active=TRUE ORDER BY category")) {
            while(rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    public void createProduct(String title,String category,String description,BigDecimal price,int stock,String sellerId) throws SQLException {
        String sql="INSERT INTO marketplace_products(title,category,description,price,stock,seller_id) VALUES(?,?,?,?,?,?)";
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1,title.trim()); ps.setString(2,category.trim());
            ps.setString(3,description==null?"":description.trim()); ps.setBigDecimal(4,price);
            ps.setInt(5,stock); ps.setString(6,sellerId); ps.executeUpdate();
        }
    }

    /** Places all cart lines as one order and deducts inventory in a single transaction. */
    public long checkout(String buyerId,Map<Integer,Integer> cart,String pickupPoint) throws SQLException {
        if(cart==null || cart.isEmpty()) throw new IllegalArgumentException("Your cart is empty.");
        try(Connection c=Database.connect()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total=BigDecimal.ZERO;
                List<Product> locked=new ArrayList<>();
                for(Map.Entry<Integer,Integer> entry:cart.entrySet()) {
                    if(entry.getValue()==null || entry.getValue()<1) throw new IllegalArgumentException("Invalid cart quantity.");
                    try(PreparedStatement ps=c.prepareStatement(
                        "SELECT id,title,price,stock,seller_id FROM marketplace_products WHERE id=? AND active=TRUE FOR UPDATE")) {
                        ps.setInt(1,entry.getKey());
                        try(ResultSet rs=ps.executeQuery()) {
                            if(!rs.next()) throw new IllegalStateException("A product in your cart is no longer available.");
                            Product p=new Product(); p.id=rs.getInt("id"); p.title=rs.getString("title");
                            p.price=rs.getBigDecimal("price"); p.stock=rs.getInt("stock"); p.sellerId=rs.getString("seller_id");
                            if(p.sellerId.equals(buyerId)) throw new IllegalArgumentException("You cannot buy your own listing.");
                            if(p.stock<entry.getValue()) throw new IllegalStateException("Not enough stock for "+p.title+". Available: "+p.stock);
                            total=total.add(p.price.multiply(BigDecimal.valueOf(entry.getValue())));
                            locked.add(p);
                        }
                    }
                }
                long orderId;
                try(PreparedStatement ps=c.prepareStatement(
                    "INSERT INTO marketplace_orders(buyer_id,total_amount,pickup_point,status) VALUES(?,?,?,'PLACED')",
                    Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1,buyerId); ps.setBigDecimal(2,total); ps.setString(3,pickupPoint.trim()); ps.executeUpdate();
                    try(ResultSet keys=ps.getGeneratedKeys()) { if(!keys.next()) throw new SQLException("Could not create order."); orderId=keys.getLong(1); }
                }
                for(Product p:locked) {
                    int qty=cart.get(p.id);
                    try(PreparedStatement ps=c.prepareStatement(
                        "INSERT INTO marketplace_order_items(order_id,product_id,product_title,seller_id,quantity,unit_price) VALUES(?,?,?,?,?,?)")) {
                        ps.setLong(1,orderId); ps.setInt(2,p.id); ps.setString(3,p.title);
                        ps.setString(4,p.sellerId); ps.setInt(5,qty); ps.setBigDecimal(6,p.price); ps.executeUpdate();
                    }
                    try(PreparedStatement ps=c.prepareStatement(
                        "UPDATE marketplace_products SET stock=stock-? WHERE id=? AND stock>=?")) {
                        ps.setInt(1,qty); ps.setInt(2,p.id); ps.setInt(3,qty);
                        if(ps.executeUpdate()!=1) throw new IllegalStateException("Stock changed during checkout. Please try again.");
                    }
                }
                c.commit();
                return orderId;
            } catch(SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally { c.setAutoCommit(true); }
        }
    }

    public List<String[]> myOrders(String userId) throws SQLException {
        String sql="SELECT o.id,o.created_at,o.status,o.total_amount,o.pickup_point,"+
            "GROUP_CONCAT(CONCAT(i.product_title,' x',i.quantity) ORDER BY i.id SEPARATOR ', ') AS items "+
            "FROM marketplace_orders o JOIN marketplace_order_items i ON i.order_id=o.id "+
            "WHERE o.buyer_id=? GROUP BY o.id,o.created_at,o.status,o.total_amount,o.pickup_point ORDER BY o.created_at DESC";
        List<String[]> out=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1,userId);
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) out.add(new String[]{String.valueOf(rs.getLong("id")),rs.getTimestamp("created_at").toString(),
                    rs.getString("status"),rs.getBigDecimal("total_amount").toPlainString(),rs.getString("pickup_point"),rs.getString("items")});
            }
        }
        return out;
    }
}
