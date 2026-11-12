package unishare;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.SQLException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/** Sign-in / create-account window. */
public class LoginFrame extends JFrame {
    private final UserDAO users = new UserDAO();
    private final CardLayout cards = new CardLayout();
    private final JPanel host = new JPanel(cards);

    private final Ui.HintField loginId = new Ui.HintField("Enter your user ID");
    private final JPasswordField loginPw = new JPasswordField();
    private final JLabel loginMsg = Ui.label(" ", Font.BOLD, 12f, Ui.DANGER);

    private final Ui.HintField regName = new Ui.HintField("e.g. Aarav Sharma");
    private final Ui.HintField regId = new Ui.HintField("e.g. cs2024017");
    private final Ui.HintField regDept = new Ui.HintField("e.g. Computer Science");
    private final JPasswordField regPw = new JPasswordField();
    private final JPasswordField regPw2 = new JPasswordField();
    private final JLabel regMsg = Ui.label(" ", Font.BOLD, 12f, Ui.DANGER);

    public LoginFrame() {
        super("Unishare - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.add(new Brand(), BorderLayout.WEST);

        host.setBackground(Color.WHITE);
        host.add(wrap(buildLogin()), "login");
        host.add(wrap(buildRegister()), "register");
        root.add(host, BorderLayout.CENTER);

        setContentPane(root);
        setSize(1020, 700);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    // ----------------------------------------------------------------- layout

    private JPanel wrap(JPanel form) {
        JPanel w = new JPanel(new GridBagLayout());
        w.setBackground(Color.WHITE);
        w.add(form);
        return w;
    }

    private JPanel column() {
        JPanel p = new JPanel() {
            @Override public Dimension getPreferredSize() {
                return new Dimension(440, super.getPreferredSize().height);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private <T extends JComponent> T left(T c) {
        c.setAlignmentX(0f);
        return c;
    }

    private JPanel buildLogin() {
        JPanel p = column();
        p.add(left(Ui.label("Welcome back", Font.BOLD, 28f, Ui.TEXT)));
        p.add(left(Ui.label("Sign in with your user ID to continue.", Font.PLAIN, 14f, Ui.MUTED)));
        p.add(Box.createVerticalStrut(28));

        Ui.style(loginId);
        Ui.height(loginId, 44);
        Ui.style(loginPw);
        Ui.height(loginPw, 44);
        p.add(left(Ui.labeled("User ID", loginId)));
        p.add(Box.createVerticalStrut(16));
        p.add(left(Ui.labeled("Password", loginPw)));
        p.add(Box.createVerticalStrut(8));

        final char echo = loginPw.getEchoChar();
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        show.setFont(Ui.font(Font.PLAIN, 12f));
        show.setForeground(Ui.MUTED);
        show.setFocusPainted(false);
        show.addActionListener(e -> loginPw.setEchoChar(show.isSelected() ? (char) 0 : echo));
        p.add(left(show));
        p.add(Box.createVerticalStrut(10));
        p.add(left(loginMsg));
        p.add(Box.createVerticalStrut(10));

        Ui.Button signIn = new Ui.Button("Sign in", Ui.Button.Kind.PRIMARY);
        Ui.height(signIn, 46);
        signIn.setAlignmentX(0f);
        signIn.addActionListener(e -> doLogin());
        p.add(signIn);
        p.add(Box.createVerticalStrut(20));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(Ui.label("New to Unishare? ", Font.PLAIN, 13f, Ui.MUTED));
        Ui.Button create = new Ui.Button("Create an account", Ui.Button.Kind.LINK);
        create.addActionListener(e -> {
            setMsg(regMsg, " ", Ui.DANGER);
            cards.show(host, "register");
        });
        row.add(create);
        row.setAlignmentX(0f);
        p.add(row);

        loginId.addActionListener(e -> loginPw.requestFocusInWindow());
        loginPw.addActionListener(e -> doLogin());
        return p;
    }

    private JPanel buildRegister() {
        JPanel p = column();
        p.add(left(Ui.label("Create your account", Font.BOLD, 28f, Ui.TEXT)));
        p.add(left(Ui.label("Join Unishare and start sharing resources.", Font.PLAIN, 14f, Ui.MUTED)));
        p.add(Box.createVerticalStrut(24));

        for (JComponent f : new JComponent[]{regName, regId, regDept, regPw, regPw2}) {
            Ui.style((javax.swing.JTextField) f);
            Ui.height(f, 44);
        }

        p.add(left(Ui.labeled("Full name", regName)));
        p.add(Box.createVerticalStrut(14));

        JPanel r1 = new JPanel(new GridLayout(1, 2, 14, 0));
        r1.setOpaque(false);
        r1.add(Ui.labeled("User ID", regId));
        r1.add(Ui.labeled("Department", regDept));
        p.add(left(r1));
        p.add(Box.createVerticalStrut(14));

        JPanel r2 = new JPanel(new GridLayout(1, 2, 14, 0));
        r2.setOpaque(false);
        r2.add(Ui.labeled("Password (min 6)", regPw));
        r2.add(Ui.labeled("Confirm password", regPw2));
        p.add(left(r2));
        p.add(Box.createVerticalStrut(12));
        p.add(left(regMsg));
        p.add(Box.createVerticalStrut(12));

        Ui.Button create = new Ui.Button("Create account", Ui.Button.Kind.PRIMARY);
        Ui.height(create, 46);
        create.setAlignmentX(0f);
        create.addActionListener(e -> doRegister());
        p.add(create);
        p.add(Box.createVerticalStrut(20));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(Ui.label("Already have an account? ", Font.PLAIN, 13f, Ui.MUTED));
        Ui.Button back = new Ui.Button("Sign in", Ui.Button.Kind.LINK);
        back.addActionListener(e -> {
            setMsg(loginMsg, " ", Ui.DANGER);
            cards.show(host, "login");
        });
        row.add(back);
        row.setAlignmentX(0f);
        p.add(row);

        regPw2.addActionListener(e -> doRegister());
        return p;
    }

    // ---------------------------------------------------------------- actions

    private void setMsg(JLabel l, String text, Color c) {
        l.setForeground(c);
        l.setText(text);
    }

    private void doLogin() {
        String id = loginId.getText().trim();
        String pw = new String(loginPw.getPassword());
        if (id.isEmpty() || pw.isEmpty()) {
            setMsg(loginMsg, "Please enter both user ID and password.", Ui.DANGER);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Models.User u = users.authenticate(id, pw);
            if (u == null) {
                setMsg(loginMsg, "Invalid user ID or password.", Ui.DANGER);
                loginPw.setText("");
                return;
            }
            new DashboardFrame(u).setVisible(true);
            dispose();
        } catch (SQLException ex) {
            setMsg(loginMsg, "Database error: " + ex.getMessage(), Ui.DANGER);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void doRegister() {
        String name = regName.getText().trim();
        String id = regId.getText().trim();
        String dept = regDept.getText().trim();
        String pw = new String(regPw.getPassword());
        String pw2 = new String(regPw2.getPassword());

        if (name.isEmpty() || id.isEmpty() || pw.isEmpty()) {
            setMsg(regMsg, "Name, user ID and password are required.", Ui.DANGER);
            return;
        }
        if (!id.matches("[A-Za-z0-9_.-]{3,30}")) {
            setMsg(regMsg, "User ID: 3-30 letters, digits, dot, dash or underscore.", Ui.DANGER);
            return;
        }
        if (pw.length() < 6) {
            setMsg(regMsg, "Password must be at least 6 characters.", Ui.DANGER);
            return;
        }
        if (!pw.equals(pw2)) {
            setMsg(regMsg, "Passwords do not match.", Ui.DANGER);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            users.register(id, name, dept, pw);
            regName.setText("");
            regId.setText("");
            regDept.setText("");
            regPw.setText("");
            regPw2.setText("");
            loginId.setText(id);
            loginPw.setText("");
            setMsg(loginMsg, "Account created! Sign in to continue.", Ui.SUCCESS);
            cards.show(host, "login");
            loginPw.requestFocusInWindow();
        } catch (IllegalArgumentException ex) {
            setMsg(regMsg, ex.getMessage(), Ui.DANGER);
        } catch (SQLException ex) {
            setMsg(regMsg, "Database error: " + ex.getMessage(), Ui.DANGER);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // ------------------------------------------------------------ brand panel

    private static class Brand extends JPanel {
        Brand() {
            setPreferredSize(new Dimension(400, 0));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(0x312E81), w, h, new Color(0x7C3AED)));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillOval(-120, h - 260, 380, 380);
            g2.fillOval(w - 160, -100, 300, 300);

            // logo
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(48, 64, 48, 48, 14, 14);
            g2.setColor(Ui.PRIMARY);
            g2.setFont(Ui.font(Font.BOLD, 28f));
            g2.drawString("U", 62, 100);
            g2.setColor(Color.WHITE);
            g2.setFont(Ui.font(Font.BOLD, 26f));
            g2.drawString("Unishare", 108, 100);

            // headline
            g2.setFont(Ui.font(Font.BOLD, 32f));
            g2.drawString("Share what you have.", 48, 250);
            g2.drawString("Borrow what you need.", 48, 292);
            g2.setColor(new Color(0xC7D2FE));
            g2.setFont(Ui.font(Font.PLAIN, 15f));
            g2.drawString("The campus hub for books, lab kits,", 48, 332);
            g2.drawString("gadgets and notes - all in one place.", 48, 354);

            // feature bullets
            String[] items = {
                "List an item in under a minute",
                "See what is available or issued",
                "Track issue dates, never lose an item"
            };
            int y = 430;
            for (String s : items) {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(48, y - 15, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setFont(Ui.font(Font.BOLD, 13f));
                g2.drawString("\u2713", 53, y + 1);
                g2.setFont(Ui.font(Font.PLAIN, 14f));
                g2.drawString(s, 84, y + 1);
                y += 38;
            }
            g2.dispose();
        }
    }
}
