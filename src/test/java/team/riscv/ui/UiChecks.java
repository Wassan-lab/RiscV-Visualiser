package team.riscv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import team.riscv.cpu.Processor;
import team.riscv.model.Instruction;
import team.riscv.model.Instruction.Operation;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.model.ProcessorSnapshot.Status;

/** Headless Swing checks; all UI work runs on the event-dispatch thread. */
public final class UiChecks {
    private static Processor cpu;
    private static ProcessorPanel panel;
    private static final AtomicReference<ProcessorSnapshot> latest = new AtomicReference<>();
    private static final AtomicLong callbacks = new AtomicLong();
    private static int groups;

    public static void main(String[] args) throws Exception {
        try {
            edt(() -> {
                cpu = new Processor();
                panel = new ProcessorPanel(cpu);
                panel.setSnapshotListener(snapshot -> {
                    require(SwingUtilities.isEventDispatchThread(), "Callback outside EDT");
                    latest.set(snapshot);
                    callbacks.incrementAndGet();
                });
                require(!button("step").isEnabled() && !button("run").isEnabled(), "Empty program enabled");
                panel.loadProgram(Member1Demo.example(0));
                for (int i = 0; i < 4; i++) button("step").doClick(0);
                JTable low = (JTable) find(panel, "registers-low");
                require(low.getValueAt(1, 2).equals("0"), "Early register display");
                button("step").doClick(0);
                require(low.getValueAt(1, 1).equals("0x00000005") && low.getValueAt(1, 2).equals("5"), "WB display wrong");
                require(!button("step").isEnabled() && !button("run").isEnabled(), "Finished buttons enabled");
                Component cell = low.prepareRenderer(low.getCellRenderer(1, 1), 1, 1);
                require(cell.getBackground().equals(UiTheme.WRITE), "WB highlight missing");
                require(((JLabel) find(panel, "summary")).getText().contains("FINISHED"), "Finished label missing");
                require(callbacks.get() == 7, "Snapshot callback count wrong");
                button("reset").doClick(0);
                require(low.getValueAt(1, 2).equals("0") && cpu.snapshot().cycleCount() == 0, "Reset display failed");
                require(button("step").isEnabled(), "Reset did not re-enable Step");
            });
            pass("Step, WB timing, register highlight, reset and callbacks");

            edt(() -> {
                panel.loadProgram(List.of(new Instruction(Operation.ADDI, 31, 0, -1, -1, 0, 1)));
                for (int i = 0; i < 5; i++) button("step").doClick(0);
                JTable high = (JTable) find(panel, "registers-high");
                JTable low = (JTable) find(panel, "registers-low");
                require(high.getValueAt(15, 0).equals("x31") && high.getValueAt(15, 1).equals("0xFFFFFFFF")
                        && high.getValueAt(15, 2).equals("-1"), "Signed/hex mapping failed");
                require(low.getValueAt(0, 2).equals("0") && !low.isCellEditable(0, 1), "x0/editability failed");
            });
            pass("All 32 register positions, signed decimal and hexadecimal");

            edt(() -> {
                panel.loadProgram(Member1Demo.example(1));
                for (int i = 0; i < 3; i++) button("step").doClick(0);
                require(((JLabel) find(panel, "stage-state-1")).getText().equals("STALLED"), "Stall label missing");
                require(((JTextArea) find(panel, "stage-1")).getText().contains("Waiting for x1"), "Stall reason missing");
                require(((JTextArea) find(panel, "stage-2")).getText().contains("ADDI"), "EX instruction missing");
            });
            pass("Pipeline occupants and dependency explanations");

            edt(() -> {
                panel.loadProgram(List.of(new Instruction(Operation.BEQ, -1, 0, 0, 0, 0, 1)));
                ((JComboBox<?>) find(panel, "speed")).setSelectedIndex(0);
                button("run").doClick(0);
                require(panel.isRunning() && !button("step").isEnabled() && button("pause").isEnabled(), "Run controls incorrect");
            });
            await(() -> latest.get().cycleCount() >= 2);
            edt(() -> button("pause").doClick(0));
            long paused = latest.get().cycleCount();
            Thread.sleep(250);
            edt(() -> {
                require(cpu.snapshot().cycleCount() == paused && !panel.isRunning(), "Pause advanced a cycle");
                button("run").doClick(0);
            });
            await(() -> latest.get().cycleCount() > paused);
            edt(() -> button("reset").doClick(0));
            Thread.sleep(250);
            edt(() -> require(cpu.snapshot().cycleCount() == 0 && !panel.isRunning(), "Reset failed to stop timer"));
            pass("Run, Pause, resume and Reset during an infinite branch loop");

            edt(() -> {
                panel.loadProgram(Member1Demo.example(0));
                button("run").doClick(0);
            });
            await(() -> latest.get().status() == Status.FINISHED);
            edt(() -> require(!panel.isRunning() && cpu.snapshot().cycleCount() == 5, "Run did not stop at completion"));
            pass("Run automatically stops at FINISHED");

            edt(() -> {
                panel.loadProgram(List.of(new Instruction(Operation.LW, 1, 0, -1, 0, 0, 1)));
                button("run").doClick(0);
            });
            await(() -> latest.get().status() == Status.ERROR);
            edt(() -> {
                require(!panel.isRunning() && !button("step").isEnabled(), "Run did not stop on error");
                require(((JTextArea) find(panel, "status")).getText().contains("MemoryAccess"), "Error explanation hidden");
                button("reset").doClick(0);
                require(button("step").isEnabled(), "Error reset did not recover");
            });
            pass("Error display, timer stop and recovery through reset");

            edt(() -> {
                panel.loadProgram(Member1Demo.example(2));
                button("run").doClick(0);
                panel.loadProgram(Member1Demo.example(0));
                require(!panel.isRunning() && cpu.snapshot().cycleCount() == 0, "Load kept old run active");
                button("run").doClick(0);
                panel.removeNotify();
                require(!panel.isRunning(), "Removing panel left timer running");
            });
            pass("Loading/replacing a program and removing the panel stop Run");

            if (args.length > 0) {
                edt(() -> {
                    javax.swing.JPanel preview = Member1Demo.createContent();
                    panel = (ProcessorPanel) preview.getClientProperty("processorPanel");
                    // The first example remains selected, matching the actual displayed program.
                    for (int i = 0; i < 5; i++) button("step").doClick(0);
                    preview.addNotify();
                    preview.setSize(1240, 900);
                    layoutTree(preview);
                    preview.validate();
                    require(button("step").getWidth() > 0 && button("step").getHeight() > 0, "Controls have no layout");
                    BufferedImage image = new BufferedImage(1240, 900, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    preview.printAll(graphics);
                    graphics.dispose();
                    try { ImageIO.write(image, "png", new File(args[0])); }
                    catch (Exception error) { throw new RuntimeException(error); }
                });
            }
            System.out.println("ALL " + groups + " UI CHECK GROUPS PASSED");
        } finally {
            edt(() -> { if (panel != null) panel.pause(); });
        }
    }

    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void pass(String name) { groups++; System.out.println("PASS: " + name); }
    private static JButton button(String name) { return (JButton) find(panel, name); }

    private static Component find(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container child) {
                Component match = find(child, name);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component component : root.getComponents()) if (component instanceof Container child) layoutTree(child);
    }

    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 4_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for timer");
            Thread.sleep(20);
        }
    }
}
