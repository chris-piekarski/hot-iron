# CB 11 m AM

US Citizens Band (47 CFR 95 Subpart D): **40 AM channels**, **26.965–27.405 MHz**. This is personal radio on **11 m**, not amateur 10 m and not FM broadcast.

Quick Select **CB** is **26–28 MHz** so all 40 channels are on-screen. Occupied RF bricks still light up on the overlay while sweeping. **Scan** is the handheld-CB kind: it **parks AM**, hops 1–40 in software (~½ s each), and **stops on chatter** (carrier plus modulation — a dead key is skipped). Press Scan again to stop hopping and stay. **Listen** parks the same 4 MS/s IQ at **27.185 MHz**. Changing channel **mixes in software** — USB does not retune.

| Item | Value |
|---|---|
| Channels | 1–40 (ch 9 emergency, ch 19 highway) |
| Modulation | AM (v1; no SSB) |
| LO | 27.185 MHz |
| Sample rate | 4 MS/s |
| Scan | Park AM, hop channels, stop on voice |
| MCP | `cb_channels` (occupancy), `cb_listen` (`channel` 1–40) |

Header-click a channel tick to Listen. A 27 MHz whip or CB antenna helps; a Wi-Fi stick is usually quiet here.
