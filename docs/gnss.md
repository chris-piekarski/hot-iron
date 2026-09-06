# GNSS L1 overlay + GPS C/A sniff

Reference for the **GNSS** Quick Select, the live sweep occupancy HUD, and parked **GPS L1 C/A sniff**. GNSS here is the RNSS L1 family (**1559–1610 MHz**), not GPS-only. GPS L1 C/A is **1575.42 MHz**. Galileo E1 shares that center; the HUD therefore says **GPS/Galileo** when that brick is occupied. BDS B1I is **1561.098 MHz**. GLONASS L1 is an FDMA envelope around **1602 MHz**.

**Sniff** parks the same HackRF at **1575.42 MHz / 4 MS/s** (analog BW ≈ 2 MHz, enough for the C/A main lobe) and lists GPS C/A PRN peaks (C/N0, Doppler). The first peak is shown; a later miss does not blank the chips. That is **not a PVT fix**: no ephemeris, no time, no position.

Receive only. Overlays still go through `FrequencyAxis` + `BandMark`.

## Sweep occupancy (no extra USB)

`GnssBandPlan` / `GnssActivityTracker` / `GnssBandLayer` label GPS, BDS, and GLONASS ticks on a 1559–1610 (or GPS-only 1573–1578) view. A one-sweep flash is not labeled. Occupancy is a **brick above noise+8 dB** — spread-spectrum C/A is usually invisible on a wide FFT; a loud brick is more often a jammer, repeater, or indoor radiator. MCP `gnss_activity` copies that snapshot.

Header-click the **GPS** tick to park sniff. BDS / GLO ticks are labels only in v1.

## Parked GPS L1 sniff

Sidebar **Sniff** or MCP `gnss_sniff` stops the sweep and parks IQ like Listen/NFC. The chart and waterfall stay **1573–1578 MHz** from that parked FFT. The tools face lists wrapping PRN chips (C/N0, Doppler). A missed 1 ms search does **not** blank the list — chips stay until a later hit updates that PRN, or sniff stops. MCP `gnss_sv` is that last-known list.

| Item | Value |
|---|---|
| LO | 1575.42 MHz |
| Sample rate | 4 MS/s |
| Analog BW | ≈ 2 MHz (`sample_rate/2`) |
| Search | 1 ms coherent C/A, Doppler ±8 kHz / 500 Hz |
| PRNs | GPS 1–32 (IS-GPS-200) |
| SMA bias | 3.3 V on for this park (active patch LNA). Not the RF amp. |

A patch / active GPS antenna helps. A Wi-Fi whip at L1 is usually quiet on the FFT; correlator peaks can still appear. Indoor HackRF without a GPS antenna often lists weak or noisy PRNs — that is expected. A bump at exactly 1575.42 is the LO / analog filter, not a sky plot. Many PRNs with Doppler jumping kHz are correlator noise, not 32 satellites.

A **USB GPS puck** is usually a **receiver** (NMEA on a second USB), not an RF antenna. Plugging it into the PC does not feed the HackRF. If it has an **SMA pigtail**, that SMA goes on the HackRF.

v1 does **not** decode Galileo E1 BOC, GLONASS FDMA, L2/L5, SBAS, or a navigation solution.
