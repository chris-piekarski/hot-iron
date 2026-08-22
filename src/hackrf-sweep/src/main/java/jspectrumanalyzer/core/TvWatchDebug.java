package jspectrumanalyzer.core;

/**
 * Immutable ATSC pipeline diagnostics sampled by the watch worker.
 */
public final class TvWatchDebug
{
	public final long timestampMs;
	public final boolean running;
	public final boolean nativeAvailable;
	public final boolean hasPat;
	public final boolean locked;
	public final boolean inverted;
	public final long packets;
	public final long badPackets;
	public final long goodPackets;
	public final long segments;
	public final long fieldSegments;
	public final long pendingBaseband;
	public final long rsGoodWindow;
	public final long rsWindow;
	public final long totalIqSamples;
	public final long droppedChunks;
	public final int queueDepth;
	public final int frames;
	public final int previewFrames;
	public final float agcGain;
	public final float rmsIq;
	public final float rmsBaseband;
	public final float rsGoodRatioDb;
	public final float equalizerMainTap;
	public final float equalizerPeakTap;

	public TvWatchDebug(long timestampMs, boolean running, boolean nativeAvailable,
			boolean hasPat, boolean locked, boolean inverted, long packets,
			long badPackets, long goodPackets, long segments, long fieldSegments,
			long pendingBaseband, long rsGoodWindow, long rsWindow, long totalIqSamples,
			long droppedChunks, int queueDepth, int frames, int previewFrames,
			float agcGain, float rmsIq, float rmsBaseband, float rsGoodRatioDb,
			float equalizerMainTap, float equalizerPeakTap)
	{
		this.timestampMs = timestampMs;
		this.running = running;
		this.nativeAvailable = nativeAvailable;
		this.hasPat = hasPat;
		this.locked = locked;
		this.inverted = inverted;
		this.packets = packets;
		this.badPackets = badPackets;
		this.goodPackets = goodPackets;
		this.segments = segments;
		this.fieldSegments = fieldSegments;
		this.pendingBaseband = pendingBaseband;
		this.rsGoodWindow = rsGoodWindow;
		this.rsWindow = rsWindow;
		this.totalIqSamples = totalIqSamples;
		this.droppedChunks = droppedChunks;
		this.queueDepth = queueDepth;
		this.frames = frames;
		this.previewFrames = previewFrames;
		this.agcGain = agcGain;
		this.rmsIq = rmsIq;
		this.rmsBaseband = rmsBaseband;
		this.rsGoodRatioDb = rsGoodRatioDb;
		this.equalizerMainTap = equalizerMainTap;
		this.equalizerPeakTap = equalizerPeakTap;
	}

	public static TvWatchDebug empty()
	{
		return new TvWatchDebug(0, false, false, false, false, false,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0);
	}

	public String stage()
	{
		if (!running)
			return "stopped";
		if (!nativeAvailable)
			return "native_missing";
		if (totalIqSamples == 0)
			return "waiting_iq";
		if (segments == 0)
			return "no_segment_sync";
		if (fieldSegments == 0)
			return "no_field_sync";
		if (packets == 0)
			return "waiting_rs";
		if ((double) badPackets / packets >= 0.99)
			return "rs_unusable";
		if (!hasPat)
			return "no_pat";
		if (frames == 0)
			return "ffmpeg_waiting";
		return "picture";
	}
}
