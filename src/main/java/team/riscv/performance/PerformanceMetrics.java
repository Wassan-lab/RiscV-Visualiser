package team.riscv.performance;

import java.util.OptionalDouble;
import team.riscv.model.ProcessorSnapshot;

/** Measurements from simulated cycles, not animation or computer wall-clock time. */
public record PerformanceMetrics(long cycles, long completedInstructions, double clockHz) {
    public static final double DEFAULT_CLOCK_HZ = 100_000_000.0;

    public PerformanceMetrics {
        if (cycles < 0 || completedInstructions < 0 || completedInstructions > cycles) {
            throw new IllegalArgumentException("Invalid cycle or instruction count.");
        }
        if (!Double.isFinite(clockHz) || clockHz <= 0) {
            throw new IllegalArgumentException("Clock frequency must be finite and positive.");
        }
        if (!Double.isFinite(cycles / clockHz) || (cycles > 0 && cycles / clockHz == 0)) {
            throw new IllegalArgumentException("Clock frequency is outside the supported numeric range.");
        }
    }

    public static PerformanceMetrics from(ProcessorSnapshot state, double clockHz) {
        return new PerformanceMetrics(state.cycleCount(), state.completedInstructions(), clockHz);
    }

    public OptionalDouble cpi() {
        return completedInstructions == 0 ? OptionalDouble.empty()
                : OptionalDouble.of((double) cycles / completedInstructions);
    }

    public OptionalDouble ipc() {
        return cycles == 0 ? OptionalDouble.empty()
                : OptionalDouble.of((double) completedInstructions / cycles);
    }

    public double seconds() {
        return cycles / clockHz;
    }
}
