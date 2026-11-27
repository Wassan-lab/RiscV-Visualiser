package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import team.riscv.model.MemoryAccess;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.performance.PerformanceMetrics;

/** Connect this panel to the same memory object that the processor uses. */
public final class Member3Panel extends JPanel {
    private final MemoryAccess memory;
    private final MemoryPanel memoryPanel = new MemoryPanel();
    private final PerformancePanel performancePanel;

    public Member3Panel(MemoryAccess memory) {
        this(memory, PerformanceMetrics.DEFAULT_CLOCK_HZ);
    }

    public Member3Panel(MemoryAccess memory, double clockHz) {
        super(new BorderLayout());
        this.memory = Objects.requireNonNull(memory);
        performancePanel = new PerformancePanel(clockHz);
        memoryPanel.setMinimumSize(new Dimension(400, 250));
        JScrollPane performanceScroll = new JScrollPane(performancePanel);
        performanceScroll.setMinimumSize(new Dimension(450, 250));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, memoryPanel, performanceScroll);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);
        memoryPanel.refresh(memory.snapshot(), Map.of());
    }

    public MemoryPanel memoryView() { return memoryPanel; }

    public PerformancePanel performanceView() { return performancePanel; }

    /** Refresh once after each cycle, on Swing's event thread. */
    public void update(ProcessorSnapshot snapshot) {
        requireEventThread();
        memoryPanel.refresh(memory.snapshot(), snapshot.memoryWrites());
        performancePanel.acceptSnapshot(snapshot);
    }

    public void setCompletedRun(String workloadId, String inputId, Map<String, Integer> outputs) {
        requireEventThread();
        performancePanel.setCompletedRun(workloadId, inputId, outputs);
    }

    private static void requireEventThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Update the display on Swing's event thread.");
        }
    }
}
