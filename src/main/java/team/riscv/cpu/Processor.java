package team.riscv.cpu;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import team.riscv.model.Instruction;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.model.ProcessorSnapshot.StageView;
import team.riscv.model.ProcessorSnapshot.Status;

public final class Processor {

    private final RegisterFile registers = new RegisterFile();

    private List<Instruction> program = List.of();
    private List<StageView> stages;

    private int nextFetchAddress;
    private long cycleCount;
    private long completedInstructions;
    private Status status;

    public Processor() {
        reset();
    }

    public void loadProgram(List<Instruction> instructions) {
        // Copy the list so external changes cannot alter our program.
        // List.copyOf also rejects null instructions.
        List<Instruction> copy = List.copyOf(instructions);

        // This initial implementation expects addresses 0, 4, 8, ...
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).address() != 4L * i) {
                throw new IllegalArgumentException(
                    "Instruction addresses must be 0, 4, 8, ..."
                );
            }
        }

        // Only replace the current program after validation succeeds.
        program = copy;
        reset();
    }

    public void reset() {
        // Keep the loaded program, but reset the processor state.
        registers.reset();
        nextFetchAddress = 0;
        cycleCount = 0;
        completedInstructions = 0;
        status = Status.READY;

        // Five empty stages in IF, ID, EX, MEM, WB order.
        stages = List.copyOf(
            Collections.nCopies(5, StageView.empty())
        );
    }

    public int programSize() {
        return program.size();
    }

    public ProcessorSnapshot snapshot() {
        return new ProcessorSnapshot(
            cycleCount,
            completedInstructions,
            nextFetchAddress,
            registers.snapshot(),
            stages,
            Map.of(),
            Map.of(),
            status,
            ""
        );
    }
}