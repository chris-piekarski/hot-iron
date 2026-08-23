# FM radio tuner (listen via HackRF)

**Status:** done 2026-08-22 (live FM Listen and listen→sweep hardware IT verified)
**Started:** 2026-08-20

## Goal

The operator can **hear broadcast FM** through the attached HackRF One: pick a live **97.3** tag, the radio leaves sweep mode, demodulates mono WFM, and plays audio on the host.

## Hard constraint

One RF path. **Listening stops the sweep.** Pause still only freezes the plot. Stop still releases USB.

## Checklist

- [x] `docs/plans/fm-radio-tuner.md` listed as active
- [x] `WfmDemodulator` + unit tests (synthetic IQ, offset LO, 1 kHz tone)
- [x] `AudioSink` + Java Sound (tests use a fake sink)
- [x] `src-c/hackrf_fm.c` + JNA + Makefile `SOURCES`
- [x] `RadioMode` on `AnalyzerSettings` + launcher sequencing
- [x] Hardware strip Listen + frequency + volume; header-tag click; HUD
- [x] MCP `sweep_config` `radioMode` / `listenMHz` + `fm_listen` write tool
- [x] Docs: usage, architecture, mcp, hackrf-setup, CHANGELOG, AGENTS
- [x] `make test` green
- [x] Hardware IT listen start/stop then resume sweep
- [x] Live listen on FM Quick Select with speakers (relaunch the GUI)
- [x] Analog knob jumps detected stations; spectrum highlights the tuned station
- [x] **Scan** surveys 88–108 MHz and pins the Seek / knob list
- [x] Main chart + MCP `fm_spectrum` show live local ±2 MHz RF from the parked IQ stream

## Non-goals (v1)

Stereo, RDS, NBFM/AM/SSB, simultaneous spectrum+audio, GNU Radio, WAV.

## DSP

4 MS/s int8 IQ → mix +100 kHz offset → decimate ×10 → polar discriminator → 75 µs de-emphasis → 48 kHz PCM.
