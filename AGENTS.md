# AGENTS.md

This document provides guidance for AI coding agents (and human contributors) working on the **hackrf-spectrum-analyzer** repository.

## Project Overview

This is a Java desktop **HackRF spectrum analyzer with a live MCP interface** for AI agents. It wraps `hackrf_sweep` as a native shared library (via JNA) for high-performance wideband sweeps, and exposes the same bins to Grok/Claude/Cursor over localhost MCP (`make mcp`) without a second USB open.

Key technologies:
- Java 21+ (Swing UI + FlatLaf + JFreeChart for plots)
- Native C/C++ (hackrf library v2026.01.3, sweep-as-library patch, parked IQ, ATSC GNU Radio/libfec DSP)
- Maven for Java build
- Custom Makefile for cross-platform native + Java packaging (Linux + Windows)
- Supports real-time spectrum, waterfall (left-side time scale), peak/persistent display, spur filter, frequency allocations, quick band selectors, Auto gain, Antenna LNA (+14 dB), and an opt-in MCP interface with read-only snapshot/diagnostic tools plus explicit FM Listen and TV Watch controls.

The project is a maintained fork of [pavsa/hackrf-spectrum-analyzer](https://github.com/pavsa/hackrf-spectrum-analyzer) with added quick-select UI and significant test coverage improvements.

**Primary use case**: An operator with a physical HackRF One, plus an optional local MCP client that reads the live sweep.

## Essential Commands

**Always start here:**

```bash
make help
```

This shows all available targets with descriptions and categories (colorized).

### Common Targets (from root)

- `make build` — Full build (natives + JAR + zip)
- `make test` — Run unit tests (Maven + JaCoCo). Does **not** require a HackRF.
- `make test-hw` — Hardware ITs (`*IT`, `@Tag("hardware")`): USB, firmware/USB API/board, `SpectrumSweepEngine` queue + dataset, start/stop/restart, LNA + antenna-power, FFT/freq restart, and parked FM IQ followed by sweep resume. Skips if no radio.
- `make info` — List attached HackRF devices, the SDK/USB API this app is pinned to, and whether a newer GSG firmware/libhackrf exists. Alias: `make list-devices`
- `make firmware-update` — Dry-run official GSG SPI flash. Write only with `CONFIRM=1` (optional `VERSION=2026.01.3`). Not part of `build` / `test`.
- `make udev` — Install persistent udev rules (sudo once) so WSL usbipd nodes stay writable.
- `make lint` — Compile/lint checks
- `make stats` — Rewrite [docs/stats.md](docs/stats.md) (first-party LOC, packages, tests, git). Do not hand-edit that file.
- `make mermaid` — Parse-check every first-party Mermaid fence (`mmdc` when installed)
- `make start` — Build (if needed) + launch the Linux app
- `make mcp` — Same as `start`, plus MCP on `127.0.0.1:8765` (`--mcp`)
- `make clean` — Clean build artifacts
- `make run` — Alias for `start`

From inside `src/hackrf-sweep/` you can also run the detailed native build targets directly (`make help` there too).

### Testing & Coverage

```bash
make test
# or directly
cd src/hackrf-sweep && mvn clean test

# Coverage report is written by `make test` (JaCoCo report bound to the test phase)
# Open src/hackrf-sweep/target/site/jacoco/index.html
```

Unit tests cover core DSP (SpurFilter, PersistentDisplay, DatasetSpectrum*, allocations, EMA, firmware parse, SpectrumSweepEngine, RadioIdentity, …) plus UI helpers. They run without hardware. **Do not bake a class count into this file** — run `make stats` and cite [docs/stats.md](docs/stats.md).

### Building Details

See [docs/building.md](docs/building.md) for full instructions, including required packages (Ubuntu recommended for cross-build).

Native build requires:
- Linux host with mingw-w64 for Windows cross-compilation
- hackrf submodule pinned to `HACKRF_SDK_PIN` (v2026.01.3) and patched automatically (`src-c/0001-hackrf_sweep-to-library-conversion-v2026.01.3.patch`)

`src/hackrf-sweep` is a **hybrid JNA module**: Maven Standard Directory Layout for Java (`src/main/java`, `src/main/resources`, `src/test/java`); Makefile + C/C++ under `src-c/` + `lib/hackrf` for natives. Do not commit Eclipse `.project` / `.classpath`, put CSVs under `src/main/java`, or vendor Java JARs into `lib/` (Maven owns Java deps). Windows cross-link trees stay under `lib/fftw-3.3.5-dll64` and `lib/libusb-1.0.21`. See [docs/development.md](docs/development.md#module-layout-srchackrf-sweep).

## Documentation

All first-class documentation lives under `docs/`:

- [docs/README.md](docs/README.md) — Documentation index
- [docs/getting-started.md](docs/getting-started.md)
- [docs/building.md](docs/building.md) — Build process & Makefile targets
- [docs/development.md](docs/development.md) — Dev workflow, testing, linting
- [docs/hackrf-setup.md](docs/hackrf-setup.md) — Hardware, udev, firmware, Zadig
- [docs/mcp.md](docs/mcp.md) — MCP for AI agents (tools, proxy, v1 limits)
- [docs/usage.md](docs/usage.md) — Running the analyzer, features, quick selects
- [docs/architecture.md](docs/architecture.md) — High-level design (core, native, UI, MCP)
- [docs/stats.md](docs/stats.md) — generated first-party stats (`make stats`)
- [docs/contributing.md](docs/contributing.md)
- [docs/plans/](docs/plans/README.md) — living implementation plans (status + checklists must stay current)

**Diagrams**: Use Mermaid fences in `docs/`. GitHub renders them with Mermaid 11. Prefer `flowchart`, `sequenceDiagram`, `classDiagram`, and `pie`. Do **not** use `deploymentDiagram` (removed in Mermaid 11). Quote sequence `Note` text if it contains `>`. After adding or changing a diagram, run `make mermaid`.

Root-level files:
- `README.md` — Project overview + quick links
- `AGENTS.md` — This file (for AI agents)
- `CONTRIBUTING.md` — Contribution guidelines
- `LICENSE`

The only project overview file is root `README.md`. Do **not** recreate `Readme.md` (Git/Linux treat that as a second file). First-class docs live under `docs/`.

## Development Workflow

1. Run `make help` to explore available commands.
2. Make changes in `src/hackrf-sweep/src/main/java/...` (or native under `src-c/` / lib/hackrf).
3. Add or update unit tests for any new logic (especially in `core/` package).
4. Run `make test` and ensure coverage doesn't regress significantly.
5. Run `make lint`. After doc or layout changes run `make mermaid` and `make stats`.
6. Update relevant docs under `docs/`. Never hand-edit `docs/stats.md`.
7. Use `make start` to manually verify with a real HackRF when possible.
8. Commit with clear messages. Reference issues when applicable.

### Adding Features

- Core DSP changes (SpurFilter, peaks, spectrum datasets, etc.) **must** have corresponding unit tests.
- Operator settings live in `AnalyzerSettings` (implements `HackRFSettings`). Do not add new `ModelValue` fields on the analyzer JFrame. Mark radio vs display via `isRadioSetting`. Auto gain is display policy; the LNA/VGA values it writes are radio settings.
- Plot overlays go through `FrequencyAxis` + `BandMark` + `BandHeaderPainter`. Do not invent a second MHz↔pixel map.
- Native interleaved hops export 5 MHz slices with holes. Pad USB start/stop with `FrequencyRange.forInterleavedNativeSweep()` (±10 MHz) so the requested window (e.g. FM 88–108) is actually filled. Dataset/axis stay on the operator range.
- Live agent access is `jspectrumanalyzer.mcp` (`SpectrumSnapshotStore` from `onFullSweepProcessed`, `SpectrumMcpServer` JSON-RPC). Snapshot/diagnostic tools are read-only (`spectrum_summary`, `spectrum_snapshot`, `radio_identity`, `sweep_config`, `fm_stations`, `spectrum_occupancy`, `spectrum_history`, `tv_debug`, `tv_debug_history`); occupancy is `SpectrumOccupancy` on filled bins, not a new USB path. Do not restart USB from a snapshot tool. `sweep_config` reports `radioMode` (`sweep`/`listen`/`watch`/`stopped`). Start with `--mcp` / `make mcp` (localhost 8765). Stdio shim: `scripts/mcp-spectrum-proxy.py`.
- FM listen (`WfmDemodulator`, `src-c/hackrf_fm.c`) is exclusive with the sweep: one HackRF, parked IQ RX, Java mono WFM. Do not time-slice. Demod belongs in `core/` with synthetic IQ tests. `AudioSink` fakes the mixer in unit tests.
- ATSC Watch parks at 16 MS/s with an 8 MHz analog filter. Keep the PFB RRC, long DC blocker, MMSE timing, field-trained equalizer, SIMD helpers, and double-precision AGC reference-compatible; 20 MS/s or the old 40+32 dB gain seed loses real-time continuity or clips. `tv_debug` should reach `stage=picture` on a usable RF channel (verified on RF 28 and 33).
- Auto-gain (`AutoGainPolicy`) must not pump: one Wi-Fi packet is not clip; a disappeared burst is not compression; settle after each apply; do not `clearHistory()` on a gain-only restart (`DatasetSpectrum.sameAxisAs`).
- UI changes should be accompanied by updates to `docs/usage.md`.
- New Makefile targets must be added to both the root `Makefile` and the detailed `src/hackrf-sweep/Makefile`, with proper `##` descriptions for `make help`.
- When touching native code, ensure the patch in `src-c/` and build process still work.

### Code Style

- Java: Follow existing conventions (no major formatter enforced yet, but keep consistent with surrounding code).
- Makefiles: Use the established colorized help pattern with `##@ Category` sections and `## description` on targets.
- Documentation: Use clear Markdown, keep examples copy-pasteable. Prefer linking to `docs/` from root files.

## Working with AI Agents

- Always begin by running `make help` (both at root and in `src/hackrf-sweep/`) to understand current targets.
- Prefer editing files under `docs/` for documentation rather than root-level Readme files.
- When asked to add tests, prioritize the `core/` package and use existing patterns (synthetic data, reflection for time/graphics state where needed).
- After structural changes (new targets, new docs, major refactors), update this `AGENTS.md` and `docs/development.md`.
- Save implementation plans under `docs/plans/<name>.md` and list them in `docs/plans/README.md`. Keep the Status block and checkboxes in sync with what is actually done; do not leave stale items.
- Do not assume a full Java environment is available in all contexts — many verification steps require the user to run `mvn` / `make` locally.
- For coverage work, after adding tests run the JaCoCo report and report specific class/line improvements.

## Upstream

GitHub's "ahead/behind" count vs `pavsa/hackrf-spectrum-analyzer` is misleading: the histories share no commit SHAs (rewritten old commits). Do not rebase onto `upstream/master`. Port individual upstream bugfixes onto this tree. See [docs/development.md](docs/development.md#syncing-with-upstream).

## Known Limitations / Gotchas

- Full end-to-end testing requires a real HackRF One + proper udev permissions.
- The native build is Linux-only for cross-compilation (mingw).
- Some UI components are still difficult to unit test (Swing-heavy). Focus unit tests on `core/` logic (`AutoGainPolicy`, `SpectrumPowerScale`, MCP store/tools without sockets).
- `HackrfSweepLibrary` is hand-maintained (`make jnabridge` does not run JNAerator). The UI requires a **headful** JDK 21+.
- Long runs on WSL/X11 can SIGSEGV in `libawt` while JFreeChart paints the spectrum line (`ChartPanel` / `FillAAPgram`). That is native AWT, not the sweep engine.
- AGC still restarts USB when it actually changes LNA/VGA; only the waterfall mapping (same MHz/FFT) is preserved across that restart.
- Listen and Watch stop the sweep and exclusively park the same HackRF. Watch uses 16 MS/s / 8 MHz and needs host `ffmpeg` for MPEG-2/AC-3; WSL audio needs Pulse/PipeWire forwarded to Windows or playback is silent.

## Questions?

Open an issue or refer to the documentation under `docs/`.

Thank you for helping keep this tool high-quality for HackRF users!