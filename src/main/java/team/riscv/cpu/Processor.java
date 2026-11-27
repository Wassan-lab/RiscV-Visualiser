package team.riscv.cpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import team.riscv.model.Instruction;
import team.riscv.model.Instruction.Operation;
import team.riscv.model.MemoryAccess;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.model.ProcessorSnapshot.StageView;
import team.riscv.model.ProcessorSnapshot.Status;

/**
 * Single-issue, five-stage processor with stalls and no forwarding.
 * A step reports the occupants and effects of one whole clock cycle.
 * WB happens before ID reads in that cycle. ID operands are latched for EX.
 * Branches stop fetching from ID through EX; no younger instruction is fetched.
 */
public final class Processor {
    private final RegisterFile registers = new RegisterFile();
    private final MemoryAccess memory;
    private List<Instruction> program = List.of();
    // Array order: IF, ID, EX, MEM, WB. A null entry is a bubble.
    private Entry[] pipeline;
    private int nextFetchAddress;
    private long cycleCount;
    private long completedInstructions;
    private long nextInstanceId;
    private Status status;
    private String message;
    private final Map<Integer, Integer> registerWrites = new HashMap<>();
    private final Map<Integer, Integer> memoryWrites = new HashMap<>();

    private static final class Entry {
        final long id;
        final Instruction instruction;
        final Map<String, Integer> values = new HashMap<>();
        boolean stalled;
        String reason = "";

        Entry(long id, Instruction instruction) {
            this.id = id;
            this.instruction = instruction;
        }

        StageView view() {
            return new StageView(id, instruction, stalled, reason, values);
        }
    }

    /** Arithmetic/branch programs can run without data memory. */
    public Processor() {
        this(null);
    }

    /** Member 3's DataMemory will implement this interface. */
    public Processor(MemoryAccess memory) {
        this.memory = memory;
        reset();
    }

    public void loadProgram(List<Instruction> instructions) {
        List<Instruction> copy = List.copyOf(instructions);
        if (4L * copy.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Program is too large.");
        }
        for (int i = 0; i < copy.size(); i++) {
            Instruction instruction = copy.get(i);
            if (instruction.address() != 4L * i) {
                throw new IllegalArgumentException("Instruction addresses must be 0, 4, 8, ...");
            }
            if (instruction.operation() == Operation.XWSUM) {
                throw new IllegalArgumentException("XWSUM needs Member 2's extension integration.");
            }
        }
        program = copy;
        reset();
    }

    /** Keep the program; clear registers, memory, pipeline, counters and events. */
    public void reset() {
        if (memory != null) memory.reset();
        registers.reset();
        pipeline = new Entry[5];
        nextFetchAddress = 0;
        cycleCount = 0;
        completedInstructions = 0;
        nextInstanceId = 1;
        status = Status.READY;
        message = "";
        registerWrites.clear();
        memoryWrites.clear();
    }

    public int programSize() {
        return program.size();
    }

    /** Advance one cycle. FINISHED and ERROR are stable until reset/load. */
    public ProcessorSnapshot step() {
        if (status == Status.FINISHED || status == Status.ERROR) return snapshot();
        if (program.isEmpty()) {
            status = Status.FINISHED;
            message = "The program is empty.";
            return snapshot();
        }

        cycleCount++;
        status = Status.RUNNING;
        message = "";
        registerWrites.clear();
        memoryWrites.clear();

        Entry[] previous = pipeline;
        boolean holdDecode = previous[1] != null && previous[1].stalled;
        pipeline = new Entry[5];
        pipeline[4] = previous[3];
        pipeline[3] = previous[2];
        pipeline[2] = holdDecode ? null : previous[1];
        pipeline[1] = holdDecode ? previous[1] : previous[0];
        pipeline[0] = holdDecode ? previous[0] : null;

        try {
            // Oldest instructions act first. An error stops younger work.
            writeBack(pipeline[4]);
            accessMemory(pipeline[3]);
            execute(pipeline[2]);
            decode(pipeline[1]);

            boolean branchPending = isBranch(pipeline[1]) || isBranch(pipeline[2]);
            if (!holdDecode && !branchPending && nextFetchAddress < program.size() * 4) {
                pipeline[0] = new Entry(nextInstanceId++, program.get(nextFetchAddress / 4));
                nextFetchAddress += 4;
            }

            if (pipeline[0] != null) {
                boolean blocked = pipeline[1] != null && pipeline[1].stalled;
                pipeline[0].stalled = blocked;
                pipeline[0].reason = blocked ? "Decode stage is stalled." : "";
            }
            if (pipeline[1] != null && pipeline[1].stalled) {
                message = pipeline[1].reason;
            } else if (branchPending) {
                message = "Fetch paused until the cycle after branch resolution in EX.";
            }

            // WB has already retired. Its displayed occupant is not still in flight.
            boolean drained = true;
            for (int i = 0; i < 4; i++) drained &= pipeline[i] == null;
            if (drained && nextFetchAddress == program.size() * 4) {
                status = Status.FINISHED;
                message = "Program finished.";
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            status = Status.ERROR;
            message = error.getMessage();
        }
        return snapshot();
    }

    private void writeBack(Entry entry) {
        if (entry == null) return;
        Instruction instruction = entry.instruction;
        if (instruction.writesRegister()) {
            int result = entry.values.get("result");
            registers.write(instruction.rd(), result);
            registerWrites.put(instruction.rd(), result);
        }
        completedInstructions++;
    }

    private void accessMemory(Entry entry) {
        if (entry == null) return;
        Operation operation = entry.instruction.operation();
        if (operation != Operation.LW && operation != Operation.SW) return;
        if (memory == null) throw new IllegalStateException("LW/SW requires a MemoryAccess implementation.");
        int address = entry.values.get("address");
        if (address < 0 || address > MemoryAccess.SIZE_BYTES - 4 || address % 4 != 0) {
            throw new IllegalArgumentException("Invalid aligned word address: " + address);
        }
        if (operation == Operation.LW) {
            // Loads to x0 must still access memory and detect bad addresses.
            int value = memory.readWord(address);
            entry.values.put("loadedValue", value);
            entry.values.put("result", value);
        } else {
            int value = entry.values.get("storeValue");
            memory.writeWord(address, value);
            memoryWrites.put(address, value);
        }
    }

    private void execute(Entry entry) {
        if (entry == null) return;
        Instruction instruction = entry.instruction;
        int first = entry.values.get("rs1Value");
        int second = entry.values.getOrDefault("rs2Value", 0);
        // Java int arithmetic naturally retains the low 32 bits.
        switch (instruction.operation()) {
            case ADD -> entry.values.put("result", first + second);
            case SUB -> entry.values.put("result", first - second);
            case ADDI -> entry.values.put("result", first + instruction.immediate());
            case LW, SW -> {
                entry.values.put("address", first + instruction.immediate());
                if (instruction.operation() == Operation.SW) entry.values.put("storeValue", second);
            }
            case BEQ, BNE -> {
                boolean taken = instruction.operation() == Operation.BEQ ? first == second : first != second;
                entry.values.put("branchTaken", taken ? 1 : 0);
                if (taken) {
                    long target = (long) instruction.address() + instruction.immediate();
                    if (target < 0 || target > program.size() * 4L || target % 4 != 0) {
                        throw new IllegalArgumentException("Invalid branch target: " + target);
                    }
                    // A target exactly one past the program is treated as normal termination.
                    nextFetchAddress = (int) target;
                }
            }
            case XWSUM -> throw new IllegalStateException("Custom instruction is not integrated yet.");
        }
    }

    private void decode(Entry entry) {
        if (entry == null) return;
        Instruction instruction = entry.instruction;
        entry.stalled = false;
        entry.reason = "";
        entry.values.clear();
        // WB is already visible to ID. EX and MEM still have pending writes.
        for (int i = 2; i <= 3; i++) {
            Entry producer = pipeline[i];
            if (producer == null || !producer.instruction.writesRegister()) continue;
            int destination = producer.instruction.rd();
            if (instruction.rs1() == destination
                    || (instruction.usesRs2() && instruction.rs2() == destination)) {
                entry.stalled = true;
                entry.reason = "Waiting for x" + destination + " to reach WB.";
                return;
            }
        }
        entry.values.put("rs1Value", registers.read(instruction.rs1()));
        if (instruction.usesRs2()) entry.values.put("rs2Value", registers.read(instruction.rs2()));
        entry.values.put("regWrite", instruction.writesRegister() ? 1 : 0);
    }

    private static boolean isBranch(Entry entry) {
        return entry != null && (entry.instruction.operation() == Operation.BEQ
                || entry.instruction.operation() == Operation.BNE);
    }

    public ProcessorSnapshot snapshot() {
        List<StageView> stages = new ArrayList<>(5);
        for (Entry entry : pipeline) stages.add(entry == null ? StageView.empty() : entry.view());
        return new ProcessorSnapshot(cycleCount, completedInstructions, nextFetchAddress,
                registers.snapshot(), stages, registerWrites, memoryWrites, status, message);
    }
}
