package team.riscv.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A small shared palette for Member 3's panels. */
final class UiStyle {
    static final Color NAVY = new Color(24, 40, 65);
    static final Color TEAL = new Color(0, 116, 122);
    static final Color LIGHT = new Color(244, 247, 250);
    static final Color WRITE = new Color(255, 230, 153);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private UiStyle() { }

    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY.deriveFont(Font.BOLD, 18));
        label.setForeground(NAVY);
        return label;
    }

    static void pad(JPanel panel) {
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);
    }
}
