// Nada Almaazmi 100066099
// Nada Raslan 100065544
// Wassan Awadallah 100067154

package team.riscv.model;

/**
 * One instruction, created by the parser and read by the processor.
 * This record stores instruction details; it does not execute them.
 * Register fields here store register numbers, not their current values.
 * Use NONE (-1) for an unused register and 0 for an unused immediate.
 *
 * Operand rules:
 * ADD, SUB, XWSUM: rd, rs1 and rs2 are used; immediate is 0.
 * ADDI, LW: rd, rs1 and immediate are used; rs2 is NONE.
 * SW: rs1 is the address base, rs2 is the value's register; rd is NONE.
 * BEQ, BNE: rs1 and rs2 are compared; rd is NONE.
 *
 * For branches, the parser resolves the label and stores:
 * immediate = target address - this instruction's address (in bytes).
 * The processor checks four-byte target alignment if the branch is taken.
 *
 * XWSUM is our proposed custom operation: rs1 + 3 * rs2, low 32 bits.
 * Its execution, encoding and datapath are implemented separately.
 */
public record Instruction(
        Operation operation, // Instruction name
        int rd,              // Destination register
        int rs1,             // First source register
        int rs2,             // Second source register, or NONE
        int immediate,       // Signed constant or byte offset
        int address,         // This instruction's byte address
        int sourceLine) {    // Line in the editor, starting at 1

    public static final int NONE = -1;

    public enum Operation {
        ADD, SUB, ADDI, LW, SW, BEQ, BNE, XWSUM
    }

    // Check the data whenever an Instruction is created.
    public Instruction {
        if (operation == null) {
            throw new IllegalArgumentException("Operation is required.");
        }
        if (sourceLine < 1) {
            throw new IllegalArgumentException("Source line must start at 1.");
        }
        if (address % 4 != 0) {
            throw new IllegalArgumentException("Instruction address must be four-byte aligned.");
        }

        checkRegister(rs1, "rs1");
        switch (operation) {
            case ADD, SUB, XWSUM -> {
                checkRegister(rd, "rd");
                checkRegister(rs2, "rs2");
                require(immediate == 0, "This operation has no immediate; use 0.");
            }
            case ADDI, LW -> {
                checkRegister(rd, "rd");
                require(rs2 == NONE, "This operation has no rs2; use NONE.");
                checkImmediate12(immediate);
            }
            case SW -> {
                require(rd == NONE, "SW has no destination register; use NONE.");
                checkRegister(rs2, "rs2");
                checkImmediate12(immediate);
            }
            case BEQ, BNE -> {
                require(rd == NONE, "Branches have no destination register; use NONE.");
                checkRegister(rs2, "rs2");
                require(immediate >= -4096 && immediate <= 4094
                                && immediate % 2 == 0,
                        "Branch byte offset must be even and between -4096 and 4094.");
            }
        }
    }

    // Used to check register dependencies and write-back.
    // False does not mean "skip this instruction": LW to x0 must still access memory.
    public boolean writesRegister() {
        return rd > 0;
    }

    // Every supported operation uses rs1; only some also use rs2.
    public boolean usesRs2() {
        return rs2 != NONE;
    }

    private static void checkRegister(int register, String name) {
        require(register >= 0 && register <= 31, name + " must be between 0 and 31.");
    }

    private static void checkImmediate12(int value) {
        require(value >= -2048 && value <= 2047,
                "Immediate must be between -2048 and 2047.");
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}

