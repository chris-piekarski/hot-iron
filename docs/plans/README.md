# Plans

Living implementation plans live here so later work (and agents) can pick them up without relying on chat history.

## Convention

- One Markdown file per plan: `docs/plans/<short-name>.md`.
- Keep a **Status** block at the top (`in progress` / `done` / `cancelled`) and a **checklist** that matches reality. Update both in the same change that implements or abandons a step. Do not leave checkboxes stale.
- When a plan finishes, leave the file in place and set Status to `done` with the date and measured outcome. Do not delete history.
- Link new plans from this index.

## Active

| Plan | Notes |
|---|---|
| [NFC park + nfc-laboratory](nfc-laboratory.md) | In progress 2026-08-23. `RadioMode.NFC`, parked 11.56 / 10 MS/s, nfc-lab frames, MCP `nfc_sniff` / `nfc_frames`. Hardware IT still open. |
| [ATSC 1.0 TV watch](atsc-tv-watch.md) | In progress 2026-08-21. Live RF 28/33 MPEG-2 preview verified; hardware IT still open. |

## Done

| Plan | Outcome |
|---|---|
| [MCP spectrum history bins](spectrum-history-bins.md) | Done 2026-08-23. `spectrum_history_bins` exports the snapshot ring as filled-bin frames (not the waterfall). |
| [NFC spectrum overlays](nfc-spectrum.md) | Done 2026-08-23. 12–15 MHz Quick Select, `NfcActivityTracker`, overlay/HUD, Scan harmonics, MCP `nfc_activity`. |
| [Radio apply coordinator](radio-apply.md) | Done 2026-08-23. `RadioCoordinator` + `SweepLiveLoop` + `RadioSession`; USB apply, live sinks, and exclusive start/stop are tested in `core`. |
| [Unit test coverage](unit-test-coverage.md) | Done 2026-08-17. `make test` 104/104. Project **56.1%** lines, `core` **90.2%**. |
| [Hardware integration tests](hardware-integration-tests.md) | Done; updated 2026-08-22. Gated `make test-hw` (8 ITs, including sweep queue/dataset and FM Listen→sweep resume). `make test` stays radio-free. |
| [Java 21 + FlatLaf UI](java-21-ui.md) | Done 2026-08-18. Java 21 floor, FlatDarkLaf, library bumps. `make test` 119/119, `make test-hw` 7/7. |
| [FM radio tuner](fm-radio-tuner.md) | Done 2026-08-22. Live mono WFM Listen plus parked-IQ→sweep resume hardware IT verified. |
