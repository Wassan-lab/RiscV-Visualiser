# Member 3: start here

Your work is memory, the memory display, module D (performance and Amdahl's Law), the main-window layout and packaging. You also test and explain your own work in the report and presentation.

This package supplies those components and a working preview. The final simulator still needs the other members' parser, processor, register/pipeline displays and custom-instruction panel. The preview's sample button does not execute assembly.

## 1. Get the team repository onto your laptop

If you have never cloned it, open VS Code, then Terminal > New Terminal:

```sh
git clone https://github.com/Wassan-lab/RiscV-Visualiser.git
cd RiscV-Visualiser
git switch -c member3-memory-performance
```

Then use File > Open Folder and open that RiscV-Visualiser folder. Clone is a one-time download of the repository and its history. Do not create a second RiscV-Visualiser folder inside it.

If you already cloned it and have no uncommitted edits, open its folder and run:

```sh
git status
git switch main
git pull --ff-only origin main
git switch -c member3-memory-performance
```

If that branch already exists, use `git switch member3-memory-performance`. If `git status` lists your own unfinished changes, keep them; do not delete or reset them just to follow these instructions.

## 2. Add the supplied work

From the downloaded pack, copy the CONTENTS of `ADD-TO-YOUR-REPO` into your local RiscV-Visualiser folder. Merge the src folders; do not replace the entire src folder. The additions deliberately do not include or overwrite the team's three shared model files or README.

If a new file already exists because a teammate has started it, compare the files before overwriting. This package was prepared against the repository containing just README and the three shared definitions on 27 November 2026.

## 3. Run it

Use a Java 21 **JDK**. This is Java with its development tools; Swing is already included. No separate Swing or Maven installation is required for this package.

In the project folder's VS Code terminal:

```sh
java -version
java Build.java test
java Build.java run
```

The test command should report all checks passing. The run command opens the preview. A desktop display is needed to open a Swing window; Google Colab is not the intended place to run this app.

Try these four things:

1. Click **Show sample cycle**. Address 0 contains bytes `78 56 34 12`, representing word `0x12345678`.
2. Enter `0xFFC` in the address box. This is the final word, shown as `0xFFFFFFFF` and signed value `-1`.
3. Move the Amdahl slider. At 50% affected time and a 2× local improvement, overall theoretical speedup is about 1.333×.
4. Click **Reset preview**. Memory and counters return to zero. Measured-comparison buttons remain disabled in this sample preview because no real program has completed.

For a quick preview without building, open a terminal in the pack's RUN-PREVIEW folder and run `java -jar member3-preview.jar` with Java 21. This is still a preview, not the final group submission.

## 4. Understand your work in this order

| Read this file | What you should be able to explain |
| --- | --- |
| `src/main/java/team/riscv/memory/DataMemory.java` | Why one word uses four bytes, why addresses must be aligned, how little-endian reads/writes work, and why snapshot returns a copy. |
| `src/main/java/team/riscv/performance/PerformanceMetrics.java` | CPI, IPC, cycles, completed instructions, and simulated execution time. |
| `src/main/java/team/riscv/performance/PerformanceEngine.java` | Amdahl's formula and why comparison runs need identical inputs and matching intended outputs. |
| `src/main/java/team/riscv/ui/MemoryPanel.java` | How the table reads a memory copy and highlights stores reported by the processor. |
| `src/main/java/team/riscv/ui/PerformancePanel.java` | Live counters, the calculator, saving two completed runs and drawing comparison bars. |
| `src/main/java/team/riscv/ui/Member3Panel.java` | The single panel your teammate updates after each processor cycle. |
| `src/main/java/team/riscv/ui/MainWindow.java` | Where the three members' panels fit. |

`UiStyle.java` only holds colours and spacing. `Member3Preview.java` only supplies a preview. `Build.java` builds/tests/packages the code. You do not have to learn every file at once.

Use `docs/MEMBER3_INTEGRATION.md` when your teammates connect their work. Use `docs/MEMBER3_REPORT_DRAFT.md` as a draft, not a claim that the whole team project is finished.

## 5. Share your tested source through GitHub

After running the tests, trying the preview and reviewing the changes:

```sh
git status
git add src/main/java/team/riscv/memory src/main/java/team/riscv/performance src/main/java/team/riscv/ui src/main/java/team/riscv/preview src/test/java/team/riscv/tests Build.java run-preview.bat run-preview.sh .gitignore MEMBER3_START_HERE.md docs
git commit -m "Add member 3 memory and performance components"
git push -u origin member3-memory-performance
```

Review the files under `docs` before staging if teammates have added their own documents there. Git does not upload changes just because you saved a file. Commit records the selected changes locally; push uploads those commits.

On GitHub, open a pull request from your branch to main so the team can review it. A pull request proposes combining the work. It does not need to be merged until the team is ready. Do not force-push or replace main.

The build and dist folders are generated and excluded from source commits. The executable JAR can be shared separately. The final team's JAR must launch the integrated simulator rather than Member3Preview.

## What is still a team integration task?

- Connect the real processor to this memory and update these panels after each cycle.
- Connect the parser, real controls, register/pipeline displays and custom datapath.
- Compare real baseline and XWSUM programs with the same inputs and verify their intended outputs.
- Run the full test cases, try the executable on a teammate's laptop, and finish each person's report section and AI log.

You can work on your components now. You do not need to implement the other members' features to test these components.

Official setup reference: https://code.visualstudio.com/docs/java/java-tutorial
