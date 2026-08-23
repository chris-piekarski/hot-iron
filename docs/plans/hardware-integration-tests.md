# Plan: Hardware smoke / integration tests (gated)

| Field | Value |
|---|---|
| **Status** | Done |
| **Started** | 2026-08-17 |
| **Last updated** | 2026-08-22 |
| **Finished** | 2026-08-17 |
| **Measured** | Eight hardware ITs pass, including analyzer queue/dataset and parked FM IQ → sweep resume. |

## Goal

Pin **USB → libhackrf API/firmware → native `hackrf_sweep` → analyzer data path** (`FFTBins` / `DatasetSpectrumPeak`) **without** putting a radio requirement on `make test`.

This is application integration against a real HackRF, plus a basic board health check (firmware + USB API), not a Swing UI launch.

## How tests are marked (must stay gated)

| Mechanism | What it does |
|---|---|
| Class name `*IT` | Default Surefire includes `*Test` only; `*IT` is excluded |
| `@Tag("hardware")` on the class; each method is `@HardwareTest` | Surefire `excludedGroups=hardware` on the default `test` goal |
| `@EnabledIf("...HardwareConditions#hackrfUsbPresent")` | If someone forces the class, tests are **disabled** (skipped) when `1d50:6089` is not enumerated |
| `Assumptions.assumeTrue` on the sweep / API ITs | Skips (does not fail) if `libhackrf-sweep.so` is missing or the usbfs node is not writable |

`make test` must never fail for lack of a HackRF.

`make test-hw` uses Maven profile `hardware` (includes `*IT`, group `hardware`).

## What landed

| File | Role |
|---|---|
| `nativebridge/HackRFDeviceQuery.java` | JNA: `hackrf_init` / `open` / firmware / USB API / board on the same `.so` the app loads |
| `nativebridge/HackRFDeviceQueryTest.java` | Unit tests for firmware parse / compare (no radio) |
| `src/hotiron/src/test/java/hotiron/hw/HardwareConditions.java` | sysfs + `lsusb` detect; find `.so`; usbfs writable check |
| `src/hotiron/src/test/java/hotiron/hw/HardwareSweepSession.java` | start → callback(s) → stop; optional full-sweep + `SpectrumSink` |
| `core/SpectrumSweepEngine.java` | Real queue + processing loop + native sweep; analyzer UI is a hook |
| `src/hotiron/src/test/java/hotiron/hw/HackRFSweepHardwareIT.java` | 8 gated ITs including engine queue/dataset and parked FM IQ → sweep resume |
| `nativebridge/HackRFSweepNativeBridge.java` | `hackrf.sweep.lib.dir` so tests can point at the built `.so` |
| `pom.xml` | default exclude `*IT` + `hardware` tag; profile `hardware` |
| Root + `src/hotiron` Makefiles | `test-hw` target |
| `docs/hardware.md` | WSL `usbipd` + chmod note |

Contracts (when device + `.so` + writable node):

- Firmware string parses as `YYYY.M.P` and is **≥ 2024.02.1**; USB API ≥ 1.00; board is Jawbreaker / One / rad1o / One r9 / Pro
- `SpectrumSweepEngine` (same class the analyzer ctor starts): accepted queue packets ≥ 1, processed ≥ 1, `datasetSpectrum` bins updated
- Live sweep with **app settings** (`FakeHackRFSettings` + `GainPolicy`) fills `DatasetSpectrumPeak` and a chart series
- ≥1 callback with bins; `freqStart` in the requested band; finite power in [−140, 20] dBm
- `stop` then `start` still produces data
- antenna port power + Antenna LNA start/stop does not throw
- restart after FFT bin + frequency change still produces data in the new band
- parked FM Listen produces IQ, stops cleanly, then sweep resumes

Does **not** launch Swing / `HotIron` ctor. Does **not** assert live RF levels (no “LNA made it louder”).

## Checklist

- [x] Exclude hardware tests from default Surefire (`*IT` + `@Tag("hardware")`)
- [x] `make test-hw` target + skip-if-no-device
- [x] Firmware / USB API / board health via the app’s `.so`
- [x] App integration: settings → sweep → `FFTBins` → `DatasetSpectrumPeak`
- [x] Extract `SpectrumSweepEngine`; IT asserts processing queue fills and `datasetSpectrum` updates (no Swing ctor)
- [x] Smoke: start → callback → stop → start again
- [x] Assert Hz range, finite dBm
- [x] Document WSL `usbipd attach` in `docs/hardware.md` / `make help`
- [x] LNA / antenna-power start/stop does not crash
- [x] Restart after FFT bin / freq change
- [x] Parked FM Listen IQ then sweep resume

## How to run

```bash
# Windows: usbipd attach --wsl --busid <HackRF BUSID>
# If usbfs is not writable: sudo chmod a+rw /dev/bus/usb/00X/00Y
# Native lib: make build   (or at least the Linux .so)
make test      # unit tests only — no radio
make info      # USB + firmware if openable
make test-hw   # skips if no 1d50:6089
```
