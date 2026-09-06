# CB 11 m overlay + AM listen

**Status:** in progress
**Started:** 2026-08-30

## Goal

Quick Select **CB** (26–28 MHz, 47 CFR 95 Subpart D / 11 m). Overlay the 40 AM channels. **Scan** parks AM and hops until chatter. **Listen** parks 4 MS/s IQ at 27.185 MHz and envelope-demodulates the selected channel (AM, not WFM). Channel changes mix in Java — no USB retune.

## Checklist

- [x] `CbChannelPlan` 40 FCC channels + occupancy detect
- [x] Overlay / tracker / tools face (wrapping ch 1–40)
- [x] `AmDemodulator` + `CbListenEngine` synthetic tests
- [x] `ListenService.CB`, `RadioMode.CB`, Scan + Listen wiring
- [x] MCP `cb_channels` / `cb_listen`
- [x] Docs: operator, agents, AGENTS, CHANGELOG
- [x] Scan hops parked AM and stops on chatter (`CbChatter` / `CbChatterScan`)

## Non-goals (v1)

SSB (USB/LSB), FM CB, freeband 27.405+, transmit.
