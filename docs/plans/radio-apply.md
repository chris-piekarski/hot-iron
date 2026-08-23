# Radio apply coordinator

Make feature and UI changes cheaper: USB apply is a tested `core` type, not a flag table on the JFrame. MCP stays a peer of the operator UI (same settings, same park path).

## Status

**Done** 2026-08-23. Characterization tests pin USB apply and live-sweep fan-out. `HotIron` calls `RadioCoordinator` and `SweepLiveLoop`; detect / scan / publish / paint are separate sinks.

## Why

`HotIron.setupParameterObservers` was 31 listeners and the Auto/USB flags. `HotIron` had 0% unit coverage. Agent edits that added a `ModelValue` listener caused extra USB restarts or Auto flipping off. File length pointed at `TvWatchEngine`; coupling pointed here.

## Rules (pinned by `RadioCoordinatorTest`)

- Operator FFT/samples → Auto-sweep off + apply now
- `applyAutoSweep` / `applyAutoGain` → Auto stays on; inner writes do not double-apply
- Frequency while sweeping → Auto may rewrite bins; USB is **debounced**
- Frequency while Listen/Watch → no USB
- `listenKHz` retunes only when FM is parked; `tvChannel` only when Watch is parked
- Operator gain/LNA → Auto-gain off + apply; Auto-gain write does not clear Auto
- Released radio → no apply
- Auto-gain seed/consider skip parked and scan (`SweepFramePolicy`)
- Chart paint ≤30 fps is independent of MCP publish (`SweepFramePolicy.shouldPaint`)
- Gain-only restart keeps waterfall history (`axisChanged` uses `sameAxisAs`)

## Checklist

- [x] `RadioCoordinator` + `Usb` port (`applyNow` / `applyDebounced`)
- [x] Characterization tests (`RadioCoordinatorTest`, `SweepFramePolicyTest`)
- [x] `HotIron` binds display listeners only; radio listeners on `radio.bind()`
- [x] MCP still uses `startListen` / `startWatch` + dial/channel ModelValues
- [x] Docs: architecture, develop, AGENTS, CHANGELOG
- [x] `SweepLiveLoop` + `StationDetectSink` + `BandScanSink`; paint stays on `HotIron`
- [x] Characterization tests (`SweepLiveLoopTest`, `StationDetectSinkTest`, `BandScanSinkTest`)
- [x] `RadioSession` last-launch-wins queue, debounce, exclusive start/stop (`RadioSessionTest`)
- [x] `RadioMode.of(settings)` shared by MCP, session, coordinator; Auto writes use `Source.AUTO_POLICY` (`RadioModeTest`)
- [x] `RadioSession.startLauncher` / `stopLauncher` unit tests; Auto-gain checkbox re-enable; `make test-hw` 8/8

## Exclusive USB (`RadioSession`)

- `applyNow()` last-launch-wins; `applyDebounced()` is the 120 ms frequency timer.
- After stop, start sweep / Listen / Watch from `settings.radioMode()`. Sweep calls `prepareSweep()` (Auto FFT) first; Listen/Watch do not.
- `release()` cancels debounce and the queue, then `abort()` (bounded join). A queued apply after Stop must not start.
- Native start/stop stay on `RadioSession.Driver` in `HotIron`. Tests use a counting driver.

## How to add a live sweep feature

1. New detect/scan/publish logic → a `core` sink with a unit test (`StationDetectSink` / `BandScanSink` style).
2. Call it from `SweepLiveLoop.accept` **or** from `SweepLiveLoop.Hooks.onPaint` if it must stay 30 fps (scan and AGC are paint-gated today).
3. Do not add branches to `HotIron.SweepUiHooks.onFullSweepProcessed` (it only dispatches).
4. MCP still reads `SpectrumSnapshotStore`; do not open USB from a snapshot tool.

## How to add a setting

1. Add the `ModelValue` on `AnalyzerSettings` (radio vs display via `isRadioSetting`).
2. If it must retune USB, add a listener on `RadioCoordinator.bind()` and a test in `RadioCoordinatorTest`. Do not add `flag*` on `HotIron`.
3. If it is display-only, bind in `HackRFSweepSettingsUI` / a HotIron display listener.
4. MCP: read it from `SpectrumSnapshotStore` / `sweep_config`; writes that park use the existing listen/watch hooks.
