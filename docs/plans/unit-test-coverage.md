# Plan: Make the test harness trustworthy, then raise project coverage

| Field | Value |
|---|---|
| **Status** | Done |
| **Started** | 2026-08-17 |
| **Last updated** | 2026-08-17 |
| **Commits** | `7112bb4` (Phase 0 harness), `02a2f4e` (Phases 2–3 extracts + UI tests) |
| **Outcome** | `make test` **104/104**. Project line coverage **56.1%** (1241 / 2214). `core` **90.2%**. `ui` **74.7%**. |

This file is the record of *what was added and why*, not just a scoreboard. Hardware/JNA smoke is a separate plan: [hardware-integration-tests.md](hardware-integration-tests.md).

---

## Why this existed

Before 2026-08-17, `make test` did not run:

- Four test classes did not compile (Java 8 `var`, APIs that never existed on `ModelValue` / `MVCController` / `FrequencyAllocationTable`).
- Surefire’s `<argLine>-Djava.awt.headless=true</argLine>` **replaced** JaCoCo’s agent, so `make test` never wrote a coverage report.
- Tests that *did* compile died in headless CI: `GraphicsToolkit.getDefaultScreenDevice()`, AWT `Label`, `getFrequencyBands(null)`.
- Several “tests” never exercised the code they named (Quick Select buttons, `addNewData` used MHz instead of Hz).

The 2026 modernization had **92 `@Test` methods on disk** but CI/dev could not use them as a gate. The goal was: make the suite green without a radio, then raise project coverage to ~50% before larger refactors.

**Non-goals (still true):**

- Do not construct `HackRFSweepSpectrumAnalyzer` (static JNA load + maximized `JFrame` + native sweep).
- Do not invent production APIs to match broken tests.
- Do not chase 80% of the whole app. The remaining miss is the God-class analyzer + native glue.

---

## How to run / measure

```bash
make test
# report: src/hackrf-sweep/target/site/jacoco/index.html
```

Rules the suite still follows:

- Java 21 source/target; existing Java 8-style syntax remains acceptable.
- JUnit 5, no Mockito. Synthetic `DatasetSpectrum` / `FFTBins`.
- Same-package access to `protected` peak fields; reflection only for private clocks on `PersistentDisplay`.
- Swing tests flush the EDT (`SwingUtilities.invokeAndWait`) because `MVCController.updateView` is `invokeLater`.

---

## Phase 0 — Make `make test` a real gate

**Commit:** `7112bb4`  
**Result:** 92 tests compile and pass; JaCoCo works; project ~45.5%, `core` ~86.9% (existing tests finally executed).

### Build

| Change | File | Why |
|---|---|---|
| Surefire `argLine` is `@{argLine} -Djava.awt.headless=true` | `src/hackrf-sweep/pom.xml` | `@{argLine}` keeps JaCoCo’s `-javaagent`. Hard-coded `argLine` had dropped it. |

### Production fixes that unblocked tests

| Change | File | Why |
|---|---|---|
| Headless `BufferedImage` fallback; reject 0×0 by using 1×1 | `ui/GraphicsToolkit.java` | `getDefaultScreenDevice()` throws `HeadlessException`. Unblocked `PersistentDisplay` ctor, allocation-table draw, `GraphicsToolkitTest`. |
| `getFrequencyBands`: if `lookupBand` is null, iterate the full set | `core/FrequencyAllocationTable.java` | `TreeSet.floor` is null when `startHz` is below every band; `tailSet(null)` NPE. Real overlap bug, not just a test issue. |
| Version widget `java.awt.Label` → `JLabel` | `ui/HackRFSweepSettingsUI.java` | AWT `Label` throws in headless; Swing does not. |

### Tests repaired (not new classes)

| File | What was wrong | What it asserts now |
|---|---|---|
| `FrequencyAllocationsTest` | `var`; `getBandCount()` does not exist | Europe + USA tables load; `getFrequencyBands(0, MAX)` has >10 bands; `lookupBand(100e6)` non-null |
| `DatasetSpectrumTest` | `var`; `addNewData` freqs were MHz | Native path uses **Hz**. `{2400e6, 2400.1e6, 2499.9e6}` writes bins; float freq compare uses 200 Hz delta (`float` cannot represent 2.4e9 exactly) |
| `MVCControllerTest` | `new MVCController<>()` illegal (class is not generic); no EDT flush | `new MVCController(`; `flushEdt()` before asserts; spinner uses `SpinnerListModel` like production (default number model rejects `"8192"`) |
| `ModelValueTest` | Invented `getName()`, `Listener`, `removeListener` | `toString()` is the name; `Consumer`/`Runnable` listeners; equals no-op; bounded `IllegalStateException` |
| `DatasetSpectrumPeakTest.testPeakFallExceedsThresholdUpdatesHold` | `dt≈100ms` / fallout 1000ms only moves EMA ~3.6 dB vs 5 dB threshold | `lastAdded = now-1000` so `k` is large enough that hold falls. Algorithm unchanged. |
| `GraphicsToolkitTest.testInvalidSize` | Expected 0-width image | 0×0 becomes 1×1; assert ≥1 |

---

## Phase 1 — Core leftovers (folded into later commit)

Existing `PersistentDisplayTest`, `FrequencyAllocationTableTest`, and `DatasetSpectrumTest` started *passing* after Phase 0. One extra production API test was added in `02a2f4e`:

| Test | File | Asserts |
|---|---|---|
| `createPeaksDatasetAndFillSeriesUseHold` | `core/DatasetSpectrumPeakTest.java` | `setPeakFalloutMillis(250)`; `createPeaksDataset` item count + Y[0] is hold; `fillPeaksToXYSeries` writes the same hold value |

---

## Phase 2 — Settings / MVC / Quick Select

**Commit:** `02a2f4e` (with Phase 3)

### `FakeHackRFSettings`

`src/test/java/jspectrumanalyzer/FakeHackRFSettings.java`

In-memory `HackRFSettings` using real `ModelValue*` (same defaults as the fork: peaks on, persistent display on, FFT 100 kHz, 2400–2500 MHz). Stores listeners and can `fireHardwareStatusChanged` / `fireCaptureStateChanged`. Lets Settings UI tests run **without** constructing the analyzer or loading JNA.

### Settings UI

Package-private accessors on `HackRFSweepSettingsUI` (same package as the test): `pauseButton()`, `connectedLabel()`, `fftBinSpinner()`, `showPeaksCheckbox()`, `peakFallSpinner()`, `persistentDisplayCheckbox()`, `decayRateCombo()`.

| Test | Asserts |
|---|---|
| `noArgConstructorDoesNotThrow` | Designer ctor `this(null)` skips `bindViewToModel`; no headless crash |
| `bindsFftBinPausePeaksPersistenceAndHardwareStatus` | Spinner shows `"100 000"`; pause label **Pause** → click → model paused + **Resume**; peaks off hides fall spinner; persistence off hides decay combo; identity + sweeping → board / SN / FW (not `"HackRF connected"`) |

### Quick Select + range binder

`FrequencySelectorRangeBinder` switch table is the product behavior. Tests click real `JButton`s via `getComponents()`.

| Test | Asserts |
|---|---|
| `quickSelectButtonsSetKnownRanges` | Every `QuickSelectPreset` button: WiFi 2 → 2402–2472, WiFi 5 → 5170–5895, LTE-1 → 1695–2200, LTE-2 → 617–960, FM → 88–108, NFC → 13–14, HF → 3–30, VHF → 30–300, UHF → 300–3000, V-TV → 54–216, U-TV → 470–608, 6m → 50–54, 2m → 144–148, 70cm → 420–450, 33cm → 902–928; `quick.getValue()` matches the label |
| `clickingSamePresetAgainRestoresRange` | Second WiFi 2 click restores 2402–2472 after the digits were edited |
| `startEndVetoKeepsOrderByNudgingTheOtherSelector` | start 2000→3500 nudges end 3000→4500; end 4500→2500 nudges start 3500→1500 |
| `startAtMaxCannotCrossEnd` | start cannot jump to 7250 when end is already 7250 (end cannot grow; veto) |

`QuickFrequencySelectorPanelTest.testValueChangeFiresPropertyAndVetoable` now `doClick()`s **NFC** and checks `getValue()` + property change (was a no-op comment).

### Frequency selector digits

`FrequencySelectorPanel` layout is 4 `+`, 4 fields, 4 `-` (thousands … units).

| Test | Asserts |
|---|---|
| `testAddSubtractDigits` | +units 1234→1235; +tens →1245; −thousands →245 |
| `digitButtonsClampAtMinAndMax` | + at max 200 stays 200; − at min 100 stays 100 |

### MVC combo

`MVCControllerTest.testJComboBoxBinding`: model `"a"` syncs the box; selecting `"b"` updates the model; `model.setValue("c")` updates the box after EDT flush.

---

## Phase 3 — Extract what the God class hid

The analyzer ctor is still untested. These slices were moved so they *can* be tested.

### `GainPolicy`

`src/main/java/jspectrumanalyzer/core/GainPolicy.java`  
`HackRFSweepSpectrumAnalyzer.recalculateGains` now calls it.

| Input total | LNA (step 8, cap 40) | VGA (only after LNA=40, step 2 via `& ~1`) |
|---|---|---|
| 0 | 0 | 0 |
| 32 | 32 | 0 |
| 40 | 40 | 0 |
| 41 | 40 | 0 |
| 42 | 40 | 2 |
| 100 | 40 | 60 |
| −8 | 0 | 0 |

Tests: `core/GainPolicyTest.java`.

### `RuntimePerformanceWatch`

Moved from private nested classes in the analyzer to `core/RuntimePerformanceWatch.java` (fields kept public: `persisentDisplay`, `waterfallUpdate`, … — same names the processing loop uses).

| Test | Asserts |
|---|---|
| `generateStatisticsIncludesEntryNamesAndResets` | Future `lastStatisticsRefreshed` still produces output (elapsed clamp to 1 ms); stats contain `Spectr.chart`, `Pers.disp`, `Total:`; `reset()` clears counts/nanos |
| `performanceEntryToStringIsName` | `toString()` is the label |

### Waterfall math (no `paintComponent`)

Static helpers on `WaterfallPlot` used by `addNewData` / mouse-frequency mapping:

- `normalizePower(power, start, size)` → 0 below start, 1 above start+size, linear in between; size≤0 → 0
- `clampPixelX(x, length)` → [0, length−1]
- `translateXToFrequency(x, chartWidth, startHz, stopHz)` → Hz, clamped; width≤0 → −1

Tests: `ui/WaterfallPlotMathTest.java`. The panel ctor (screen-sized images) is still not instantiated in unit tests.

---

## Full suite inventory (28 classes, 104 tests)

What each class is *for*. Starred items were added or substantially rewritten in this plan.

| Class | # | Pins |
|---|---|---|
| `EMATest` | 4 | Static / instance / time-dependent EMA |
| `FFTBinsTest` | 1 | Value object holds arrays |
| `PowerCalibrationTest` | 2 | Offset + `correctPower` |
| `FrequencyBandTest` | 2 | Getters + `compareTo` by start Hz |
| `FrequencyRangeTest` | 2 | Getters + `equals` |
| `SpurFilterTest` | 10 | Calibrate N iterations; spur vs noise; reflection on debug/filter |
| `DatasetSpectrumTest` * | 10 | Ctor, **Hz** ingest, clone/copy, XY export |
| `DatasetSpectrumPeakTest` * | 10 | Hold vs EMA threshold, reset, copy, **peaks dataset/fill** |
| `PersistentDisplayTest` | 9 | `map`; ctor; draw/decay via reflection on calibration clock (needs headless images) |
| `FrequencyAllocationsTest` * | 1 | Builtin Europe/USA CSV load |
| `FrequencyAllocationTableTest` | 6 | Lookup, range (incl. below-first-band), draw |
| `GainPolicyTest` * | 2 | LNA/VGA split table |
| `RuntimePerformanceWatchTest` * | 2 | Stats string + reset |
| `XYSeriesImmutableTest` | 3 | Access, length check, `getDataItem` null-by-design |
| `XYSeriesCollectionImmutableTest` | 1 | Delegate |
| `XYLineAndShapeRendererApiTest` * | 1 | JFreeChart 1.5 `setDefault*` (not removed `setBase*`) |
| `ModelValueTest` * | 4 | Real API: toString, listeners, bounds |
| `MVCControllerTest` * | 6 | Generic ctor, view→model, checkbox, slider, list-spinner, **combo** |
| `HotIronBluePaletteTest` | 4 | Color math |
| `GraphicsToolkitTest` * | 3 | Opaque/translucent/degenerate size under headless |
| `FrequencySelectorPanelTest` * | 5 | set/get, veto, **digit +/−**, clamp |
| `QuickFrequencySelectorPanelTest` * | 3 | Initial value, **NFC click** fires events, binder wiring |
| `FrequencySelectorRangeBinderTest` * | 4 | Initial range, **all quick bands**, veto nudge, max veto |
| `HackRFSweepSettingsUITest` * | 2 | No-arg ctor, **bind FFT/pause/peaks/persist/hw status** |
| `WaterfallPlotMathTest` * | 3 | normalize / clampX / x→Hz |
| `GifSequenceWriterTest` | 1 | In-memory GIF header |
| `VersionTest` | 1 | version string + github URL |
| `HackRFSweepSpectrumAnalyzerTest` | 2 | `SPECTRUM_PALETTE_SIZE_MIN`; empty main-args placeholder (does **not** construct the app) |

Helper (not a `*Test`): `FakeHackRFSettings`.

---

## Production files touched for testability

| File | Role |
|---|---|
| `pom.xml` | JaCoCo + headless `argLine` |
| `GraphicsToolkit.java` | Headless images |
| `FrequencyAllocationTable.java` | Null-safe range query |
| `HackRFSweepSettingsUI.java` | `JLabel`; package-private widget accessors |
| `HackRFSweepSpectrumAnalyzer.java` | Delegates gain + perf watch |
| `GainPolicy.java` | **New** |
| `RuntimePerformanceWatch.java` | **New** (was private nested) |
| `WaterfallPlot.java` | Static math extracted from `addNewData` / mouse map |

---

## What this does *not* cover

| Area | Why it is still out |
|---|---|
| `HackRFSweepSpectrumAnalyzer` ctor / `sweep` / `setupChart` | JNA + JFrame + native start |
| `HackRFSweepNativeBridge` / `HackrfSweepLibrary` | Needs `.so` + device; see [hardware-integration-tests.md](hardware-integration-tests.md) |
| `processingThread` orchestration | Extracted as `SpectrumSweepEngine` (headless + analyzer hooks). Remaining UI-only bits stay in the analyzer. |
| `WaterfallPlot.paintComponent` / live raster | Pixel-flaky; math is tested |
| `ScreenCapture.captureFrame` | Calls `System.exit(0)` when the GIF window ends |
| Absolute RF levels | Environment-dependent; use synthetic `FFTBins` |

---

## Checklist (historical — all done)

- [x] Phase 0: compile, JaCoCo, headless, allocation NPE, Label→JLabel
- [x] Phase 1: existing core tests actually run; peak dataset/fill
- [x] Phase 2: FakeHackRFSettings, Settings bind, Quick Select table, digit buttons, combo binder
- [x] Phase 3: GainPolicy, RuntimePerformanceWatch, waterfall math
- [x] Docs: `docs/development.md`, `AGENTS.md`, this file

**Follow-on (not this plan):** gated `make test-hw` — [hardware-integration-tests.md](hardware-integration-tests.md).
