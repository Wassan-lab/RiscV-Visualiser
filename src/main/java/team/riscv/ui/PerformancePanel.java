package team.riscv.ui;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
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
    private final JLabel theoreticalDetail = new JLabel();
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
        setLayout(new BorderLayout());
        setBackground(UiTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        add(content, BorderLayout.NORTH);

        JLabel heading = UiTheme.label("Performance", 26, true, UiTheme.INK);
        content.add(heading);
        content.add(Box.createVerticalStrut(5));
        content.add(UiTheme.label(String.format(Locale.ROOT,
                "Execution metrics  |  %.1f MHz simulated clock", clockHz / 1e6),
                12, false, UiTheme.MUTED));
        content.add(Box.createVerticalStrut(18));
        JPanel stats = new JPanel(new GridLayout(2, 3, 12, 12));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 184));
        addStat(stats, "Cycles", cycles);
        addStat(stats, "Completed", completed);
        addStat(stats, "CPI", cpi);
        addStat(stats, "IPC", ipc);
        addStat(stats, "Execution time", time);
        addStat(stats, "Status", runStatus);
        content.add(stats);
        content.add(Box.createVerticalStrut(18));

        JPanel amdahlCard = UiTheme.card();
        amdahlCard.setLayout(new BoxLayout(amdahlCard, BoxLayout.Y_AXIS));
        amdahlCard.add(UiTheme.label("Amdahl's Law", 19, true, UiTheme.INK));
        amdahlCard.add(Box.createVerticalStrut(5));
        amdahlCard.add(UiTheme.label("Estimate the overall speedup when part of a program improves.",
                12, false, UiTheme.MUTED));
        amdahlCard.add(Box.createVerticalStrut(17));
        fractionLabel.setFont(UiTheme.font(13, true));
        fractionLabel.setForeground(UiTheme.INK);
        amdahlCard.add(fractionLabel);
        amdahlCard.add(Box.createVerticalStrut(8));
        affected.setOpaque(true);
        affected.setBackground(UiTheme.WHITE);
        affected.setForeground(UiTheme.BLUE);
        affected.setFont(UiTheme.font(11, false));
        affected.setPaintTicks(false);
        affected.setPaintLabels(false);
        affected.setUI(new SmoothSliderUI(affected));
        affected.addChangeListener(e -> affected.repaint());
        affected.setPreferredSize(new Dimension(420, 34));
        affected.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        amdahlCard.add(affected);
        // Equal-width columns keep the midpoint label centered under the thumb.
        JPanel scale = new JPanel(new GridLayout(1, 3));
        scale.setOpaque(false);
        scale.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel zeroLabel = UiTheme.label("0%", 11, false, UiTheme.MUTED);
        JLabel halfLabel = UiTheme.label("50%", 11, false, UiTheme.MUTED);
        JLabel fullLabel = UiTheme.label("100%", 11, false, UiTheme.MUTED);
        zeroLabel.setHorizontalAlignment(SwingConstants.LEFT);
        halfLabel.setHorizontalAlignment(SwingConstants.CENTER);
        fullLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        scale.add(zeroLabel);
        scale.add(halfLabel);
        scale.add(fullLabel);
        amdahlCard.add(scale);
        amdahlCard.add(Box.createVerticalStrut(12));
        JPanel factor = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        factor.setOpaque(false);
        factor.add(UiTheme.label("Speedup of improved portion", 12, false, UiTheme.MUTED));
        localSpeedup.setFont(UiTheme.font(13, false));
        localSpeedup.setPreferredSize(new Dimension(82, 34));
        localSpeedup.setUI(new CleanSpinnerUI());
        localSpeedup.setBorder(BorderFactory.createLineBorder(UiTheme.LINE));
        JFormattedTextField speedupField = ((JSpinner.DefaultEditor) localSpeedup.getEditor()).getTextField();
        speedupField.setFont(UiTheme.font(13, true));
        speedupField.setForeground(UiTheme.INK);
        speedupField.setBackground(UiTheme.WHITE);
        speedupField.setHorizontalAlignment(JTextField.CENTER);
        speedupField.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 2));
        factor.add(localSpeedup);
        amdahlCard.add(factor);
        amdahlCard.add(Box.createVerticalStrut(16));
        JPanel resultCard = new JPanel(new BorderLayout(0, 6));
        resultCard.setBackground(UiTheme.TINT);
        resultCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        resultCard.add(UiTheme.label("THEORETICAL OVERALL SPEEDUP", 11, true, UiTheme.MUTED), BorderLayout.NORTH);
        theoretical.setFont(UiTheme.font(20, true));
        theoretical.setForeground(UiTheme.BLUE);
        JPanel resultCenter = new JPanel(new BorderLayout(0, 3));
        resultCenter.setOpaque(false);
        resultCenter.add(theoretical, BorderLayout.NORTH);
        theoreticalDetail.setFont(UiTheme.font(11, false));
        theoreticalDetail.setForeground(UiTheme.MUTED);
        // Keep the exact descriptive text for accessible output and existing UI tests.
        theoreticalDetail.setVisible(false);
        resultCenter.add(theoreticalDetail, BorderLayout.SOUTH);
        resultCard.add(resultCenter, BorderLayout.CENTER);
        resultCard.add(UiTheme.label("S = 1 / ((1 - f) + f / s)", 11, false, UiTheme.MUTED), BorderLayout.SOUTH);
        amdahlCard.add(resultCard);
        content.add(amdahlCard);
        content.add(Box.createVerticalStrut(18));

        JPanel compareCard = UiTheme.card();
        compareCard.setLayout(new BoxLayout(compareCard, BoxLayout.Y_AXIS));
        compareCard.add(UiTheme.label("Run Comparison", 19, true, UiTheme.INK));
        compareCard.add(Box.createVerticalStrut(6));
        compareCard.add(UiTheme.label("Compare completed runs with matching inputs and outputs.",
                12, false, UiTheme.MUTED));
        compareCard.add(Box.createVerticalStrut(14));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        JButton clear = UiTheme.button("Clear comparison", false);
        buttons.add(baselineButton);
        buttons.add(improvedButton);
        buttons.add(clear);
        compareCard.add(buttons);
        compareCard.add(Box.createVerticalStrut(12));
        measured.setFont(UiTheme.font(17, true));
        measured.setForeground(UiTheme.BLUE);
        comparisonStatus.setFont(UiTheme.font(12, false));
        comparisonStatus.setForeground(UiTheme.MUTED);
        compareCard.add(measured);
        compareCard.add(Box.createVerticalStrut(4));
        compareCard.add(comparisonStatus);
        compareCard.add(Box.createVerticalStrut(8));
        compareCard.add(bars);
        content.add(compareCard);
        for (Component component : content.getComponents()) {
            if (component instanceof JComponent item) item.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        for (JPanel card : new JPanel[] {amdahlCard, compareCard}) {
            for (Component component : card.getComponents()) {
                if (component instanceof JComponent item) item.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        baselineButton.setEnabled(false);
        improvedButton.setEnabled(false);
        baselineButton.addActionListener(e -> { baseline = currentRun; updateComparison(); });
        improvedButton.addActionListener(e -> { improved = currentRun; updateComparison(); });
        clear.addActionListener(e -> { baseline = null; improved = null; updateComparison(); });
        affected.addChangeListener(e -> updateAmdahl());
        localSpeedup.addChangeListener(e -> updateAmdahl());
        updateAmdahl();
    }

    private static void addStat(JPanel panel, String name, JLabel value) {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 6));
        card.add(UiTheme.label(name.toUpperCase(Locale.ROOT), 11, true, UiTheme.MUTED), BorderLayout.NORTH);
        value.setFont(UiTheme.font(22, true));
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
        theoretical.setText(String.format(Locale.ROOT, "%.3f×", prediction));
        theoreticalDetail.setText(String.format(Locale.ROOT, "Theoretical overall speedup: %.3f×", prediction));
        theoretical.setToolTipText(theoreticalDetail.getText());
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

    /** Repaint the entire slider on every movement to prevent stale thumb trails. */
    private static final class SmoothSliderUI extends BasicSliderUI {
        SmoothSliderUI(JSlider slider) { super(slider); }

        @Override protected Dimension getThumbSize() { return new Dimension(20, 20); }

        @Override public void paint(Graphics graphics, JComponent component) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(UiTheme.WHITE);
                g.fillRect(0, 0, component.getWidth(), component.getHeight());
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int left = trackRect.x;
                int right = trackRect.x + trackRect.width;
                int y = trackRect.y + trackRect.height / 2;
                int selected = xPositionForValue(slider.getValue());
                g.setColor(UiTheme.LINE);
                g.fillRoundRect(left, y - 4, right - left, 8, 8, 8);
                g.setColor(UiTheme.BLUE);
                g.fillRoundRect(left, y - 4, Math.max(0, selected - left), 8, 8, 8);
                int cx = selected;
                g.setColor(UiTheme.WHITE);
                g.fillOval(cx - 10, y - 10, 20, 20);
                g.setColor(UiTheme.BLUE);
                g.setStroke(new BasicStroke(2.5f));
                g.drawOval(cx - 9, y - 9, 18, 18);
            } finally {
                g.dispose();
            }
        }

        @Override public void setThumbLocation(int x, int y) {
            super.setThumbLocation(x, y);
            slider.repaint();
        }
    }

    /** Small themed spinner arrows without the platform's default beveled buttons. */
    private static final class CleanSpinnerUI extends BasicSpinnerUI {
        @Override protected Component createNextButton() { return arrowButton(true); }
        @Override protected Component createPreviousButton() { return arrowButton(false); }

        private JButton arrowButton(boolean up) {
            JButton button = new JButton(up ? "▴" : "▾");
            button.setFont(UiTheme.font(11, true));
            button.setForeground(UiTheme.BLUE);
            button.setBackground(UiTheme.TINT);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            button.setPreferredSize(new Dimension(24, 17));
            if (up) installNextButtonListeners(button);
            else installPreviousButtonListeners(button);
            return button;
        }
    }

    private static final class ComparisonBars extends JPanel {
        private RunResult baseline;
        private RunResult improved;

        ComparisonBars() {
            setOpaque(false);
            setPreferredSize(new Dimension(450, 48));
            setMinimumSize(new Dimension(250, 48));
        }

        void setRuns(RunResult before, RunResult after) {
            baseline = before;
            improved = after;
            int height = before == null || after == null ? 48 : 150;
            setPreferredSize(new Dimension(450, height));
            setMinimumSize(new Dimension(250, height));
            revalidate();
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(UiTheme.font(12, false));
                if (baseline == null || improved == null) {
                    g.setColor(UiTheme.MUTED);
                    g.drawString("Execution-time comparison appears after two valid runs.", 4, 26);
                    return;
                }
                double maximum = Math.max(baseline.metrics().seconds(), improved.metrics().seconds());
                drawBar(g, baseline, "Baseline", new Color(128, 153, 190), 8, maximum);
                drawBar(g, improved, "Improved", UiTheme.BLUE, 82, maximum);
            } finally { g.dispose(); }
        }

        private void drawBar(Graphics2D g, RunResult run, String name, Color color, int y, double maximum) {
            int left = 5;
            int width = Math.max(20, getWidth() - 12);
            String value = String.format(Locale.ROOT, "%d cycles  ·  %.3f µs",
                    run.metrics().cycles(), run.metrics().seconds() * 1e6);
            g.setFont(UiTheme.font(12, true));
            g.setColor(UiTheme.INK);
            g.drawString(name, left, y + 15);
            g.setFont(UiTheme.font(12, false));
            g.setColor(UiTheme.MUTED);
            g.drawString(value, Math.max(left, left + width - g.getFontMetrics().stringWidth(value)), y + 15);
            int barY = y + 26;
            g.setColor(UiTheme.TINT);
            g.fillRoundRect(left, barY, width, 13, 13, 13);
            double ratio = maximum > 0 ? run.metrics().seconds() / maximum : 0;
            int filled = (int) Math.round(width * ratio);
            if (filled > 0) {
                g.setColor(color);
                g.fillRoundRect(left, barY, Math.max(8, filled), 13, 13, 13);
            }
        }
    }
}
