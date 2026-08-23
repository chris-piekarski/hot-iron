package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TvWatchDebugTest {

	@Test
	void atscStagesStayAheadOfFfmpeg() {
		assertEquals("stopped", TvWatchDebug.empty().stage());
		assertEquals("native_missing", locked(false, 0, 10, 5, 0, 0, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("waiting_iq", locked(true, 0, 10, 5, 0, 0, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("no_segment_sync",
				sample(true, 100, 0, 0, 10, 1, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("no_field_sync",
				sample(true, 100, 10, 0, 10, 1, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("waiting_rs",
				sample(true, 100, 10, 5, 0, 0, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("rs_unusable",
				sample(true, 100, 10, 5, 100, 99, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("no_pat",
				sample(true, 100, 10, 5, 100, 1, false, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("waiting_rs_health", new TvWatchDebug(1, true, true, true, true, false,
				100, 80, 20, 1000, 500, 0, 10, 64, 1_000_000, 0, 1, 0, 10, 400f, 0.5f, 1f,
				-10f, 0.8f, 1f).stage());
	}

	@Test
	void ffmpegStagesSplitAZeroFramePat() {
		assertEquals("picture", atPat(1, ffmpegWaiting()).stage());
		assertEquals("ffmpeg_not_started", atPat(0, MpegTsPlayer.Stats.empty()).stage());
		assertEquals("ffmpeg_missing", atPat(0, stats(true, true, false, 0, 0, 0, 0, 0, false,
				MpegTsProbe.Snapshot.empty())).stage());
		assertEquals("ffmpeg_stdin_dead", atPat(0, stats(true, false, false, 1, 100, 0, 3, 0, false,
				MpegTsProbe.Snapshot.empty())).stage());
		assertEquals("ffmpeg_exited", atPat(0, stats(true, false, false, 1, 100, 100, 0, 0, true,
				videoTs(10))).stage());
		assertEquals("ffmpeg_starved", atPat(0, stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE,
				0, 0, 0, 0, false, MpegTsProbe.Snapshot.empty())).stage());
		assertEquals("ffmpeg_blocked", atPat(0, stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE,
				400, 0, 0, 0, false, MpegTsProbe.Snapshot.empty())).stage());
		assertEquals("no_pmt", atPat(0, stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE, 400,
				400, 0, 0, false, MpegTsProbe.Snapshot.empty())).stage());
		assertEquals("no_video_pid", atPat(0, stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE,
				400, 400, 0, 0, false, ts(0x30, -1, 0))).stage());
		assertEquals("no_video_pes", atPat(0, stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE,
				400, 400, 0, 0, false, ts(0x30, 0x31, 0))).stage());
		assertEquals("ffmpeg_no_stdout", atPat(0, stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE,
				400, 400, 0, 0, true, videoTs(8))).stage());
		assertEquals("ffmpeg_partial_frame", atPat(0, stats(true, false, true,
				MpegTsPlayer.Stats.EXIT_NONE, 400, 400, 0, 1000, false, videoTs(8))).stage());
		assertEquals("ffmpeg_waiting", atPat(0, ffmpegWaiting()).stage());
	}

	@Test
	void withPlayerRefreshesPatAndFfmpeg() {
		TvWatchDebug d = atPat(0, MpegTsPlayer.Stats.empty());
		assertEquals("ffmpeg_not_started", d.stage());
		d = d.withPlayer(2, true, true, 0, 3, ffmpegWaiting());
		assertEquals("ffmpeg_waiting", d.stage());
		assertEquals(3, d.previewFrames);
	}

	private static TvWatchDebug atPat(int frames, MpegTsPlayer.Stats ffmpeg) {
		return sample(true, 1_000_000, 1000, 500, 100, 1, true, frames, ffmpeg);
	}

	private static TvWatchDebug locked(boolean nativeOk, long iq, long segments, long fields,
			long packets, long bad, boolean hasPat, MpegTsPlayer.Stats ffmpeg) {
		return sample(nativeOk, iq, segments, fields, packets, bad, hasPat, 0, ffmpeg);
	}

	private static TvWatchDebug sample(boolean nativeOk, long iq, long segments, long fields,
			long packets, long bad, boolean hasPat, MpegTsPlayer.Stats ffmpeg) {
		return sample(nativeOk, iq, segments, fields, packets, bad, hasPat, 0, ffmpeg);
	}

	private static TvWatchDebug sample(boolean nativeOk, long iq, long segments, long fields,
			long packets, long bad, boolean hasPat, int frames, MpegTsPlayer.Stats ffmpeg) {
		return new TvWatchDebug(1, true, nativeOk, hasPat, true, false, packets, bad,
				Math.max(0, packets - bad), segments, fields, 0, 50, 64, iq, 0, 1, frames, 10,
				400f, 0.2f, 1f, -10f, 0.8f, 1f, ffmpeg);
	}

	private static MpegTsPlayer.Stats ffmpegWaiting() {
		return stats(true, false, true, MpegTsPlayer.Stats.EXIT_NONE, 400, 400, 0, 0, false,
				videoTs(8));
	}

	private static MpegTsPlayer.Stats stats(boolean started, boolean launchFailed, boolean videoAlive,
			int exit, long offered, long written, long writeErrors, long stdout, boolean stdoutEof,
			MpegTsProbe.Snapshot ts) {
		return new MpegTsPlayer.Stats(started, launchFailed, videoAlive, false, exit, 1, 100,
				offered, written, 0, MpegTsPlayer.TS_QUEUE_CAP, 0, writeErrors, stdout, 0, stdoutEof,
				"", ts);
	}

	private static MpegTsProbe.Snapshot videoTs(long videoPackets) {
		return ts(0x30, 0x31, videoPackets);
	}

	private static MpegTsProbe.Snapshot ts(int pmtPid, int videoPid, long videoPackets) {
		return new MpegTsProbe.Snapshot(10, 0, 1, pmtPid, pmtPid >= 0 ? 1 : 0, videoPid,
				videoPid >= 0 ? 2 : 0, videoPackets, videoPackets > 0 ? 1 : 0, -1, 0, 0, 0);
	}
}
