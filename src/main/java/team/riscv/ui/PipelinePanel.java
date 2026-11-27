package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import team.riscv.model.Instruction;
import team.riscv.model.ProcessorSnapshot;

/** Five stage cards; each update shows one complete simulated cycle. */
public final class PipelinePanel extends JPanel {
    private final JPanel[] cards = new JPanel[5];
    private final JTextArea[] details = new JTextArea[5];
    private final JLabel[] states = new JLabel[5];

    public PipelinePanel() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(UiTheme.label("Pipeline", 16, true, UiTheme.INK), BorderLayout.WEST);
        heading.add(UiTheme.label("One step = one clock cycle", 12, false, UiTheme.MUTED), BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);
        JPanel row = new JPanel(new GridLayout(1, 5, 10, 0));
        row.setOpaque(false);
        String[] titles = {"IF", "ID", "EX", "MEM", "WB"};
        String[] names = {"Fetch", "Decode", "Execute", "Memory", "Write-back"};
        for (int i = 0; i < 5; i++) {
            cards[i] = UiTheme.card();
            cards[i].setLayout(new BorderLayout(0, 10));
            cards[i].setPreferredSize(new Dimension(170, 172));
            cards[i].setBorder(BorderFactory.createEmptyBorder(14, 12, 12, 12));
            JPanel title = new JPanel(new BorderLayout());
            title.setOpaque(false);
            title.add(UiTheme.label(titles[i], 15, true, UiTheme.BLUE), BorderLayout.WEST);
            title.add(UiTheme.label(names[i], 11, false, UiTheme.MUTED), BorderLayout.EAST);
            details[i] = new JTextArea(7, 12);
            details[i].setName("stage-" + i);
            details[i].setEditable(false);
            details[i].setLineWrap(true);
            details[i].setWrapStyleWord(true);
            details[i].setOpaque(false);
            details[i].setFont(UiTheme.mono(12));
            details[i].setForeground(UiTheme.INK);
            details[i].setBorder(BorderFactory.createEmptyBorder());
            states[i] = UiTheme.label("Empty", 11, true, UiTheme.MUTED);
            states[i].setName("stage-state-" + i);
            cards[i].add(title, BorderLayout.NORTH);
            cards[i].add(details[i], BorderLayout.CENTER);
            cards[i].add(states[i], BorderLayout.SOUTH);
            row.add(cards[i]);
        }
        add(row, BorderLayout.CENTER);
    }

    public void updateSnapshot(ProcessorSnapshot snapshot) {
        for (int i = 0; i < 5; i++) {
            var stage = snapshot.stages().get(i);
            boolean empty = stage.instruction() == null;
            Color background = empty ? UiTheme.WHITE
                    : stage.stalled() ? UiTheme.TINT : i == 4 ? UiTheme.WRITE : new Color(247, 250, 255);
            cards[i].setBackground(background);
            states[i].setForeground(empty ? UiTheme.MUTED : UiTheme.BLUE);
            if (empty) {
                details[i].setText("Empty");
                details[i].setForeground(UiTheme.MUTED);
                details[i].setToolTipText(null);
                states[i].setText("Empty");
                continue;
            }
            details[i].setForeground(UiTheme.INK);
            Instruction instruction = stage.instruction();
            StringBuilder text = new StringBuilder(format(instruction));
            text.append("\nPC ").append(instruction.address());
            text.append(" | line ").append(instruction.sourceLine());
            text.append("\nInstance #").append(stage.instanceId());
            if (stage.stalled()) {
                text.append("\n").append(stage.stallReason());
            } else {
                for (String key : new String[] {"rs1Value", "rs2Value", "result", "address", "storeValue", "branchTaken"}) {
                    if (stage.values().containsKey(key)) text.append("\n").append(key).append(" = ").append(stage.values().get(key));
                }
            }
            details[i].setText(text.toString());
            details[i].setCaretPosition(0);
            details[i].setToolTipText(stage.stalled() ? stage.stallReason() : format(instruction));
            states[i].setText(stage.stalled() ? "STALLED" : i == 4 ? "Retired this cycle" : "Active");
        }
    }

    public static String format(Instruction instruction) {
        String operation = instruction.operation().name();
        return switch (instruction.operation()) {
            case ADD, SUB, XWSUM -> operation + " x" + instruction.rd() + ", x" + instruction.rs1() + ", x" + instruction.rs2();
            case ADDI -> operation + " x" + instruction.rd() + ", x" + instruction.rs1() + ", " + instruction.immediate();
            case LW -> operation + " x" + instruction.rd() + ", " + instruction.immediate() + "(x" + instruction.rs1() + ")";
            case SW -> operation + " x" + instruction.rs2() + ", " + instruction.immediate() + "(x" + instruction.rs1() + ")";
            case BEQ, BNE -> operation + " x" + instruction.rs1() + ", x" + instruction.rs2() + ", " + instruction.immediate();
        };
    }
}
