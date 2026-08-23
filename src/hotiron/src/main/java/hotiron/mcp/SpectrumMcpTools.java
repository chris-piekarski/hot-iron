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

	private final SpectrumSnapshotStore store;
	private final TvWatchHook tvWatch;
	private final FmListenHook fmListen;

	public SpectrumMcpTools(SpectrumSnapshotStore store)
	{
		this(store, null, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch)
	{
		this(store, tvWatch, null);
	}

	public SpectrumMcpTools(SpectrumSnapshotStore store, TvWatchHook tvWatch, FmListenHook fmListen)
	{
		if (store == null)
			throw new IllegalArgumentException("store");
		this.store = store;
		this.tvWatch = tvWatch;
		this.fmListen = fmListen;
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
						"Armed radio settings (range, FFT, gain, CLKOUT), radioMode (sweep|listen|watch|stopped), listenMHz, tvChannel, plus display flags (autoSweep, autoGain, autoScale, peaks).",
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
				+ tool("fm_spectrum",
						"Live local RF spectrum from the same parked 4 MS/s IQ stream used by FM Listen (dBFS).",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("spectrum_occupancy",
						"Emitters above noise+8 dB: width, occupied fraction, optional Wi-Fi ch label.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("spectrum_history",
						"Recent summaries from the snapshot ring (not full bins). Optional seconds and maxSamples.",
						"{\"type\":\"object\",\"properties\":{\"seconds\":{\"type\":\"number\",\"minimum\":0.1},"
								+ "\"maxSamples\":{\"type\":\"integer\",\"minimum\":1}}}")
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
		if ("fm_spectrum".equals(name))
		{
			FmListenSpectrum fm = store.fmListenSpectrum();
			return textResult(fm.isEmpty() ? "{\"error\":\"no live FM spectrum\"}" : fm.toJson(), fm.isEmpty());
		}
		if ("spectrum_occupancy".equals(name))
			return occupancyCall();
		if ("spectrum_history".equals(name))
			return historyCall(args);
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

	private String snapshotCall(Map<String, Object> args)
	{
		SpectrumSnapshot latest = store.latest();
		Integer max = McpJson.getInt(args, "maxPoints");
		Double min = McpJson.getDouble(args, "minDbm");
		if (max == null && min == null)
			return textResult(latest.toJson(), latest.isEmpty());
		// Re-filter from the stored points (already hole-stripped).
		int cap = max == null ? latest.mhz.length : Math.max(1, max.intValue());
		return textResult(downsampleStored(latest, cap, min == null ? null : min.floatValue()).toJson(),
				latest.isEmpty());
	}

	static SpectrumSnapshot downsampleStored(SpectrumSnapshot src, int maxPoints, Float minDbm)
	{
		if (src == null || src.isEmpty())
			return src == null ? SpectrumSnapshot.empty(0L) : src;
		int n = src.mhz.length;
		float[] m = new float[Math.min(n, maxPoints)];
		float[] d = new float[m.length];
		int out = 0;
		if (n <= maxPoints)
		{
			for (int i = 0; i < n; i++)
			{
				if (minDbm != null && src.dbm[i] < minDbm.floatValue())
					continue;
				if (out < m.length)
				{
					m[out] = src.mhz[i];
					d[out] = src.dbm[i];
					out++;
				}
			}
		}
		else
		{
			for (int p = 0; p < maxPoints; p++)
			{
				int i0 = (int) ((long) p * n / maxPoints);
				int i1 = Math.max(i0 + 1, (int) ((long) (p + 1) * n / maxPoints));
				float peak = Float.NEGATIVE_INFINITY;
				float xAt = src.mhz[i0];
				boolean any = false;
				for (int i = i0; i < i1 && i < n; i++)
				{
					if (minDbm != null && src.dbm[i] < minDbm.floatValue())
						continue;
					any = true;
					if (src.dbm[i] > peak)
					{
						peak = src.dbm[i];
						xAt = src.mhz[i];
					}
				}
				if (!any)
					continue;
				m[out] = xAt;
				d[out] = peak;
				out++;
			}
		}
		float[] mo = new float[out];
		float[] do_ = new float[out];
		System.arraycopy(m, 0, mo, 0, out);
		System.arraycopy(d, 0, do_, 0, out);
		return new SpectrumSnapshot(src.timestampMs, src.startMHz, src.endMHz, src.fftBinHz, mo, do_, src.filledBins,
				src.omittedHoles, src.noiseDbm, src.peakDbm, src.peakMhz, src.freqStartHz);
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
