package team.riscv.tests;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import team.riscv.memory.DataMemory;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.performance.PerformanceEngine;
import team.riscv.performance.PerformanceEngine.RunResult;
import team.riscv.performance.PerformanceMetrics;
import team.riscv.ui.Member3Panel;
import team.riscv.ui.MemoryPanel;
import team.riscv.ui.PerformancePanel;

/** Run with java Build.java test. These tests do not need external libraries. */
public final class Member3Tests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        memoryTests();
        performanceTests();
        SwingUtilities.invokeAndWait(Member3Tests::displayTests);
        System.out.println("PASS: " + checks + " checks (memory, metrics, Amdahl, comparisons and Swing panels).");
    }

    private static void memoryTests() {
        DataMemory memory = new DataMemory();
        check(Arrays.equals(memory.snapshot(), new byte[4096]), "Memory starts at zero");
        memory.writeWord(0, 0x12345678);
        byte[] bytes = memory.snapshot();
        check(Byte.toUnsignedInt(bytes[0]) == 0x78 && Byte.toUnsignedInt(bytes[1]) == 0x56
                && Byte.toUnsignedInt(bytes[2]) == 0x34 && Byte.toUnsignedInt(bytes[3]) == 0x12,
                "Little-endian byte order");
        check(memory.readWord(0) == 0x12345678, "Read a stored word");
        memory.writeWord(4092, Integer.MIN_VALUE);
        check(memory.readWord(4092) == Integer.MIN_VALUE, "Final aligned address and sign bit");
        memory.writeWord(4, -1);
        check(memory.readWord(4) == -1, "All bits set");
        memory.writeWord(4, -1);
        check(memory.readWord(4) == -1, "Repeated same-value store is accepted");
        bytes[0] = 0;
        check(memory.readWord(0) == 0x12345678, "Snapshot is a separate copy");
        for (int address : new int[]{-4, -1, 1, 2, 3, 4093, 4096, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            byte[] before = memory.snapshot();
            rejects(() -> memory.readWord(address), "Reject invalid read " + address);
            rejects(() -> memory.writeWord(address, 7), "Reject invalid store " + address);
            check(Arrays.equals(before, memory.snapshot()), "Invalid store changes no bytes");
        }
        // Cover every word boundary with repeatable values, including negative values.
        Random random = new Random(1234);
        int[] expected = new int[1024];
        for (int i = 0; i < expected.length; i++) { expected[i] = random.nextInt(); memory.writeWord(i * 4, expected[i]); }
        for (int i = 0; i < expected.length; i++) check(memory.readWord(i * 4) == expected[i], "Word " + i);
        memory.reset();
        check(Arrays.equals(memory.snapshot(), new byte[4096]), "Reset clears every byte");
    }

    private static void performanceTests() {
        PerformanceMetrics metrics = new PerformanceMetrics(12, 4, 100_000_000);
        close(metrics.cpi().orElseThrow(), 3, "CPI");
        close(metrics.ipc().orElseThrow(), 1.0 / 3, "IPC");
        close(metrics.seconds(), 0.00000012, "Simulated execution time");
        check(new PerformanceMetrics(0, 0, 100e6).cpi().isEmpty(), "CPI undefined before retirement");
        check(new PerformanceMetrics(0, 0, 100e6).ipc().isEmpty(), "IPC undefined before cycling");
        close(new PerformanceMetrics(3, 0, 100e6).ipc().orElseThrow(), 0, "IPC during pipeline fill");
        rejects(() -> new PerformanceMetrics(1, 2, 100e6), "Impossible retirement count");
        for (double clock : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> new PerformanceMetrics(1, 1, clock), "Invalid frequency");
        }
        close(PerformanceEngine.amdahl(0, 10), 1, "No affected time");
        close(PerformanceEngine.amdahl(1, 4), 4, "All time affected");
        close(PerformanceEngine.amdahl(0.5, 2), 4.0 / 3, "Amdahl worked example");
        close(PerformanceEngine.amdahl(0.9, 1), 1, "No local improvement");
        for (double fraction : new double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> PerformanceEngine.amdahl(fraction, 2), "Invalid affected fraction");
        }
        for (double factor : new double[]{0, 0.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> PerformanceEngine.amdahl(0.5, factor), "Invalid local speedup");
        }
        RunResult baseline = result(20, 5, 100e6, "same inputs", 50);
        RunResult improved = result(10, 3, 100e6, "same inputs", 50);
        close(PerformanceEngine.measuredSpeedup(baseline, improved), 2, "Measured speedup");
        close(PerformanceEngine.measuredSpeedup(improved, baseline), 0.5, "Slowdowns remain visible");
        close(PerformanceEngine.measuredSpeedup(baseline, result(10, 3, 50e6, "same inputs", 50)), 1,
                "Compare time, accounting for frequency");
        rejects(() -> PerformanceEngine.measuredSpeedup(baseline, result(10, 3, 100e6, "different inputs", 50)), "Mismatched inputs");
        rejects(() -> PerformanceEngine.measuredSpeedup(baseline, result(10, 3, 100e6, "same inputs", 51)), "Mismatched outputs");
        rejects(() -> RunResult.from("mix", "inputs", state(0, 0, ProcessorSnapshot.Status.READY), 100e6,
                Map.of("x3", 50)), "Reject unfinished run");
        rejects(() -> RunResult.from("mix", "inputs", state(10, 3, ProcessorSnapshot.Status.ERROR), 100e6,
                Map.of("x3", 50)), "Reject failed run");
        rejects(() -> new RunResult("mix", "inputs", metrics, Map.of()), "Require output checks");
        Map<String, Integer> outputs = new HashMap<>(); outputs.put("x3", 50);
        RunResult copied = new RunResult("mix", "inputs", metrics, outputs);
        outputs.put("x3", 90);
        check(copied.outputs().get("x3") == 50, "Saved outputs cannot change later");
    }

    private static void displayTests() {
        DataMemory memory = new DataMemory();
        memory.writeWord(0, 0x12345678);
        memory.writeWord(4092, -1);
        MemoryPanel panel = new MemoryPanel();
        panel.refresh(memory.snapshot(), Map.of(0, 0x12345678));
        JTable table = find(panel, JTable.class).getFirst();
        check(table.getRowCount() == 1024, "All memory words displayed");
        check(table.getValueAt(0, 1).equals("78") && table.getValueAt(0, 4).equals("12"), "Byte columns");
        check(table.getValueAt(1023, 6).equals("-1"), "Signed word display");
        Color written = table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getBackground();
        panel.refresh(memory.snapshot(), Map.of());
        Color cleared = table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getBackground();
        check(!written.equals(cleared), "Write highlight cleared next cycle");
        panel.refresh(memory.snapshot(), Map.of(0, 0x12345678));
        check(table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getBackground().equals(written),
                "Same-value store still highlighted");
        JTextField input = find(panel, JTextField.class).getFirst();
        input.setText("0xFFF"); input.postActionEvent();
        check(table.getSelectedRow() == 1023, "Jump to an individual byte");
        input.setText("4096"); input.postActionEvent();
        check(find(panel, JLabel.class).stream().anyMatch(l -> l.getText().startsWith("Enter an address")),
                "Invalid jump has a readable message");

        PerformancePanel performance = new PerformancePanel(100e6);
        JButton saveBaseline = button(performance, "Save baseline");
        JButton saveImproved = button(performance, "Save improved");
        performance.acceptSnapshot(state(5, 1, ProcessorSnapshot.Status.RUNNING));
        check(!saveBaseline.isEnabled(), "Cannot save incomplete run");
        performance.acceptSnapshot(state(20, 5, ProcessorSnapshot.Status.FINISHED));
        performance.setCompletedRun("mix", "a=20;b=10", Map.of("x3", 50));
        saveBaseline.doClick();
        performance.acceptSnapshot(state(10, 3, ProcessorSnapshot.Status.FINISHED));
        check(!saveImproved.isEnabled(), "New snapshot requires matching context");
        performance.setCompletedRun("mix", "a=20;b=10", Map.of("x3", 50));
        rejects(() -> performance.setCompletedRun("mix", "a=20;b=10", Map.of()), "Reject incomplete comparison context");
        check(!saveImproved.isEnabled(), "Bad replacement context disables saving");
        performance.setCompletedRun("mix", "a=20;b=10", Map.of("x3", 50));
        saveImproved.doClick();
        check(find(performance, JLabel.class).stream().anyMatch(l -> l.getText().equals("Measured speedup: 2.000×")),
                "Comparison buttons show correct speedup");
        find(performance, JSlider.class).getFirst().setValue(100);
        check(find(performance, JLabel.class).stream().anyMatch(l -> l.getText().equals("Theoretical overall speedup: 2.000×")),
                "Interactive Amdahl slider updates prediction");
        performance.acceptSnapshot(state(10, 3, ProcessorSnapshot.Status.FINISHED));
        performance.setCompletedRun("mix", "a=20;b=10", Map.of("x3", 51));
        saveImproved.doClick();
        check(find(performance, JLabel.class).stream().anyMatch(l -> l.getText().contains("outputs do not match")),
                "Bad comparison shown as an error");

        Member3Panel dashboard = new Member3Panel(memory);
        dashboard.update(new ProcessorSnapshot(12, 4, 0x3000, new int[32],
                Collections.nCopies(5, ProcessorSnapshot.StageView.empty()), Map.of(), Map.of(0, 0x12345678),
                ProcessorSnapshot.Status.RUNNING, "Test sample"));
        dashboard.setSize(1220, 640);
        layout(dashboard);
        BufferedImage image = new BufferedImage(1220, 640, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        dashboard.printAll(graphics);
        graphics.dispose();
        try {
            Files.createDirectories(Path.of("build"));
            ImageIO.write(image, "png", Path.of("build", "member3-panels.png").toFile());
        } catch (Exception ex) { throw new AssertionError(ex); }
    }

    private static RunResult result(long cycles, long completed, double hz, String inputs, int output) {
        return RunResult.from("mix", inputs, state(cycles, completed, ProcessorSnapshot.Status.FINISHED),
                hz, Map.of("x3", output));
    }

    private static ProcessorSnapshot state(long cycles, long completed, ProcessorSnapshot.Status status) {
        return new ProcessorSnapshot(cycles, completed, 0x3000, new int[32],
                Collections.nCopies(5, ProcessorSnapshot.StageView.empty()), Map.of(), Map.of(), status, "");
    }

    private static JButton button(Container root, String text) {
        return find(root, JButton.class).stream().filter(b -> b.getText().equals(text)).findFirst().orElseThrow();
    }

    private static <T> java.util.List<T> find(Container root, Class<T> type) {
        java.util.List<T> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container container) result.addAll(find(container, type));
        }
        return result;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container container) layout(container);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void close(double actual, double expected, String description) {
        check(Math.abs(actual - expected) <= Math.max(1e-15, Math.abs(expected) * 1e-12), description);
    }

    private static void rejects(Runnable action, String description) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(description);
    }
}
