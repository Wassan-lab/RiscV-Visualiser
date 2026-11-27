package team.riscv.memory;

import java.util.Arrays;
import team.riscv.model.MemoryAccess;

/** The byte-addressed data memory used by our processor. */
public final class DataMemory implements MemoryAccess {
    private final byte[] bytes = new byte[SIZE_BYTES];

    @Override
    public synchronized int readWord(int address) {
        checkAddress(address);
        // The byte at the lowest address is the least significant byte.
        return Byte.toUnsignedInt(bytes[address])
                | (Byte.toUnsignedInt(bytes[address + 1]) << 8)
                | (Byte.toUnsignedInt(bytes[address + 2]) << 16)
                | (Byte.toUnsignedInt(bytes[address + 3]) << 24);
    }

    @Override
    public synchronized void writeWord(int address, int value) {
        checkAddress(address); // Reject the whole store before changing any byte.
        for (int i = 0; i < 4; i++) {
            bytes[address + i] = (byte) (value >>> (8 * i));
        }
        // The processor records this SW in its memoryWrites map, even if unchanged.
    }

    @Override
    public synchronized void reset() {
        Arrays.fill(bytes, (byte) 0);
    }

    @Override
    public synchronized byte[] snapshot() {
        return bytes.clone();
    }

    private static void checkAddress(int address) {
        if (address < 0 || address > SIZE_BYTES - 4 || address % 4 != 0) {
            throw new IllegalArgumentException(
                    "Memory address must be a multiple of 4 between 0 and 4092: " + address);
        }
    }
}
