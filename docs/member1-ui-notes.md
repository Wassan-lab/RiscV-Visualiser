# Member 1 graphical interface milestone

This package adds Swing panels and a runnable demo to the processor milestone.
It does not replace the processor, register storage or shared model files.

## Windows installation and checks

Save all files in VS Code, exit JShell if open, then run in PowerShell:

```powershell
cd "C:\Projects\RiscV-Visualiser"
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\member1-ui.zip" -DestinationPath . -Force
$sources = Get-ChildItem -Path .\src\main\java, .\src\test\java -Recurse -Filter *.java
javac -encoding UTF-8 -d out $sources.FullName
java -cp out team.riscv.cpu.ProcessorChecks
java "-Djava.awt.headless=true" -cp out team.riscv.ui.UiChecks
java -cp out team.riscv.ui.Member1Demo
```

The two check runners should report 14 processor groups and 7 UI groups passed.
The final command opens a window; the terminal stays occupied until it closes.
If compilation fails, stop and fix the compiler error before running anything.

## Try these three examples

1. Single ADDI: click Step five times. The instruction moves through IF, ID,
   EX, MEM, WB. x1 remains zero until the fifth cycle, then shows 0x00000005 / 5.
   The register row turns blue because it was written at WB that cycle.
2. Dependent arithmetic: choose it and click Load example. Step to cycle 3 to
   see the pale-blue waiting stages and their reasons. Run finishes in 11 cycles
   with x1=5, x2=10, x3=5 and 3 completed instructions.
3. Branch loop: choose it and click Load example. Run executes three loop
   iterations, finishing with x1=0, x2=3 and 10 completed instructions. Try
   Pause and resume while it runs. Reset clears values and counters and keeps
   the loaded program so you can replay it.

The animation delay changes only wall-clock playback speed, never modeled
processor timing. Single-step cycles are shown as PAUSED in the display until
the program finishes; the engine's underlying state remains RUNNING while it
has unfinished work. This UI label does not change ProcessorSnapshot.Status.
The demo's assembly listing is read-only. It is not Member 2's parser/editor.

## Files and responsibilities

| File | Responsibility |
| --- | --- |
| UiTheme.java | Shared white-and-blue colors, fonts, cards, buttons and scroll styling. |
| RegisterPanel.java | Read-only x0-x31 tables, fixed-width hexadecimal and signed decimal, WB highlights. |
| PipelinePanel.java | Five stage cards, instruction/source information, instance IDs, computed values, stalls. |
| ProcessorPanel.java | Bind processor snapshots to panels and implement Step, Run, Pause, Reset and playback speed. |
| Member1Demo.java | Standalone window with three hardcoded example programs for development and presentation practice. |
| UiChecks.java | Automated component and timer tests without opening a desktop window. |

All UI source files are under src/main/java/team/riscv/ui. The check runner is
under src/test/java/team/riscv/ui. No external GUI dependency is required.

## Integration for Member 3

Create the processor with your real MemoryAccess implementation, then create
the ProcessorPanel on Swing's event-dispatch thread and add it to your window.
The ProcessorPanel is reusable; the Member1Demo window is just a development
launcher and need not be used in the final application.

```java
// Run this inside SwingUtilities.invokeLater or an existing Swing event.
Processor cpu = new Processor(dataMemory);
ProcessorPanel processorPanel = new ProcessorPanel(cpu);
processorPanel.setSnapshotListener(snapshot -> {
    // Refresh your memory and performance panels from this snapshot.
    // Read full memory from dataMemory.snapshot().
});
// After Member 2 parses a program:
processorPanel.loadProgram(parsedInstructions);
```

The names dataMemory and parsedInstructions represent your teammates' objects;
they are not additional classes supplied by this package.

- Use panel.loadProgram so loading also stops a running timer and refreshes
  the display. Handle invalid input in the editor before or around this call.
- The snapshot listener runs on the event-dispatch thread after load, reset
  and each CPU step, and immediately when the listener is registered.
- Only one listener is stored. Use one callback to update all dependent panels.
- Keep callbacks short. Avoid file/network work and long loops on this thread.
- Do not drive the same processor from another timer/thread simultaneously.
- Run performs one step per Swing Timer tick. It is pausable even for an
  infinite assembly loop. FINISHED or ERROR stops the timer automatically.
- Reset and program loading stop Run. Removing the panel stops its timer too.
- Write maps are display events for that snapshot. Do not apply them back to
  processor memory or count them as additional completed instructions.
- The register table and pipeline panel can also be embedded separately;
  both expose updateSnapshot(ProcessorSnapshot).
- The displayed counts are the CPU's actual counters. Member 3 still owns
  CPI, IPC, execution time, speedup and Amdahl calculations and their UI.

## Verification and limits

The AI-side environment compiled with OpenJDK 21 using -source 17 -target 17
and ran with Java 21. The code uses Java 17-compatible features and standard
Swing APIs. Run the supplied checks and demo on your Windows Java 17 setup.

All 14 existing processor check groups passed again. All 7 UI groups passed:

1. Step advances exactly once; WB values/highlights, reset and callbacks.
2. All register positions including x31, signed values, hexadecimal and x0.
3. Pipeline instruction text and dependency explanations.
4. Run, Pause, resume and Reset during an infinite branch loop.
5. Automatic Run termination on FINISHED.
6. Error explanation, automatic timer stop, and reset recovery.
7. Loading a different program and removing the panel stop Run.

An offscreen image of the panels was rendered and visually inspected. The
full native Windows window, desktop scaling, mouse interaction and final
team application integration still require checks on your computer.

Remaining team integration includes Member 2's parser/editor and XWSUM,
Member 3's memory and performance panels, packaging and full end-to-end tests.

## AI log entry

Date: 2026-11-27. Tool: ChatGPT/Codex.
Context: the student supplied successful results for all 14 processor check
groups and a successful push to member1-processor; continued with the agreed
Member 1 register display, pipeline display and controls milestone.
Generated: the Swing panels and launcher; the later white-and-blue restyle adds UiTheme.java, the UI check runner and these notes.
AI verification: the automated checks and offscreen visual inspection above.
Student verification: record your own Windows check output, manual example
results, code review and any changes here or in your team's AI usage log.
