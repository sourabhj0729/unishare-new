package unishare;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** Theme (colours, fonts) and small custom components used across Unishare. */
public final class Ui {
    private Ui() {}

    public static final Color PRIMARY = new Color(0x079B83);
    public static final Color PRIMARY_DARK = new Color(0x087F70);
    public static final Color PRIMARY_SOFT = new Color(0xE6F7F3);
    public static final Color BG = new Color(0xF5F8F9);
    public static final Color CARD = Color.WHITE;
    public static final Color SIDEBAR = new Color(0x123C3A);
    public static final Color SIDEBAR_HOVER = new Color(0x1D5A54);
    public static final Color TEXT = new Color(0x111827);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color BORDER = new Color(0xE5E7EB);
    public static final Color SUCCESS = new Color(0x079B83);
    public static final Color SUCCESS_BG = new Color(0xDDF7EF);
    public static final Color WARN = new Color(0xB45309);
    public static final Color WARN_BG = new Color(0xFEF3C7);
    public static final Color DANGER = new Color(0xDC2626);

    public static Font font(int style, float size) {
        return new Font("Segoe UI", Font.PLAIN, 12).deriveFont(style, size);
    }

    /** Call once before creating any window. */
    public static void install() {
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusFocus", PRIMARY);
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("Table.alternateRowColor", Color.WHITE);
        UIManager.put("defaultFont", font(Font.PLAIN, 13f));
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
    }

    // ------------------------------------------------------------------ helpers

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** Caption above a component, stacked vertically. */
    public static JPanel labeled(String caption, JComponent c) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = label(caption, Font.BOLD, 12f, new Color(0x374151));
        l.setAlignmentX(0f);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        c.setAlignmentX(0f);
        p.add(l);
        p.add(c);
        return p;
    }

    public static void height(JComponent c, int h) {
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, h));
        c.setMinimumSize(new Dimension(10, h));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    }

    private static Border idleBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }

    private static Border focusBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 2),
                BorderFactory.createEmptyBorder(8, 11, 8, 11));
    }

    public static void style(JTextField f) {
        f.setFont(font(Font.PLAIN, 14f));
        f.setBackground(Color.WHITE);
        f.setForeground(TEXT);
        f.setCaretColor(PRIMARY);
        f.setBorder(idleBorder());
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.setBorder(focusBorder()); }
            @Override public void focusLost(FocusEvent e) { f.setBorder(idleBorder()); }
        });
    }

    public static void style(JComboBox<?> c) {
        c.setFont(font(Font.PLAIN, 13f));
        c.setBackground(Color.WHITE);
    }

    // --------------------------------------------------------------- components

    /** Text field that shows grey placeholder text while empty and unfocused. */
    public static class HintField extends JTextField {
        private final String hint;

        public HintField(String hint) {
            this.hint = hint;
            style(this);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0x9CA3AF));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, getInsets().left, y);
                g2.dispose();
            }
        }
    }

    /** White rounded panel with a soft shadow and thin border. */
    public static class Card extends JPanel {
        private final int arc;
        private final Color fill;

        public Card(int arc, Color fill) {
            this.arc = arc;
            this.fill = fill;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(17, 24, 39, 14));
            g2.fillRoundRect(0, 2, w, h - 2, arc, arc);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h - 3, arc, arc);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 4, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Flat, rounded, custom-painted button in several styles. */
    public static class Button extends JButton {
        public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST, LINK, NAV }

        private final Kind kind;
        private boolean hover;
        private boolean active;

        public Button(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(font(kind == Kind.NAV ? Font.PLAIN : Font.BOLD, 13f));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        /** Highlights a sidebar (NAV) button as the current page. */
        public void setActive(boolean on) {
            active = on;
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int tw = fm.stringWidth(getText());
            switch (kind) {
                case LINK: return new Dimension(tw + 8, 28);
                case NAV:  return new Dimension(tw + 40, 44);
                default:   return new Dimension(tw + 40, 40);
            }
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color bg = null, fg = Color.WHITE, line = null;
            boolean on = isEnabled();

            switch (kind) {
                case PRIMARY:
                    bg = !on ? new Color(0xE5E7EB) : hover ? PRIMARY_DARK : PRIMARY;
                    fg = on ? Color.WHITE : new Color(0x9CA3AF);
                    break;
                case DANGER:
                    bg = !on ? new Color(0xE5E7EB) : hover ? new Color(0xB91C1C) : DANGER;
                    fg = on ? Color.WHITE : new Color(0x9CA3AF);
                    break;
                case SECONDARY:
                    bg = !on ? new Color(0xF9FAFB) : hover ? PRIMARY_SOFT : Color.WHITE;
                    line = on && hover ? PRIMARY : BORDER;
                    fg = on ? TEXT : new Color(0x9CA3AF);
                    break;
                case GHOST:
                    bg = hover ? SIDEBAR_HOVER : null;
                    line = new Color(0x4F46E5);
                    fg = new Color(0xC7D2FE);
                    break;
                case LINK:
                    fg = hover ? PRIMARY_DARK : PRIMARY;
                    break;
                case NAV:
                    bg = active ? PRIMARY : hover ? SIDEBAR_HOVER : null;
                    fg = active ? Color.WHITE : new Color(0xC7D2FE);
                    break;
                default:
                    break;
            }

            if (bg != null) {
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
            }
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int x = kind == Kind.NAV ? 18 : (w - fm.stringWidth(getText())) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}
