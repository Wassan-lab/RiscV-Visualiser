package team.riscv.model;

import java.util.List;
import java.util.Map;

/**
 * A copy of the processor's state for the displays and performance calculations.
 * The processor creates one after each cycle; this record does not run the CPU.
 * Stage entries show that cycle's occupants. Register values show its final state.
 * After reset, use zero counts/registers, empty stages/maps and status READY.
 * The full memory contents come from MemoryAccess.snapshot().
 */
public record ProcessorSnapshot(
        long cycleCount,                    // Cycles since reset
        long completedInstructions,        // Finished WB, including branches and stores; no bubbles/failures
        int nextFetchAddress,               // Address to fetch when fetching resumes
        int[] registers,                   // Values of x0 to x31 after this cycle
        List<StageView> stages,             // Always IF, ID, EX, MEM, WB order
        Map<Integer, Integer> registerWrites, // Register number -> value written at WB this cycle
        Map<Integer, Integer> memoryWrites, // Word address -> value stored this cycle
        Status status,
        String message) {                  // Explanation, or "" if there is nothing to report

    public enum Status { READY, RUNNING, FINISHED, ERROR }

    public ProcessorSnapshot {
        // Our pipeline can finish at most one instruction per cycle.
        if (cycleCount < 0 || completedInstructions < 0
                || completedInstructions > cycleCount) {
            throw new IllegalArgumentException("Invalid cycle or completion count.");
        }
        // Keep all 32 registers, including x0 which must stay zero.
        if (registers == null || registers.length != 32 || registers[0] != 0) {
            throw new IllegalArgumentException("Supply 32 registers with x0 equal to zero.");
        }
        // Empty stages still need an entry so each display position stays fixed.
        if (stages == null || stages.size() != 5) {
            throw new IllegalArgumentException("Supply five stages in IF, ID, EX, MEM, WB order.");
        }
        // Use empty collections/text when there are no updates.
        if (status == null || message == null
                || registerWrites == null || memoryWrites == null) {
            throw new IllegalArgumentException("Use empty maps/text instead of null.");
        }

        // Keep our own copies so later changes cannot alter this snapshot.
        registers = registers.clone();
        stages = List.copyOf(stages);
        registerWrites = Map.copyOf(registerWrites);
        memoryWrites = Map.copyOf(memoryWrites);

        // Record writes even if the value did not change, but never a write to x0.
        for (var entry : registerWrites.entrySet()) {
            int register = entry.getKey();
            if (register < 1 || register > 31
                    || registers[register] != entry.getValue()) {
                throw new IllegalArgumentException("Register writes must match the updated registers.");
            }
        }
        // Each stored word needs four bytes inside our memory range.
        for (int address : memoryWrites.keySet()) {
            if (address < 0 || address > MemoryAccess.SIZE_BYTES - 4 || address % 4 != 0) {
                throw new IllegalArgumentException("Invalid memory-write address.");
            }
        }
    }

    // Return a copy so the GUI cannot edit the saved register values.
    @Override
    public int[] registers() {
        return registers.clone();
    }

    /**
     * Display information for one pipeline stage. A null instruction means empty.
     * Give each fetched instruction a new positive instanceId, even in a loop.
     * Keep its ID as it moves or stalls; an empty stage uses ID 0.
     *
     * Only put available values in the map. Use these names consistently:
     * rs1Value, rs2Value, result, address, storeValue, loadedValue.
     * branchTaken, customSelected and regWrite use 0 for false and 1 for true.
     * XWSUM also uses doubledRs2 and tripledRs2 for its intermediate results.
     * The decoder can supply opcode, funct3 and funct7 after decoding in ID.
     * Register numbers and source lines are already available from Instruction.
     */
    public record StageView(
            long instanceId,         // Identifies this execution of the instruction
            Instruction instruction,
            boolean stalled,         // True while this instruction is held in place
            String stallReason,      // Explain the stall; otherwise use ""
            Map<String, Integer> values) {

        public StageView {
            if (stallReason == null || values == null) {
                throw new IllegalArgumentException("Use empty text/map instead of null.");
            }
            // An empty stage has no instruction ID, values or stall reason.
            if (instruction == null) {
                if (instanceId != 0 || stalled || !stallReason.isEmpty() || !values.isEmpty()) {
                    throw new IllegalArgumentException("An empty stage cannot have instruction data.");
                }
            } else {
                // IDs let us distinguish repeated executions of the same loop instruction.
                if (instanceId <= 0) {
                    throw new IllegalArgumentException("A fetched instruction needs a positive ID.");
                }
                // A stalled instruction needs a reason; a moving one does not.
                if ((stalled && stallReason.isBlank())
                        || (!stalled && !stallReason.isEmpty())) {
                    throw new IllegalArgumentException("Provide a reason only when stalled.");
                }
            }
            // Keep these values fixed after creating the stage entry.
            values = Map.copyOf(values);
        }

        // Use this for a bubble or a stage with no instruction.
        public static StageView empty() {
            return new StageView(0, null, false, "", Map.of());
        }
    }
}
