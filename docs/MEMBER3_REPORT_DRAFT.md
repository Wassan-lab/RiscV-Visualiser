# Member 3 — Memory and Performance Implementation

**Course:** CCEN 350 — Computer Architecture  
**Project:** Interactive RISC-V Architectural Visualiser  
**Contribution:** Data Memory Inspector and Module D — Performance and Amdahl's Law Engine

## 1. Overview

The Member 3 contribution implements byte-addressable data memory, an interactive memory inspector, processor performance measurements, an Amdahl's Law calculator, and baseline/improved-run comparisons.

These components are integrated with Member 1's five-stage processor through shared memory and processor-state snapshots. The application uses a Java Swing interface with separate Processor, Memory, Performance, and Custom Instructions tabs.

## 2. Data Memory Implementation

The `DataMemory` class implements the shared `MemoryAccess` interface and provides 4,096 bytes (4 KiB) of memory.

Each 32-bit word occupies four consecutive bytes. Word reads and writes require addresses divisible by four, with valid starting addresses from 0 to 4092.

Memory uses little-endian byte ordering. For example, the value `0x12345678` is stored as:

`78 56 34 12`

The implementation validates memory addresses before modifying memory. Invalid accesses are rejected without partially changing the stored bytes.

The memory snapshot method returns a separate copy of the memory array, preventing the interface from modifying processor memory unintentionally.

The Data Memory Inspector displays addresses, individual bytes, hexadecimal words, and signed decimal values. It also supports address navigation and highlights successful `SW` operations using the processor's memory-write information.

## 3. Performance Metrics

The Performance panel receives cycle and completed-instruction counts through `ProcessorSnapshot`.

It calculates:

**Cycles per instruction (CPI)**

\[
CPI=\frac{\text{Cycle Count}}{\text{Completed Instructions}}
\]

**Instructions per cycle (IPC)**

\[
IPC=\frac{\text{Completed Instructions}}{\text{Cycle Count}}
\]

**Simulated execution time**

\[
T=\frac{\text{Cycle Count}}{\text{Clock Frequency}}
\]

The current implementation uses an assumed 100 MHz simulated clock.

Cycle counts include pipeline filling, stalls, and draining. Completed instructions are counted when they successfully retire at write-back.

Simulated execution time is different from the actual time taken by the Java application to run.

## 4. Amdahl's Law Engine

The interactive Amdahl's Law calculator estimates the theoretical overall speedup when part of a workload is improved.

\[
S=\frac{1}{(1-f)+\frac{f}{s}}
\]

Here, \(f\) is the fraction of original execution time affected, and \(s\) is the improvement factor for that portion.

For example, improving 50% of execution time by a factor of two gives:

\[
S=\frac{1}{0.5+0.25}=1.333
\]

The calculator updates its theoretical speedup when the user changes the slider or improvement factor.

## 5. Measured Performance Comparison

The Performance panel also supports comparing two completed simulation runs.

Measured speedup is calculated as:

\[
S_{\text{measured}}=\frac{T_{\text{baseline}}}{T_{\text{improved}}}
\]

The comparison engine checks matching workload identifiers, input identifiers, and selected intended outputs before displaying a valid comparison.

The interface presents the measured result using labeled execution-time bars.

The completed-run workflow was tested using two identical executions of the `Single ADDI` program. Both completed in five cycles, producing a measured speedup of `1.000×`.

This verifies the comparison mechanism but does not represent an actual optimization.

The final demonstration will compare a baseline RV32I implementation against Member 2's custom XWSUM instruction after integration.

## 6. Interface and Integration

The application uses Java Swing and the shared `UiTheme` styling to maintain a consistent blue-and-white interface.

The `CombinedApp` launcher creates a shared `DataMemory` object for the processor and memory display.

Processor snapshots are used to update memory contents and performance metrics after execution steps.

The tabbed layout separates the processor, memory inspector, and performance analysis into dedicated views while preserving the shared simulation state.

The Custom Instructions tab is reserved for Member 2's final integration.

## 7. Testing and Verification

Automated testing produced the following results:

| Test Suite | Result |
|---|---|
| Member 3 memory, performance, Amdahl, comparison, and Swing checks | 1,105 passed |
| Processor checks | 14 groups passed |
| Processor UI checks | 7 groups passed |

The 1,105 checks include repeated tests across memory addresses and do not represent 1,105 different architectural scenarios.

Manual integration testing also verified:

- Correct `SW` and `LW` operations.
- Little-endian memory representation.
- Store highlighting and memory reset.
- Register updates after instruction completion.
- Live CPI, IPC, cycle count, and execution time.
- Theoretical Amdahl speedup.
- Saving and comparing two completed runs.

The four-instruction memory example completed in 10 cycles, with four completed instructions, CPI of 2.500, IPC of 0.400, and simulated execution time of 0.100 µs.

Additional test evidence is recorded in `MEMBER3_TEST_RESULTS.md`.

## 8. Design Decisions and Trade-offs

The implementation uses a fixed 4 KiB memory to keep the simulator manageable and the memory inspector readable.

A byte array makes memory addressing and little-endian word operations explicit.

Copying memory snapshots requires additional storage but prevents accidental changes through the graphical interface.

A fixed simulated clock allows reproducible performance calculations but does not represent physical processor timing.

Saving completed runs provides a straightforward comparison mechanism without maintaining two simultaneous processor instances.

The Java JAR supports portable execution on compatible Java installations but requires a Java runtime.

## 9. Current Status and Remaining Integration

The Member 3 memory and performance components have been implemented, integrated with Member 1's processor, and tested.

The application builds as `RiscV-Visualiser.jar` and launches the combined tabbed interface.

Remaining team tasks include integrating Member 2's assembly parser and custom ISA extension, verifying the XWSUM datapath and measured performance comparison, and running final tests on the completed application.

## 10. References

- RISC-V RV32I specification: https://docs.riscv.org/reference/isa/v20260120/unpriv/rv32.html
- UC Berkeley CS61C performance notes: https://notes.cs61c.org/content/parallel-performance/
- Java 21 Swing documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/package-summary.html
- CCEN 350 Computer Architecture term project guidelines.
