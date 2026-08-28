# AGENTS.md

This repository is **HotIron**: a Java 21 HackRF desktop with Sweep / Listen / Watch / NFC Sniff, live FM/TV station lock, and an in-process MCP server on the same JVM as the GUI. One rig. Agents copy the RF bins. They never open USB.

## Project Overview

HotIron wraps `hackrf_sweep` as a native shared library (via JNA) for high-performance wideband sweeps, parks the same HackRF for FM Listen, ATSC Watch, and NFC Sniff, and exposes the same bins to Grok/Claude/Cursor over localhost MCP (`make mcp`) without a second USB open.

Key technologies:
- Java 21+ (Swing UI + FlatLaf + JFreeChart for plots)
- Native C/C++ (hackrf library v2026.01.3, sweep-as-library patch, parked IQ, ATSC GNU Radio/libfec DSP, nfc-laboratory decoder)
- Maven for Java build
- Custom Makefile for cross-platform native + Java packaging (Linux + Windows)
- Supports real-time spectrum, waterfall (left-side time scale), peak/persistent display, spur filter, frequency allocations, quick band selectors, Auto gain, Antenna LNA (+14 dB), and an opt-in MCP interface with read-only snapshot/diagnostic tools plus explicit FM Listen, TV Watch, NFC Sniff, and parallel nRF BLE sniff controls.

Derived from [pavsa/hackrf-spectrum-analyzer](https://github.com/pavsa/hackrf-spectrum-analyzer) (GPL-3). Port upstream bugfixes; do not rebase onto `upstream/master`.

**Primary use case**: An operator with a physical HackRF, plus an optional local MCP client that reads the live sweep and can park Listen/Watch/Sniff.

## Essential Commands

**Always start here:**

```bash
make help
```

This shows all available targets with descriptions and categories (colorized).

### Common Targets (from root)

- `make build` — Full build (natives + JAR + zip)
- `make test` — Native C/C++ self-tests (`sweep_power_average` + `atsc_selftest`) with gcov, then Maven + JaCoCo. Does **not** require a HackRF. Native report: `src/hotiron/obj/gcov/coverage.txt`.
- `make test-hw` — Hardware ITs (`*IT`, `@Tag("hardware")`): USB, firmware/USB API/board, `SpectrumSweepEngine` queue + dataset, start/stop/restart, LNA + antenna-power, FFT/freq restart, and parked FM IQ followed by sweep resume. Skips if no radio.
- `make info` — List attached HackRF devices, the SDK/USB API this app is pinned to, and whether a newer GSG firmware/libhackrf exists.
- `make firmware-update` — Dry-run official GSG SPI flash. Write only with `CONFIRM=1` (optional `VERSION=2026.01.3`). Not part of `build` / `test`.
- `make udev` — Install persistent udev rules (sudo once) so WSL usbipd nodes stay writable.
- `make lint` — Compile/lint checks
- `make stats` — Rewrite [docs/stats.md](docs/stats.md) (first-party LOC, packages, tests, git). Do not hand-edit that file.
- `make mermaid` — Parse-check every first-party Mermaid fence (`mmdc` when installed)
- `make start` — Build (if needed) + launch the Linux app
- `make mcp` — Same as `start`, plus MCP on `127.0.0.1:8765` (`--mcp`)
- `make clean` — Clean build artifacts

From inside `src/hotiron/` you can also run the detailed native build targets directly (`make help` there too).

### Testing & Coverage

```bash
make test
# Java-only shortcut (skips the native averaging self-test)
cd src/hotiron && mvn clean test

# Coverage report is written by `make test` (JaCoCo report bound to the test phase)
# Open src/hotiron/target/site/jacoco/index.html
```

Unit tests cover core DSP (SpurFilter, PersistentDisplay, DatasetSpectrum*, allocations, EMA, firmware parse, SpectrumSweepEngine, RadioIdentity, …) plus UI helpers. They run without hardware. **Do not bake a class count into this file** — run `make stats` and cite [docs/stats.md](docs/stats.md).

### Building Details

See [docs/building.md](docs/building.md) for full instructions, including required packages (Ubuntu recommended for cross-build).

Native build requires:
- Linux host with mingw-w64 for Windows cross-compilation
- hackrf submodule pinned to `HACKRF_SDK_PIN` (v2026.01.3) and patched automatically (`src-c/0001-hackrf_sweep-to-library-conversion-v2026.01.3.patch`)

`src/hotiron` is a **hybrid JNA module**: Maven Standard Directory Layout for Java (`src/main/java`, `src/main/resources`, `src/test/java`); Makefile + C/C++ under `src-c/` + `lib/hackrf` for natives. Do not commit Eclipse `.project` / `.classpath`, put CSVs under `src/main/java`, or vendor Java JARs into `lib/` (Maven owns Java deps). Windows cross-link trees stay under `lib/fftw-3.3.5-dll64` and `lib/libusb-1.0.21`. See [docs/develop.md](docs/develop.md#module-layout-srchotiron).

## Documentation

All first-class documentation lives under `docs/`:

- [docs/README.md](docs/README.md) — Documentation index
- [docs/getting-started.md](docs/getting-started.md)
- [docs/building.md](docs/building.md) — Build process & Makefile targets
- [docs/develop.md](docs/develop.md) — Dev workflow, testing, linting
- [docs/hardware.md](docs/hardware.md) — Hardware, udev, firmware, Zadig
- [docs/agents.md](docs/agents.md) — MCP for AI agents (tools, proxy, v1 limits)
- [docs/operator.md](docs/operator.md) — Running the analyzer, features, quick selects
- [docs/nfc.md](docs/nfc.md) — NFC / HF RFID at 13.56 MHz (PHY, sidebands, overlay / `nfc_activity`, parked Sniff / `nfc_frames`)
- [docs/nrf-sniffer.md](docs/nrf-sniffer.md) — bench J-Link / nRF DK (`1366:1015`), BLE overlay + UART host (15.4 / ANT decode not wired)
- [docs/architecture.md](docs/architecture.md) — Layers, exclusive USB, MVC/hooks, queues, policies, MCP snapshot store
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
2. Make changes in `src/hotiron/src/main/java/...` (or native under `src-c/` / lib/hackrf).
3. Add or update unit tests for any new logic (especially in `core/` package).
4. Run `make test` and ensure coverage doesn't regress significantly.
5. Run `make lint`. After doc or layout changes run `make mermaid` and `make stats`.
6. Update relevant docs under `docs/`. Never hand-edit `docs/stats.md`.
7. Use `make start` to manually verify with a real HackRF when possible.
8. Commit with clear messages. Reference issues when applicable.

### Adding Features

- Core DSP changes (SpurFilter, peaks, spectrum datasets, etc.) **must** have corresponding unit tests.
- Operator settings live in `AnalyzerSettings` (implements `HackRFSettings`). Do not add new `ModelValue` fields on the analyzer JFrame. Mark radio vs display via `isRadioSetting`. Auto gain is display policy; the LNA/VGA values it writes are radio settings. Auto FFT/samples (`AutoSweepPolicy`) is the same: display policy, default on; the bin/sample values it writes are radio settings. Radio `setValue` listeners that restart USB belong on `RadioCoordinator.bind()`, not on the JFrame. Exclusive USB start/stop/join belongs on `RadioSession` (last-launch-wins queue, debounce, mode at start); do not add a second launcher in `HotIron`. USB hotplug uses `RadioHotPlug`: `listSerials()` only while idle/absent; sysfs `usbEnumerated()` while a session holds USB. Auto-`restartSweep` only when not Stopped. Full-sweep detect / scan / MCP publish / paint belong on `SweepLiveLoop` sinks (`StationDetectSink`, `BandScanSink`), not a growing `SweepUiHooks.onFullSweepProcessed`. Auto writes go through `applyAutoSweep` / `applyAutoGain` so they do not look like spinner overrides. MCP parks with the same `startListen` / `startWatch` / `startSniff` as the sidebar. `auto_gain` writes the existing Auto gain checkbox; `sweep` is `restartSweep`. Hysteresis must not thrash USB on pan.
- Plot overlays go through `FrequencyAxis` + `BandMark` + `BandHeaderPainter`. Do not invent a second MHz↔pixel map. The banner survey strip is a different axis (`SpectrumSurveyAxis`, HackRF 1–7250 MHz log) — do not reuse it for the plot. Operator chrome: `OperatorShell` + `OperatorLayout` sizes; Quick Select + sweep range live in `OperatorNavBanner`. Band faces register on `BandToolsSlot` as `BandTool` (`BandToolKind` + view). Visibility policy is `BandToolKind` / `BandContext` — do not add an if/else in `HackRFSweepSettingsUI` for a new tuner. Do not `pack()` the frame or change column width when the sweep range changes.
- Native interleaved hops export 5 MHz slices with holes. Pad USB start/stop with `FrequencyRange.forInterleavedNativeSweep()` (±10 MHz) so the requested window (e.g. FM 88–108) is actually filled. Dataset/axis stay on the operator range.
- Live agent access is `hotiron.mcp` (`SpectrumSnapshotStore` from `onFullSweepProcessed` and parked-IQ publishers, `SpectrumMcpServer` JSON-RPC). Snapshot/diagnostic tools are read-only (`spectrum_summary`, `spectrum_snapshot`, `radio_identity`, `sweep_config`, `fm_stations`, `nfc_activity`, `nfc_frames`, `fm_spectrum`, `tv_spectrum`, `spectrum_occupancy`, `spectrum_history`, `spectrum_history_bins`, `tv_debug`, `tv_debug_history`, `ble_frames`, `ble_activity`); occupancy is `SpectrumOccupancy` on filled bins, not a new USB path. `spectrum_history_bins` is the snapshot ring as filled-bin frames (capped), not the waterfall image. `nfc_activity` is the live 13.56 sweep classifier from those bins (no extra USB). `nfc_sniff` parks like the sidebar; `nfc_frames` is the decoder ring. `ble_sniff` opens the nRF ACM (second USB) and sets the sweep to 2400–2484; it does not park the HackRF. `ble_frames` / `ble_activity` only read the ring. Do not restart USB from a snapshot tool. `sweep_config` reports `radioMode` (`sweep`/`listen`/`watch`/`nfc`/`stopped`). Start with `--mcp` / `make mcp` (localhost 8765). Stdio shim: `scripts/mcp-hotiron-proxy.py`. While Listen/Watch/Sniff is parked, `spectrum_history` / `spectrum_history_bins` are the local IQ window, not the last wideband sweep.
- FM listen (`WfmDemodulator`, `src-c/hackrf_fm.c`) is exclusive with the wideband sweep: one HackRF, parked 4 MS/s IQ RX, Java mono WFM. The main chart and `fm_spectrum` use an `IqSpectrum` FFT of that same IQ (local ±2 MHz RF) through `DatasetSpectrumPeak` (peak half-life + persistence + history). While Listening, the bottom strip is two waterfalls: that same parked RF FFT beside 0–16 kHz demodulated audio. Leave Listen to restore the single sweep RF waterfall. Do not time-slice. Demod belongs in `core` with synthetic IQ tests. `AudioSink` fakes the mixer in unit tests.
- ATSC Watch parks at 16 MS/s with an 8 MHz analog filter. The main chart, VIDEO waterfall, and `tv_spectrum` all derive from that same local ±8 MHz IQ. The chart uses `DatasetSpectrumPeak` so peak half-life, persistence, and `spectrum_history` stay live while parked. Keep the PFB RRC, long DC blocker, MMSE timing, field-trained equalizer, SIMD helpers, and double-precision AGC reference-compatible; 20 MS/s or the old 40+32 dB gain seed loses real-time continuity or clips. Auto Watch enables the RF amp for that parked session only and keeps trimming IF toward ~0.5 RMS; do not write the Antenna LNA checkbox (that restarts USB / leaves the amp on after Watch). `tv_debug` should reach `stage=picture` on a usable RF channel (verified on RF 28 and 33). Start host `ffmpeg` only after a healthy rolling RS window (`waiting_rs_health` until then); stop it when RS collapses. When frames stay at 0 after PAT, use `tv_debug.ffmpeg` (process, TS offered/written, PMT/video PID, last stderr) rather than treating all stalls as `ffmpeg_waiting`.
- Auto-gain (`AutoGainPolicy`) must not pump: one Wi-Fi packet is not clip; a disappeared burst is not compression; settle after each apply; do not `clearHistory()` on a gain-only restart (`DatasetSpectrum.sameAxisAs`).
- Auto FFT/samples (`AutoSweepPolicy`) biases high waterfall FPS: finest list bin with ≤~4000 dataset points, always 8192 samples, keep the current bin while length stays 1500–6000. Zoomed-in windows go fine; **All** stays ~2 MHz. Manual spinner changes turn Auto off (same as gain).
- NFC / HF RFID (13.56 PHY, sidebands at 12.71 / 14.41, HackRF LO offset) live in [docs/nfc.md](docs/nfc.md). Overlay/tracker is `NfcBandPlan` + `NfcActivityTracker` + `NfcBandLayer`. Parked sniff is `NfcSniffEngine` + nfc-laboratory (`nfc_dec_*`), LO **11.56e6**, rate **10e6**. Waterfall is the 12–15 MHz parked FFT; `NfcEnvelopeTrace` is the sidebar 13.56 |IQ| scope (IF mix, not wideband |z|). Do not invent a second NFC stack or a TX path; overlays still go through `FrequencyAxis` + `BandMark`. Header-click Scan stays Scan.
- Bench nRF / J-Link (`1366:1015`) is a **second USB** (BLE / 15.4 / ANT notes in [docs/nrf-sniffer.md](docs/nrf-sniffer.md)). Do not park the HackRF for BLE. Sidebar / MCP `ble_sniff` opens the ACM; snapshot tools (`ble_frames`, `ble_activity`) only read the ring. Overlays still go through `FrequencyAxis` + `BandMark` (`BleBandPlan` / `BleBandLayer`). Official packet sniff is Nordic UART/SLIP (v1 host commands, v2 frames on this nRF51 HEX), not a Java BLE stack. ANT has no official Nordic sniffer image.
- UI changes should be accompanied by updates to `docs/operator.md`.
- New Makefile targets must be added to both the root `Makefile` and the detailed `src/hotiron/Makefile`, with proper `##` descriptions for `make help`.
- When touching native code, ensure the patch in `src-c/` and build process still work.

### Code Style

- Java: Follow existing conventions (no major formatter enforced yet, but keep consistent with surrounding code).
- Makefiles: Use the established colorized help pattern with `##@ Category` sections and `## description` on targets.
- Documentation: Use clear Markdown, keep examples copy-pasteable. Prefer linking to `docs/` from root files.

## Working with AI Agents

- Always begin by running `make help` (both at root and in `src/hotiron/`) to understand current targets.
- Prefer editing files under `docs/` for documentation rather than root-level Readme files.
- When asked to add tests, prioritize the `core/` package and use existing patterns (synthetic data, reflection for time/graphics state where needed).
- After structural changes (new targets, new docs, major refactors), update this `AGENTS.md` and `docs/develop.md`.
- Save implementation plans under `docs/plans/<name>.md` and list them in `docs/plans/README.md`. Keep the Status block and checkboxes in sync with what is actually done; do not leave stale items.
- Do not assume a full Java environment is available in all contexts — many verification steps require the user to run `mvn` / `make` locally.
- For coverage work, after adding tests run the JaCoCo report and report specific class/line improvements.

## Upstream

GitHub's "ahead/behind" count vs `pavsa/hackrf-spectrum-analyzer` is misleading: the histories share no commit SHAs (rewritten old commits). Do not rebase onto `upstream/master`. Port individual upstream bugfixes onto this tree. See [docs/develop.md](docs/develop.md#syncing-with-upstream).

## Known Limitations / Gotchas

- Full end-to-end testing requires a real HackRF One + proper udev permissions.
- The native build is Linux-only for cross-compilation (mingw).
- Some UI components are still difficult to unit test (Swing-heavy). Focus unit tests on `core/` logic (`AutoGainPolicy`, `SpectrumPowerScale`, MCP store/tools without sockets).
- `HackrfSweepLibrary` is hand-maintained; JNAerator is not part of the build. The UI requires a **headful** JDK 21+.
- Long runs on WSL/X11 can SIGSEGV in `libawt` while JFreeChart paints the spectrum line (`ChartPanel` / `FillAAPgram`). That is native AWT, not the sweep engine.
- AGC still restarts USB when it actually changes LNA/VGA; only the waterfall mapping (same MHz/FFT) is preserved across that restart.
- Listen, Watch, and NFC Sniff stop the sweep and exclusively park the same HackRF. Watch uses 16 MS/s / 8 MHz and needs host `ffmpeg` for MPEG-2/AC-3; WSL audio needs Pulse/PipeWire forwarded to Windows or playback is silent. Tuner **Scan** leaves Listen/Watch, surveys FM (88–108) or TV (VHF then UHF), and pins those hits as the Seek list. NFC Scan (click a 13.56 header tick) dwells 12–15 then 27.12 / 40.68; it classifies the field, it does not pin a Seek list. NFC **Sniff** is the sidebar/MCP park (loop antenna; Type A ASK holes are not clip).

## Questions?

Open an issue or refer to the documentation under `docs/`.

Thank you for helping keep this tool high-quality for HackRF users!