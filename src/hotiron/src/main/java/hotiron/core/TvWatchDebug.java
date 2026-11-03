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
	public final float clipFraction;
	public final float cfoHz;
	public final float sroPpm;
	public final float eqSnrDb;
	public final float spectrumSnrDb;
	public final float interfererDb;
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
		this(timestampMs, running, nativeAvailable, hasPat, locked, inverted, packets, badPackets,
				goodPackets, segments, fieldSegments, pendingBaseband, rsGoodWindow, rsWindow,
				totalIqSamples, droppedChunks, queueDepth, frames, previewFrames, agcGain, rmsIq,
				rmsBaseband, rsGoodRatioDb, equalizerMainTap, equalizerPeakTap, 0f, 0f, 0f, 0f,
				Float.NaN, Float.NaN, ffmpeg);
	}

	public TvWatchDebug(long timestampMs, boolean running, boolean nativeAvailable,
			boolean hasPat, boolean locked, boolean inverted, long packets,
			long badPackets, long goodPackets, long segments, long fieldSegments,
			long pendingBaseband, long rsGoodWindow, long rsWindow, long totalIqSamples,
			long droppedChunks, int queueDepth, int frames, int previewFrames,
			float agcGain, float rmsIq, float rmsBaseband, float rsGoodRatioDb,
			float equalizerMainTap, float equalizerPeakTap, float clipFraction,
			float cfoHz, float sroPpm, float eqSnrDb, float spectrumSnrDb, float interfererDb,
			MpegTsPlayer.Stats ffmpeg)
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
		this.clipFraction = clipFraction;
		this.cfoHz = cfoHz;
		this.sroPpm = sroPpm;
		this.eqSnrDb = eqSnrDb;
		this.spectrumSnrDb = spectrumSnrDb;
		this.interfererDb = interfererDb;
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
				equalizerPeakTap, clipFraction, cfoHz, sroPpm, eqSnrDb, spectrumSnrDb,
				interfererDb, ffmpeg);
	}

	public TvWatchDebug withSpectrumMetrics(float spectrumSnrDb, float interfererDb)
	{
		return new TvWatchDebug(timestampMs, running, nativeAvailable, hasPat, locked, inverted,
				packets, badPackets, goodPackets, segments, fieldSegments, pendingBaseband,
				rsGoodWindow, rsWindow, totalIqSamples, droppedChunks, queueDepth, frames,
				previewFrames, agcGain, rmsIq, rmsBaseband, rsGoodRatioDb, equalizerMainTap,
				equalizerPeakTap, clipFraction, cfoHz, sroPpm, eqSnrDb, spectrumSnrDb,
				interfererDb, ffmpeg);
	}

	/**
	 * Reception Doctor root-cause code explaining why 8-VSB is or is not
	 * locking (overload, drops, weak/missing signal, adjacent interferer,
	 * carrier offset, or multipath).
	 */
	public String diagnosisCode()
	{
		if (!running)
			return "stopped";
		if (!nativeAvailable)
			return "native_missing";
		if (totalIqSamples == 0)
			return "waiting_iq";
		if (TvWatchGainPolicy.isClipped(rmsIq, clipFraction))
			return "clipping";
		if (!locked && droppedChunks > 0)
			return "iq_drops";
		if (!locked && rmsIq > 0f && rmsIq < TvWatchGainPolicy.MIN_RMS)
			return "weak_signal";
		if (!locked && Float.isFinite(spectrumSnrDb) && spectrumSnrDb < TvWatchGainPolicy.MIN_USABLE_SNR_DB)
			return "no_signal";
		if (!locked && Float.isFinite(interfererDb) && interfererDb >= TvWatchGainPolicy.INTERFERER_DB)
			return "interferer";
		if (!locked && Math.abs(cfoHz) > 60_000f)
			return "off_centre";
		if (!locked && segments > 0 && rsWindow >= TvWatchEngine.RS_MIN_WINDOW && rsGoodWindow == 0)
			return "multipath_or_low_snr";
		return "looks_fine";
	}

	public String diagnosisFix()
	{
		switch (diagnosisCode())
		{
		case "clipping":
		case "interferer":
			return "lower_gain";
		case "weak_signal":
			return "raise_gain";
		case "no_signal":
			return "check_antenna";
		case "off_centre":
			return "calibrate_ppm";
		case "multipath_or_low_snr":
			return "optimise_gain";
		default:
			return "none";
		}
	}

	public String diagnosisText()
	{
		switch (diagnosisCode())
		{
		case "stopped":
			return "TV Watch is stopped.";
		case "native_missing":
			return "Native ATSC 8-VSB receiver is not loaded.";
		case "waiting_iq":
			return "Waiting for IQ samples from HackRF.";
		case "clipping":
			return String.format(java.util.Locale.US,
					"ADC converter is overloaded (rms=%.2f, clip=%.2f%%); lower IF gain.",
					rmsIq, clipFraction * 100f);
		case "iq_drops":
			return "IQ chunks were dropped (" + droppedChunks + ") before 8-VSB trellis lock.";
		case "weak_signal":
			return String.format(java.util.Locale.US,
					"IF level is too low (rms=%.3f); raise gain or enable LNA.", rmsIq);
		case "no_signal":
			return String.format(java.util.Locale.US,
					"No 6 MHz ATSC channel stands above the noise floor (in-band SNR %.1f dB).",
					spectrumSnrDb);
		case "interferer":
			return String.format(java.util.Locale.US,
					"Strong out-of-channel signal (+%.1f dB) in the 16 MS/s window can swamp the receiver; lower gain.",
					interfererDb);
		case "off_centre":
			return String.format(java.util.Locale.US,
					"ATSC pilot is shifted by %+.1f kHz; check channel or crystal PPM.",
					cfoHz / 1000f);
		case "multipath_or_low_snr":
			return String.format(java.util.Locale.US,
					"Sync found but Reed-Solomon is failing (eq SNR %.1f dB); multipath echo or low SNR.",
					eqSnrDb);
		default:
			return locked ? "Decoder is locked and front-end looks healthy."
					: "Front-end looks healthy; acquiring ATSC sync.";
		}
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
