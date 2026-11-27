package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;

/** The main layout accepts the working panels supplied by each member. */
public final class MainWindow extends JFrame {
    public MainWindow(JComponent editorPanel, JComponent processorPanel,
                      JComponent customInstructionPanel, Member3Panel member3Panel) {
        super("RISC-V Visualiser · Signal Mixer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel upper = new JPanel(new BorderLayout(8, 8));
        editorPanel.setPreferredSize(new Dimension(300, 230));
        customInstructionPanel.setPreferredSize(new Dimension(280, 230));
        upper.add(editorPanel, BorderLayout.WEST);
        upper.add(processorPanel, BorderLayout.CENTER);
        upper.add(customInstructionPanel, BorderLayout.EAST);
        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, member3Panel);
        main.setResizeWeight(0.30);
        main.setDividerLocation(230);
        add(main);
        setSize(1280, 900);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);
    }
}
