package team.riscv.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.DefaultListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Shared presentation settings. No processor or simulation logic lives here. */
public final class UiTheme {
    public static final Color WHITE = Color.WHITE;
    public static final Color PAGE = new Color(246, 249, 253);
    public static final Color INK = new Color(24, 48, 79);
    public static final Color MUTED = new Color(92, 113, 139);
    public static final Color BLUE = new Color(37, 99, 219);
    public static final Color LINE = new Color(220, 230, 243);
    public static final Color TINT = new Color(239, 246, 255);
    public static final Color WRITE = new Color(220, 235, 255);
    private UiTheme() { }

    public static Font font(int size, boolean bold) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
    }

    public static Font mono(int size) { return new Font(Font.MONOSPACED, Font.PLAIN, size); }

    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        return label;
    }

    public static JPanel card() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground());
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        panel.setOpaque(false);
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        return panel;
    }

    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean active = getModel().isRollover() || getModel().isPressed();
                Color background = !isEnabled() ? PAGE : primary ? (active ? new Color(24, 77, 179) : BLUE)
                        : active ? TINT : WHITE;
                g.setColor(background);
                g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                g.setColor(isFocusOwner() ? BLUE : primary && isEnabled() ? BLUE : LINE);
                g.setStroke(new BasicStroke(isFocusOwner() ? 2 : 1));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                g.setFont(getFont());
                g.setColor(!isEnabled() ? new Color(154, 169, 189) : primary ? WHITE : INK);
                var metrics = g.getFontMetrics();
                g.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g.dispose();
            }
        };
        button.setFont(font(13, true));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setMargin(new Insets(8, 18, 8, 18));
        button.setPreferredSize(new Dimension(Math.max(76, text.length() * 8 + 32), 36));
        return button;
    }

    public static void styleCombo(JComboBox<String> combo) {
        combo.setFont(font(12, false));
        combo.setBackground(WHITE);
        combo.setForeground(INK);
        combo.setBorder(BorderFactory.createLineBorder(LINE));
        combo.setUI(new BasicComboBoxUI() {
            @Override public void paintCurrentValueBackground(Graphics graphics, java.awt.Rectangle bounds, boolean focus) {
                graphics.setColor(WHITE);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(MUTED);
                        int x = getWidth() / 2, y = getHeight() / 2;
                        g.drawLine(x - 3, y - 2, x, y + 1);
                        g.drawLine(x, y + 1, x + 3, y - 2);
                        g.dispose();
                    }
                };
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setContentAreaFilled(false);
                return arrow;
            }
        });
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
                setBackground(selected ? TINT : WHITE);
                setForeground(INK);
                return this;
            }
        });
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width + 8, 34));
    }

    public static void styleScroll(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        for (JScrollBar bar : new JScrollBar[] {scroll.getVerticalScrollBar(), scroll.getHorizontalScrollBar()}) {
            bar.setUI(new BasicScrollBarUI() {
                @Override protected void configureScrollBarColors() {
                    thumbColor = new Color(192, 210, 234);
                    trackColor = WHITE;
                }
                @Override protected JButton createDecreaseButton(int orientation) { return emptyArrow(); }
                @Override protected JButton createIncreaseButton(int orientation) { return emptyArrow(); }
                private JButton emptyArrow() {
                    JButton button = new JButton();
                    button.setPreferredSize(new Dimension(0, 0));
                    return button;
                }
            });
            bar.setPreferredSize(bar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(8, 0) : new Dimension(0, 8));
        }
    }
}
