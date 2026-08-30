# Operator layout: banner + fixed tools column

**Status:** done 2026-08-28, width 520 px 2026-08-29. Banner Quick Select + sweep range, Spectrum tools column, in-view band slot, MCP log, ~55/45 plot split.

## Goal

Make the window operator-friendly: plots a bit smaller, tools wider, no flicker when the sweep range changes.

## Checklist

- [x] `OperatorNavBanner`: grouped Quick Select + compact sweep range, frame NORTH
- [x] Tools column fixed 520 px (`OperatorLayout.TOOLS_WIDTH`); do not pack on range change
- [x] FM tuner digital readout + Simpson SIG needle (live audio while Listening) + 1970s slide-rule; MCP log under the slot
- [x] One band face at a time (Quick Select / parked); slot fills the column and scrolls above the MCP log
- [x] `BandContext` in `core`: in-view policy + parked/scan pin + hysteresis
- [x] Auto gain / LNA / VGA always visible under the slot
- [x] Spectrum/waterfall split ~0.55
- [x] Unit tests (`BandContextTest`, settings UI parent checks, Quick Select groups)
- [x] `docs/operator.md` / architecture / CHANGELOG
- [x] Split chrome so a feature change does not retouch the others: `BandToolKind` policy, `BandToolsSlot` registry, `OperatorLayout` sizes, `OperatorShell` frame, `RadioSessionStrip` / `SpectrumGainRail` / `ChartToggleBar` / `OperatorNavBanner` as siblings
- [x] Gain + chart toggles on the field of play; HackRF Settings / Chart options tabs removed; Hardware ⋯ overflow; line width frozen at 1.5 px
- [x] Survey banner: larger QS chips on `SpectrumSurveyAxis` (HackRF 1–7250 MHz log) over a chirp wave (`SpectrumWavePainter` / `SurveyChipLayout`)
- [x] Sweep-range digits above `◀ − + ▶` in the column immediately right of the wave; gold window grows/shrinks with − / +
- [x] Listen: top RF line chart, bottom RF ±2 MHz waterfall | AUDIO 0–16 kHz (same parked IQ; no second USB)
- [x] Watch: same dual-strip approach, RF ±8 MHz waterfall | AUDIO 0–16 kHz (MPEG-2 stays in the 16:9 tuner)

## How to add a band tool

1. Add a `BandToolKind` constant with `inView` / `hold` / `pinned`.
2. Build the Swing face (like `TunerPanel`).
3. Register `new BandTool(kind, panel)` on the `BandToolsSlot` in `HackRFSweepSettingsUI`.
4. Bind that panel’s buttons to `AnalyzerSettings` the same way FM/TV already bind.

Do not edit slot layout, frame sizes, or other kinds’ visibility rules.
