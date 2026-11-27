package team.riscv.preview;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.util.Collections;
import java.util.Map;
import javax.swing.*;
import team.riscv.memory.DataMemory;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.ui.MainWindow;
import team.riscv.ui.Member3Panel;

/** A preview of Member 3's work. This is deliberately not a processor simulator. */
public final class Member3Preview {
    private Member3Preview() { }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Member 3 preview needs a desktop display. Use 'java Build.java test' for headless checks.");
            return;
        }
        SwingUtilities.invokeLater(Member3Preview::open);
    }

    private static void open() {
        DataMemory memory = new DataMemory();
        Member3Panel dashboard = new Member3Panel(memory);
        JPanel controls = new JPanel();
        JButton sample = new JButton("Show sample cycle");
        JButton reset = new JButton("Reset preview");
        controls.add(sample);
        controls.add(reset);
        JPanel processor = placeholder("Processor panel",
                "Member 1 connects the real pipeline and controls here.\n\n"
                + "PREVIEW ONLY: the sample button supplies invented counters.\n"
                + "It does not execute assembly or measure program speedup.");
        processor.add(controls, BorderLayout.SOUTH);
        MainWindow window = new MainWindow(
                placeholder("Assembly editor", "Member 2 connects the editor and parser here."), processor,
                placeholder("Custom datapath", "Member 2 connects XWSUM decoding and routing here."), dashboard);
        window.setTitle("Member 3 preview · sample data, not a complete simulator");
        sample.addActionListener(e -> {
            memory.writeWord(0, 0x12345678);
            memory.writeWord(4092, -1);
            dashboard.update(sampleState(12, 4, Map.of(0, 0x12345678, 4092, -1)));
        });
        reset.addActionListener(e -> {
            memory.reset();
            dashboard.update(sampleState(0, 0, Map.of()));
        });
        dashboard.update(sampleState(0, 0, Map.of()));
        window.setVisible(true);
    }

    public static ProcessorSnapshot sampleState(long cycles, long completed, Map<Integer, Integer> writes) {
        return new ProcessorSnapshot(cycles, completed, 0x3000, new int[32],
                Collections.nCopies(5, ProcessorSnapshot.StageView.empty()), Map.of(), writes,
                cycles == 0 ? ProcessorSnapshot.Status.READY : ProcessorSnapshot.Status.RUNNING,
                "Sample data for the Member 3 preview; no processor execution.");
    }

    private static JPanel placeholder(String title, String text) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JTextArea explanation = new JTextArea(text);
        explanation.setEditable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setBackground(panel.getBackground());
        panel.add(explanation, BorderLayout.CENTER);
        return panel;
    }
}
