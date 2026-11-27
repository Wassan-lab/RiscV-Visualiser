package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import team.riscv.cpu.Processor;
import team.riscv.model.Instruction;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.model.ProcessorSnapshot.Status;

/**
 * Member 1's reusable panel. Construct and call its methods on Swing's EDT.
 * A Swing Timer performs one bounded CPU step per event; Run remains pausable.
 */
public final class ProcessorPanel extends JPanel {
    private final Processor processor;
    private final PipelinePanel pipeline = new PipelinePanel();
    private final RegisterPanel registers = new RegisterPanel();
    private final JButton step = UiTheme.button("Step", true);
    private final JButton run = UiTheme.button("Run", false);
    private final JButton pause = UiTheme.button("Pause", false);
    private final JButton reset = UiTheme.button("Reset", false);
    private final JComboBox<String> speed = new JComboBox<>(new String[] {"100 ms", "300 ms", "700 ms", "1200 ms"});
    private final JLabel summary = new JLabel();
    private final JTextArea statusText = new JTextArea(2, 60);
    private final Timer timer;
    private Consumer<ProcessorSnapshot> listener = snapshot -> { };

    public ProcessorPanel(Processor processor) {
        super(new BorderLayout(0, 18));
        requireEdt();
        this.processor = Objects.requireNonNull(processor);
        setBackground(UiTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        timer = new Timer(700, event -> {
            if (timerRunning()) advance();
        });
        timer.setCoalesce(true);

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(UiTheme.label("Processor", 26, true, UiTheme.INK), BorderLayout.WEST);
        heading.add(UiTheme.label("RV32I  /  FIVE-STAGE PIPELINE", 11, true, UiTheme.MUTED), BorderLayout.EAST);
        top.add(heading, BorderLayout.NORTH);

        JPanel toolbar = UiTheme.card();
        toolbar.setLayout(new BorderLayout(16, 12));
        toolbar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        step.setName("step");
        run.setName("run");
        pause.setName("pause");
        reset.setName("reset");
        speed.setName("speed");
        controls.add(step);
        controls.add(run);
        controls.add(pause);
        controls.add(reset);
        toolbar.add(controls, BorderLayout.WEST);
        JPanel playback = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        playback.setOpaque(false);
        playback.add(UiTheme.label("Playback delay", 12, false, UiTheme.MUTED));
        UiTheme.styleCombo(speed);
        speed.setSelectedIndex(2);
        playback.add(speed);
        toolbar.add(playback, BorderLayout.EAST);
        top.add(toolbar, BorderLayout.CENTER);

        summary.setName("summary");
        summary.setFont(UiTheme.font(12, false));
        summary.setForeground(UiTheme.MUTED);
        summary.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        top.add(summary, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel displays = new JPanel(new BorderLayout(0, 18));
        displays.setOpaque(false);
        displays.add(pipeline, BorderLayout.NORTH);
        displays.add(registers, BorderLayout.CENTER);
        add(displays, BorderLayout.CENTER);
        statusText.setName("status");
        statusText.setFont(UiTheme.font(12, false));
        statusText.setEditable(false);
        statusText.setLineWrap(true);
        statusText.setWrapStyleWord(true);
        statusText.setOpaque(false);
        statusText.setPreferredSize(new Dimension(100, 34));
        statusText.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        add(statusText, BorderLayout.SOUTH);

        step.addActionListener(event -> advance());
        run.addActionListener(event -> {
            if (canAdvance()) {
                timer.start();
                render();
            }
        });
        pause.addActionListener(event -> pause());
        reset.addActionListener(event -> {
            timer.stop();
            processor.reset();
            publish();
        });
        speed.addActionListener(event -> {
            int delay = new int[] {100, 300, 700, 1200}[speed.getSelectedIndex()];
            timer.setDelay(delay);
            timer.setInitialDelay(delay);
        });
        render();
    }

    public void loadProgram(List<Instruction> instructions) {
        requireEdt();
        timer.stop();
        try {
            processor.loadProgram(instructions);
        } finally {
            render();
        }
        listener.accept(processor.snapshot());
    }

    /** Callback for Member 3's memory/performance panels; called on the EDT. */
    public void setSnapshotListener(Consumer<ProcessorSnapshot> listener) {
        requireEdt();
        this.listener = Objects.requireNonNull(listener);
        this.listener.accept(processor.snapshot());
    }

    public void pause() {
        requireEdt();
        timer.stop();
        render();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    private boolean timerRunning() { return timer.isRunning(); }

    private boolean canAdvance() {
        Status status = processor.snapshot().status();
        return processor.programSize() > 0 && status != Status.FINISHED && status != Status.ERROR;
    }

    private void advance() {
        if (!canAdvance()) {
            timer.stop();
            render();
            return;
        }
        processor.step();
        if (!canAdvance()) timer.stop();
        publish();
    }

    private void publish() {
        render();
        listener.accept(processor.snapshot());
    }

    private void render() {
        ProcessorSnapshot snapshot = processor.snapshot();
        pipeline.updateSnapshot(snapshot);
        registers.updateSnapshot(snapshot);
        boolean running = timer.isRunning();
        step.setEnabled(canAdvance() && !running);
        run.setEnabled(canAdvance() && !running);
        pause.setEnabled(running);
        String state = snapshot.status() == Status.RUNNING && !running ? "PAUSED" : snapshot.status().name();
        summary.setText("Cycle " + snapshot.cycleCount() + "   |   Completed " + snapshot.completedInstructions()
                + "   |   Next fetch " + snapshot.nextFetchAddress() + "   |   " + state);
        String message = snapshot.message();
        if (message.isEmpty()) message = processor.programSize() == 0 ? "Load a program to begin."
                : "Step advances one simulated cycle. Animation delay does not change processor cycle counts.";
        statusText.setForeground(snapshot.status() == Status.ERROR ? new Color(173, 38, 38) : UiTheme.MUTED);
        statusText.setText(message);
        statusText.setCaretPosition(0);
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Use ProcessorPanel on Swing's event-dispatch thread.");
        }
    }
}
