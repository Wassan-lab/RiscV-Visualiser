package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import team.riscv.cpu.Processor;
import team.riscv.model.Instruction;
import team.riscv.model.Instruction.Operation;

/** Standalone Member 1 demo. The final application's editor is Member 2's work. */
public final class Member1Demo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { /* Use Swing's default look and feel. */ }
            JPanel content = createContent();
            ProcessorPanel processor = (ProcessorPanel) content.getClientProperty("processorPanel");
            JFrame window = new JFrame("RISC-V Visualiser | Processor");
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setContentPane(content);
            java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            window.setSize(Math.min(1240, screen.width), Math.min(900, screen.height));
            window.setMinimumSize(new Dimension(Math.min(1080, screen.width), Math.min(720, screen.height)));
            window.setLocationRelativeTo(null);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) { processor.pause(); }
            });
            window.setVisible(true);
        });
    }

    // Also used for an offscreen layout preview; it creates the same content as the window.
    static JPanel createContent() {
        ProcessorPanel processor = new ProcessorPanel(new Processor());
        JComboBox<String> examples = new JComboBox<>(new String[] {"Single ADDI", "Dependent arithmetic", "Branch loop"});
        UiTheme.styleCombo(examples);
        JButton load = UiTheme.button("Load example", false);
        JTextArea listing = new JTextArea(8, 20);
        listing.setEditable(false);
        listing.setFont(UiTheme.mono(12));
        listing.setForeground(UiTheme.INK);
        listing.setBackground(UiTheme.WHITE);
        listing.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        Runnable loadExample = () -> {
            List<Instruction> program = example(examples.getSelectedIndex());
            processor.loadProgram(program);
            StringBuilder text = new StringBuilder();
            for (Instruction instruction : program) {
                text.append(String.format("%04d  %s%n", instruction.address(), PipelinePanel.format(instruction)));
            }
            listing.setText(text.toString());
            listing.setCaretPosition(0);
        };
        load.addActionListener(event -> loadExample.run());

        JPanel selector = new JPanel();
        selector.setOpaque(false);
        selector.setLayout(new javax.swing.BoxLayout(selector, javax.swing.BoxLayout.Y_AXIS));
        JLabel programTitle = UiTheme.label("Program", 20, true, UiTheme.INK);
        programTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        selector.add(programTitle);
        selector.add(javax.swing.Box.createVerticalStrut(6));
        JLabel description = UiTheme.label("Choose an example to explore.", 11, false, UiTheme.MUTED);
        description.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        selector.add(description);
        selector.add(javax.swing.Box.createVerticalStrut(20));
        examples.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        examples.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        selector.add(examples);
        selector.add(javax.swing.Box.createVerticalStrut(10));
        load.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        load.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        selector.add(load);
        selector.add(javax.swing.Box.createVerticalStrut(28));

        JPanel programCard = UiTheme.card();
        programCard.setLayout(new BorderLayout(0, 8));
        programCard.add(selector, BorderLayout.NORTH);
        JScrollPane code = new JScrollPane(listing);
        UiTheme.styleScroll(code);
        JPanel preview = new JPanel(new BorderLayout(0, 4));
        preview.setOpaque(false);
        preview.add(UiTheme.label("ASSEMBLY PREVIEW", 10, true, UiTheme.MUTED), BorderLayout.NORTH);
        preview.add(code, BorderLayout.CENTER);
        programCard.add(preview, BorderLayout.CENTER);
        JTextArea help = new JTextArea("Step advances one cycle.\nRun plays the program.\nReset starts it again.");
        help.setFont(UiTheme.font(11, false));
        help.setForeground(UiTheme.MUTED);
        help.setEditable(false);
        help.setOpaque(false);
        help.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        programCard.add(help, BorderLayout.SOUTH);
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiTheme.PAGE);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        sidebar.setPreferredSize(new Dimension(260, 100));
        sidebar.add(programCard);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UiTheme.PAGE);
        content.add(sidebar, BorderLayout.WEST);
        content.add(processor, BorderLayout.CENTER);
        content.putClientProperty("processorPanel", processor);
        loadExample.run();
        return content;
    }

    static List<Instruction> example(int index) {
        return switch (index) {
            case 0 -> List.of(addi(0, 1, 0, 5));
            case 1 -> List.of(addi(0, 1, 0, 5),
                    new Instruction(Operation.ADD, 2, 1, 1, 0, 4, 2),
                    new Instruction(Operation.SUB, 3, 2, 1, 0, 8, 3));
            case 2 -> List.of(addi(0, 1, 0, 3), addi(1, 2, 2, 1), addi(2, 1, 1, -1),
                    new Instruction(Operation.BNE, -1, 1, 0, -8, 12, 4));
            default -> throw new IllegalArgumentException("Unknown example");
        };
    }

    private static Instruction addi(int index, int destination, int source, int value) {
        return new Instruction(Operation.ADDI, destination, source, -1, value, index * 4, index + 1);
    }
}
