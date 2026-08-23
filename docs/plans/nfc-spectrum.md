# NFC spectrum overlays + live activity

**Status:** done 2026-08-23
**Started:** 2026-08-23
**Finished:** 2026-08-23

Sweep catalog + tracker + overlay + `nfc_activity` are in. Parked-IQ NFC demod is still a non-goal.

## Goal

When the operator (or an MCP client) is on the NFC window, HotIron labels the **13.56 MHz** picture the way it labels Wi-Fi channels and live FM/TV hits: catalog ticks, a one-line classification, and a read-only MCP tool. Scan dwells the PHY window then the **27.12 / 40.68 MHz** harmonics.

## What this is not

- Not an AirTag finder. AirTags / Tile / Find My beacon on **Bluetooth 2.4 GHz** (AirTag precision is UWB ~6–8 GHz).
- Not nfc-laboratory / Proxmark. No UID, APDU, or Morse-text decode.
- No parked-IQ NFC demod in this pass (sweep + history only).

A Morse-like blink at 13.56 on the waterfall is usually **HiFER / Part 15 CW** or a **reader poll**, not “NFC data.” Sidebands at 12.71 / 14.41 mean a card is load-modulating.

## Checklist

- [x] `NfcBandPlan` + `NfcActivity` / tracker + `NfcBandLayer` (unit tests, synthetic spectra)
- [x] Quick Select NFC **12–15 MHz** so Type A/B sidebands are on-screen
- [x] Overlay + HUD in `HotIron`; occupancy can label `13.56` / `NFC-A/B`
- [x] `BandScan.NFC` hops 12–15 → 26–28 → 40–42; click a header tick toggles Scan
- [x] MCP `nfc_activity` (kind, duty, keying, sidebands, tracking hint)
- [x] Docs: `nfc.md`, operator, agents, architecture, CHANGELOG
- [x] `make test` green; `make mermaid`; `make stats`

## Non-goals (v1)

Parked IQ at 11.56 / 10 MS/s, ISO 14443 frame names, Morse decoder, BLE AirTag overlay.
