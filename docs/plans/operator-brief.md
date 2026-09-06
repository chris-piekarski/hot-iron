# Operator brief: agent notes on the live UI

**Status:** done 2026-08-30
**Started:** 2026-08-30

## Goal

Agents read sniff rings over MCP, then post operator prose back onto the GUI: one **Agent brief** pane under the Spectrum tools face, plus a one-line plot HUD and optional MHz ticks. One store, every sniff/listen face.

## Checklist

- [x] `OperatorBrief` + `OperatorBriefLayer` (FrequencyAxis + BandMark)
- [x] `SpectrumSnapshotStore` per-face latest brief; no USB
- [x] MCP write `operator_brief`, read `operator_briefs`
- [x] `OperatorBriefPane` under band slot, above MCP log
- [x] Plot HUD + mark overlay
- [x] Unit tests (`core` + `mcp`)
- [x] Docs: agents, operator, CHANGELOG, this plan

## Non-goals (v1)

Chat history, auto-generated briefs in the JVM, body text on the waterfall, USB from a brief.
