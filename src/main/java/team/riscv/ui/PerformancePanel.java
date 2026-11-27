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
    private final JButton baselineButton = UiTheme.button("Save baseline", true);
    private final JButton improvedButton = UiTheme.button("Save improved", false);
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
        setBackground(UiTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        add(UiTheme.label("Performance", 26, true, UiTheme.INK));
        add(UiTheme.label(String.format(Locale.ROOT, "Assumed clock: %.1f MHz  |  Simulated time", clockHz / 1e6), 12, false, UiTheme.MUTED));
        add(Box.createVerticalStrut(10));
        JPanel stats = new JPanel(new GridLayout(2, 3, 10, 10));
        stats.setOpaque(false);
        addStat(stats, "Cycles", cycles);
        addStat(stats, "Completed", completed);
        addStat(stats, "CPI", cpi);
        addStat(stats, "IPC", ipc);
        addStat(stats, "Time", time);
        addStat(stats, "State", runStatus);
        add(stats);
        add(Box.createVerticalStrut(14));
        add(sectionDivider());
        add(Box.createVerticalStrut(10));
        add(UiTheme.label("Amdahl's Law  /  Theoretical", 19, true, UiTheme.INK));
        add(fractionLabel);
        affected.setOpaque(false);
        affected.setForeground(UiTheme.BLUE);
        affected.setFont(UiTheme.font(11, false));
        fractionLabel.setFont(UiTheme.font(13, true));
        fractionLabel.setForeground(UiTheme.INK);
        affected.setMajorTickSpacing(25);
        affected.setPaintTicks(true);
        affected.setPaintLabels(true);
        add(affected);
        JPanel factor = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        factor.setOpaque(false);
        factor.add(UiTheme.label("Speedup of that part: ", 12, false, UiTheme.MUTED));
        localSpeedup.setFont(UiTheme.font(12, false));
        factor.add(localSpeedup);
        add(factor);
        theoretical.setFont(UiTheme.font(15, true));
        theoretical.setForeground(UiTheme.BLUE);
        add(theoretical);
        add(UiTheme.label("S = 1 / ((1 − f) + f / s)", 12, false, UiTheme.INK));
        add(UiTheme.label("f is the fraction of original execution time affected.", 12, false, UiTheme.MUTED));
        add(Box.createVerticalStrut(14));
        add(sectionDivider());
        add(Box.createVerticalStrut(10));
        add(UiTheme.label("Simulator comparison  /  Measured", 19, true, UiTheme.INK));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        buttons.setOpaque(false);
        JButton clear = UiTheme.button("Clear comparison", false);
        buttons.add(baselineButton);
        buttons.add(improvedButton);
        buttons.add(clear);
        add(buttons);
        measured.setFont(UiTheme.font(15, true));
        measured.setForeground(UiTheme.BLUE);
        comparisonStatus.setFont(UiTheme.font(12, false));
        comparisonStatus.setForeground(UiTheme.MUTED);
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
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 6));
        card.add(UiTheme.label(name.toUpperCase(Locale.ROOT), 11, true, UiTheme.MUTED), BorderLayout.NORTH);
        value.setFont(UiTheme.font(20, true));
        value.setForeground(UiTheme.INK);
        card.add(value, BorderLayout.CENTER);
        panel.add(card);
    }

    private static JSeparator sectionDivider() {
        JSeparator divider = new JSeparator();
        divider.setForeground(UiTheme.LINE);
        return divider;
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
                g.setFont(UiTheme.font(12, false));
                g.setColor(UiTheme.INK);
                if (baseline == null || improved == null) {
                    g.drawString("Execution-time bars appear after a valid comparison.", 0, 30);
                    return;
                }
                double max = Math.max(baseline.metrics().seconds(), improved.metrics().seconds());
                int width = Math.max(1, getWidth() - 155);
                drawBar(g, baseline, "Baseline", UiTheme.INK, 10, max, width);
                drawBar(g, improved, "Improved", UiTheme.BLUE, 65, max, width);
            } finally {
                g.dispose();
            }
        }

        private void drawBar(Graphics2D g, RunResult run, String name, Color color,
                             int y, double maximum, int width) {
            g.setColor(UiTheme.INK);
            g.drawString(name, 0, y + 17);
            g.setColor(color);
            g.fillRoundRect(80, y, Math.max(1, (int) (width * (run.metrics().seconds() / maximum))), 23, 8, 8);
            g.setColor(UiTheme.INK);
            g.drawString(String.format(Locale.ROOT, "%d cycles · %.3f µs", run.metrics().cycles(),
                    run.metrics().seconds() * 1e6), 80, y + 40);
        }
    }
}
