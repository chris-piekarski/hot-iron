package hotiron.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool schemas and handlers. No Swing, no USB.
 */
public final class SpectrumMcpTools
{
	public static final String PROTOCOL = "2024-11-05";
	public static final String SERVER_NAME = "hotiron";
	public static final String SERVER_VERSION = "2.0.2";

	@FunctionalInterface
	public interface TvWatchHook
	{
		void watch(int fccChannel);
	}

	@FunctionalInterface
	public interface FmListenHook
	{
		void listen(double mhz);
	}

	@FunctionalInterface
	public interface NfcSniffHook
	{
		void sniff();
	}

	@FunctionalInterface
	public interface AutoGainHook
	{
		void setEnabled(boolean enabled);
	}

	@FunctionalInterface
	public interface SweepHook
	{
		void sweep();
	}

	@FunctionalInterface
	public interface BleSniffHook
	{
		void setEnabled(boolean enabled);
	}

	@FunctionalInterface
	public interface GnssSniffHook
	{
		void sniff();
	}

	@FunctionalInterface
	public interface CbListenHook
	{
		void listen(int channel);
	}

	private final SpectrumSnapshotStore store;
	private final TvWatchHook tvWatch;
	private final FmListenHook fmListen;
	private final NfcSniffHook nfcSniff;
	private final AutoGainHook autoGain;
	private final SweepHook sweep;
	private final BleSniffHook bleSniff;
	private final GnssSniffHook gnssSniff;
	private final CbListenHook cbListen;

	public SpectrumMcpTools(SpectrumSnapshotStore store)
	{
		this(store, null, null, null, null, null, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch)
	{
		this(store, tvWatch, null, null, null, null, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen)
	{
		this(store, tvWatch, fmListen, null, null, null, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen,
			NfcSniffHook nfcSniff)
	{
		this(store, tvWatch, fmListen, nfcSniff, null, null, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen,
			NfcSniffHook nfcSniff, AutoGainHook autoGain, SweepHook sweep)
	{
		this(store, tvWatch, fmListen, nfcSniff, autoGain, sweep, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen,
			NfcSniffHook nfcSniff, AutoGainHook autoGain, SweepHook sweep, BleSniffHook bleSniff)
	{
		this(store, tvWatch, fmListen, nfcSniff, autoGain, sweep, bleSniff, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen,
			NfcSniffHook nfcSniff, AutoGainHook autoGain, SweepHook sweep, BleSniffHook bleSniff,
			GnssSniffHook gnssSniff)
	{
		this(store, tvWatch, fmListen, nfcSniff, autoGain, sweep, bleSniff, gnssSniff, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen,
			NfcSniffHook nfcSniff, AutoGainHook autoGain, SweepHook sweep, BleSniffHook bleSniff,
			GnssSniffHook gnssSniff, CbListenHook cbListen)
	{
		if (store == null)
			throw new IllegalArgumentException("store");
		this.store = store;
		this.tvWatch = tvWatch;
		this.fmListen = fmListen;
		this.nfcSniff = nfcSniff;
		this.autoGain = autoGain;
		this.sweep = sweep;
		this.bleSniff = bleSniff;
		this.gnssSniff = gnssSniff;
		this.cbListen = cbListen;
	}

	public SpectrumSnapshotStore store()
	{
		return store;
	}

	public String initializeResult()
	{
		return "{\"protocolVersion\":\"" + PROTOCOL + "\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\""
				+ SERVER_NAME + "\",\"version\":\"" + SERVER_VERSION + "\"}}";
	}

	public String toolsListResult()
	{
		return "{\"tools\":[" + tool("spectrum_snapshot",
				"Latest filled-bin sweep (hop holes omitted). Optional maxPoints and minDbm.",
				"{\"type\":\"object\",\"properties\":{\"maxPoints\":{\"type\":\"integer\",\"minimum\":1},"
						+ "\"minDbm\":{\"type\":\"number\"}}}")
				+ ","
				+ tool("spectrum_summary",
						"Noise floor, peak, span, pause/released, and sweep rate for the current window.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("radio_identity", "Attached radio board, short serial, firmware, and USB API.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("sweep_config",
						"Armed radio settings (range, FFT, gain, CLKOUT), radioMode (sweep|listen|watch|nfc|gnss|cb|stopped), listenMHz, tvChannel, plus display flags (autoSweep, autoGain, autoScale, peaks).",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("tv_debug",
						"Current ATSC Watch stage diagnostics: IQ, sync, RS, PAT, plus ffmpeg process/TS/stdout counters when frames stay at 0.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("tv_spectrum",
						"Live local RF spectrum from the same parked 16 MS/s IQ stream used by TV Watch (dBFS).",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("tv_debug_history",
						"Recent ATSC Watch diagnostics for trend comparison. Optional maxSamples (1-200).",
						"{\"type\":\"object\",\"properties\":{\"maxSamples\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}}}")
				+ ","
				+ tool("fm_stations",
						"Live FM dial hits for an FM-scale view, or an empty list when zoomed out.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("tv_stations",
						"TV Seek list: occupancy vs ATSC-like vs picture/no-lock on this HackRF. picture is MPEG-2 frames; occupied is not a decoded station.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("nfc_activity",
						"Live 13.56 MHz NFC / HF-RFID classification (field, poll, HiFER/CW, A/B/F/V sidebands). Sweep classifier only. AirTags are not in this band.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("nfc_frames",
						"Recent NFC frames from the parked sniff decoder (tech, name, hex). Empty unless radioMode is nfc. Optional maxSamples (1-200).",
						"{\"type\":\"object\",\"properties\":{\"maxSamples\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}}}")
				+ ","
				+ tool("nfc_sniff",
						"Park the HackRF at 11.56 MHz / 10 MS/s and decode NFC frames. Same exclusive RF path as the Sniff control. Receive only.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("fm_spectrum",
						"Live local RF spectrum from the same parked 4 MS/s IQ stream used by FM Listen (dBFS).",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("gnss_activity",
						"Live RNSS L1 occupancy (GPS/Galileo 1575.42, BDS B1I, GLONASS L1). Sweep classifier only — not a GPS fix.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("gnss_sv",
						"Last-known GPS L1 C/A PRN peaks from parked sniff (C/N0, Doppler, dwell). A missed search does not clear the list. Empty unless radioMode is gnss.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("gnss_sniff",
						"Park the HackRF at GPS L1 1575.42 MHz / 4 MS/s and acquire C/A PRNs. Same exclusive RF path as the GNSS Sniff control. Not a PVT fix.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("cb_channels",
						"Occupied US CB 11 m AM channels (1–40) from the live sweep or last Scan. Occupancy is not a decoded voice call.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("cb_listen",
						"Park the HackRF on a US CB AM channel (1-40) and start Listen. Same exclusive RF path as the UI. AM, not FM broadcast.",
						"{\"type\":\"object\",\"properties\":{\"channel\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":40}},"
								+ "\"required\":[\"channel\"]}")
				+ ","
				+ tool("spectrum_occupancy",
						"Emitters above noise+8 dB: width, occupied fraction, optional BLE / Wi-Fi / NFC / GNSS label.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("spectrum_history",
						"Recent summaries from the snapshot ring (not full bins). Optional seconds and maxSamples.",
						"{\"type\":\"object\",\"properties\":{\"seconds\":{\"type\":\"number\",\"minimum\":0.1},"
								+ "\"maxSamples\":{\"type\":\"integer\",\"minimum\":1}}}")
				+ ","
				+ tool("spectrum_history_bins",
						"Recent filled-bin frames from the snapshot ring (same axis as the live window). Optional seconds, maxSamples (capped at 50), maxPoints, minDbm. Not the waterfall image.",
						"{\"type\":\"object\",\"properties\":{\"seconds\":{\"type\":\"number\",\"minimum\":0.1},"
								+ "\"maxSamples\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50},"
								+ "\"maxPoints\":{\"type\":\"integer\",\"minimum\":1},"
								+ "\"minDbm\":{\"type\":\"number\"}}}")
				+ ","
				+ tool("tv_watch",
						"Park the HackRF on a US ATSC 1.0 channel (2-36) and start Watch. Same exclusive RF path as the UI.",
						"{\"type\":\"object\",\"properties\":{\"channel\":{\"type\":\"integer\",\"minimum\":2,\"maximum\":36}},"
								+ "\"required\":[\"channel\"]}")
				+ ","
				+ tool("fm_listen",
						"Park the HackRF on a US FM dial (88.1-107.9 MHz, 200 kHz raster) and start Listen. Same exclusive RF path as the UI.",
						"{\"type\":\"object\",\"properties\":{\"mhz\":{\"type\":\"number\",\"minimum\":88.1,\"maximum\":107.9}},"
								+ "\"required\":[\"mhz\"]}")
				+ ","
				+ tool("auto_gain",
						"Set the existing Auto gain checkbox (same AnalyzerSettings.isAutoGain as the sidebar). Does not change LNA/VGA until the policy or operator does.",
						"{\"type\":\"object\",\"properties\":{\"enabled\":{\"type\":\"boolean\"}},\"required\":[\"enabled\"]}")
				+ ","
				+ tool("sweep",
						"Leave Listen/Watch/Sniff and restart the wideband sweep (same as Stop / restartSweep).",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("ble_sniff",
						"Start or stop parallel nRF BLE sniff. Sets the HackRF sweep to 2400–2484 MHz. Does not park USB. Optional enabled (default true).",
						"{\"type\":\"object\",\"properties\":{\"enabled\":{\"type\":\"boolean\"}}}")
				+ ","
				+ tool("ble_frames",
						"Recent Nordic sniffer UART frames (name, address, channel, RSSI, hex). Empty until ble_sniff. Optional maxSamples (1-200).",
						"{\"type\":\"object\",\"properties\":{\"maxSamples\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}}}")
				+ ","
				+ tool("ble_activity",
						"HackRF spectrum summary plus the BLE frame ring in one payload (same stream). Optional maxSamples.",
						"{\"type\":\"object\",\"properties\":{\"maxSamples\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}}}")
				+ ","
				+ tool("operator_brief",
						"Post a note on the operator Spectrum tools (Agent brief) and a one-line plot HUD. No USB. Optional face (ble|nfc|gnss|cb|fm|tv|sweep), title, body, marks[{mhz,label}], clear.",
						"{\"type\":\"object\",\"properties\":{\"face\":{\"type\":\"string\"},\"title\":{\"type\":\"string\"},"
								+ "\"body\":{\"type\":\"string\"},\"clear\":{\"type\":\"boolean\"},"
								+ "\"marks\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{"
								+ "\"mhz\":{\"type\":\"number\"},\"label\":{\"type\":\"string\"}}}}}}")
				+ ","
				+ tool("operator_briefs",
						"Last agent note per Spectrum tools face. Snapshot only — does not open USB.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ "]}";
	}

	public String call(String name, Map<String, Object> args)
	{
		if (name == null)
			throw new IllegalArgumentException("missing tool name");
		if (args == null)
			args = Map.of();
		if ("spectrum_snapshot".equals(name))
			return snapshotCall(args);
		if ("spectrum_summary".equals(name))
			return textResult(store.latest().toSummaryJson(store.context()), store.latest().isEmpty());
		if ("radio_identity".equals(name))
			return textResult(store.context().identityJson(), false);
		if ("sweep_config".equals(name))
			return textResult(store.sweepConfigJson(), false);
		if ("tv_debug".equals(name))
			return textResult(store.watchDebugJson(), false);
		if ("tv_spectrum".equals(name))
		{
			TvWatchSpectrum tv = store.tvWatchSpectrum();
			return textResult(tv.isEmpty() ? "{\"error\":\"no live TV spectrum\"}" : tv.toJson(), tv.isEmpty());
		}
		if ("tv_debug_history".equals(name))
			return textResult(store.watchDebugHistoryJson(McpJson.getInt(args, "maxSamples")), false);
		if ("tv_watch".equals(name))
			return tvWatchCall(args);
		if ("fm_listen".equals(name))
			return fmListenCall(args);
		if ("fm_stations".equals(name))
			return textResult(store.context().fmStationsJson(), false);
		if ("tv_stations".equals(name))
			return textResult(store.context().tvStationsJson(), false);
		if ("nfc_activity".equals(name))
			return textResult(store.nfcActivityJson(), false);
		if ("nfc_frames".equals(name))
			return textResult(store.nfcFramesJson(McpJson.getInt(args, "maxSamples")), false);
		if ("nfc_sniff".equals(name))
			return nfcSniffCall();
		if ("gnss_activity".equals(name))
			return textResult(store.gnssActivityJson(), false);
		if ("gnss_sv".equals(name))
			return textResult(store.gnssSvJson(), false);
		if ("gnss_sniff".equals(name))
			return gnssSniffCall();
		if ("cb_channels".equals(name))
			return textResult(store.cbChannelsJson(), false);
		if ("cb_listen".equals(name))
			return cbListenCall(args);
		if ("auto_gain".equals(name))
			return autoGainCall(args);
		if ("sweep".equals(name))
			return sweepCall();
		if ("ble_sniff".equals(name))
			return bleSniffCall(args);
		if ("ble_frames".equals(name))
			return textResult(store.bleFramesJson(McpJson.getInt(args, "maxSamples")), false);
		if ("ble_activity".equals(name))
			return textResult(store.bleActivityJson(McpJson.getInt(args, "maxSamples")), false);
		if ("operator_brief".equals(name))
			return operatorBriefCall(args);
		if ("operator_briefs".equals(name))
			return textResult(store.briefsJson(), false);
		if ("fm_spectrum".equals(name))
		{
			FmListenSpectrum fm = store.fmListenSpectrum();
			return textResult(fm.isEmpty() ? "{\"error\":\"no live FM spectrum\"}" : fm.toJson(), fm.isEmpty());
		}
		if ("spectrum_occupancy".equals(name))
			return occupancyCall();
		if ("spectrum_history".equals(name))
			return historyCall(args);
		if ("spectrum_history_bins".equals(name))
			return historyBinsCall(args);
		throw new IllegalArgumentException("unknown tool: " + name);
	}

	public String handleRpc(String json)
	{
		Map<String, Object> req;
		try
		{
			req = McpJson.parseObject(json);
		}
		catch (RuntimeException e)
		{
			return McpJson.rpcError(null, -32700, "parse error");
		}
		Object id = req.get("id");
		String method = McpJson.getString(req, "method");
		if (method == null)
			return McpJson.rpcError(id, -32600, "missing method");
		if (method.startsWith("notifications/"))
			return null;
		if ("initialize".equals(method) || "ping".equals(method))
			return McpJson.rpcResult(id, "initialize".equals(method) ? initializeResult() : "{}");
		if ("tools/list".equals(method))
			return McpJson.rpcResult(id, toolsListResult());
		if ("tools/call".equals(method))
		{
			Map<String, Object> params = McpJson.getObject(req, "params");
			String name = McpJson.getString(params, "name");
			Map<String, Object> arguments = McpJson.getObject(params, "arguments");
			if (arguments == null)
				arguments = new LinkedHashMap<String, Object>();
			try
			{
				return McpJson.rpcResult(id, call(name, arguments));
			}
			catch (IllegalArgumentException e)
			{
				return McpJson.rpcError(id, -32601, e.getMessage());
			}
		}
		return McpJson.rpcError(id, -32601, "method not found: " + method);
	}

	private String tvWatchCall(Map<String, Object> args)
	{
		if (tvWatch == null)
			throw new IllegalArgumentException("tv_watch is not bound");
		Integer ch = McpJson.getInt(args, "channel");
		if (ch == null)
			throw new IllegalArgumentException("tv_watch requires channel");
		hotiron.core.TvChannel plan = hotiron.core.TvChannelPlan.clamp(ch.intValue());
		tvWatch.watch(plan.fccChannel);
		return textResult("{\"ok\":true,\"tvChannel\":" + plan.fccChannel + ",\"centerMHz\":" + plan.centerMHz() + "}",
				false);
	}

	private String nfcSniffCall()
	{
		if (nfcSniff == null)
			throw new IllegalArgumentException("nfc_sniff is not bound");
		nfcSniff.sniff();
		return textResult("{\"ok\":true,\"radioMode\":\"nfc\",\"loHz\":11560000,\"sampleRate\":10000000}", false);
	}

	private String gnssSniffCall()
	{
		if (gnssSniff == null)
			throw new IllegalArgumentException("gnss_sniff is not bound");
		gnssSniff.sniff();
		return textResult("{\"ok\":true,\"radioMode\":\"gnss\",\"loHz\":1575420000,\"sampleRate\":4000000}", false);
	}

	private String cbListenCall(Map<String, Object> args)
	{
		if (cbListen == null)
			throw new IllegalArgumentException("cb_listen is not bound");
		Integer ch = McpJson.getInt(args, "channel");
		if (ch == null)
			throw new IllegalArgumentException("cb_listen requires channel");
		hotiron.core.CbChannel plan = hotiron.core.CbChannelPlan.clamp(ch.intValue());
		cbListen.listen(plan.number);
		return textResult(String.format(java.util.Locale.US,
				"{\"ok\":true,\"cbChannel\":%d,\"mhz\":%.3f,\"radioMode\":\"cb\"}", plan.number, plan.centerMHz()),
				false);
	}

	private String autoGainCall(Map<String, Object> args)
	{
		if (autoGain == null)
			throw new IllegalArgumentException("auto_gain is not bound");
		Boolean enabled = McpJson.getBoolean(args, "enabled");
		if (enabled == null)
			throw new IllegalArgumentException("auto_gain requires enabled");
		autoGain.setEnabled(enabled.booleanValue());
		return textResult("{\"ok\":true,\"autoGain\":" + enabled + "}", false);
	}

	private String sweepCall()
	{
		if (sweep == null)
			throw new IllegalArgumentException("sweep is not bound");
		sweep.sweep();
		return textResult("{\"ok\":true,\"radioMode\":\"sweep\"}", false);
	}

	private String operatorBriefCall(Map<String, Object> args)
	{
		String face = McpJson.getString(args, "face");
		if (face == null || face.isBlank())
			face = store.inferBriefFace();
		face = hotiron.core.OperatorBrief.normalizeFace(face);
		Boolean clear = McpJson.getBoolean(args, "clear");
		if (Boolean.TRUE.equals(clear))
		{
			store.publishBrief(hotiron.core.OperatorBrief.clear(face));
			return textResult("{\"ok\":true,\"cleared\":true,\"face\":\"" + face + "\"}", false);
		}
		java.util.List<hotiron.core.OperatorBrief.Mark> marks = new java.util.ArrayList<>();
		java.util.List<Object> rawMarks = McpJson.getList(args, "marks");
		if (rawMarks != null)
		{
			for (Object o : rawMarks)
			{
				if (!(o instanceof Map))
					continue;
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) o;
				Double mhz = McpJson.getDouble(m, "mhz");
				if (mhz == null)
					continue;
				String label = McpJson.getString(m, "label");
				marks.add(new hotiron.core.OperatorBrief.Mark(mhz.doubleValue(), label));
			}
		}
		hotiron.core.OperatorBrief brief = new hotiron.core.OperatorBrief(face, McpJson.getString(args, "title"),
				McpJson.getString(args, "body"), marks, System.currentTimeMillis(), "mcp");
		store.publishBrief(brief);
		return textResult(brief.toJson(), false);
	}

	private String bleSniffCall(Map<String, Object> args)
	{
		if (bleSniff == null)
			throw new IllegalArgumentException("ble_sniff is not bound");
		Boolean enabled = McpJson.getBoolean(args, "enabled");
		boolean on = enabled == null || enabled.booleanValue();
		bleSniff.setEnabled(on);
		return textResult("{\"ok\":true,\"bleSniff\":" + on + ",\"startMHz\":2400,\"endMHz\":2484}", false);
	}

	private String fmListenCall(Map<String, Object> args)
	{
		if (fmListen == null)
			throw new IllegalArgumentException("fm_listen is not bound");
		Double mhz = McpJson.getDouble(args, "mhz");
		if (mhz == null)
			throw new IllegalArgumentException("fm_listen requires mhz");
		hotiron.core.FmChannel ch = hotiron.core.FmChannelPlan.clamp(mhz.doubleValue());
		fmListen.listen(ch.centerMHz());
		return textResult(String.format(java.util.Locale.US,
				"{\"ok\":true,\"listenMHz\":%.1f,\"listenKHz\":%d}", ch.centerMHz(), ch.centerKHz), false);
	}

	private String occupancyCall()
	{
		SpectrumSnapshot latest = store.latest();
		if (latest == null || latest.isEmpty())
			return textResult("{\"error\":\"no sweep yet\",\"emitters\":[]}", true);
		hotiron.core.SpectrumOccupancy.Result occ = hotiron.core.SpectrumOccupancy.from(latest.mhz,
				latest.dbm, latest.noiseDbm, latest.fftBinHz, latest.startMHz, latest.endMHz);
		return textResult(occ.toJson(), false);
	}

	private String historyCall(Map<String, Object> args)
	{
		Double seconds = McpJson.getDouble(args, "seconds");
		Integer max = McpJson.getInt(args, "maxSamples");
		String json = store.historyJson(seconds, max);
		boolean empty = store.latest() == null || store.latest().isEmpty();
		return textResult(json, empty);
	}

	private String historyBinsCall(Map<String, Object> args)
	{
		String json = store.historyBinsJson(McpJson.getDouble(args, "seconds"), McpJson.getInt(args, "maxSamples"),
				McpJson.getInt(args, "maxPoints"), McpJson.getDouble(args, "minDbm"));
		boolean empty = store.latest() == null || store.latest().isEmpty();
		return textResult(json, empty);
	}

	private String snapshotCall(Map<String, Object> args)
	{
		SpectrumSnapshot latest = store.latest();
		Integer max = McpJson.getInt(args, "maxPoints");
		Double min = McpJson.getDouble(args, "minDbm");
		if (max == null && min == null)
			return textResult(store.attachBle(latest.toJson(), 20), latest.isEmpty());
		int cap = max == null ? latest.mhz.length : Math.max(1, max.intValue());
		return textResult(store.attachBle(latest.downsampled(cap, min == null ? null : min.floatValue()).toJson(), 20),
				latest.isEmpty());
	}

	private static String textResult(String json, boolean isError)
	{
		String escaped = SpectrumSnapshot.Json.quote(json);
		return "{\"content\":[{\"type\":\"text\",\"text\":" + escaped + "}]" + (isError ? ",\"isError\":true" : "")
				+ "}";
	}

	private static String tool(String name, String description, String schema)
	{
		return "{\"name\":\"" + name + "\",\"description\":" + SpectrumSnapshot.Json.quote(description)
				+ ",\"inputSchema\":" + schema + "}";
	}
}
