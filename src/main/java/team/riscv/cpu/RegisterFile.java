package team.riscv.cpu;

import java.util.Arrays;

public final class RegisterFile {

    // Array positions 0 through 31 represent x0 through x31.
    private final int[] registers = new int[32];

    public int read(int index) {
        checkIndex(index);
        return registers[index];
    }

    // The processor will call this during write-back (WB).
    public void write(int index, int value) {
        checkIndex(index);

        // Writes to x0 are ignored.
        if (index != 0) {
            registers[index] = value;
        }
    }

    public void reset() {
        Arrays.fill(registers, 0);
    }

    public int[] snapshot() {
        // Return a copy so the display cannot change processor state.
        return registers.clone();
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= 32) {
            throw new IllegalArgumentException(
                "Register index must be between 0 and 31."
            );
        }
    }
}