package team.riscv.model;

/**
 * Memory operations used by the processor and memory display.
 * The DataMemory class will provide the code for these methods.
 * Our memory starts at address 0 and contains 4096 bytes, initially zero.
 */
public interface MemoryAccess {
    int SIZE_BYTES = 4096; // 4 KiB

    /**
     * Read a 32-bit word without changing memory.
     * Valid addresses are 0, 4, 8, ..., 4092.
     * Use little-endian order: the lowest address holds the least significant byte.
     * Throw IllegalArgumentException if the address is invalid.
     */
    int readWord(int address);

    /**
     * Store a 32-bit word in four bytes, using little-endian order.
     * Check the address before changing memory; valid addresses are 0, 4, ..., 4092.
     * Throw IllegalArgumentException if it is invalid.
     * Still report the store as a write if the value stays the same.
     */
    void writeWord(int address, int value);

    // Clear all bytes back to zero.
    void reset();

    /**
     * Return a separate copy of all 4096 bytes for the display.
     * The array index is the byte address. Editing this copy must not change memory.
     * Use Byte.toUnsignedInt(...) when displaying a byte as a number from 0 to 255.
     */
    byte[] snapshot();
}
