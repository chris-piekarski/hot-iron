# MCP spectrum history bins

## Status

**Done 2026-08-23.** `spectrum_history_bins` serializes the existing snapshot ring as filled-bin frames. Not the waterfall image.

## Intent

Agents could see poll / HiFER blinks on the RF waterfall, but MCP only had `spectrum_history` summaries. The ring already stored full snapshots. Export those frames (capped), not `WaterfallPlot` pixels.

## Checklist

- [x] `SpectrumSnapshot.downsampled` (same peak-pick as the snapshot tool)
- [x] `SpectrumSnapshotStore.historyBinsJson` (same-axis, seconds, `maxSamples` ≤50, `maxPoints`, `minDbm`)
- [x] MCP tool `spectrum_history_bins`
- [x] Store + tools unit tests
- [x] Docs: agents, architecture, operator, AGENTS, changelog
