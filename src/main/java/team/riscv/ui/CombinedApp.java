package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import team.riscv.model.ProcessorSnapshot;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import team.riscv.cpu.Processor;
import team.riscv.memory.DataMemory;
import team.riscv.model.Instruction;
import team.riscv.model.Instruction.Operation;

/** Combined desktop application. Member 2's panels can be attached later. */
public final class CombinedApp {
    private CombinedApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) { /* Retain installed look and feel. */ }

            DataMemory memory = new DataMemory();
            Processor processor = new Processor(memory);
            ProcessorPanel processorPanel = new ProcessorPanel(processor);
            Member3Panel member3 = new Member3Panel(memory);
            final String[] workload = {"single-addi"};
            final boolean[] recorded = {false};
            processorPanel.setSnapshotListener(state -> {
                member3.update(state);
                if (state.status() == ProcessorSnapshot.Status.FINISHED && !recorded[0]) {
                    // Match runs only when they execute the same example from the same initial state.
                    Map<String, Integer> outputs = new LinkedHashMap<>();
                    int[] registers = state.registers();
                    for (int i = 0; i < registers.length; i++) {
                        outputs.put("x" + i, registers[i]);
                    }
                    member3.setCompletedRun(workload[0], "reset-zero-registers-and-memory", outputs);
                    recorded[0] = true;
                } else if (state.status() != ProcessorSnapshot.Status.FINISHED) {
                    recorded[0] = false;
                }
            });

            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(UiTheme.font(14, true));
            tabs.setBackground(UiTheme.PAGE);
            tabs.setForeground(UiTheme.INK);
            tabs.addTab("Processor", processorPage(processorPanel, workload, recorded));
            tabs.addTab("Memory", page(member3.memoryView()));
            tabs.addTab("Performance", page(new JScrollPane(member3.performanceView())));
            JPanel custom = UiTheme.card();
            custom.setLayout(new BorderLayout());
            custom.add(UiTheme.label("Custom instructions", 22, true, UiTheme.INK), BorderLayout.NORTH);
            tabs.addTab("Custom instructions", page(custom));
            tabs.setSelectedIndex(0);

            JFrame window = new JFrame("RISC-V Visualiser");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(tabs);
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            window.setSize(Math.min(1300, screen.width), Math.min(920, screen.height));
            window.setMinimumSize(new Dimension(Math.min(960, screen.width), Math.min(680, screen.height)));
            window.setLocationRelativeTo(null);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) { processorPanel.pause(); }
            });
            window.setVisible(true);
        });
    }

    /** A small RV32I memory demonstration; the production parser belongs to Member 2. */
    private static List<Instruction> memoryExample() {
        return List.of(
                new Instruction(Operation.ADDI, 1, 0, -1, 100, 0, 1),
                new Instruction(Operation.ADDI, 2, 0, -1, 42, 4, 2),
                new Instruction(Operation.SW, -1, 1, 2, 0, 8, 3),
                new Instruction(Operation.LW, 3, 1, -1, 0, 12, 4));
    }

    private static JPanel page(java.awt.Component content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.PAGE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private static JPanel processorPage(ProcessorPanel processorPanel, String[] workload, boolean[] recorded) {
        JComboBox<String> examples = new JComboBox<>(new String[] {
                "Single ADDI", "Dependent arithmetic", "Branch loop", "Memory Load / Store" });
        UiTheme.styleCombo(examples);
        JButton load = UiTheme.button("Load example", false);
        JTextArea listing = new JTextArea(8, 20);
        listing.setEditable(false);
        listing.setFont(UiTheme.mono(12));
        listing.setForeground(UiTheme.INK);
        listing.setBackground(UiTheme.WHITE);
        listing.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        Runnable loadExample = () -> {
            List<Instruction> program = examples.getSelectedIndex() == 3
                    ? memoryExample()
                    : Member1Demo.example(examples.getSelectedIndex());
            workload[0] = switch (examples.getSelectedIndex()) {
                case 0 -> "single-addi";
                case 1 -> "dependent-arithmetic";
                case 2 -> "branch-loop";
                case 3 -> "memory-load-store";
                default -> throw new IllegalArgumentException("Unknown example");
            };
            recorded[0] = false;
            processorPanel.loadProgram(program);
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
        selector.setLayout(new BoxLayout(selector, BoxLayout.Y_AXIS));
        javax.swing.JLabel title = UiTheme.label("Program", 20, true, UiTheme.INK);
        title.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        selector.add(title);
        selector.add(Box.createVerticalStrut(6));
        javax.swing.JLabel hint = UiTheme.label("Choose an example to explore.", 11, false, UiTheme.MUTED);
        hint.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        selector.add(hint);
        selector.add(Box.createVerticalStrut(20));
        examples.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        examples.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        selector.add(examples);
        selector.add(Box.createVerticalStrut(10));
        load.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        load.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        selector.add(load);

        JPanel programCard = UiTheme.card();
        programCard.setLayout(new BorderLayout(0, 24));
        programCard.add(selector, BorderLayout.NORTH);
        JScrollPane code = new JScrollPane(listing);
        UiTheme.styleScroll(code);
        JPanel assembly = new JPanel(new BorderLayout(0, 8));
        assembly.setOpaque(false);
        assembly.add(UiTheme.label("ASSEMBLY PREVIEW", 10, true, UiTheme.MUTED), BorderLayout.NORTH);
        assembly.add(code, BorderLayout.CENTER);
        programCard.add(assembly, BorderLayout.CENTER);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiTheme.PAGE);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        sidebar.setPreferredSize(new Dimension(260, 100));
        sidebar.add(programCard, BorderLayout.CENTER);

        JPanel result = new JPanel(new BorderLayout());
        result.setBackground(UiTheme.PAGE);
        result.add(sidebar, BorderLayout.WEST);
        result.add(processorPanel, BorderLayout.CENTER);
        loadExample.run();
        return result;
    }
}
