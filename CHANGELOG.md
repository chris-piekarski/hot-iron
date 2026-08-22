# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **TV watch** (ATSC 1.0): US ch **2–36** overlay and sidebar **Watch**. Parks the HackRF at 16 MS/s with an 8 MHz analog bandwidth, demodulates 8VSB (vendored GNU Radio `gr-dtv`) to MPEG-TS, and shows MPEG-2 video (host `ffmpeg`) in the 16:9 TV-tuner preview while the waterfall remains VIDEO · ±8 MHz IQ. AC-3 uses the same Pulse/Java Sound sink as Listen. HUD is **ATSC lock** + SNR or **no ATSC lock**. MCP `sweep_config` reports `radioMode=watch` and `tvChannel`. Needs `ffmpeg` on `PATH`. HackRF is 8-bit — weak indoor VHF often will not lock; UHF 14–36 is the usual bet.
- Sidebar and status bar **MCP** status: bind (`127.0.0.1:8765`), connected clients (from `initialize.clientInfo`), last tool, or **off** / **failed**.
- **Listen**: park the HackRF on a US FM dial and play mono audio (click a **97.3** header tag or the Listen control). An analog-style **knob** jumps between detected stations (right = higher MHz). The tuned station is a gold cursor on the spectrum. The waterfall panel shows a live **audio** spectrum (0–16 kHz) of the demodulated FM. Stops the RF sweep; **Restart** or Listen again resumes it. MCP `sweep_config` reports `radioMode` and `listenMHz`.
- MCP write tools `fm_listen` (`mhz` 88.1–107.9) and `tv_watch` (`channel` 2–36) park the same exclusive RF path as the UI. `sweep_config` also reports `tvLocked` / `tvSnrDb` / `tvPackets`.
- MCP `tv_debug` and `tv_debug_history` expose the live ATSC stage, IQ/AGC levels, segment/field sync, RS error ratio, equalizer taps, PAT/frame state, and queue continuity.
- MCP `spectrum_occupancy` (emitters above noise+8 dB, width, optional Wi-Fi `ch N` label) and `spectrum_history` (ring of summaries, new series on MHz/FFT change). `spectrum_summary` now includes `occupiedFraction` and `emitterCount`.

### Changed
- Watch keeps the **same waterfall strip and split** as Listen (**VIDEO · ±8 MHz** IQ at 16 MS/s). Decoded ATSC video is a 16:9 preview **under Watching ch N** in the TV tuner, not on the waterfall.
- Window title is **Spectrum Analyzer** (board name stays in the sidebar).
- Sweep range is one readout (type `88-108`, pan, zoom) instead of two Frequency start/end digit wheels. Quick Select and plot drag/scroll still set the same window.
- Docs and GitHub about/topics present the app as an **MCP interface for AI agents** on a live HackRF sweep (same JVM as the GUI). New [docs/mcp.md](docs/mcp.md).
- `src/hackrf-sweep` layout: Maven-standard Java tree (`src/main/{java,resources}`, `src/test/java`). Drop Eclipse CDT files, duplicate CSVs, unused Ant/JNAerator/32-bit/Zadig binaries. POM is indented, plugins version-pinned, `groupId` is `io.github.chris-piekarski`.

### Fixed
- Watch 8VSB front-end matches GNU Radio `atsc_rx`: a 16-phase RRC filters **before** decimation, then FPLL → long-form DC blocker → AGC ref 4 → 8-tap MMSE timing → sync…RS. The old linear decimator aliased discarded wideband samples, its 4-point cubic timing shim was not GNU Radio's MMSE interpolator, and decision-directed equalizer updates diverged from upstream field-only LMS. A recursive NCO, SIMD equalizer, and 16 MS/s / 8 MHz capture keep this chain near real time while preserving 1 MHz guard around the ATSC brick. Double-precision AGC avoids stalling at gain 1024; the UHF RF seed now targets ~0.25 ADC RMS (40 dB LNA + 22 dB VGA) without clipping.
- Watch could starve `ffmpeg` without ever painting a decoded frame: the equalizer started with all-zero taps (muted trellis), field-sync flushed the convolutional deinterleaver, and a 64-packet output buffer stopped native processing partway through every ~106-packet HackRF transfer. Center-tap init + continuous deinterleaving + a full-transfer TS buffer + start decode after a CRC-valid MPEG PAT. Queue overflow now drops stale backlog and recreates the full decoder instead of carrying invalid trellis state across an IQ gap. Watch also retries conjugated IQ when the first RF polarity cannot reach field sync, and FFmpeg teardown no longer deadlocks behind a full stdin pipe. Live MPEG-2 picture was verified on RF channels 28 and 33.
- Listen no longer slides the spectrum’s right-hand dB color bar: audio waterfall uses its own dBFS window; the RF palette stays on the plot axis.
- Auto-gain was restarting the radio in a 32↔40 dB loop on Wi-Fi (quiet gap raised, a packet or a dropped burst reversed it). Each restart wiped the waterfall. Gain now only drops on real clip or a sustained hot streak; a disappeared burst is not treated as compression. The waterfall history is kept across gain-only retunes.

## [2.0.0] - 2026-08-19

### Added
- **Auto gain** (default on): live AGC picks LNA then VGA per Quick Select so the plot is not all-blue or all-red. Seeds by band (FM higher, 2.4 GHz lower), aims the peak near −28 dBm, drops immediately on clip, and holds Wi‑Fi bursts so quiet gaps do not pump. Uncheck **Auto** on the Gain row to take the sliders.
- Waterfall left-side time scale (`now`, `2s`, `5s`, …) aligned to the spectrum Y-axis gutter. Ages come from per-row timestamps so Pause does not drift the labels. Hover shows the row’s age next to the MHz readout.
- Opt-in MCP server (`make mcp` / `--mcp`) so local agents can read `spectrum_summary`, `spectrum_snapshot`, `radio_identity`, `sweep_config`, and `fm_stations` from the same JVM that holds the radio. Snapshots omit hop holes and are sampled at ≤10 Hz. Stdio proxy: `scripts/mcp-spectrum-proxy.py`.
- README status badges: Java 21, HackRF SDK v2026.01.3, min firmware, Linux|Windows, last commit.
- `FrequencyAxis`, `BandMark` layers, and a shared `BandHeaderPainter` so Wi-Fi / FM / Quick Select overlays share one MHz↔pixel map and header. `AnalyzerSettings` owns all `HackRFSettings` model values (radio vs display) so the analyzer frame no longer stores them.
- Spectrum plot Grafana-style frequency zoom: drag a span to zoom in (retunes the sweep), double-click or scroll down to zoom out, scroll up to zoom around the cursor. Start/end digits follow. Quick Select resets the zoom stack. Zoomed out past a single preset, Quick Select ranges are drawn as labeled vertical bands.
- FM overlay labels **live** sweep peaks as US station frequencies (e.g. **97.3**): local maxima ≥ 8 dB above the noise floor, snapped to the 47 CFR 73.201 200 kHz dial. A tracker raises confidence over ~0.4 s of repeated hits and holds the label ~1–2 s after the peak drops so IDs are readable. Empty channels are not marked.
- Hardware strip: **Restart** (re-open the sweep), **Stop** (release USB), radio serial picker, and **CLKOUT 10 MHz**. Pause still only freezes the plot.
- Spectrum plot draws occupied 20 MHz Wi-Fi bands (US ch 1–11 and 36–177). Quick Select **WiFi 2** is 2402–2472 (ch 1 start through ch 11 end; 2407 is ch 2’s start) and **WiFi 5** is 5170–5895. 2.4 GHz bands overlap; the axis is locked to the occupied envelope so channel 11 stays 20 MHz wide.
- `make stats` / `scripts/repo-stats.py` — regenerate [docs/stats.md](docs/stats.md) (first-party LOC, Java packages, tests, git, pins). `make mermaid` / `scripts/check-mermaid.sh` parse-checks every first-party Mermaid fence.
- `make info` / `make list-devices` — list attached HackRF USB devices, the libhackrf/USB API this app is pinned to, device firmware when openable, and whether a newer Great Scott Gadgets release exists
- `make firmware-update` — dry-run official GSG firmware flash; writes SPI only with `CONFIRM=1` (refuses Pro image on a One; not part of `build`/`test`)
- `make udev` — install persistent udev rules so WSL usbipd HackRF nodes stay writable after attach
- Comprehensive unit test suite (30 test classes) focused on core DSP logic
- `SpectrumSweepEngine` — analyzer sweep/processing path without Swing; hardware IT asserts the queue fills and `datasetSpectrum` updates
- First-class `docs/` documentation structure
- Root `README.md`, `AGENTS.md`, `CONTRIBUTING.md`
- Improved top-level `Makefile` with `make help`, `make test`, `make lint`, `make start`, categorized colored output
- Enhanced `src/hackrf-sweep/Makefile` with matching help and quality targets

### Fixed
- Waterfall stayed on a fixed −90…−25 palette after the spectrum Y-axis started auto-scaling, so a typical FM band (−85…−65 dBm) rendered as solid blue with no station streaks. Auto-scale now drives the waterfall colors from the same live window; turning auto-scale off restores the Chart-options sliders.

### Changed
- Auto-scale pad around the live noise/peak is **10 dB** (was 20) so a 15 dB FM contrast actually fills the plot.
- Java **8 → 21** (`--release 21`). FlatLaf 3.7.2 dark look-and-feel. JFreeChart 1.5.6, MigLayout 11.4.3, JNA 5.19.1, JUnit 5.13.4, JaCoCo 0.8.15. Launchers refuse older or headless JREs. `HackrfSweepLibrary` is hand-maintained (`make jnabridge` no longer runs JNAerator).
- Host libhackrf / SDK pin **v2024.02.1 → v2026.01.3** (USB API 1.16). Sweep-as-library patch rebased (`num_fft_bins`, `stdbool.h`). JNA ABI unchanged. `isKnownHackrfBoard` accepts HackRF Pro (board id 5). Min firmware remains 2024.02.1.
- Modernized build (Maven + cross-platform native)
- Brought in upstream improvements (Antenna LNA support, firmware v2024.02.1, min FFT bin fix, etc.)
- Preserved and integrated Quick Select feature from this fork

### Changed
- Operator-facing copy no longer brands the app as `hackrf_sweep`. README, getting-started, and usage talk about the spectrum analyzer; the window title is **Spectrum Analyzer**. The sidebar shows board, short serial, and firmware instead of “HackRF connected”.
- Quick Select ranges checked against FCC / ITU / 3GPP / ARRL Part 97. Wi-Fi 2 is the occupied US ch 1–11 envelope, 2402–2472 MHz; Wi-Fi 5 is the occupied US ch 36–177 envelope, 5170–5895 MHz. LTE-1/2 cover AWS+PCS and 600–900 MHz, U-TV is post-repack 470–608. Added US amateur **6m / 2m / 70cm / 33cm**. Hover a button for the citation.
- Moved RBW / FFT bins / fps / peak power off the waterfall HUD into a full-width status bar with readable labels (Resolution, FFT bins, Waterfall rate, Peak).

### Fixed
- Frequency zoom / Quick Select keep the last sweep on screen and debounce the radio apply (~120 ms) so a wheel flick is one USB restart, not one per tick. Chart series skip −150 dB hop holes and downsample to the plot width. Domain-axis updates run on the EDT.
- Spectrum **Auto-scale dB axis** is on by default (10 dB pad, edges locked to multiples of 10) so FM/Wi-Fi peaks are readable. Chart options still offers a fixed **−100…+20** window. Hop holes at −150 dB are ignored. Live follow holds through wobble and bursty peaks, expands only when a signal would clip, and shrinks at most one 10 dB tick every 3 s if that whole window stayed quiet.
- Narrow sweep windows (FM 88–108 is one 20 MHz hop) finished 400+ sweeps/s and flooded the waterfall plus Swing updates, so the plot looked frozen. Display work is capped at 30 fps; the radio still sweeps at full rate.
- Quick Select hover is an in-panel range line (`2402–2472 MHz`), not Swing/X11 tooltip windows. Moving to another button replaces the same line; unit tests dispatch enter/exit and assert a single hint.
- Wi-Fi 2 vertical bands were the 5 MHz numbering raster in a 2407–2467 window, so the left edge was channel 2’s occupied start (2417−10) and channel 1’s 20 MHz (2402–2422) was clipped. Overlay now draws occupied 20 MHz (ch 1 = 2402–2422, ch 11 = 2452–2472) and **WiFi 2** is 2402–2472.
- JVM SIGSEGV in `libawt.so` `BufImg_GetRasInfo` after long runs: persistent-display `setRGB` raced ChartPanel paint on an accelerated image. Draw on a heap buffer and publish a snapshot to the chart.
- Window would not shrink and the settings column was clipped at the bottom (pack/preferred height + no scroll). Frame is resizable, settings sit in a scroll pane, and the content pane has bottom padding.
- Quick Select applied start and end as two model updates, each restarting the native sweep (USB reset). The second start then retried in a tight loop and the spectrum froze. Presets now publish one range; {@code runSweepLoop} no longer auto-restarts {@code start()}.
- Finish remaining pavsa/hackrf-spectrum-analyzer v2024.11.10 ports: JFreeChart 1.5 renderer API (`setDefault*` instead of removed `setBase*`) and Settings UI null-safety for no-arg/designer construction
- `make test` is a real gate: Java 8-compatible tests, JaCoCo agent no longer dropped by Surefire, headless `GraphicsToolkit` fallback, null-safe allocation-table range queries, and Settings version `JLabel` (AWT `Label` threw in headless)
- Extracted `GainPolicy` and `RuntimePerformanceWatch` from the analyzer for unit testing; waterfall palette/x mapping is now static and tested
- Settings UI, Quick Select bands, and frequency-selector digit buttons covered without constructing the native analyzer
- Gated hardware integration tests (`make test-hw`, `@Tag("hardware")`, `*IT`): USB present, firmware/USB API/board via libhackrf, live sweep into the analyzer dataset path, start/stop/restart, antenna power + LNA, restart after FFT bin / frequency change. Skipped when no HackRF; not run by `make test`

See the [docs/](docs/) directory for current usage and development information.
