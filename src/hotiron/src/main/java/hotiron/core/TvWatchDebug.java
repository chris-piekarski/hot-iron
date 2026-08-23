package hotiron.core;

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
	public final MpegTsPlayer.Stats ffmpeg;

	public TvWatchDebug(long timestampMs, boolean running, boolean nativeAvailable,
			boolean hasPat, boolean locked, boolean inverted, long packets,
			long badPackets, long goodPackets, long segments, long fieldSegments,
			long pendingBaseband, long rsGoodWindow, long rsWindow, long totalIqSamples,
			long droppedChunks, int queueDepth, int frames, int previewFrames,
			float agcGain, float rmsIq, float rmsBaseband, float rsGoodRatioDb,
			float equalizerMainTap, float equalizerPeakTap)
	{
		this(timestampMs, running, nativeAvailable, hasPat, locked, inverted, packets, badPackets,
				goodPackets, segments, fieldSegments, pendingBaseband, rsGoodWindow, rsWindow,
				totalIqSamples, droppedChunks, queueDepth, frames, previewFrames, agcGain, rmsIq,
				rmsBaseband, rsGoodRatioDb, equalizerMainTap, equalizerPeakTap,
				MpegTsPlayer.Stats.empty());
	}

	public TvWatchDebug(long timestampMs, boolean running, boolean nativeAvailable,
			boolean hasPat, boolean locked, boolean inverted, long packets,
			long badPackets, long goodPackets, long segments, long fieldSegments,
			long pendingBaseband, long rsGoodWindow, long rsWindow, long totalIqSamples,
			long droppedChunks, int queueDepth, int frames, int previewFrames,
			float agcGain, float rmsIq, float rmsBaseband, float rsGoodRatioDb,
			float equalizerMainTap, float equalizerPeakTap, MpegTsPlayer.Stats ffmpeg)
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
		this.ffmpeg = ffmpeg == null ? MpegTsPlayer.Stats.empty() : ffmpeg;
	}

	public static TvWatchDebug empty()
	{
		return new TvWatchDebug(0, false, false, false, false, false,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0);
	}

	TvWatchDebug withPlayer(long now, boolean hasPat, boolean locked, int frames,
			int previewFrames, MpegTsPlayer.Stats ffmpeg)
	{
		return new TvWatchDebug(now, running, nativeAvailable, hasPat, locked, inverted,
				packets, badPackets, goodPackets, segments, fieldSegments, pendingBaseband,
				rsGoodWindow, rsWindow, totalIqSamples, droppedChunks, queueDepth, frames,
				previewFrames, agcGain, rmsIq, rmsBaseband, rsGoodRatioDb, equalizerMainTap,
				equalizerPeakTap, ffmpeg);
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
		if (frames == 0 && !ffmpeg.started && rsWindow >= TvWatchEngine.RS_MIN_WINDOW
				&& !TvWatchEngine.rsHealthyForDecode(rsGoodWindow, rsWindow))
			return "waiting_rs_health";
		if (frames > 0)
			return "picture";
		MpegTsPlayer.Stats f = ffmpeg;
		MpegTsProbe.Snapshot ts = f.ts;
		if (!f.started)
			return "ffmpeg_not_started";
		if (f.launchFailed)
			return "ffmpeg_missing";
		if (f.writeErrors > 0 && !f.videoAlive)
			return "ffmpeg_stdin_dead";
		if (!f.videoAlive)
			return "ffmpeg_exited";
		if (f.tsBytesOffered == 0 && f.tsBytesWritten == 0)
			return "ffmpeg_starved";
		if (f.tsBytesOffered > 0 && f.tsBytesWritten == 0)
			return "ffmpeg_blocked";
		if (ts.pmtPid < 0)
			return "no_pmt";
		if (ts.videoPid < 0)
			return "no_video_pid";
		if (ts.videoPackets == 0)
			return "no_video_pes";
		if (f.stdoutEof && f.stdoutBytes == 0)
			return "ffmpeg_no_stdout";
		if (f.stdoutBytes > 0)
			return "ffmpeg_partial_frame";
		return "ffmpeg_waiting";
	}
}
