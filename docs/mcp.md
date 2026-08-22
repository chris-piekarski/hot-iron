# MCP — AI agents on the live sweep

This is a **HackRF spectrum analyzer with a Model Context Protocol (MCP) interface**. You watch the plot. A local agent (Grok, Claude, Cursor, or anything that speaks MCP) queries the **same bins**, on the **same JVM**, without opening a second radio.

```mermaid
flowchart LR
    Radio[HackRF on USB] --> Native[Sweep library]
    Native --> JVM[Analyzer JVM]
    JVM --> UI[Spectrum + waterfall]
    JVM --> MCP[MCP tools]
    UI --> You[Operator]
    MCP --> Agent[AI agent]
```

Start it with `make mcp`. Point the agent at `scripts/mcp-spectrum-proxy.py`. Ask what is on the air.

## What an agent can answer

| Question | Tool |
|---|---|
| Where is the peak, and how loud is the noise? | `spectrum_summary` |
| Give me the filled bins (optional `maxPoints`, `minDbm`) | `spectrum_snapshot` |
| What radio is attached (board, serial, firmware, USB API)? | `radio_identity` |
| What is the radio vs display config (range, FFT, gain, `radioMode`, `tvChannel`, `tvLocked`)? | `sweep_config` |
| Park Watch on a US ATSC channel (2–36) | `tv_watch` |
| Where is ATSC Watch failing (IQ, segment/field sync, RS, PAT, FFmpeg)? | `tv_debug` |
| How have those ATSC stages changed over recent samples? | `tv_debug_history` |
| Park Listen on a US FM dial (88.1–107.9) | `fm_listen` |
| Which US FM dial hits are live right now? | `fm_stations` |
| What is occupying the window (width, fraction, Wi-Fi ch)? | `spectrum_occupancy` |
| How did peak/noise/occupancy move over the last few seconds? | `spectrum_history` |

Hop holes are **omitted**, not reported as −150 dBm. Snapshots are sampled at most **10 Hz**. `spectrum_history` returns **summaries** from the ring (~20 s), not full bin arrays, and starts a new series when the MHz/FFT window changes. Occupancy is deterministic (noise+8 dB, merge adjacent bins); Wi-Fi views may label `ch 6`. `tv_watch` and `fm_listen` park the same exclusive RF path the operator uses (US TV ch 2–36, or FM 88.1–107.9). Gain and sweep range stay operator-only. `sweep_config` includes `radioMode`, `listenMHz`, `tvChannel`, `tvLocked`, `tvSnrDb`, and `tvPackets`. `tv_debug` labels the current failing stage (`no_segment_sync`, `no_field_sync`, `rs_unusable`, `no_pat`, `ffmpeg_waiting`, or `picture`) and exposes queue, AGC, equalizer, and packet-error measurements. `tv_debug_history` returns up to 200 recent diagnostic samples. While listening or watching, RF snapshots and `fm_stations` are stale (no full sweeps).

## Start

The GUI owns USB. MCP is opt-in on that process:

```bash
make mcp                 # GUI + listen on 127.0.0.1:8765
# or
./src/hackrf-sweep/build/hackrf-spectrum-analyzer/hackrf_sweep_spectrum_analyzer_linux.sh --mcp
# optional: --mcp-port=8765   --mcp-stdio
```

Client config (stdio proxy; the analyzer must already be listening):

```json
{
  "mcpServers": {
    "spectrum-analyzer": {
      "command": "python3",
      "args": ["scripts/mcp-spectrum-proxy.py"],
      "env": { "SPECTRUM_MCP_PORT": "8765" }
    }
  }
}
```

Run the proxy from the repo root (or pass an absolute path to the script).

## Why this shape

- **One radio.** A second `hackrf_sweep` would fight the GUI for USB.
- **Same data as the plot.** The store is filled from `onFullSweepProcessed`, not from screenshots or log scraping.
- **Safe for agents.** JSON-RPC on localhost / stdio. No LAN listen in v1. No USB restart from a snapshot tool.
- **Operator-first defaults.** Auto gain, auto-scale dB, and a waterfall time axis so humans and agents see a usable band.

The operator sidebar (and the status-bar **MCP** field) shows bind address, connected clients (`initialize.clientInfo.name`), and the last tool call. **MCP off** means this process was started without `--mcp`.

Implementation: `jspectrumanalyzer.mcp` (`SpectrumSnapshotStore`, `SpectrumMcpServer`, `SpectrumMcpTools`) plus `McpStatus` for the UI. Design notes: [architecture.md](architecture.md). Operator UI: [usage.md](usage.md).
