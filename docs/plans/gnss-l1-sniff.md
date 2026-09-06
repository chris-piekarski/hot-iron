# GNSS L1 overlay + GPS C/A sniff

**Status:** in progress (hardware sniff not yet live-verified)
**Started:** 2026-08-30

## Goal

On Quick Select **GNSS** (1559–1610 MHz RNSS L1), show operator-friendly L1 ticks and occupancy. **Sniff** parks the HackRF on **GPS L1 1575.42 MHz** (4 MS/s) and lists visible C/A PRNs (C/N0, Doppler). Not a PVT fix.

## Checklist

- [x] `GnssBandPlan` + overlay ticks (GPS L1, BDS B1I, GLONASS L1)
- [x] Sweep occupancy `GnssActivity` / tracker / HUD / MCP `gnss_activity`
- [x] `GpsCaCode` + `GpsL1Acquirer` unit tests (synthetic IQ)
- [x] Parked sniff `GnssSniffEngine`, `ListenService.GNSS`, `RadioMode.GNSS`
- [x] Spectrum tools face + wrapping PRN list
- [x] Hold last-known PRN chips; miss does not blank HUD / `gnss_sv`
- [x] MCP `gnss_sniff` write, `gnss_sv` read
- [x] Docs: operator, agents, AGENTS, CHANGELOG

## Non-goals (v1)

Position/time, Galileo E1 BOC decode, GLONASS FDMA tracking, L2/L5, SBAS.
