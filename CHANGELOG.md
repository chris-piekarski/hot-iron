# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Listen and Watch parked-IQ spectrum uses the same peak half-life, persistence overlay, and `spectrum_history` ring as the wideband sweep. AUDIO/VIDEO waterfalls stay on the parked IQ strip.

## [2.0.2] - 2026-08-22

### Added
- Watch `tv_debug` / `tv_debug_history` and the 2 s `ATSC watch:` console line report FFmpeg process health (alive/exit, stdin write errors, stdout bytes / partial frame), TS bytes offered vs written, queue drops, last stderr, and PAT/PMT/video PID counts. `stage` now splits a stuck decode (`ffmpeg_missing`, `ffmpeg_blocked`, `no_pmt`, `no_video_pes`, `ffmpeg_waiting`, …) instead of a single `ffmpeg_waiting` bucket.
- **Auto FFT / samples** (default on): `AutoSweepPolicy` picks FFT Bin and Number of samples from the sweep span so zoomed-in windows stay detailed and wide scans stay fast. Hysteresis avoids USB thrash on pan. Uncheck Auto to override.

### Changed
- TV Scan dwells each window only after the first live sweep (USB retune is not counted), clears the waterfall on the VHF→UHF hop, and holds Auto-gain so a late USB restart does not wipe the UHF graph.
- Peak hold and persistent display use a real half-life (default 15 s / 30 s). The peak line falls toward live instead of sitting then snapping after 15 dB; `0` follows live; empty hop bins do not pull peaks down. Persistence ages from wall-clock so Auto FFT / zoom does not change the fade, and a pause does not wipe the glow. Quick Select, FM↔TV, and Listen/Watch transitions flush the old glow with a ~350 ms decay so it does not remap onto the new axis.
- Startup Quick Select is **WiFi 2** (highlighted) and the first sweep/chart opens on 2402–2472 MHz.
- Quick Select while **Listen** or **Watch** is parked stops FM/TV and resumes the sweep on that band.
- FM and TV tuners have **Scan**: survey the broadcast band (FM 88–108; TV VHF then UHF) and pin those hits as the list **Seek** jumps between.
- Button hover tips are painted on the existing frame (no extra X11 window, no sidebar reflow). Quick Select still uses the reserved range line under the grid.
- Architecture doc now records exclusive USB, MVC/hooks, producer–consumer queues, policy objects, and restart coalescing — not only the component map.
- Listen/Watch HUD and waterfall banner say **parked IQ** instead of “sweep paused”; the console prints `QRX parked IQ.` while the VFO is parked.
- TV Seek keeps occupied UHF/VHF hits across a Wi‑Fi sweep and merges live 6 MHz bricks from parked IQ, same as FM Seek.
- Watch UHF Auto-gain may raise VGA live when parked IQ is short of ~0.5 RMS; the 8VSB receiver keeps running so Reed-Solomon is not reset to the 16-packet flush. Auto Watch also enables the RF amp (+14 dB) for the parked session only and keeps trimming IF after fade, instead of a single 1.5 s pass.
- Removed superseded Java/UI compatibility paths, unreachable diagnostics, unused native ATSC receive files, legacy Make aliases, and old MCP environment names.
- Release archives now contain only the canonical `hotiron/` runnable tree and are named `hotiron.zip`.

### Fixed
- Watch UHF Auto-gain seeds LNA 40 + VGA 22, then trims IF live toward ~0.5 RMS without reopening USB. 0.25 RMS found field sync but Reed-Solomon never left the 16-packet flush; a USB restart to do the same trim threw the lock away; +32 dB clipped ch 33 (~0.69 RMS).
- Watch no longer starts `ffmpeg` on the first PAT while Reed-Solomon is still mostly failing, and it stops the player when the RS window collapses so a poisoned GOP is not held on screen. FFmpeg maps the PMT video/audio PIDs, uses a larger probe, and the console no longer prints every concealment line (that flood was filling the IQ queue and resetting the 8VSB receiver). Auto Watch holds IF while RS is usable unless the ADC is clipping; a stuck all-bad RS window recreates the native receiver. After a polarity has produced a healthy RS window, Watch will not flip I/Q on a later drop. Watch waits for a PMT video+audio pair before starting `ffmpeg`, maps those PIDs, and prefers main English AC-3 over a visually-impaired / SAP track that the mux lists first.
- Export the ATSC C API explicitly from Windows builds so JNA can resolve TV Watch symbols.

## [2.0.1] - 2026-08-22

### Changed
- **HotIron 2.0.1 identity.** Product name, Java packages `hotiron.*`, module `src/hotiron`, launchers `hotiron.sh` / `hotiron.cmd`, window title, MCP `serverInfo.name`, and docs IA (`operator.md`, `agents.md`, `hardware.md`, `develop.md`). License stays GPLv3; derived from pavsa’s analyzer. Repository links continue to use the live `chris-piekarski/hackrf-spectrum-analyzer` slug.
- ASCII **HotIron** wordmark on the README, `make help`, `make info`, and the Linux/Windows launchers. Not printed on the MCP stdio proxy.
- Branding copy and product console lines use ham lingo (QSY / QRV / QRT / copy) without changing MCP tool names.

### Added
- `make test` runs `atsc_selftest` and `sweep_power_average_selftest` with **gcov**; summary at `src/hotiron/obj/gcov/coverage.txt`. Release `.so` is not instrumented.
- Quick Select **All** scans the full 1–7250 MHz selectable range; use coarse FFT bins for practical update rates.
- **TV watch** (ATSC 1.0): US ch **2–36** overlay and sidebar **Watch**. Parks the HackRF at 16 MS/s with an 8 MHz analog bandwidth, demodulates 8VSB (vendored GNU Radio `gr-dtv`) to MPEG-TS, and shows MPEG-2 video (host `ffmpeg`) in the 16:9 TV-tuner preview. The same parked IQ drives the main ±8 MHz RF chart, VIDEO waterfall, and MCP `tv_spectrum`. AC-3 uses the same Pulse/Java Sound sink as Listen. HUD is **ATSC lock** + SNR or **no ATSC lock**. MCP `sweep_config` reports `radioMode=watch` and `tvChannel`. Needs `ffmpeg` on `PATH`. HackRF is 8-bit — weak indoor VHF often will not lock; UHF 14–36 is the usual bet.
- Sidebar and status bar **MCP** status: bind (`127.0.0.1:8765`), connected clients (from `initialize.clientInfo`), last tool, or **off** / **failed**.
- **Listen**: park the HackRF on a US FM dial and play mono audio (click a **97.3** header tag or the Listen control). An analog-style **knob** jumps between detected stations (right = higher MHz). The main chart shows the live ±2 MHz local RF spectrum from the same parked 4 MS/s IQ, while the waterfall remains the demodulated 0–16 kHz audio spectrum. MCP `fm_spectrum` publishes that dBFS RF view. Stops the wideband sweep; **Restart** or Listen again resumes it. MCP `sweep_config` reports `radioMode` and `listenMHz`.
- MCP write tools `fm_listen` (`mhz` 88.1–107.9) and `tv_watch` (`channel` 2–36) park the same exclusive RF path as the UI. `sweep_config` also reports `tvLocked` / `tvSnrDb` / `tvPackets`.
- MCP `tv_debug` and `tv_debug_history` expose the live ATSC stage, IQ/AGC levels, segment/field sync, RS error ratio, equalizer taps, PAT/frame state, and queue continuity.
- MCP `spectrum_occupancy` (emitters above noise+8 dB, width, optional Wi-Fi `ch N` label) and `spectrum_history` (ring of summaries, new series on MHz/FFT change). `spectrum_summary` now includes `occupiedFraction` and `emitterCount`.

### Changed
- Watch keeps the **same waterfall strip and split** as Listen (**VIDEO · ±8 MHz** IQ at 16 MS/s). Decoded ATSC video is a 16:9 preview **under Watching ch N** in the TV tuner, not on the waterfall.
- Window title is **HotIron** (board name stays in the sidebar).
- Sweep range is one readout (type `88-108`, pan, zoom) instead of two Frequency start/end digit wheels. Quick Select and plot drag/scroll still set the same window.
- Docs and GitHub about/topics present the app as an **MCP interface for AI agents** on a live HackRF sweep (same JVM as the GUI). See [docs/agents.md](docs/agents.md).
- `src/hotiron` layout: Maven-standard Java tree (`src/main/{java,resources}`, `src/test/java`). Drop Eclipse CDT files, duplicate CSVs, unused Ant/JNAerator/32-bit/Zadig binaries. POM is indented, plugins version-pinned, `groupId` is `io.github.chris-piekarski`.

### Fixed
- **Number of samples** now controls sweep dwell instead of being a no-op: 8192 uses one firmware block; larger choices request multiple blocks at each tuning frequency and average their FFTs in linear power before publishing. Sweep latency grows with the selected sample count while FFT-bin resolution remains unchanged.
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
