package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class TvWatchEngineTest {

	@Test
	void transportBufferHoldsAWholeHackrfTransfer() {
		double complexSamples = 262144 / 2.0;
		double atscSymbolRate = 4.5e6 / 286.0 * 684.0;
		double packetsPerTransfer = complexSamples / TvChannelPlan.IQ_RATE_HZ * atscSymbolRate / 832.0;
		assertTrue(TvWatchEngine.TS_OUTPUT_PACKETS >= Math.ceil(packetsPerTransfer),
				"native output must not stop mid-transfer");
	}

	@Test
	void polarityRetryOnlyRunsBeforePacketSync() {
		assertFalse(TvWatchEngine.shouldRetryPolarity(false, 0, TvWatchEngine.POLARITY_RETRY_MS - 1));
		assertTrue(TvWatchEngine.shouldRetryPolarity(false, 0, TvWatchEngine.POLARITY_RETRY_MS));
		assertFalse(TvWatchEngine.shouldRetryPolarity(false, 12, TvWatchEngine.POLARITY_RETRY_MS));
		assertFalse(TvWatchEngine.shouldRetryPolarity(true, 0, TvWatchEngine.POLARITY_RETRY_MS * 2));
	}

	@Test
	void queueOverflowDropsBacklogAndKeepsNewestIq() throws Exception {
		TvWatchEngine engine = new TvWatchEngine();
		java.lang.reflect.Field run = TvWatchEngine.class.getDeclaredField("run");
		run.setAccessible(true);
		run.setBoolean(engine, true);
		byte[] iq = new byte[8];
		for (int i = 0; i < TvWatchEngine.QUEUE_CAP; i++)
			assertTrue(engine.offerIq(iq));
		assertTrue(engine.offerIq(iq), "newest IQ is retained after overflow");
		assertEquals(1, engine.droppedChunks());
		engine.stop();
	}

	@Test
	void startStopWithoutNativeDoesNotThrow() {
		TvWatchEngine engine = new TvWatchEngine();
		AtomicInteger frames = new AtomicInteger();
		assertDoesNotThrow(() -> engine.start(img -> frames.incrementAndGet(), new RecordingAudioSink()));
		assertTrue(engine.isRunning());
		assertFalse(engine.locked());
		engine.offerIq(new byte[16]);
		engine.offerIq(null);
		engine.setVolume(40);
		engine.setVolume(-1);
		engine.setVolume(200);
		engine.stop();
		assertFalse(engine.isRunning());
		assertEquals(0, frames.get());
	}

	@Test
	void containsPatRequiresAValidPatSectionAndCrc() {
		byte[] ts = new byte[188];
		ts[0] = 0x47;
		ts[3] = 0x10;
		ts[2] = 0x10; // PID 16, not PAT
		assertFalse(TvWatchEngine.containsPat(ts, 188));
		ts[1] = 0x40;
		ts[2] = 0x00;
		assertFalse(TvWatchEngine.containsPat(ts, 188), "PID 0 alone is not a PAT");
		ts[4] = 0x00; // pointer field
		byte[] section = { 0x00, (byte) 0xb0, 0x0d, 0x00, 0x01, (byte) 0xc1,
				0x00, 0x00, 0x00, 0x01, (byte) 0xe1, 0x00 };
		System.arraycopy(section, 0, ts, 5, section.length);
		int crc = TvWatchEngine.mpegCrc32(ts, 5, section.length);
		for (int i = 0; i < 4; i++)
			ts[5 + section.length + i] = (byte) (crc >>> (24 - 8 * i));
		assertTrue(TvWatchEngine.containsPat(ts, 188));

		byte[] first = new byte[188];
		first[0] = 0x47;
		first[1] = 0x40;
		first[3] = 0x30;
		first[4] = (byte) 174;
		first[179] = 0;
		System.arraycopy(ts, 5, first, 180, 8);
		byte[] second = new byte[188];
		second[0] = 0x47;
		second[3] = 0x11;
		System.arraycopy(ts, 13, second, 4, 8);
		TvWatchEngine.PatDetector detector = new TvWatchEngine.PatDetector();
		assertFalse(detector.accept(first, first.length));
		assertTrue(detector.accept(second, second.length), "PAT spanning two packets");

		ts[10] ^= 1;
		assertFalse(TvWatchEngine.containsPat(ts, 188), "corrupt PAT CRC");
		assertFalse(TvWatchEngine.containsPat(ts, 0));
	}

	@Test
	void spectrumListenerGetsARowFromIq() throws Exception {
		TvWatchEngine engine = new TvWatchEngine();
		java.util.concurrent.CountDownLatch row = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch frame = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.atomic.AtomicReference<java.awt.image.BufferedImage> img = new java.util.concurrent.atomic.AtomicReference<>();
		engine.setSpectrumListener(db -> {
			if (db != null && db.length == IqSpectrum.FFT_N)
				row.countDown();
		});
		engine.start(got -> {
			if (got != null && got.getWidth() == WatchPreview.WIDTH)
			{
				img.set(got);
				frame.countDown();
			}
		}, new RecordingAudioSink());
		byte[] iq = new byte[IqSpectrum.FFT_N * 2];
		for (int i = 0; i < IqSpectrum.FFT_N; i++)
			iq[2 * i] = 40;
		assertTrue(engine.offerIq(iq));
		assertTrue(row.await(2, java.util.concurrent.TimeUnit.SECONDS), "IQ FFT row");
		assertTrue(frame.await(2, java.util.concurrent.TimeUnit.SECONDS), "IQ video frame");
		assertEquals(WatchPreview.HEIGHT, img.get().getHeight());
		assertTrue(engine.previewFrames() >= 1);
		engine.stop();
	}

	@Test
	void previewKeepsUpdatingWhenDemodQueueIsFull() throws Exception {
		TvWatchEngine engine = new TvWatchEngine();
		java.util.concurrent.CountDownLatch frame = new java.util.concurrent.CountDownLatch(1);
		engine.start(got -> {
			if (got != null && got.getWidth() == WatchPreview.WIDTH)
				frame.countDown();
		}, new RecordingAudioSink());
		byte[] iq = new byte[IqSpectrum.FFT_N * 2];
		for (int i = 0; i < IqSpectrum.FFT_N; i++)
			iq[2 * i] = 40;
		for (int i = 0; i < TvWatchEngine.QUEUE_CAP + 8; i++)
			engine.offerIq(iq);
		assertTrue(frame.await(2, java.util.concurrent.TimeUnit.SECONDS), "IQ video while demod is backed up");
		engine.stop();
	}

	@Test
	void offerIqIgnoredWhenStopped() {
		TvWatchEngine engine = new TvWatchEngine();
		assertFalse(engine.offerIq(new byte[4]));
		engine.start(img -> {
		}, new RecordingAudioSink());
		assertTrue(engine.offerIq(new byte[8]));
		engine.stop();
		assertFalse(engine.offerIq(new byte[4]));
	}
}
