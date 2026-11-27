# Member 1: minimal white-and-blue interface

This update changes presentation only. Processor.java, RegisterFile.java,
Instruction.java, ProcessorSnapshot.java and MemoryAccess.java are unchanged.
No forwarding, cache, or performance module was added.

## Changes

- White cards, a very pale blue page background, blue accents and softer borders.
- A program-selection sidebar with read-only assembly preview.
- One toolbar for Step, Run, Pause, Reset and playback delay.
- Clear sections for processor status, pipeline and registers.
- Compact register banks showing x0-x15 and x16-x31 side by side.
- Blue register rows indicate a WB write in the displayed cycle. Stalled cards
  use a pale-blue fill and retain explicit STALLED labels and explanations.
- Shared styling in UiTheme.java makes future color and font changes easier.
- The demo fits its initial window to the available desktop area. On smaller
  screens or larger display scaling, register tables can scroll.

## Install on Windows

Close the demo window and save all files in VS Code. Save the ZIP in Downloads,
then run the following in PowerShell. These commands install the GUI update;
they do not push anything to GitHub.

```powershell
cd "C:\Projects\RiscV-Visualiser"
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\member1-white-blue.zip" -DestinationPath . -Force
$sources = Get-ChildItem -Path .\src\main\java, .\src\test\java -Recurse -Filter *.java
javac -encoding UTF-8 -d out $sources.FullName
```

If compilation succeeds, run:

```powershell
java "-Djava.awt.headless=true" -cp out team.riscv.ui.UiChecks
java -cp out team.riscv.ui.Member1Demo
```

The check runner should finish with ALL 7 UI CHECK GROUPS PASSED. Quotes around
the -D argument are intentional for PowerShell. The demo occupies the terminal
until its window closes. Try all three example programs and the controls.

After confirming the result, save the updated files:

```powershell
git add src/main/java/team/riscv/ui src/test/java/team/riscv/ui docs/member1-ui-notes.md docs/member1-white-blue-notes.md docs/white-blue-preview.png
git commit -m "Restyle processor interface with a minimal white and blue theme"
git push
```

## Validation

All 14 existing processor check groups and all 7 UI check groups passed in the
AI-side environment. Compilation used OpenJDK 21 with -source 17 -target 17;
execution and offscreen rendering used Java 21. The code uses Java 17-compatible
language features and APIs. Validate the native Windows window on your Java 17
installation using the commands above.

The screenshot in docs/white-blue-preview.png renders the actual demo content
after five steps of the Single ADDI example. It shows x1 written at WB and all
32 register rows. Offscreen rendering does not replace native desktop testing.

The existing UiChecks highlight assertion now uses UiTheme.WRITE instead of a
literal green color. The original behavior checks remain in place.

## AI usage entry

Date: 2026-11-27. Tool: ChatGPT/Codex.
Request: "okay lets focus on my part and change its aesthetics, make it more
minimalistic, white & blues, and more organized".
Generated/updated: UiTheme and the four UI classes, the existing UI check's
presentation expectations/preview, installation notes and a rendered preview.
Verification: checks and visual inspection described above.
Student verification: add your Windows check result and manual observations
after installing. The shared processor/model interfaces were preserved.
