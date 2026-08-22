package jspectrumanalyzer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MpegTsPlayerTest {

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
			int off = 0;
			while (off < data.length)
			{
				int n = Math.min(188 * 8, data.length - off);
				byte[] slice = new byte[n];
				System.arraycopy(data, off, slice, 0, n);
				player.writeTs(slice, n);
				off += n;
			}
			boolean got = frame.await(8, TimeUnit.SECONDS);
			player.stop();
			assertFalse(player.running());
			assertTrue(got, "expected a 640x360 frame from ffmpeg");
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
