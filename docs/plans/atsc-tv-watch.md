# ATSC 1.0 TV watch (via HackRF)

**Status:** in progress (live RF channels 28 and 33 → MPEG-TS → ffmpeg video verified; hardware IT still open)
**Started:** 2026-08-21

## Goal

The operator can **tune and watch US ATSC 1.0** the same way they listen to FM: pick a live **ch 14** tag (or the TV tuner), the radio leaves sweep mode, demodulates 8VSB to an MPEG-2 transport stream, and **shows video + plays AC-3** in this JVM.

OTA TV in 2026 is digital. Analog NTSC is not this plan.

## Hard constraint

One RF path. **Watching stops the sweep.** Pause still only freezes the plot. Stop still releases USB. FM listen and TV watch cannot run together.

## Checklist

- [x] `docs/plans/atsc-tv-watch.md` listed as active
- [x] `TvChannelPlan` + occupancy detect + unit tests
- [x] Overlay tags + header click Watch
- [x] TV tuner (Tune / Seek / Watch)
- [x] `RadioMode.WATCH` + MCP `tvChannel`
- [x] Watch parks IQ at 16 MS/s (reuse `hackrf_fm_lib_*`; analog BB filter 8 MHz)
- [x] HUD lock / no-lock
- [x] Native 8VSB → MPEG-TS (`atsc_rx_*` in `libhackrf-sweep`)
- [x] FFmpeg MPEG-2 video in the 16:9 TV-tuner preview + AC-3 on `AudioSink`
- [x] Watch waterfall is a live IQ **VIDEO · ±8 MHz** raster (Listen-style), not a blank card
- [x] Watch uses the same waterfall strip as Listen (VIDEO · ±8 MHz); split unchanged
- [x] Docs: usage, architecture, mcp, CHANGELOG
- [x] 16:9 preview under the TV tuner Watch button (waterfall stays VIDEO · ±8 MHz)
- [x] GNU Radio-compatible 16-phase RRC resampler (filter before decimation)
- [x] GNU Radio-compatible 8-tap MMSE symbol interpolator + real-time recursive FPLL NCO
- [x] GNU Radio long-form DC blocker + field-sync-only equalizer adaptation
- [x] Preserve a complete HackRF transfer (about 106 TS packets) and retry IQ polarity before lock
- [x] Drop stale IQ backlog and reset the complete decoder after any discontinuity
- [x] Native stage diagnostics + MCP `tv_debug` / `tv_debug_history`
- [x] Main chart + MCP `tv_spectrum` show live local ±8 MHz RF from the parked IQ stream
- [ ] Hardware IT Watch start/stop then resume sweep
- [x] Live ATSC lock + CRC-valid PAT + decoded MPEG-2 frames on RF channels 28 and 33

## Non-goals (v1)

ATSC 3.0, analog NTSC/PAL, cable QAM, GNU Radio runtime, EPG, recording, simultaneous spectrum+video.

## DSP

16 MS/s int8 IQ → 16-phase polyphase RRC/resampler (GNU Radio `atsc_rx_filter` shape) → recursive FPLL → long-form DC blocker → AGC → 8-tap MMSE symbol timing → field-trained equalizer → 8VSB (GNU Radio `gr-dtv`, GPL-3, vendored C + libfec) → MPEG-TS → host FFmpeg MPEG-2 + AC-3. The 8 MHz analog filter leaves 1 MHz guard around the 6 MHz channel while reducing front-end work enough to preserve IQ continuity. Decoded frames go in the TV-tuner preview; the waterfall is the parked IQ strip. If no field reaches the packet decoder, Watch retries with conjugated IQ rather than assuming one RF mixing orientation. Any IQ queue overflow discards stale backlog and recreates the whole native decoder.
