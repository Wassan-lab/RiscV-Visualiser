package team.riscv.ui;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import javax.swing.*;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.performance.PerformanceEngine;
import team.riscv.performance.PerformanceEngine.RunResult;
import team.riscv.performance.PerformanceMetrics;

/** Live counters, an independent Amdahl calculator, and saved run comparisons. */
public final class PerformancePanel extends JPanel {
    private final JLabel cycles = new JLabel("0");
    private final JLabel completed = new JLabel("0");
    private final JLabel cpi = new JLabel("—");
    private final JLabel ipc = new JLabel("—");
    private final JLabel time = new JLabel("0.000 µs");
    private final JLabel runStatus = new JLabel("READY");
    private final JLabel theoretical = new JLabel();
    private final JLabel measured = new JLabel("Measured speedup: —");
    private final JLabel comparisonStatus = new JLabel("Save two completed runs to compare.");
    private final JSlider affected = new JSlider(0, 100, 50);
    private final JSpinner localSpeedup = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 100.0, 0.5));
    private final JLabel fractionLabel = new JLabel();
    private final JButton baselineButton = new JButton("Save baseline");
    private final JButton improvedButton = new JButton("Save improved");
    private final ComparisonBars bars = new ComparisonBars();
    private final double clockHz;
    private ProcessorSnapshot currentState;
    private RunResult currentRun;
    private RunResult baseline;
    private RunResult improved;

    public PerformancePanel(double clockHz) {
        new PerformanceMetrics(0, 0, clockHz); // Validate even before the first cycle.
        this.clockHz = clockHz;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        UiStyle.pad(this);
        add(UiStyle.title("Performance"));
        add(new JLabel(String.format(Locale.ROOT, "Assumed clock: %.1f MHz | simulated time", clockHz / 1e6)));
        add(Box.createVerticalStrut(10));
        JPanel stats = new JPanel(new GridLayout(3, 4, 8, 8));
        stats.setOpaque(false);
        addStat(stats, "Cycles", cycles);
        addStat(stats, "Completed", completed);
        addStat(stats, "CPI", cpi);
        addStat(stats, "IPC", ipc);
        addStat(stats, "Time", time);
        addStat(stats, "State", runStatus);
        add(stats);
        add(Box.createVerticalStrut(14));
        add(new JSeparator());
        add(Box.createVerticalStrut(10));
        add(UiStyle.title("Amdahl's Law · theoretical"));
        add(fractionLabel);
        affected.setOpaque(false);
        affected.setMajorTickSpacing(25);
        affected.setPaintTicks(true);
        affected.setPaintLabels(true);
        add(affected);
        JPanel factor = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        factor.setOpaque(false);
        factor.add(new JLabel("Speedup of that part: "));
        factor.add(localSpeedup);
        add(factor);
        add(theoretical);
        add(new JLabel("S = 1 / ((1 − f) + f / s)"));
        add(new JLabel("f is the fraction of original execution time affected."));
        add(Box.createVerticalStrut(14));
        add(new JSeparator());
        add(Box.createVerticalStrut(10));
        add(UiStyle.title("Simulator comparison · measured"));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        buttons.setOpaque(false);
        JButton clear = new JButton("Clear comparison");
        buttons.add(baselineButton);
        buttons.add(improvedButton);
        buttons.add(clear);
        add(buttons);
        add(measured);
        add(comparisonStatus);
        add(bars);
        baselineButton.setEnabled(false);
        improvedButton.setEnabled(false);
        baselineButton.addActionListener(e -> { baseline = currentRun; updateComparison(); });
        improvedButton.addActionListener(e -> { improved = currentRun; updateComparison(); });
        clear.addActionListener(e -> { baseline = null; improved = null; updateComparison(); });
        affected.addChangeListener(e -> updateAmdahl());
        localSpeedup.addChangeListener(e -> updateAmdahl());
        updateAmdahl();
        // Keep the labels aligned even when this panel is stretched in the dashboard.
        for (Component component : getComponents()) {
            if (component instanceof JComponent item) item.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    private static void addStat(JPanel panel, String name, JLabel value) {
        panel.add(new JLabel(name));
        value.setFont(UiStyle.BODY.deriveFont(Font.BOLD));
        panel.add(value);
    }

    public void acceptSnapshot(ProcessorSnapshot state) {
        currentState = state;
        currentRun = null; // A new cycle invalidates any unsaved result/context.
        baselineButton.setEnabled(false);
        improvedButton.setEnabled(false);
        PerformanceMetrics metrics = PerformanceMetrics.from(state, clockHz);
        cycles.setText(Long.toString(metrics.cycles()));
        completed.setText(Long.toString(metrics.completedInstructions()));
        cpi.setText(format(metrics.cpi()));
        ipc.setText(format(metrics.ipc()));
        time.setText(String.format(Locale.ROOT, "%.3f µs", metrics.seconds() * 1e6));
        runStatus.setText(state.status().name());
        runStatus.setToolTipText(state.message().isBlank() ? null : state.message());
    }

    /** Call after a real run finishes; inputId must describe all relevant initial inputs. */
    public void setCompletedRun(String workloadId, String inputId, Map<String, Integer> outputs) {
        if (currentState == null) throw new IllegalStateException("Supply the final snapshot first.");
        currentRun = null;
        baselineButton.setEnabled(false);
        improvedButton.setEnabled(false);
        currentRun = RunResult.from(workloadId, inputId, currentState, clockHz, outputs);
        baselineButton.setEnabled(true);
        improvedButton.setEnabled(true);
    }

    private static String format(OptionalDouble number) {
        return number.isPresent() ? String.format(Locale.ROOT, "%.3f", number.getAsDouble()) : "—";
    }

    private void updateAmdahl() {
        fractionLabel.setText("Original time affected: " + affected.getValue() + "%");
        double prediction = PerformanceEngine.amdahl(affected.getValue() / 100.0,
                ((Number) localSpeedup.getValue()).doubleValue());
        theoretical.setText(String.format(Locale.ROOT, "Theoretical overall speedup: %.3f×", prediction));
    }

    private void updateComparison() {
        measured.setText("Measured speedup: —");
        comparisonStatus.setToolTipText(null);
        bars.setRuns(null, null);
        if (baseline == null || improved == null) {
            comparisonStatus.setText(baseline != null ? "Baseline saved. Finish and save the improved run."
                    : improved != null ? "Improved run saved. Finish and save the baseline."
                    : "Save two completed runs to compare.");
            return;
        }
        try {
            double speedup = PerformanceEngine.measuredSpeedup(baseline, improved);
            measured.setText(String.format(Locale.ROOT, "Measured speedup: %.3f×", speedup));
            comparisonStatus.setText("Selected outputs match · " + baseline.workloadId());
            comparisonStatus.setToolTipText("Inputs: " + baseline.inputId() + "; outputs: " + baseline.outputs());
            bars.setRuns(baseline, improved);
        } catch (IllegalArgumentException ex) {
            comparisonStatus.setText(ex.getMessage());
            comparisonStatus.setToolTipText(ex.getMessage());
        }
    }

    private static final class ComparisonBars extends JPanel {
        private RunResult baseline;
        private RunResult improved;

        ComparisonBars() {
            setOpaque(false);
            setPreferredSize(new Dimension(450, 120));
            setMinimumSize(new Dimension(250, 110));
        }

        void setRuns(RunResult before, RunResult after) {
            baseline = before;
            improved = after;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setFont(UiStyle.BODY);
                g.setColor(UiStyle.NAVY);
                if (baseline == null || improved == null) {
                    g.drawString("Execution-time bars appear after a valid comparison.", 0, 30);
                    return;
                }
                double max = Math.max(baseline.metrics().seconds(), improved.metrics().seconds());
                int width = Math.max(1, getWidth() - 155);
                drawBar(g, baseline, "Baseline", UiStyle.NAVY, 10, max, width);
                drawBar(g, improved, "Improved", UiStyle.TEAL, 65, max, width);
            } finally {
                g.dispose();
            }
        }

        private void drawBar(Graphics2D g, RunResult run, String name, Color color,
                             int y, double maximum, int width) {
            g.setColor(UiStyle.NAVY);
            g.drawString(name, 0, y + 17);
            g.setColor(color);
            g.fillRoundRect(80, y, Math.max(1, (int) (width * (run.metrics().seconds() / maximum))), 23, 8, 8);
            g.setColor(UiStyle.NAVY);
            g.drawString(String.format(Locale.ROOT, "%d cycles · %.3f µs", run.metrics().cycles(),
                    run.metrics().seconds() * 1e6), 80, y + 40);
        }
    }
}
