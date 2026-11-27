# Member 3 — Integration Guide

## 1. Overview

The Data Memory Inspector and Performance and Amdahl's Law Engine have been integrated with Member 1's processor in the RISC-V Visualiser.

The application currently includes:
- Five-stage processor pipeline and register display.
- Data memory inspection with real-time store highlighting.
- Live performance metrics (cycles, completed instructions, CPI, IPC, and execution time).
- Interactive Amdahl's Law calculator.
- Baseline and improved-run comparison with measured speedup.

The remaining integration work involves Member 2's assembly editor, parser, and custom ISA extension.

## 2. Existing Integration

The application entry point is:

`src/main/java/team/riscv/ui/CombinedApp.java`

The processor and memory inspector share the same `DataMemory` instance:

```java
DataMemory memory = new DataMemory();
Processor processor = new Processor(memory);
Member3Panel member3 = new Member3Panel(memory);
```

Processor snapshots update the memory and performance displays through the existing snapshot listener.

The memory model must remain shared between the processor and the GUI. Creating separate memory objects would cause inconsistent displayed values.

## 3. Memory Operations

The processor performs memory operations during the MEM stage.

- `LW` reads a 32-bit word from an aligned memory address.
- `SW` writes a 32-bit word to an aligned memory address.
- Memory uses little-endian byte ordering.
- Successful stores are highlighted using the current cycle's memory-write information.
- Invalid memory accesses must produce an appropriate processor error without partially modifying memory.

The existing Memory Load / Store example has been manually tested.

## 4. Performance Metrics

Performance values are updated using processor snapshots.

The main calculations are:

**Cycles per instruction:**

\[
CPI = \frac{\text{Cycles}}{\text{Completed Instructions}}
\]

**Instructions per cycle:**

\[
IPC = \frac{\text{Completed Instructions}}{\text{Cycles}}
\]

**Simulated execution time:**

\[
T = \frac{\text{Cycles}}{\text{Clock Frequency}}
\]

The current application assumes a simulated clock frequency of 100 MHz.

These values represent simulated processor execution, not the real time taken by the Java application.

## 5. Integrating the Custom Instruction

Member 2 will integrate the custom XWSUM instruction with the processor and its corresponding datapath visualisation.

The instruction must be correctly decoded and executed by the processor. Its results must appear in the register display and be reflected in the performance metrics.

The custom-instruction interface should follow the existing blue-and-white application styling.

## 6. Baseline and Improved-Run Comparison

The Performance tab already supports saving and comparing completed runs.

For the final XWSUM demonstration, the baseline and improved programs must use equivalent initial inputs and produce matching intended outputs.

**Baseline program:**

```asm
ADDI x1, x0, 20
ADDI x2, x0, 10
ADD x5, x2, x2
ADD x5, x5, x2
ADD x3, x1, x5
```

**Improved program:**

```asm
ADDI x1, x0, 20
ADDI x2, x0, 10
XWSUM x3, x1, x2
```

Both programs should produce `x3 = 50`.

The comparison workflow must register each completed run with matching workload and input identifiers and the same selected intended outputs.

Temporary registers such as `x5` should not be compared when their values are not part of the intended program result.

The measured speedup is calculated as:

\[
S = \frac{T_{\text{baseline}}}{T_{\text{improved}}}
\]

The application displays execution-time comparison bars after two valid completed runs are saved.

Actual speedup must be calculated from the processor's simulated execution results rather than hardcoded values.

## 7. Build and Run

From the project directory:

**Run Member 3 tests:**

```bash
java Build.java test
```

**Build the application:**

```bash
java Build.java
```

**Launch the application:**

```bash
java Build.java run
```

The executable is generated at:

`dist/RiscV-Visualiser.jar`

The current launcher opens the combined tabbed application. Final team packaging should be verified again after Member 2's integration.

## 8. Final Integration Checklist

- [ ] Connect the assembly editor and parser.
- [ ] Integrate XWSUM execution into the processor.
- [ ] Connect the custom-instruction datapath visualisation.
- [ ] Verify the custom instruction's register results.
- [ ] Compare baseline and improved programs using matching inputs and intended outputs.
- [ ] Confirm the measured execution-time bars display correctly.
- [ ] Run all automated tests after integration.
- [ ] Verify the final executable JAR launches the completed application.

## 9. Current Verification

The following tests have passed:

- 1,105 Member 3 memory, performance, Amdahl, comparison, and Swing checks.
- All 14 processor test groups.
- All 7 processor UI test groups.

Manual testing also verified LW/SW operations, memory highlighting, register updates, reset behavior, performance calculations, and a 1.000× comparison using two identical completed runs.

The final XWSUM comparison remains pending integration.
