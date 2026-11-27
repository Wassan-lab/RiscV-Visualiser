# Member 3 — Test Results and Verification

**Project:** RISC-V Visualiser  
**Course:** CCEN 350 — Computer Architecture  
**Test environment:** Windows, Java JDK 25.0.2  
**Status:** Member 3 implementation and Member 1 integration tested successfully.

## 1. Automated Testing

The following test suites were executed successfully.

| Test Suite | Result |
|---|---|
| Member 3 memory, performance, Amdahl's Law, comparison, and Swing tests | 1,105 checks passed |
| Member 1 processor tests | All 14 check groups passed |
| Member 1 user-interface tests | All 7 check groups passed |

The 1,105 checks include repeated memory-address tests and should not be interpreted as 1,105 distinct architectural scenarios.

### Test Commands

```powershell
java Build.java test
java -cp "build/classes;build/test-classes" team.riscv.cpu.ProcessorChecks
java "-Djava.awt.headless=true" -cp "build/classes;build/test-classes" team.riscv.ui.UiChecks
```

## 2. Data Memory Verification

The Data Memory Inspector was tested using a four-instruction program:

```asm
ADDI x1, x0, 100
ADDI x2, x0, 42
SW x2, 0(x1)
LW x3, 0(x1)
```

The following results were observed:

| Test | Expected Result | Observed Result |
|---|---|---|
| Store address | `0x0064` | `0x0064` |
| Stored bytes | `2A 00 00 00` | `2A 00 00 00` |
| Hexadecimal word | `0x0000002A` | `0x0000002A` |
| Signed decimal word | `42` | `42` |
| Loaded register x3 | `42` | `42` |
| SW highlighting | Memory row highlighted | Passed |
| Memory reset | Word returns to zero | Passed |

These results verify the shared-memory connection between the processor and the memory inspector for the tested program.

## 3. Performance Metrics Verification

The Memory Load / Store program completed in 10 simulated cycles with four completed instructions at the configured 100 MHz clock frequency.

| Metric | Expected | Observed |
|---|---|---|
| Cycles | 10 | 10 |
| Completed instructions | 4 | 4 |
| CPI | 2.500 | 2.500 |
| IPC | 0.400 | 0.400 |
| Simulated execution time | 0.100 µs | 0.100 µs |
| Processor status | FINISHED | FINISHED |

The calculations were verified using:

\[
CPI = \frac{10}{4} = 2.5
\]

\[
IPC = \frac{4}{10} = 0.4
\]

\[
T = \frac{10}{100,000,000} = 0.100\ \mu s
\]

The execution time represents modeled processor time, not the real elapsed time of the Java application.

## 4. Amdahl's Law Verification

The interactive calculator was tested using 50% affected execution time and a local improvement factor of 2.

\[
S = \frac{1}{(1-0.5)+\frac{0.5}{2}} \approx 1.333
\]

**Observed result:** `1.333×`

The slider and improvement-factor control update the theoretical result interactively.

## 5. Measured Speedup Comparison

The completed-run comparison was tested by executing the `Single ADDI` example twice with identical initial conditions.

| Metric | Baseline | Improved |
|---|---|---|
| Cycles | 5 | 5 |
| Simulated execution time | 0.050 µs | 0.050 µs |
| Intended outputs | Matching | Matching |

The measured speedup was:

\[
S = \frac{0.050}{0.050} = 1.000\times
\]

Both completed runs were saved successfully, and the execution-time comparison bars displayed equal lengths.

This verifies the basic completed-run comparison workflow. It does not demonstrate an actual optimization.

## 6. GUI and Integration Verification

Manual testing confirmed that:

- Processor steps update the Memory and Performance tabs.
- Switching tabs preserves the simulation state.
- Register values update after instruction completion.
- Memory writes are highlighted during the relevant processor cycle.
- Reset clears memory and performance counters.
- The application builds and launches as `RiscV-Visualiser.jar`.
- The interface uses a consistent blue-and-white tabbed design.

## 7. Remaining Final Team Tests

The following checks remain pending Member 2's integration:

- Assembly editor and parser validation.
- Custom XWSUM instruction execution and datapath visualization.
- Baseline versus XWSUM comparison using matching initial inputs and intended outputs.
- Final regression testing of the complete team application.
- Verification of the final executable on a teammate's Java 21 installation.

## 8. Conclusion

The completed tests support the correctness of the implemented Member 3 memory and performance components for the verified cases, including integration with Member 1's processor.

Final validation of the complete simulator remains dependent on Member 2's parser and custom ISA extension.
