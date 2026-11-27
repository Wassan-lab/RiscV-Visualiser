# Member 3 report section — draft for review

Status: memory and performance components are implemented and component-tested. Final processor integration, whole-program timing validation and native desktop testing remain pending. Replace this status after completing those checks. This is a section draft for the combined technical report, not a final whole-project report.

## Scope

This contribution covers the byte-addressable data memory, memory inspector, performance counters, interactive Amdahl calculator, comparison of completed simulator runs, dashboard layout and executable Java packaging support. The processor produces the cycle snapshots; this contribution consumes them.

## Data memory

DataMemory implements the existing MemoryAccess interface. It contains 4096 bytes with valid word starting addresses 0, 4, ..., 4092. The memory capacity is a project design choice. Each word contains four bytes in little-endian order. Reads reconstruct the 32-bit bit pattern, including negative values. Stores validate their address before changing any byte, preventing partial invalid writes.

Reset clears all bytes. A memory snapshot returns a copy so display code cannot alter the simulator's memory. The model and GUI are separate classes, allowing the model to be tested without a window.

The table shows each word's starting address, its four bytes in hexadecimal, its complete hexadecimal word and its signed decimal value. Users can jump to any byte address. Successful stores are highlighted using the processor's per-cycle memoryWrites map. This also shows a store that writes the same value as before; it does not rely on comparing old and new bytes.

## Performance model

Cycles and completed instructions are taken from ProcessorSnapshot. CPI = cycles / completed instructions. IPC = completed instructions / cycles. Undefined ratios display a dash. Simulated time = cycles / clock frequency. The default 100 MHz is an assumed model parameter, not a measured hardware frequency. Animation delays and host execution time do not enter these metrics.

Counts include the pipeline's initial filling, stalls and draining. Stores and branches count as completed instructions when they finish WB. These counting rules must be implemented and verified in the processor; the performance code cannot determine whether a supplied count is architecturally correct.

## Amdahl and measured comparisons

The calculator uses S = 1 / ((1-f) + f/s). The slider represents the percentage of original execution time affected, and the second control sets the local speedup. This is an interactive theoretical prediction. It is not automatically derived from instruction percentages, which would be inaccurate for a stalled pipeline.

The measured comparison uses baseline simulated execution time divided by improved simulated execution time. Results are saved after successful program completion. It checks matching workload/input identifiers and matching selected outputs before displaying speedup and comparison bars. Slower improved versions remain visible as speedups below 1. Inputs and output selections must be supplied honestly by integration code; checking selected outputs is not a proof of all possible program behavior.

The proposed weighted-sum example compares three ADD operations with one XWSUM operation while retaining identical setup inputs. Both produce x3 = 50 for inputs 20 and 10. Temporary registers need not match. Final measured cycle results must be added after the real processor is connected.

## Interface and packaging

Member3Panel combines the memory inspector and performance panel. MainWindow accepts the editor, processor and custom-instruction panels supplied by the other members. Display refreshes happen on Swing's event dispatch thread. Build.java compiles, tests and packages the project using Java's own compiler and JAR APIs, without additional build dependencies.

The supplied runnable JAR launches a clearly labelled preview. It is not the final simulator. Before submission, its entry point must be changed to the team's integrated application, rebuilt and tested on a desktop computer.

## Validation evidence

Automated checks cover zero initialization; little-endian byte order; first and last aligned addresses; signed values; all 1024 word positions; invalid-address rejection without partial writes; copied snapshots; reset; CPI/IPC/time; Amdahl boundaries and invalid inputs; valid, mismatched and failed comparison cases; GUI table values, highlights, address navigation, calculator updates and comparison buttons. See Member3Tests.java and run `java Build.java test` for the current count.

Panels were rendered off-screen and visually inspected. This verifies panel layout and interactions exercised by the tests. Native window behavior on Windows/macOS, final processor integration and complete program execution are not established by these component tests.

Before submission, add screenshots from the real integrated app, actual baseline/improved measurements, and test results from the target laptop. Each member should review and be able to explain the code they present.

## Trade-offs

- A fixed 4 KiB memory keeps the memory display manageable and satisfies the aligned-access requirement.
- A byte array makes byte addresses and little-endian word organization explicit.
- Snapshot copies use a little extra memory to prevent accidental state changes through displays.
- The assumed fixed clock permits reproducible comparisons; it does not estimate physical circuit timing or the actual maximum frequency of a custom datapath.
- Saving two completed runs is simpler than running two live simulator instances.
- A JAR keeps packaging portable but requires an installed Java runtime.

## Sources

- RISC-V RV32I specification: https://docs.riscv.org/reference/isa/v20260120/unpriv/rv32.html
- UC Berkeley CS61C Amdahl notes: https://notes.cs61c.org/content/parallel-performance/
- Java 21 Swing threading policy: https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/package-summary.html
- Course project requirements supplied by the instructor: baseline memory inspector, module D, UI/UX, documentation and AI log.
