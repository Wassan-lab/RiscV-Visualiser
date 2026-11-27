package team.riscv.cpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import team.riscv.model.Instruction;
import team.riscv.model.Instruction.Operation;
import team.riscv.model.MemoryAccess;
import team.riscv.model.ProcessorSnapshot;
import team.riscv.model.ProcessorSnapshot.Status;

/** Standalone checks: no JUnit installation needed. Throws on any failure. */
public final class ProcessorChecks {
    private static int groups;

    public static void main(String[] args) {
        test("One instruction: five cycles, register writes only at WB", ProcessorChecks::single);
        test("Independent instructions overlap", ProcessorChecks::independent);
        test("RAW stalls and stable instruction IDs", ProcessorChecks::dependencies);
        test("Store at MEM, load at WB, dependent memory operations", ProcessorChecks::memory);
        test("Taken and untaken BEQ/BNE", ProcessorChecks::branches);
        test("Backward loop, retirement counts, unique fetch IDs", ProcessorChecks::loop);
        test("x0 protection and valid loads to x0", ProcessorChecks::zero);
        test("Misaligned memory faults and older instruction completion", ProcessorChecks::faults);
        test("Taken branch target validation", ProcessorChecks::branchFaults);
        test("Reset, immutable snapshots and input validation", ProcessorChecks::reset);
        test("32-bit overflow", ProcessorChecks::overflow);
        test("Empty programs and stable finished state", ProcessorChecks::empty);
        test("Missing memory and custom extension errors", ProcessorChecks::integrationErrors);
        test("100 generated programs against a sequential reference", ProcessorChecks::differential);
        System.out.println("ALL " + groups + " CHECK GROUPS PASSED");
        System.out.println("\nExample: ADDI x1, x0, 5");
        Processor cpu = cpu(List.of(imm(0, 1, 0, 5)));
        while (cpu.snapshot().status() != Status.FINISHED) {
            ProcessorSnapshot s = cpu.step();
            String[] names = {"IF", "ID", "EX", "MEM", "WB"};
            String occupied = "";
            for (int i = 0; i < 5; i++) if (s.stages().get(i).instruction() != null) occupied = names[i];
            System.out.println("Cycle " + s.cycleCount() + ": " + occupied + ", x1=" + s.registers()[1]);
        }
    }

    private static void test(String name, Runnable check) {
        check.run();
        groups++;
        System.out.println("PASS: " + name);
    }

    private static Instruction imm(int index, int rd, int rs1, int value) {
        return new Instruction(Operation.ADDI, rd, rs1, -1, value, index * 4, index + 1);
    }

    private static Instruction reg(int index, Operation op, int rd, int rs1, int rs2) {
        return new Instruction(op, rd, rs1, rs2, 0, index * 4, index + 1);
    }

    private static Instruction mem(int index, Operation op, int register, int base, int offset) {
        return new Instruction(op, op == Operation.LW ? register : -1, base,
                op == Operation.SW ? register : -1, offset, index * 4, index + 1);
    }

    private static Instruction branch(int index, Operation op, int rs1, int rs2, int offset) {
        return new Instruction(op, -1, rs1, rs2, offset, index * 4, index + 1);
    }

    private static Processor cpu(List<Instruction> program) {
        Processor cpu = new Processor();
        cpu.loadProgram(program);
        return cpu;
    }

    private static ProcessorSnapshot finish(Processor cpu) {
        for (int i = 0; i < 5000; i++) {
            ProcessorSnapshot s = cpu.step();
            if (s.status() == Status.ERROR) throw new AssertionError(s.message());
            if (s.status() == Status.FINISHED) return s;
        }
        throw new AssertionError("Program did not finish within the cycle limit.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void single() {
        Processor cpu = cpu(List.of(imm(0, 1, 0, 5)));
        ProcessorSnapshot first = null;
        for (int cycle = 1; cycle <= 5; cycle++) {
            ProcessorSnapshot s = cpu.step();
            if (cycle == 1) first = s;
            require(s.cycleCount() == cycle, "Wrong cycle count");
            require(s.stages().get(cycle - 1).instruction() != null, "Wrong stage");
            require(s.registers()[1] == (cycle == 5 ? 5 : 0), "Early/missing WB");
            require(s.registerWrites().isEmpty() == (cycle != 5), "Wrong write event timing");
            require(s.completedInstructions() == (cycle == 5 ? 1 : 0), "Early retirement");
        }
        require(cpu.snapshot().status() == Status.FINISHED, "Extra drain cycle");
        require(first.registers()[1] == 0 && first.stages().get(0).values().isEmpty(), "Snapshot mutated");
    }

    private static void independent() {
        ProcessorSnapshot s = finish(cpu(List.of(imm(0, 1, 0, 1), imm(1, 2, 0, 2), imm(2, 3, 0, 3))));
        require(s.cycleCount() == 7 && s.completedInstructions() == 3, "Expected N+4 cycles");
    }

    private static void dependencies() {
        Processor cpu = cpu(List.of(imm(0, 1, 0, 5), reg(1, Operation.ADD, 2, 1, 1), reg(2, Operation.SUB, 3, 2, 1)));
        int stalls = 0;
        long heldId = 0;
        while (cpu.snapshot().status() != Status.FINISHED) {
            ProcessorSnapshot s = cpu.step();
            var id = s.stages().get(1);
            if (id.stalled()) {
                stalls++;
                if (heldId != 0) require(heldId == id.instanceId(), "Stall changed instruction ID");
                heldId = id.instanceId();
            } else heldId = 0;
        }
        ProcessorSnapshot s = cpu.snapshot();
        require(s.registers()[2] == 10 && s.registers()[3] == 5, "Stale operand");
        require(stalls == 4 && s.cycleCount() == 11, "Wrong no-forwarding stall count");
        // Two outstanding writes to the same register: wait for the newer value too.
        s = finish(cpu(List.of(imm(0, 1, 0, 1), imm(1, 1, 0, 2), reg(2, Operation.ADD, 2, 1, 1))));
        require(s.registers()[2] == 4, "Used older pending write");
    }

    private static void memory() {
        TestMemory memory = new TestMemory();
        Processor cpu = new Processor(memory);
        cpu.loadProgram(List.of(imm(0, 1, 0, 8), imm(1, 2, 0, -123),
                mem(2, Operation.SW, 2, 1, 0), mem(3, Operation.LW, 3, 1, 0),
                reg(4, Operation.ADD, 4, 3, 2), mem(5, Operation.SW, 4, 1, 4)));
        int storeEvents = 0;
        while (cpu.snapshot().status() != Status.FINISHED) {
            ProcessorSnapshot s = cpu.step();
            require(s.status() != Status.ERROR, s.message());
            if (!s.memoryWrites().isEmpty()) {
                storeEvents++;
                require(s.stages().get(3).instruction().operation() == Operation.SW, "Store outside MEM");
            }
            if (s.registerWrites().containsKey(3)) {
                require(s.stages().get(4).instruction().operation() == Operation.LW, "Load outside WB");
            }
        }
        require(memory.readWord(8) == -123 && memory.readWord(12) == -246, "Bad memory values");
        require(cpu.snapshot().registers()[4] == -246 && storeEvents == 2, "Load-use/store dependency");
        require(cpu.snapshot().completedInstructions() == 6, "Store not counted");
        cpu.loadProgram(List.of(mem(0, Operation.SW, 0, 0, 0)));
        for (int i = 0; i < 4; i++) cpu.step();
        require(cpu.snapshot().memoryWrites().containsKey(0), "Same-value store event missing");
    }

    private static void branches() {
        for (Operation operation : List.of(Operation.BEQ, Operation.BNE)) {
            for (int rhs : List.of(0, 1)) {
                TestMemory memory = new TestMemory();
                Processor cpu = new Processor(memory);
                cpu.loadProgram(List.of(imm(0, 1, 0, 1), imm(1, 2, 0, rhs),
                        branch(2, operation, 1, 2, 8), mem(3, Operation.SW, 1, 0, 0), imm(4, 3, 0, 7)));
                ProcessorSnapshot s = finish(cpu);
                boolean taken = operation == Operation.BEQ ? rhs == 1 : rhs != 1;
                require(memory.readWord(0) == (taken ? 0 : 1), "Wrong branch path");
                require(s.registers()[3] == 7 && s.completedInstructions() == (taken ? 4 : 5), "Branch retirement");
            }
        }
    }

    private static void loop() {
        Processor cpu = cpu(List.of(imm(0, 1, 0, 3), imm(1, 2, 2, 1), imm(2, 1, 1, -1), branch(3, Operation.BNE, 1, 0, -8)));
        Set<Long> fetched = new HashSet<>();
        for (int count = 0; count < 200 && cpu.snapshot().status() != Status.FINISHED; count++) {
            ProcessorSnapshot s = cpu.step();
            if (s.stages().get(0).instruction() != null) fetched.add(s.stages().get(0).instanceId());
        }
        ProcessorSnapshot s = cpu.snapshot();
        require(s.status() == Status.FINISHED && s.registers()[1] == 0 && s.registers()[2] == 3, "Loop failed");
        require(s.completedInstructions() == 10 && fetched.size() == 10, "Loop instance/retirement count");
    }

    private static void zero() {
        TestMemory memory = new TestMemory();
        Processor cpu = new Processor(memory);
        cpu.loadProgram(List.of(imm(0, 0, 0, 99), imm(1, 1, 0, 7), mem(2, Operation.LW, 0, 0, 0)));
        ProcessorSnapshot s = finish(cpu);
        require(s.registers()[0] == 0 && s.registers()[1] == 7, "x0 overwritten");
        require(memory.reads == 1 && s.cycleCount() == 7, "x0 load skipped or false dependency");
    }

    private static void faults() {
        for (int address : List.of(-4, 2, 4096)) {
            TestMemory memory = new TestMemory();
            Processor cpu = new Processor(memory);
            // 4096 is constructed via ADD, since ADDI's immediate is only 12 bits.
            List<Instruction> p = address == 4096
                    ? List.of(imm(0, 1, 0, 1024), reg(1, Operation.ADD, 1, 1, 1), reg(2, Operation.ADD, 1, 1, 1), mem(3, Operation.LW, 0, 1, 0))
                    : List.of(imm(0, 1, 0, 9), mem(1, Operation.LW, 0, 0, address), imm(2, 2, 0, 77));
            cpu.loadProgram(p);
            for (int i = 0; i < 100 && cpu.snapshot().status() != Status.ERROR; i++) cpu.step();
            ProcessorSnapshot s = cpu.snapshot();
            require(s.status() == Status.ERROR && memory.reads == 0, "Invalid load to x0 ignored");
            require(s.registers()[2] == 0, "Younger instruction wrote after fault");
            require(s.completedInstructions() == (address == 4096 ? 3 : 1), "Older completion lost");
            require(cpu.step().cycleCount() == s.cycleCount(), "Error keeps advancing");
        }
        Processor cpu = new Processor(new TestMemory());
        cpu.loadProgram(List.of(mem(0, Operation.SW, 0, 0, 2)));
        for (int i = 0; i < 4; i++) cpu.step();
        require(cpu.snapshot().status() == Status.ERROR && cpu.snapshot().memoryWrites().isEmpty(), "Invalid store committed");
    }

    private static void branchFaults() {
        for (int offset : List.of(-4, 2, 8)) {
            Processor cpu = cpu(List.of(branch(0, Operation.BEQ, 0, 0, offset)));
            for (int i = 0; i < 3; i++) cpu.step();
            require(cpu.snapshot().status() == Status.ERROR, "Invalid taken branch accepted");
        }
        ProcessorSnapshot s = finish(cpu(List.of(branch(0, Operation.BNE, 0, 0, 2))));
        require(s.completedInstructions() == 1, "Untaken target checked unnecessarily");
        s = finish(cpu(List.of(branch(0, Operation.BEQ, 0, 0, 4))));
        require(s.cycleCount() == 5, "Branch to end should drain normally");
    }

    private static void reset() {
        TestMemory memory = new TestMemory();
        Processor cpu = new Processor(memory);
        List<Instruction> input = new ArrayList<>(List.of(imm(0, 1, 0, 5), mem(1, Operation.SW, 1, 0, 0)));
        cpu.loadProgram(input);
        input.clear();
        ProcessorSnapshot saved = finish(cpu);
        saved.registers()[1] = 123;
        require(cpu.snapshot().registers()[1] == 5, "Register snapshot escaped");
        cpu.reset();
        ProcessorSnapshot s = cpu.snapshot();
        require(s.status() == Status.READY && s.cycleCount() == 0 && s.completedInstructions() == 0, "Reset counters/status");
        require(s.registers()[1] == 0 && memory.readWord(0) == 0 && cpu.programSize() == 2, "Reset data/program");
        require(s.registerWrites().isEmpty() && s.memoryWrites().isEmpty(), "Reset events");
        require(s.stages().stream().allMatch(v -> v.instruction() == null), "Reset stages");
        require(saved.registers()[1] == 5, "Reset mutated saved snapshot");
        try { cpu.loadProgram(List.of(imm(1, 1, 0, 2))); throw new AssertionError("Bad address accepted"); }
        catch (IllegalArgumentException expected) { require(cpu.programSize() == 2, "Invalid load replaced program"); }
        require(finish(cpu).registers()[1] == 5, "Replay failed");
    }

    private static void overflow() {
        List<Instruction> program = new ArrayList<>();
        program.add(imm(0, 1, 0, 1));
        for (int i = 1; i <= 31; i++) program.add(reg(i, Operation.ADD, 1, 1, 1));
        program.add(imm(32, 2, 1, -1));
        ProcessorSnapshot s = finish(cpu(program));
        require(s.registers()[1] == Integer.MIN_VALUE && s.registers()[2] == Integer.MAX_VALUE, "32-bit wrap failed");
    }

    private static void empty() {
        Processor cpu = new Processor();
        require(cpu.step().status() == Status.FINISHED && cpu.snapshot().cycleCount() == 0, "Empty program count");
        cpu.loadProgram(List.of(imm(0, 1, 0, 0)));
        ProcessorSnapshot s = finish(cpu);
        require(s.registerWrites().containsKey(1), "Same-value register write event");
        require(cpu.step().cycleCount() == 5 && cpu.snapshot().completedInstructions() == 1, "Finished keeps advancing");
    }

    private static void integrationErrors() {
        Processor cpu = cpu(List.of(mem(0, Operation.LW, 1, 0, 0)));
        for (int i = 0; i < 4; i++) cpu.step();
        require(cpu.snapshot().status() == Status.ERROR, "Missing memory ignored");
        try { cpu.loadProgram(List.of(reg(0, Operation.XWSUM, 1, 0, 0))); throw new AssertionError("Custom op silently accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    private static void differential() {
        Random random = new Random(68021);
        for (int sample = 0; sample < 100; sample++) {
            List<Instruction> program = new ArrayList<>();
            int count = 60;
            for (int i = 0; i < count; i++) {
                int rd = random.nextInt(8), a = random.nextInt(8), b = random.nextInt(8);
                int jump = 4 * (1 + random.nextInt(Math.min(4, count - i)));
                program.add(switch (random.nextInt(7)) {
                    case 0 -> imm(i, rd, a, random.nextInt(41) - 20);
                    case 1 -> reg(i, Operation.ADD, rd, a, b);
                    case 2 -> reg(i, Operation.SUB, rd, a, b);
                    case 3 -> mem(i, Operation.LW, rd, 0, random.nextInt(8) * 4);
                    case 4 -> mem(i, Operation.SW, b, 0, random.nextInt(8) * 4);
                    case 5 -> branch(i, Operation.BEQ, a, b, jump);
                    default -> branch(i, Operation.BNE, a, b, jump);
                });
            }
            int[] reference = new int[32];
            int[] words = new int[1024];
            int pc = 0, retired = 0;
            while (pc < count) {
                Instruction ins = program.get(pc);
                int a = reference[ins.rs1()];
                int b = ins.usesRs2() ? reference[ins.rs2()] : 0;
                int next = pc + 1;
                switch (ins.operation()) {
                    case ADD -> reference[ins.rd()] = a + b;
                    case SUB -> reference[ins.rd()] = a - b;
                    case ADDI -> reference[ins.rd()] = a + ins.immediate();
                    case LW -> reference[ins.rd()] = words[(a + ins.immediate()) / 4];
                    case SW -> words[(a + ins.immediate()) / 4] = b;
                    case BEQ -> { if (a == b) next = pc + ins.immediate() / 4; }
                    case BNE -> { if (a != b) next = pc + ins.immediate() / 4; }
                    default -> throw new AssertionError("Unexpected operation");
                }
                reference[0] = 0;
                retired++;
                pc = next;
            }
            TestMemory memory = new TestMemory();
            Processor cpu = new Processor(memory);
            cpu.loadProgram(program);
            ProcessorSnapshot s = finish(cpu);
            require(Arrays.equals(reference, s.registers()), "Register mismatch, sample " + sample);
            require(s.completedInstructions() == retired, "Retirement mismatch, sample " + sample);
            for (int i = 0; i < words.length; i++) require(memory.readWord(i * 4) == words[i], "Memory mismatch, sample " + sample);
        }
    }

    /** Test fixture only. Member 3 still owns the application's DataMemory. */
    private static final class TestMemory implements MemoryAccess {
        private final byte[] bytes = new byte[SIZE_BYTES];
        int reads;
        private void check(int address) {
            if (address < 0 || address > SIZE_BYTES - 4 || address % 4 != 0) throw new IllegalArgumentException("Bad test address");
        }
        public int readWord(int address) {
            check(address);
            reads++;
            int value = 0;
            for (int i = 0; i < 4; i++) value |= (bytes[address + i] & 255) << (8 * i);
            return value;
        }
        public void writeWord(int address, int value) {
            check(address);
            for (int i = 0; i < 4; i++) bytes[address + i] = (byte) (value >>> (8 * i));
        }
        public void reset() { Arrays.fill(bytes, (byte) 0); reads = 0; }
        public byte[] snapshot() { return bytes.clone(); }
    }
}
