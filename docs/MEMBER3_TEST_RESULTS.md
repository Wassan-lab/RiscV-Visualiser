# Member 3 component validation

Date: 27 November 2026. Environment: OpenJDK 21, Linux; Swing panel checks in headless mode.

Command: `java Build.java test`

Result: **1105 checks passed**. This count includes a read/write check at every one of the 1024 aligned word positions, rather than 1105 independent architectural scenarios.

Covered:

- All-zero initial/reset memory; little-endian byte layout; signed 32-bit values; first/last word; copied memory snapshots.
- Invalid reads/writes, including negative, misaligned, out-of-range and extreme integer addresses; no partial mutation from rejected stores.
- CPI, IPC and modeled execution time; zero denominators and invalid parameters.
- Amdahl examples, endpoints and invalid inputs.
- Measured speedup, slower results, frequency-aware time ratios, input/output mismatches, unfinished/failed runs and immutable saved outputs.
- Swing table data, same-value store highlighting, clearing highlights next cycle, byte-address navigation and error text.
- Calculator interaction, comparison saving, stale/invalid comparison context and mismatched-output display.
- Off-screen panel rendering and visual inspection of the memory/performance panels.

The runnable JAR was built, and its entry point was checked in a headless environment. Opening the native window on the user's desktop remains to be verified. No real processor/parser is present in the supplied base repository, so these results do not validate whole-program execution, pipeline timing or the final integrated application.

The three original shared Java files are unchanged. The delivered additions omit those files so they cannot overwrite the team's definitions when copied into the repository.
