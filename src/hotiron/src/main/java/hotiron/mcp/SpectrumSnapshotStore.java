package hotiron.mcp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hotiron.core.FmBandLayer;
import hotiron.core.FmStationHit;
import hotiron.core.NfcActivity;
import hotiron.core.NfcFrame;
import hotiron.core.HackRFSettings;
import hotiron.core.MpegTsPlayer;
import hotiron.core.MpegTsProbe;
import hotiron.core.RadioIdentity;
import hotiron.core.RadioMode;
import hotiron.core.SweepConfig;
import hotiron.core.TvWatchDebug;
import hotiron.mcp.SpectrumSnapshot.FmHit;
import hotiron.mcp.SpectrumSnapshot.RadioContext;

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
	/** Default / hard cap so a bin dump stays smaller than the waterfall image. */
	public static final int DEFAULT_HISTORY_BINS_SAMPLES = 20;
	public static final int MAX_HISTORY_BINS_SAMPLES = 50;
	public static final int DEFAULT_HISTORY_BINS_POINTS = 512;

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
	private FmListenSpectrum fmListenSpectrum = FmListenSpectrum.empty();
	private TvWatchSpectrum tvWatchSpectrum = TvWatchSpectrum.empty();
	private NfcActivity nfc = NfcActivity.hidden();
	private final ArrayDeque<NfcFrame> nfcFrames = new ArrayDeque<NfcFrame>(DEFAULT_RING);

	public SpectrumSnapshotStore()
	{
		this(DEFAULT_RING);
	}

	public SpectrumSnapshotStore(int ringCap)
	{
		this.ringCap = Math.max(1, ringCap);
		this.ring = new ArrayDeque<RingEntry>(this.ringCap);
		this.context = new RadioContext(false, false, 0, null, null, null, null, false, 0, 0, 0, 0, 0, 0, false, false,
				false, "", false, false, false, false, List.of(), "sweep", 0, 0);
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
		boolean autoSweep = settings.isAutoSweep() != null && Boolean.TRUE.equals(settings.isAutoSweep().getValue());
		int listenKHz = settings.getListenKHz() != null ? settings.getListenKHz().getValue() : 0;
		int tvChannel = settings.getTvChannel() != null ? settings.getTvChannel().getValue() : 0;
		String mode = RadioMode.of(settings).jsonName();
		RadioContext next = new RadioContext(paused, released, sweepsPerSec, id.displayBoard(), id.shortSerial(),
				id.displayFirmware(), id.usbApi, id.present, radio.startMHz, radio.endMHz, radio.fftBinHz, radio.samples,
				radio.lnaGain, radio.vgaGain, radio.antennaPower, radio.antennaLna, radio.clkout, radio.serial, peaks,
				auto, autoGain, autoSweep, fm, mode, listenKHz, tvChannel);
		synchronized (lock)
		{
			if (context == null || next.listenKHz != context.listenKHz
					|| !next.radioMode.equals(context.radioMode))
				fmListenSpectrum = FmListenSpectrum.empty();
			if (context == null || next.tvChannel != context.tvChannel
					|| !next.radioMode.equals(context.radioMode))
			{
				tvDebug = TvWatchDebug.empty();
				tvDebugRing.clear();
				tvWatchSpectrum = TvWatchSpectrum.empty();
			}
			context = next;
		}
	}

	public void publishNfc(NfcActivity next)
	{
		synchronized (lock)
		{
			nfc = next == null ? NfcActivity.hidden() : next;
		}
	}

	public NfcActivity nfcActivity()
	{
		synchronized (lock)
		{
			return nfc;
		}
	}

	public String nfcActivityJson()
	{
		return nfcActivity().toJson();
	}

	public void publishNfcFrame(NfcFrame frame)
	{
		if (frame == null)
			return;
		synchronized (lock)
		{
			nfcFrames.addLast(frame);
			while (nfcFrames.size() > ringCap)
				nfcFrames.removeFirst();
		}
	}

	public List<NfcFrame> nfcFrames()
	{
		synchronized (lock)
		{
			return List.copyOf(nfcFrames);
		}
	}

	public String nfcFramesJson(Integer max)
	{
		int cap = max == null ? 50 : Math.max(1, Math.min(200, max.intValue()));
		List<NfcFrame> all = nfcFrames();
		int from = Math.max(0, all.size() - cap);
		StringBuilder sb = new StringBuilder(64 + all.size() * 80);
		sb.append("{\"count\":").append(all.size()).append(",\"frames\":[");
		for (int i = from; i < all.size(); i++)
		{
			if (i > from)
				sb.append(',');
			sb.append(all.get(i).toJson());
		}
		sb.append("]}");
		return sb.toString();
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

	public void publishFmListenSpectrum(FmListenSpectrum next)
	{
		if (next == null)
			return;
		synchronized (lock)
		{
			fmListenSpectrum = next;
		}
	}

	public FmListenSpectrum fmListenSpectrum()
	{
		synchronized (lock)
		{
			return fmListenSpectrum;
		}
	}

	public void publishTvWatchSpectrum(TvWatchSpectrum next)
	{
		if (next == null)
			return;
		synchronized (lock)
		{
			tvWatchSpectrum = next;
		}
	}

	public TvWatchSpectrum tvWatchSpectrum()
	{
		synchronized (lock)
		{
			return tvWatchSpectrum;
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
				.append(SpectrumSnapshot.Json.num(d.equalizerPeakTap)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "ffmpeg");
		appendFfmpeg(sb, d.ffmpeg);
		sb.append('}');
	}

	private static void appendFfmpeg(StringBuilder sb, MpegTsPlayer.Stats f)
	{
		if (f == null)
			f = MpegTsPlayer.Stats.empty();
		MpegTsProbe.Snapshot ts = f.ts;
		sb.append('{');
		SpectrumSnapshot.Json.appendKey(sb, "started").append(f.started).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "launchFailed").append(f.launchFailed).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "videoAlive").append(f.videoAlive).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "audioAlive").append(f.audioAlive).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "videoExitCode");
		if (f.videoExitCode == MpegTsPlayer.Stats.EXIT_NONE)
			sb.append("null");
		else
			sb.append(f.videoExitCode);
		sb.append(',');
		SpectrumSnapshot.Json.appendKey(sb, "startedMs").append(f.startedMs).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "waitMs").append(f.waitMs).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tsBytesOffered").append(f.tsBytesOffered).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tsBytesWritten").append(f.tsBytesWritten).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tsQueueDepth").append(f.tsQueueDepth).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tsQueueCap").append(f.tsQueueCap).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tsDropped").append(f.tsDropped).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "writeErrors").append(f.writeErrors).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "stdoutBytes").append(f.stdoutBytes).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "partialFrameBytes").append(f.partialFrameBytes).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "stdoutEof").append(f.stdoutEof).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "lastStderr").append(SpectrumSnapshot.Json.quote(f.lastStderr))
				.append(',');
		SpectrumSnapshot.Json.appendKey(sb, "ts").append('{');
		SpectrumSnapshot.Json.appendKey(sb, "packets").append(ts.packets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "syncErrors").append(ts.syncErrors).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "patPackets").append(ts.patPackets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "pmtPid").append(ts.pmtPid).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "pmtPackets").append(ts.pmtPackets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "videoPid").append(ts.videoPid).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "videoStreamType").append(ts.videoStreamType).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "videoPackets").append(ts.videoPackets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "videoPesStarts").append(ts.videoPesStarts).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "audioPid").append(ts.audioPid).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "audioStreamType").append(ts.audioStreamType).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "audioPackets").append(ts.audioPackets).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "audioPesStarts").append(ts.audioPesStarts);
		sb.append("}}");
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
		double sec = clampSeconds(seconds);
		int cap = maxSamples == null || maxSamples.intValue() < 1 ? DEFAULT_HISTORY_SAMPLES : maxSamples.intValue();
		SpectrumSnapshot now = latestCopy();
		if (now == null || now.isEmpty())
			return "{\"error\":\"no sweep yet\",\"samples\":[]}";
		List<RingEntry> same = recentSameAxis(now, sec, cap);
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
			hotiron.core.SpectrumOccupancy.Result occ = hotiron.core.SpectrumOccupancy.from(e.snap.mhz,
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

	/**
	 * Same-axis ring frames with filled bins (not the waterfall image).
	 * Caps samples and points so a dump stays smaller than a PNG.
	 */
	public String historyBinsJson(Double seconds, Integer maxSamples, Integer maxPoints, Double minDbm)
	{
		double sec = clampSeconds(seconds);
		int cap = maxSamples == null || maxSamples.intValue() < 1 ? DEFAULT_HISTORY_BINS_SAMPLES
				: Math.min(MAX_HISTORY_BINS_SAMPLES, maxSamples.intValue());
		int points = maxPoints == null || maxPoints.intValue() < 1 ? DEFAULT_HISTORY_BINS_POINTS
				: Math.min(SpectrumSnapshot.DEFAULT_MAX_POINTS, maxPoints.intValue());
		Float floor = minDbm == null ? null : minDbm.floatValue();
		SpectrumSnapshot now = latestCopy();
		if (now == null || now.isEmpty())
			return "{\"error\":\"no sweep yet\",\"samples\":[]}";
		List<RingEntry> same = recentSameAxis(now, sec, cap);
		StringBuilder sb = new StringBuilder(128 + same.size() * (64 + points * 24));
		sb.append('{');
		SpectrumSnapshot.Json.appendKey(sb, "seconds").append(String.format(Locale.US, "%.1f", sec)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "startMHz").append(now.startMHz).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "endMHz").append(now.endMHz).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "fftBinHz").append(SpectrumSnapshot.Json.num(now.fftBinHz)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "sampleCount").append(same.size()).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "maxPoints").append(points).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "samples").append('[');
		for (int i = 0; i < same.size(); i++)
		{
			if (i > 0)
				sb.append(',');
			RingEntry e = same.get(i);
			SpectrumSnapshot frame = e.snap.downsampled(points, floor);
			sb.append('{');
			SpectrumSnapshot.Json.appendKey(sb, "timestampMs").append(frame.timestampMs).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "noiseDbm").append(SpectrumSnapshot.Json.num(frame.noiseDbm)).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "peakDbm").append(SpectrumSnapshot.Json.num(frame.peakDbm)).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "peakMhz").append(SpectrumSnapshot.Json.num(frame.peakMhz)).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "lnaGain").append(e.lnaGain).append(',');
			SpectrumSnapshot.Json.appendKey(sb, "vgaGain").append(e.vgaGain).append(',');
			frame.appendPoints(sb);
			sb.append('}');
		}
		sb.append("]}");
		return sb.toString();
	}

	private SpectrumSnapshot latestCopy()
	{
		synchronized (lock)
		{
			return latest;
		}
	}

	private List<RingEntry> recentSameAxis(SpectrumSnapshot now, double sec, int cap)
	{
		List<RingEntry> entries;
		synchronized (lock)
		{
			entries = List.copyOf(ring);
		}
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
			return same.subList(same.size() - cap, same.size());
		return same;
	}

	private static double clampSeconds(Double seconds)
	{
		return seconds == null || !(seconds.doubleValue() > 0) ? DEFAULT_HISTORY_SEC : seconds.doubleValue();
	}

	static boolean sameAxis(SpectrumSnapshot a, SpectrumSnapshot b)
	{
		if (a == null || b == null)
			return false;
		return a.freqStartHz == b.freqStartHz && a.endMHz == b.endMHz && Math.abs(a.fftBinHz - b.fftBinHz) < 1f;
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
