package jspectrumanalyzer.mcp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jspectrumanalyzer.core.AnalyzerSettings;
import jspectrumanalyzer.core.FmBandLayer;
import jspectrumanalyzer.core.FmStationHit;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.ListenService;
import jspectrumanalyzer.core.RadioIdentity;
import jspectrumanalyzer.core.RadioMode;
import jspectrumanalyzer.core.SweepConfig;
import jspectrumanalyzer.core.TvWatchDebug;
import jspectrumanalyzer.mcp.SpectrumSnapshot.FmHit;
import jspectrumanalyzer.mcp.SpectrumSnapshot.RadioContext;

/**
 * Latest sweep plus a short summary ring. Writers are the processing
 * hook; MCP tools only read.
 */
public final class SpectrumSnapshotStore
{
	/** ~20 s at the 10 Hz publish cap. */
	public static final int DEFAULT_RING = 200;
	public static final long MIN_PUBLISH_INTERVAL_MS = 100L;
	public static final double DEFAULT_HISTORY_SEC = 15;
	public static final int DEFAULT_HISTORY_SAMPLES = 50;

	private final Object lock = new Object();
	private final int ringCap;
	private final ArrayDeque<RingEntry> ring;
	private SpectrumSnapshot latest = SpectrumSnapshot.empty(0L);
	private RadioContext context;
	private long lastPublishMs;
	private boolean tvLocked;
	private float tvSnrDb;
	private int tvPackets;
	private TvWatchDebug tvDebug = TvWatchDebug.empty();
	private final ArrayDeque<TvWatchDebug> tvDebugRing = new ArrayDeque<TvWatchDebug>(DEFAULT_RING);

	public SpectrumSnapshotStore()
	{
		this(DEFAULT_RING);
	}

	public SpectrumSnapshotStore(int ringCap)
	{
		this.ringCap = Math.max(1, ringCap);
		this.ring = new ArrayDeque<RingEntry>(this.ringCap);
		this.context = new RadioContext(false, false, 0, null, null, null, null, false, 0, 0, 0, 0, 0, 0, false, false,
				false, "", false, false, false, List.of());
	}

	public boolean shouldPublish(long nowMs)
	{
		synchronized (lock)
		{
			return nowMs - lastPublishMs >= MIN_PUBLISH_INTERVAL_MS;
		}
	}

	public void publishSweep(SpectrumSnapshot snap, long nowMs)
	{
		if (snap == null)
			return;
		int lna;
		int vga;
		synchronized (lock)
		{
			lna = context == null ? 0 : context.lnaGain;
			vga = context == null ? 0 : context.vgaGain;
			latest = snap;
			lastPublishMs = nowMs;
			if (ring.size() >= ringCap)
				ring.removeFirst();
			ring.addLast(new RingEntry(snap, lna, vga));
		}
	}

	public void publishContext(HackRFSettings settings, List<FmStationHit> stations, double sweepsPerSec)
	{
		if (settings == null)
			return;
		RadioIdentity id = settings.getRadioIdentity() != null ? settings.getRadioIdentity().getValue()
				: RadioIdentity.ABSENT;
		if (id == null)
			id = RadioIdentity.ABSENT;
		SweepConfig radio = SweepConfig.from(settings);
		List<FmHit> fm = new ArrayList<FmHit>();
		if (stations != null && FmBandLayer.tagsReadable(radio.startMHz, radio.endMHz))
		{
			for (FmStationHit hit : stations)
			{
				if (hit == null || hit.channel == null)
					continue;
				fm.add(new FmHit(hit.label(), (float) hit.channel.centerMHz(), hit.powerDbm, hit.confidence));
			}
		}
		boolean paused = settings.isCapturingPaused() != null && Boolean.TRUE.equals(settings.isCapturingPaused().getValue());
		boolean released = settings.isRadioReleased() != null && Boolean.TRUE.equals(settings.isRadioReleased().getValue());
		boolean peaks = settings.isChartsPeaksVisible() != null
				&& Boolean.TRUE.equals(settings.isChartsPeaksVisible().getValue());
		boolean auto = settings.isPowerAutoScale() != null && Boolean.TRUE.equals(settings.isPowerAutoScale().getValue());
		boolean autoGain = settings.isAutoGain() != null && Boolean.TRUE.equals(settings.isAutoGain().getValue());
		boolean listening = settings.isListening() != null && Boolean.TRUE.equals(settings.isListening().getValue());
		int listenKHz = settings.getListenKHz() != null ? settings.getListenKHz().getValue() : 0;
		ListenService service = settings.getListenService() != null ? settings.getListenService().getValue()
				: ListenService.FM;
		int tvChannel = settings.getTvChannel() != null ? settings.getTvChannel().getValue() : 0;
		String mode = RadioMode.from(released, listening, service).jsonName();
		RadioContext next = new RadioContext(paused, released, sweepsPerSec, id.displayBoard(), id.shortSerial(),
				id.displayFirmware(), id.usbApi, id.present, radio.startMHz, radio.endMHz, radio.fftBinHz, radio.samples,
				radio.lnaGain, radio.vgaGain, radio.antennaPower, radio.antennaLna, radio.clkout, radio.serial, peaks,
				auto, autoGain, fm, mode, listenKHz, tvChannel);
		synchronized (lock)
		{
			if (context == null || next.tvChannel != context.tvChannel
					|| !next.radioMode.equals(context.radioMode))
			{
				tvDebug = TvWatchDebug.empty();
				tvDebugRing.clear();
			}
			context = next;
		}
	}

	/** Convenience for tests that already have {@link AnalyzerSettings}. */
	public void publishContext(AnalyzerSettings settings, List<FmStationHit> stations, double sweepsPerSec)
	{
		publishContext((HackRFSettings) settings, stations, sweepsPerSec);
	}

	public void publishWatchStats(boolean locked, float snrDb, int packets)
	{
		synchronized (lock)
		{
			tvLocked = locked;
			tvSnrDb = snrDb;
			tvPackets = packets;
		}
	}

	public void publishWatchDebug(TvWatchDebug next)
	{
		if (next == null)
			return;
		synchronized (lock)
		{
			if (tvDebug != null && next.timestampMs == tvDebug.timestampMs)
				return;
			tvDebug = next;
			if (tvDebugRing.size() >= DEFAULT_RING)
				tvDebugRing.removeFirst();
			tvDebugRing.addLast(next);
		}
	}

	public String watchDebugJson()
	{
		TvWatchDebug current;
		int channel;
		synchronized (lock)
		{
			current = tvDebug;
			channel = context == null ? 0 : context.tvChannel;
		}
		StringBuilder sb = new StringBuilder(640);
		appendWatchDebug(sb, current, channel);
		return sb.toString();
	}

	public String watchDebugHistoryJson(Integer maxSamples)
	{
		int cap = maxSamples == null ? 50 : Math.max(1, Math.min(DEFAULT_RING, maxSamples.intValue()));
		List<TvWatchDebug> samples;
		int channel;
		synchronized (lock)
		{
			samples = List.copyOf(tvDebugRing);
			channel = context == null ? 0 : context.tvChannel;
		}
		if (samples.size() > cap)
			samples = samples.subList(samples.size() - cap, samples.size());
		StringBuilder sb = new StringBuilder(64 + samples.size() * 500);
		sb.append('{');
		SpectrumSnapshot.Json.appendKey(sb, "tvChannel").append(channel).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "sampleCount").append(samples.size()).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "samples").append('[');
		for (int i = 0; i < samples.size(); i++)
		{
			if (i > 0)
				sb.append(',');
			appendWatchDebug(sb, samples.get(i), channel);
		}
		sb.append("]}");
		return sb.toString();
	}

	private static void appendWatchDebug(StringBuilder sb, TvWatchDebug d, int channel)
	{
		if (d == null)
			d = TvWatchDebug.empty();
		float badFraction = d.packets > 0 ? (float) ((double) d.badPackets / d.packets) : 0f;
		float fieldFraction = d.segments > 0 ? (float) ((double) d.fieldSegments / d.segments) : 0f;
		sb.append('{');
		SpectrumSnapshot.Json.appendKey(sb, "timestampMs").append(d.timestampMs).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tvChannel").append(channel).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "stage").append(SpectrumSnapshot.Json.quote(d.stage())).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "running").append(d.running).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "nativeAvailable").append(d.nativeAvailable).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "inverted").append(d.inverted).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "locked").append(d.locked).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "hasPat").append(d.hasPat).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "frames").append(d.frames).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "previewFrames").append(d.previewFrames).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "totalIqSamples").append(d.totalIqSamples).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "droppedChunks").append(d.droppedChunks).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "queueDepth").append(d.queueDepth).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "segments").append(d.segments).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "fieldSegments").append(d.fieldSegments).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "fieldSyncFraction")
				.append(SpectrumSnapshot.Json.num(fieldFraction)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "packets").append(d.packets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "badPackets").append(d.badPackets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "goodPackets").append(d.goodPackets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "badPacketFraction")
				.append(SpectrumSnapshot.Json.num(badFraction)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "rsGoodWindow").append(d.rsGoodWindow).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "rsWindow").append(d.rsWindow).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "rsGoodRatioDb")
				.append(SpectrumSnapshot.Json.num(d.rsGoodRatioDb)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "agcGain").append(SpectrumSnapshot.Json.num(d.agcGain)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "rmsIq").append(SpectrumSnapshot.Json.num(d.rmsIq)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "rmsBaseband")
				.append(SpectrumSnapshot.Json.num(d.rmsBaseband)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "pendingBaseband").append(d.pendingBaseband).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "equalizerMainTap")
				.append(SpectrumSnapshot.Json.num(d.equalizerMainTap)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "equalizerPeakTap")
				.append(SpectrumSnapshot.Json.num(d.equalizerPeakTap));
		sb.append('}');
	}

	public String sweepConfigJson()
	{
		RadioContext ctx;
		boolean locked;
		float snr;
		int pkts;
		synchronized (lock)
		{
			ctx = context;
			locked = tvLocked;
			snr = tvSnrDb;
			pkts = tvPackets;
		}
		String base = ctx.sweepConfigJson();
		if (base == null || base.length() < 2 || base.charAt(base.length() - 1) != '}')
			return base;
		return base.substring(0, base.length() - 1) + String.format(Locale.US,
				",\"tvLocked\":%s,\"tvSnrDb\":%.1f,\"tvPackets\":%d}", locked, snr, pkts);
	}

	public SpectrumSnapshot latest()
	{
		synchronized (lock)
		{
			return latest;
		}
	}

	public RadioContext context()
	{
		synchronized (lock)
		{
			return context;
		}
	}

	public int ringSize()
	{
		synchronized (lock)
		{
			return ring.size();
		}
	}

	/** Oldest-first copy of the ring. */
	public List<RingEntry> ringCopy()
	{
		synchronized (lock)
		{
			return List.copyOf(ring);
		}
	}

	public String historyJson(Double seconds, Integer maxSamples)
	{
		double sec = seconds == null || !(seconds.doubleValue() > 0) ? DEFAULT_HISTORY_SEC : seconds.doubleValue();
		int cap = maxSamples == null || maxSamples.intValue() < 1 ? DEFAULT_HISTORY_SAMPLES : maxSamples.intValue();
		SpectrumSnapshot now;
		List<RingEntry> entries;
		synchronized (lock)
		{
			now = latest;
			entries = List.copyOf(ring);
		}
		if (now == null || now.isEmpty())
			return "{\"error\":\"no sweep yet\",\"samples\":[]}";
		long oldestTs = now.timestampMs - (long) Math.round(sec * 1000.0);
		List<RingEntry> same = new ArrayList<>();
		for (RingEntry e : entries)
		{
			if (e == null || e.snap == null || e.snap.isEmpty())
				continue;
			if (!sameAxis(now, e.snap))
				continue;
			if (e.snap.timestampMs < oldestTs)
				continue;
			same.add(e);
		}
		if (same.size() > cap)
			same = same.subList(same.size() - cap, same.size());
		StringBuilder sb = new StringBuilder(64 + same.size() * 96);
		sb.append('{');
		SpectrumSnapshot.Json.appendKey(sb, "seconds").append(String.format(Locale.US, "%.1f", sec)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "startMHz").append(now.startMHz).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "endMHz").append(now.endMHz).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "sampleCount").append(same.size()).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "samples").append('[');
		for (int i = 0; i < same.size(); i++)
		{
			if (i > 0)
				sb.append(',');
			RingEntry e = same.get(i);
			jspectrumanalyzer.core.SpectrumOccupancy.Result occ = jspectrumanalyzer.core.SpectrumOccupancy.from(e.snap.mhz,
					e.snap.dbm, e.snap.noiseDbm, e.snap.fftBinHz, e.snap.startMHz, e.snap.endMHz);
			sb.append('{');
			SpectrumSnapshot.Json.appendKey(sb, "timestampMs").append(e.snap.timestampMs).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "startMHz").append(e.snap.startMHz).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "endMHz").append(e.snap.endMHz).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "noiseDbm").append(SpectrumSnapshot.Json.num(e.snap.noiseDbm)).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "peakDbm").append(SpectrumSnapshot.Json.num(e.snap.peakDbm)).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "peakMhz").append(SpectrumSnapshot.Json.num(e.snap.peakMhz)).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "occupiedFraction").append(SpectrumSnapshot.Json.num(occ.occupiedFraction))
					.append(',');
			SpectrumSnapshot.Json.appendKey(sb, "lnaGain").append(e.lnaGain).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "vgaGain").append(e.vgaGain);
			sb.append('}');
		}
		sb.append("]}");
		return sb.toString();
	}

	static boolean sameAxis(SpectrumSnapshot a, SpectrumSnapshot b)
	{
		if (a == null || b == null)
			return false;
		return a.startMHz == b.startMHz && a.endMHz == b.endMHz && Math.abs(a.fftBinHz - b.fftBinHz) < 1f;
	}

	public static final class RingEntry
	{
		public final SpectrumSnapshot snap;
		public final int lnaGain;
		public final int vgaGain;

		public RingEntry(SpectrumSnapshot snap, int lnaGain, int vgaGain)
		{
			this.snap = snap;
			this.lnaGain = lnaGain;
			this.vgaGain = vgaGain;
		}
	}
}
