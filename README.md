# RISC-V Visualiser

**CCEN 350 — Computer Architecture**  
**Term Project: Interactive RISC-V Architectural Visualizer & Simulator**

## Project Overview

RISC-V Visualiser is a Java-based desktop application designed to demonstrate the internal operation of an RV32I processor through an interactive graphical interface.

The application allows users to observe instruction execution across a five-stage pipeline, inspect register and memory values, and analyze processor performance.

The project is developed using Java Swing.

## Features

### 1. Processor Visualisation
- Five-stage pipeline: IF, ID, EX, MEM, and WB.
- Step, Run, Pause, and Reset controls.
- Cycle-by-cycle instruction execution.
- Pipeline stalls and instruction dependencies.
- Register file displaying x0–x31 in hexadecimal and signed decimal.

### 2. Data Memory Inspector
- Byte-addressable memory with word-aligned access.
- Little-endian memory representation.
- Hexadecimal and signed decimal word values.
- Address navigation.
- Real-time highlighting of memory writes during SW instructions.

### 3. Performance and Amdahl's Law
- Live cycle count and completed instruction count.
- CPI and IPC calculations.
- Simulated execution time.
- Interactive Amdahl's Law calculator.
- Baseline and improved-run comparison.
- Measured speedup and execution-time visualisation.

### 4. Custom ISA Extension
The custom-instruction and datapath module is reserved for final team integration.

## Technologies

- Java 21-compatible source code
- Java Swing
- Git and GitHub

## Build and Run

A Java Development Kit (JDK) version 21 or newer is required.

From the project directory, run:

```bash
java Build.java test
```

To build the executable JAR:

```bash
java Build.java
```

To launch the application:

```bash
java Build.java run
```

Alternatively, after building:

```bash
java -jar dist/RiscV-Visualiser.jar
```

## Testing

The current implementation has passed:

- 1,105 memory, performance, Amdahl's Law, comparison, and Swing checks.
- 14 processor test groups.
- 7 processor UI test groups.

Manual integration tests have also verified memory stores and loads, register updates, memory reset, performance metrics, and completed-run comparisons.

## Shared Model Files

- `Instruction.java` — Represents an instruction shared between the parser and processor.
- `ProcessorSnapshot.java` — Contains processor state, pipeline information, register values, and execution counters.
- `MemoryAccess.java` — Defines the interface for memory reading, writing, resetting, and snapshots.

## Development Status

The processor, register display, data-memory inspector, and performance components have been integrated and tested.

Final team integration includes the assembly editor/parser, custom ISA extension and datapath, and validation of optimized-program comparisons.

## Course

CCEN 350 — Computer Architecture