# nRF 2.4 GHz spectrum + packet sniff

**Status:** in progress 2026-08-27 — PCA **PCA10031 / nRF51822**; `sniffer_pca10031_1c2a221.hex` flashed; host is Nordic SLIP + v1 TX / v2 RX with 1M then 460800 fallback
**Started:** 2026-08-23

Living plan so a later agent can implement without chat history. Study notes live in [`docs/nrf-sniffer.md`](../nrf-sniffer.md). NFC on the HackRF is a different radio ([nfc-laboratory.md](nfc-laboratory.md)).

## Goal

The operator (or MCP) can **see 2.4 GHz energy on the HackRF** with BLE / 15.4 / ANT ticks, and **decode packets from the bench nRF DK** for every protocol that device has an official (or later, researched) capture path.

v1 success (proposed):

1. Sweep overlay on **2400–2484 MHz** (or a Quick Select) labels BLE adv 37/38/39 and does not pretend WiFi 2 is the whole BLE band.
2. With official **BLE sniffer** firmware on the identified PCA board, HotIron (or a thin host helper) lists advertisers / named PDUs the way `nfc_frames` lists REQA — receive only.
3. HackRF sweep keeps running. The nRF is a **second USB**. Agents still never open it.

## Hard rules

- **Do not steal HackRF USB.** `RadioSession` / `RadioMode` stay sweep / listen / watch / nfc. nRF I/O is a separate session (`NrfSniffer` or similar), not a fifth `RadioMode` that parks the HackRF.
- Agents copy snapshots. They never open `/dev/ttyACM0` or J-Link.
- Overlays still go through `FrequencyAxis` + `BandMark`. Clone the `NfcBandLayer` / `WifiChannelPlan` pattern (`BleBandPlan`, …).
- Receive only. No advertise, inject, MITM, pairing, or key recovery. Nordic’s follow-connection + operator-supplied LTK is own-product debug only if we ever expose it.
- **Do not rewrite BLE or 802.15.4 in Java.** Consume Nordic’s UART/SLIP protocol (v3) or run their extcap/`nrfutil` as a child. Same rule as nfc-laboratory.
- One nRF firmware image at a time. Switching BLE ↔ 15.4 is an explicit operator/MCP write, not a snapshot tool.
- Identify **PCA / SoC** (`nrfutil device device-info` or the board sticker) before flashing. This bench J-Link is `1366:1015` serial `000680852409` — debugger only.

## Study findings (do not rediscover)

| Fact | Detail |
|---|---|
| USB | J-Link OB `1366:1015` / `000680852409` / WSL `ttyACM0`. Not PCA10059 dongle USB. |
| SoC | **This bench: PCA10031 / nRF51822** (`nrfutil device device-info` 2026-08-24). Not PCA10040/10056. nRF51 BLE only; no 15.4. |
| Official sniffers | BLE: all common nRF52 DKs. 802.15.4: 52840/5340 only. **ANT: none.** |
| BLE channels | 37@2402, 38@2426, 39@2480; data 0–36 @ 2404–2478. |
| 15.4 channels | 11–26 @ 2405–2480, step 5 MHz. |
| ANT+ HRM | 2457 MHz; ANT host freq \(N\) → 2400+N MHz. |
| WiFi 2 preset | 2402–2472 — misses BLE 39 and 15.4 ch 26. |
| Host API | Wireshark extcap + sniffer UART v3; firmware HEX per board. |

## Checklist

### Research (done)

- [x] Identify bench USB as J-Link OB, not the packet radio
- [x] Document BLE / 15.4 / ANT channel plans and official vs on-chip support
- [x] Record WSL attach (`usbipd` 6-2 / `1366:1015`) and ACM path
- [x] Write [`docs/nrf-sniffer.md`](../nrf-sniffer.md)

### Identify the PCA (before flash)

- [x] Read board sticker or `nrfutil device device-info` (WSL needs `nrfutil` / `nrfjprog` + writable usbfs)
- [x] Record PCA + SoC in `docs/nrf-sniffer.md` “What is plugged in” (**PCA10031 / nRF51822**)

### Spectrum (HackRF, no nRF USB)

- [x] `BleBandPlan` + `BleBandLayer` on `FrequencyAxis` (adv 37/38/39 + ANT+ 2457)
- [ ] 15.4 channel marks (11–26)
- [x] Quick Select **BLE** 2400–2484 so ch 39 is on-screen (Wi-Fi 2 stays 2402–2472)
- [x] Occupancy labels for adv ticks (`BLE 39`) and ANT+ — energy only
- [x] Tests in `core/` with synthetic bins
- [x] `docs/operator.md` + MCP `ble_activity` / `ble_frames` (spectrum + frame ring)

### Packet sniff BLE

- [x] Flash matching BLE sniffer HEX for the identified board (operator/docs, not `make build`). Recipe: [`docs/nrf-sniffer.md`](../nrf-sniffer.md#flash-recipe). This bench is nRF51 — `sniffer_pca10031_1c2a221.hex` (do not write 4.1.1 nRF52 HEX).
- [x] Host reader for Nordic UART/SLIP (`0xAB`/`0xBC`, v1 host / v2 device) (`NordicSlip` + `NordicSnifferProto` + `BleSniffEngine`); unit tests with synthetic frames (no radio); 1M then 460800
- [x] Sidebar `BleSniffPanel` + MCP `ble_sniff` / `ble_frames` / `ble_activity` (second USB; does not park the HackRF)
- [ ] Hardware IT later: ACM open → frame → close without touching HackRF

### Packet sniff 802.15.4 (only if PCA10056 / 52840)

- [ ] Separate firmware image; same “one image” rule
- [ ] Nordic 15.4 extcap/protocol, not a second Java MAC

### ANT (research, not v1 success)

- [ ] Confirm no official Nordic ANT sniffer still
- [ ] If we still want decode: find a lawful receive-only capture path (do not invent TX)

## Out of scope

Flashing from `make build`. Vendoring Wireshark. Proxmark-style BLE stack. ESB/Gazell decode. Using the DK NFC antenna. Parking the HackRF to “listen” to BLE (HackRF stays sweep for spectrum). AirTag find / UWB.

## When this lands

Update [`docs/nrf-sniffer.md`](../nrf-sniffer.md), [`docs/operator.md`](../operator.md), [`docs/agents.md`](../agents.md), [`docs/architecture.md`](../architecture.md) (second USB, agents still copy), `AGENTS.md`, and this Status block.
