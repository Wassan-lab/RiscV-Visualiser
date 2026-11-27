package team.riscv.performance;

import java.util.Map;
import java.util.Objects;
import team.riscv.model.ProcessorSnapshot;

/** Theoretical Amdahl calculations and comparisons of completed simulator runs. */
public final class PerformanceEngine {
    private PerformanceEngine() { }

    public static double amdahl(double fraction, double localSpeedup) {
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Affected time fraction must be between 0 and 1.");
        }
        if (!Double.isFinite(localSpeedup) || localSpeedup < 1) {
            throw new IllegalArgumentException("Local speedup must be finite and at least 1.");
        }
        // fraction describes original execution TIME, not the instruction percentage.
        return 1.0 / ((1.0 - fraction) + fraction / localSpeedup);
    }

    public static double measuredSpeedup(RunResult baseline, RunResult improved) {
        Objects.requireNonNull(baseline);
        Objects.requireNonNull(improved);
        if (!baseline.workloadId().equals(improved.workloadId())
                || !baseline.inputId().equals(improved.inputId())) {
            throw new IllegalArgumentException("Compare the same workload with identical inputs.");
        }
        if (!baseline.outputs().equals(improved.outputs())) {
            throw new IllegalArgumentException("The selected outputs do not match. Check the programs first.");
        }
        double ratio = baseline.metrics().seconds() / improved.metrics().seconds();
        if (!Double.isFinite(ratio) || ratio <= 0) {
            throw new IllegalArgumentException("Execution times are outside the supported comparison range.");
        }
        return ratio;
    }

    /** Store the intended outputs, excluding scratch registers that may differ. */
    public record RunResult(String workloadId, String inputId, PerformanceMetrics metrics,
                            Map<String, Integer> outputs) {
        public RunResult {
            if (workloadId == null || workloadId.isBlank() || inputId == null || inputId.isBlank()) {
                throw new IllegalArgumentException("Provide a workload name and an input identifier.");
            }
            Objects.requireNonNull(metrics);
            if (metrics.cycles() == 0 || metrics.completedInstructions() == 0) {
                throw new IllegalArgumentException("A comparison needs a non-empty completed run.");
            }
            if (outputs == null || outputs.isEmpty()) {
                throw new IllegalArgumentException("Choose at least one intended output to verify.");
            }
            outputs = Map.copyOf(outputs);
            if (outputs.keySet().stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("Output names cannot be blank.");
            }
        }

        public static RunResult from(String workloadId, String inputId,
                                     ProcessorSnapshot state, double clockHz,
                                     Map<String, Integer> outputs) {
            if (state.status() != ProcessorSnapshot.Status.FINISHED) {
                throw new IllegalArgumentException("Finish the run successfully before saving a comparison.");
            }
            return new RunResult(workloadId, inputId, PerformanceMetrics.from(state, clockHz), outputs);
        }
    }
}
