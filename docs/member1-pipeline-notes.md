# Member 1: first executable pipeline milestone

## What changed

Processor.java now implements cycle-by-cycle execution of ADD, SUB, ADDI, LW,
SW, BEQ and BNE. RegisterFile.java and all shared model files stay unchanged.
ProcessorChecks.java is a standalone test runner, not the application's GUI.

## Install on Windows

1. Save all files in VS Code. Exit any old JShell session with /exit.
2. Extract member1-pipeline.zip into C:\Projects\RiscV-Visualiser, replacing
   the previous Processor.java. The ZIP contains paths starting with src/ and
   docs/; do not extract it into out/ or into another nested project folder.
3. In PowerShell at the repository root:

```powershell
$sources = Get-ChildItem -Path .\src\main\java, .\src\test\java -Recurse -Filter *.java
javac -d out $sources.FullName
java -cp out team.riscv.cpu.ProcessorChecks
```

Expected: ALL 14 CHECK GROUPS PASSED, followed by a five-cycle ADDI trace.
If javac reports an error, resolve that before running the checks.

## Public methods

| Method | Meaning |
| --- | --- |
| new Processor() | Create a processor for programs without LW/SW. |
| new Processor(memory) | Connect Member 3's MemoryAccess implementation. |
| loadProgram(instructions) | Validate/copy the program, then reset state and memory. |
| step() | Advance one processor clock cycle and return a ProcessorSnapshot. |
| snapshot() | Read current state without advancing. |
| reset() | Clear state and connected memory while retaining the program. |
| programSize() | Return the number of loaded static instructions. |

## Architectural choices to explain in the viva

- Each step represents one entire clock cycle; stages overlap.
- For one ADDI, the successive snapshots show IF, ID, EX, MEM, WB. The
  destination register changes only in cycle 5, at WB.
- There is no forwarding. ID checks both EX and MEM for pending register
  writes. It holds IF/ID and inserts EX bubbles until inputs are available.
- WB writes are visible to ID in the same cycle. ID latches its operands;
  EX uses those latched values in the following cycle.
- Immediate dependencies therefore insert two stalls in this design.
- An independent sequence of N instructions takes N+4 cycles, including
  filling and draining the pipeline.
- x0 writes are ignored and do not cause RAW stalls. A load into x0 still
  reads memory and can fail on an invalid address.
- SW uses rs1 as its address base and rs2 as its data. Both are checked for
  dependencies. Memory reads/writes happen only in MEM.
- Branches pause fetching in ID and EX. They compare latched register values
  in EX; fetching resumes the next cycle. No younger wrong-path instruction
  is fetched, so this policy needs no speculative flush or branch predictor.
- Instruction addresses are 0, 4, 8, ... . Branch immediates are signed byte
  offsets from the branch instruction's own address.
- A taken target must be aligned and within the program or exactly one past
  its end. A target exactly at the end terminates normally after draining.
  These are this educational simulator's program-boundary rules.
- All instructions, including branches and stores, count as completed at WB.
  Bubbles never count. The final snapshot can still display the just-retired
  WB instruction while status is FINISHED; there is no extra empty clock tick.
- Reset clears all connected memory to zero. Seed test/input memory after
  loadProgram, because loading calls reset.
- Snapshots copy their mutable contents. The GUI cannot modify CPU registers
  by editing a returned array. Each fetched instruction gets a distinct ID,
  including successive executions of the same instruction in a loop.
- Integer arithmetic wraps to the low 32 bits. Presentation as hexadecimal or
  signed decimal does not change the stored bits.

## Integration boundaries and remaining work

- Member 3 supplies the application's MemoryAccess implementation. The nested
  TestMemory inside ProcessorChecks exists only to test the processor and is
  not an application memory component.
- XWSUM is explicitly rejected at load time until Member 2's operation,
  encoding/decoding and datapath integration are added. Do not interpret the
  presence of XWSUM in the shared enum as completed custom-instruction support.
- The GUI, Step/Run/Reset buttons, register table, pipeline animation, custom
  extension, performance panels and executable package remain to be built or
  integrated. This milestone adds the engine behind Step, not the button.
- Run must be stoppable and driven by bounded calls to step(), rather than an
  unbounded loop on Swing's event-dispatch thread. Infinite assembly loops are
  valid; stopping them is a responsibility of the controller.
- Invalid accesses stop the processor with ERROR. Older stage effects already
  performed that cycle are retained. This is a fail-stop educational simulator,
  not an implementation of architectural exception/trap recovery.
- FINISHED and ERROR steps return the existing snapshot unchanged. A UI should
  refresh from snapshots, not replay their write maps as new memory operations.

## Validation performed

Compiled with the OpenJDK 21 compiler using -source 17 -target 17 and ran on
OpenJDK 21. The environment did not have the release-17 API signature archive,
so this was not a --release 17 validation. The code uses Java 17-compatible
language features and standard APIs; run the commands above on your Java 17
installation to confirm in your environment.

All 14 check groups passed:

1. Single-instruction stage timing and WB-only register writes.
2. Independent instruction overlap and N+4 cycle count.
3. RAW stalls, stable IDs, and multiple pending writes to one register.
4. Store/load timing and dependent address/data operands.
5. Taken and untaken BEQ/BNE with a store on the skipped path.
6. Backward loops, unique fetch IDs and retirement counts.
7. x0 protection, no false x0 dependency, and loads into x0.
8. Misaligned/out-of-range memory, invalid stores, and older retirement.
9. Branch target alignment/range and untaken target behavior.
10. Reset, replay, defensive copies and invalid program loading.
11. 32-bit arithmetic overflow.
12. Empty programs, same-value write events and stable finished state.
13. Explicit errors for missing memory and unintegrated XWSUM.
14. 100 deterministic generated programs compared with a simple sequential
    interpreter for registers, memory and completed-instruction counts.

These checks do not constitute a RARS cross-check or a GUI/integration test.
Add RARS comparisons and integration tests during later milestones.

## AI usage log entry to incorporate into your own log

Date: 2026-11-27.
Tool: ChatGPT/Codex.
Context: continued the user's Member 1 processor setup after initial register,
program-loading and reset checks passed; latest user message was "okay done".
Generated: the pipeline implementation, standalone checks and these notes.
AI-side verification: the validation listed above.
Student verification: fill this in after running the checks locally, reviewing
the code and explaining the stall, branch and WB behavior in your own words.
Do not claim student verification before doing it.
