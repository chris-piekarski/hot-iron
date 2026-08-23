package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MpegTsPlayerTest {

	@Test
	void videoCommandMapsPidAndUsesALargerProbe() {
		assertTrue(MpegTsPlayer.videoCommand(49).contains("0:i:0x31"));
		assertTrue(MpegTsPlayer.videoCommand(49).contains(MpegTsPlayer.PROBE_SIZE));
		assertFalse(MpegTsPlayer.videoCommand(-1).contains("-map"));
		assertTrue(MpegTsPlayer.audioCommand(52).contains("0:i:0x34"));
	}

	@Test
	void concealmentLinesAreDecodeSpam() {
		assertTrue(MpegTsPlayer.isDecodeSpam("concealing 4830 DC, 4830 AC, 4830 MV errors in P frame"));
		assertTrue(MpegTsPlayer.isDecodeSpam("Invalid mb type in P-frame at 19 45"));
		assertTrue(MpegTsPlayer.isDecodeSpam("Warning MVs not available"));
		assertTrue(MpegTsPlayer.isDecodeSpam("Packet corrupt (stream = 3, dts = 6023484203)."));
		assertTrue(MpegTsPlayer.isDecodeSpam("non-existing PPS 0 referenced"));
		assertTrue(MpegTsPlayer.isDecodeSpam("Invalid frame dimensions 0x0."));
		assertFalse(MpegTsPlayer.isDecodeSpam("Stream mapping:"));
	}

	@Test
	void writeTsBeforeStartIsIgnored() {
		MpegTsPlayer player = new MpegTsPlayer();
		byte[] ts = new byte[188];
		ts[0] = 0x47;
		player.writeTs(ts, ts.length);
		MpegTsPlayer.Stats s = player.stats();
		assertFalse(s.started);
		assertEquals(0, s.tsBytesOffered);
		assertEquals("", s.lastStderr);
	}

	@Test
	@Timeout(30)
	void decodesMpegTsToAFrameWhenFfmpegExists() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(ffmpegOk(), "ffmpeg on PATH");
		Path ts = Files.createTempFile("atsc-test", ".ts");
		Process gen = null;
		MpegTsPlayer player = null;
		try
		{
			gen = new ProcessBuilder("ffmpeg", "-y", "-f", "lavfi", "-i",
					"testsrc=size=640x360:rate=15", "-f", "lavfi", "-i",
					"sine=frequency=1000:sample_rate=48000", "-t", "2", "-c:v", "mpeg2video", "-b:v",
					"800k", "-g", "15", "-c:a", "mp2", "-f", "mpegts", ts.toString())
					.redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.DISCARD).start();
			assertTrue(gen.waitFor(5, TimeUnit.SECONDS), "ffmpeg mux timed out");
			assertEquals(0, gen.exitValue(), "ffmpeg muxed a TS");
			byte[] data = Files.readAllBytes(ts);
			assertTrue(data.length > 188);

			CountDownLatch frame = new CountDownLatch(1);
			player = new MpegTsPlayer();
			player.start(img -> {
				if (img != null && img.getWidth() == MpegTsPlayer.WIDTH)
					frame.countDown();
			}, pcm -> {
			});
			assertTrue(player.running());
			Thread.sleep(150);
			long deadline = System.currentTimeMillis() + 7000;
			int off = 0;
			while (frame.getCount() > 0 && System.currentTimeMillis() < deadline)
			{
				int n = Math.min(188 * 8, data.length - off);
				byte[] slice = new byte[n];
				System.arraycopy(data, off, slice, 0, n);
				player.writeTs(slice, n);
				off += n;
				if (off >= data.length)
					off = 0;
			}
			boolean got = frame.await(1, TimeUnit.SECONDS);
			MpegTsPlayer.Stats live = player.stats();
			assertTrue(live.started);
			assertTrue(live.videoAlive);
			assertTrue(live.tsBytesOffered > 0);
			assertTrue(live.tsBytesWritten > 0);
			assertTrue(live.ts.pmtPid > 0, "ffmpeg muxed a PMT");
			assertTrue(live.ts.videoPackets > 0, "video PID packets reached the player");
			player.stop();
			assertFalse(player.running());
			assertFalse(player.stats().started);
			assertTrue(got, "expected a 640x360 frame from ffmpeg");
			assertTrue(player.frames() >= 1);
		}
		finally
		{
			if (player != null)
				player.stop();
			if (gen != null && gen.isAlive())
			{
				gen.destroyForcibly();
				gen.waitFor(2, TimeUnit.SECONDS);
			}
			Files.deleteIfExists(ts);
		}
	}

	private static boolean ffmpegOk() {
		Process p = null;
		try
		{
			p = new ProcessBuilder("ffmpeg", "-version")
					.redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.DISCARD).start();
			return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
		}
		catch (Exception e)
		{
			return false;
		}
		finally
		{
			if (p != null && p.isAlive())
				p.destroyForcibly();
		}
	}
}
