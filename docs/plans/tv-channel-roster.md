# TV channel roster (occupancy vs picture)

**Status:** in progress
**Started:** 2026-08-29

## Goal

The TV tuner Seek list must distinguish **RF occupancy** from **MPEG-2 frames this HackRF can decode**. Scan remains a sweep first; a UHF qualify pass parks Watch on the strongest ATSC-like bricks.

## Checklist

- [x] `TvChannelGrade` + `TvStationHit` grade/stage/frames
- [x] Sweep brick scorer: occupied vs ATSC 1.0 pilot (`ATSC_LIKE`)
- [x] Scan keeps picture/no-lock memory; Seek prefers picture
- [x] `TvQualifySession` queues strongest UHF ATSC-like (max 6, 20 s)
- [x] TV tuner roster in spectrum tools (picture vs RF only)
- [x] Overlay style by grade
- [x] MCP `tv_stations`
- [x] `HotIron` qualify-after-scan + Watch stamp
- [ ] Live UHF Scan → roster picture on a known brick (ch 33)

## Notes

Exclusive USB: qualify uses the same `startWatch` as the Watch button. VHF is occupancy-only. ATSC 3.0 stays `OCCUPIED` / `NO_LOCK`.
