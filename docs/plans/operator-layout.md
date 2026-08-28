# Operator layout: banner + fixed tools column

**Status:** done 2026-08-28. Banner Quick Select + sweep range, 400 px tools column, in-view band slot, ~55/45 plot split.

## Goal

Make the window operator-friendly: plots a bit smaller, tools wider, no flicker when the sweep range changes.

## Checklist

- [x] `OperatorNavBanner`: grouped Quick Select + compact sweep range, frame NORTH
- [x] Tools column fixed 400 px (`HackRFSweepSettingsUI.TOOLS_WIDTH`); do not pack on range change
- [x] Locked-height band slot; FM/TV side-by-side when both apply (V-TV)
- [x] `BandContext` in `core`: in-view policy + parked/scan pin + hysteresis
- [x] Auto gain / LNA / VGA always visible under the slot
- [x] Spectrum/waterfall split ~0.55
- [x] Unit tests (`BandContextTest`, settings UI parent checks, Quick Select groups)
- [x] `docs/operator.md` / architecture / CHANGELOG
- [x] Split chrome so a feature change does not retouch the others: `BandToolKind` policy, `BandToolsSlot` registry, `OperatorLayout` sizes, `OperatorShell` frame, `RadioSessionStrip` / `GainStrip` / `OperatorNavBanner` as siblings
- [x] Survey banner: larger QS chips on `SpectrumSurveyAxis` (HackRF 1–7250 MHz log) over a chirp wave (`SpectrumWavePainter` / `SurveyChipLayout`)
- [x] Sweep-range digits above `◀ − + ▶` in the column immediately right of the wave; gold window grows/shrinks with − / +
- [x] Listen: top RF line chart, bottom RF ±2 MHz waterfall | AUDIO 0–16 kHz (same parked IQ; no second USB)

## How to add a band tool

1. Add a `BandToolKind` constant with `inView` / `hold` / `pinned`.
2. Build the Swing face (like `TunerPanel`).
3. Register `new BandTool(kind, panel)` on the `BandToolsSlot` in `HackRFSweepSettingsUI`.
4. Bind that panel’s buttons to `AnalyzerSettings` the same way FM/TV already bind.

Do not edit slot layout, frame sizes, or other kinds’ visibility rules.
