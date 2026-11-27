# Connecting Member 3's work

These notes describe the supplied components. Processor and parser names below refer to the team's proposed API; those implementations are not included here.

## One memory object for the whole simulation

Create a `team.riscv.memory.DataMemory` and give the SAME object to the processor and `team.riscv.ui.Member3Panel`.

```java
DataMemory memory = new DataMemory();
Member3Panel member3 = new Member3Panel(memory);
// Once Member 1's Processor class exists:
Processor processor = new Processor(memory);
```

Member 1 uses `memory.readWord(address)` in MEM for LW and `memory.writeWord(address, value)` in MEM for SW. On a successful SW, include its address and value in that cycle's `ProcessorSnapshot.memoryWrites`, even when the stored value is unchanged. Clear the map next cycle. The memory class cannot infer pipeline cycle boundaries.

Address errors throw IllegalArgumentException before memory changes. The processor converts a program's invalid access into an ERROR snapshot and stops advancing. LW to x0 must still perform its memory access even though its value is discarded.

Do not instantiate a second memory just for the GUI. Do not write into the array returned by snapshot and expect the memory to change: it is a separate copy.

## Update on each cycle

In the Step button handler, and on each Run timer tick:

```java
ProcessorSnapshot state = processor.step();
member3.update(state);
```

Use the returned snapshot for the other displays too. Calling snapshot repeatedly should not advance the processor. After load/reset, also update the panel with the new starting snapshot. Baseline and improved saved comparison results deliberately survive a reset so two separate runs can be compared; the Clear comparison button removes them.

Create and update Swing components on the event dispatch thread. For this small simulator, a javax.swing.Timer can call one bounded processor step and then update all panels. Do not run an endless simulation loop in a button handler. Stop the timer on FINISHED or ERROR, and before loading/resetting. Do not queue a snapshot for later while allowing its separate memory object to advance ahead of it: refresh both from the same cycle.

The processor must provide counts with these meanings:

- cycleCount includes pipeline filling, stalls and draining.
- completedInstructions counts successful instructions completing WB, including stores and branches. It excludes bubbles and failures.
- The live table uses the register and memory write maps for the cycle just executed.
- FINISHED means no instruction remains to execute, not merely that fetching reached the program end.

## Save real measured results

After a successful complete run, first call `member3.update(finalState)`, then:

```java
member3.setCompletedRun(
    "weighted-sum",
    "a=20;b=10;other initial registers and memory zero",
    Map.of("x3", finalState.registers()[3])
);
```

The display now enables Save baseline and Save improved. Run the baseline, save it, reset/load the improved program, then save that result. The component requires equal workload IDs, equal input IDs and equal selected output maps before displaying speedup.

The input ID is supplied by the integration code; the component does not independently inspect a program's entire starting state. Use an identifier that faithfully captures every relevant initial input. Include every intended output in the output map, including relevant memory results where needed; exclude scratch registers that need not match. Equal metadata is not a proof of program equivalence.

Example programs for a real integrated comparison:

Baseline:
```asm
ADDI x1, x0, 20
ADDI x2, x0, 10
ADD x5, x2, x2
ADD x5, x5, x2
ADD x3, x1, x5
```

Improved:
```asm
ADDI x1, x0, 20
ADDI x2, x0, 10
XWSUM x3, x1, x2
```

Both must finish with x3 = 50. The baseline's temporary x5 can differ. Count all cycles, including setup instructions and stalls. Do not hardcode the measured cycle counts or claim a guaranteed 3× whole-program speedup. XWSUM is proposed to take one EX cycle; document this modeling assumption when interpreting results.

## Main window

`new MainWindow(editorPanel, processorPanel, customInstructionPanel, member3Panel)` assembles the components. Each argument is a Swing component; Member 1's processor panel can contain its register table, five-stage display and Step/Run/Reset controls. Member 2 supplies editor and custom-instruction panels. Call setVisible(true) on the completed window.

MainWindow supplies layout, not execution logic. The real app/controller still connects the editor, parser and processor. The preview uses labelled placeholders so nobody confuses it with the completed group app.

## Final packaging

`java Build.java` produces a runnable JAR in dist. Currently its entry point is Member3Preview. Once the real app exists, update the MAIN_CLASS value and JAR filename in Build.java to the team's real launch class and chosen package name. Rebuild, run the actual assembly tests and try the JAR on a fresh teammate machine with Java 21.

A JAR is a Java executable package; it needs Java installed. A bundled-runtime installer is a separate packaging choice, if your doctor requires one. This preview package does not include a Windows/macOS installer.

The existing shared files are unchanged. There is no new mandatory shared model definition.
