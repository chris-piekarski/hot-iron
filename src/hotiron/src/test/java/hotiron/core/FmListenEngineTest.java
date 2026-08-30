package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class FmListenEngineTest
{
	@Test
	void publishesRfSpectrumFromTheSameQueuedIq() throws Exception
	{
		FmListenEngine engine = new FmListenEngine();
		CountDownLatch frame = new CountDownLatch(1);
		AtomicReference<float[]> got = new AtomicReference<>();
		engine.setRfSpectrumListener(row -> {
			got.set(row);
			frame.countDown();
		});
		engine.setSettleMs(0);
		engine.start(new RecordingAudioSink());
		try
		{
			byte[] iq = new byte[IqSpectrum.FFT_N * 2];
			for (int i = 0; i < IqSpectrum.FFT_N; i++)
			{
				double phase = 2 * Math.PI * 500_000 * i / WfmDemodulator.IQ_RATE_HZ;
				iq[2 * i] = (byte) Math.round(100 * Math.cos(phase));
				iq[2 * i + 1] = (byte) Math.round(100 * Math.sin(phase));
			}
			assertTrue(engine.offerIq(iq));
			assertTrue(frame.await(2, TimeUnit.SECONDS));
			assertNotNull(got.get());
			assertEquals(IqSpectrum.FFT_N, got.get().length);
		}
		finally
		{
			engine.stop();
		}
	}

	@Test
	void settleDropsAudioThenArmsTheDemod() throws Exception
	{
		RecordingAudioSink sink = new RecordingAudioSink();
		FmListenEngine engine = new FmListenEngine();
		engine.setSettleMs(80);
		engine.setVolume(100);
		engine.start(sink);
		try
		{
			byte[] iq = WfmDemodulatorTest.modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.04);
			assertTrue(engine.offerIq(iq));
			Thread.sleep(30);
			assertEquals(0, sink.size(), "PLL-settle window must not play the first IQ");
			Thread.sleep(80);
			assertTrue(engine.offerIq(iq));
			long deadline = System.currentTimeMillis() + 1000;
			while (sink.size() < 200 && System.currentTimeMillis() < deadline)
				Thread.sleep(20);
			assertTrue(sink.size() > 200, "audio should start after settle, got " + sink.size());
		}
		finally
		{
			engine.stop();
		}
	}

	@Test
	void pcmDoesNotWaitOnABlockingSpectrumListener() throws Exception
	{
		CountDownLatch entered = new CountDownLatch(1);
		FmListenEngine engine = new FmListenEngine();
		engine.setSettleMs(0);
		engine.setVolume(100);
		engine.setRfSpectrumListener(row -> {
			entered.countDown();
			try
			{
				Thread.sleep(400);
			}
			catch (InterruptedException ignored)
			{
				Thread.currentThread().interrupt();
			}
		});
		RecordingAudioSink sink = new RecordingAudioSink();
		engine.start(sink);
		try
		{
			byte[] iq = WfmDemodulatorTest.modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.04);
			int chunk = 262144;
			for (int off = 0; off < iq.length; )
			{
				int n = Math.min(chunk, iq.length - off);
				if ((n & 1) == 1)
					n--;
				byte[] part = new byte[n];
				System.arraycopy(iq, off, part, 0, n);
				assertTrue(engine.offerIq(part));
				off += n;
			}
			assertTrue(entered.await(2, TimeUnit.SECONDS), "RF listener should still run");
			long deadline = System.currentTimeMillis() + 500;
			while (sink.size() < 200 && System.currentTimeMillis() < deadline)
				Thread.sleep(20);
			assertTrue(sink.size() > 200,
					"PCM must not wait on the RF listener, got " + sink.size());
		}
		finally
		{
			engine.stop();
		}
	}

	@Test
	void publishesALiveSignalLevelFromDemodPcm() throws Exception
	{
		FmListenEngine engine = new FmListenEngine();
		CountDownLatch got = new CountDownLatch(1);
		AtomicReference<Double> level = new AtomicReference<>();
		engine.setSettleMs(0);
		engine.setVolume(100);
		engine.setLevelListener(v -> {
			level.set(Double.valueOf(v));
			if (v > 0.05)
				got.countDown();
		});
		engine.start(new RecordingAudioSink());
		try
		{
			byte[] iq = WfmDemodulatorTest.modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.08);
			assertTrue(engine.offerIq(iq));
			assertTrue(got.await(2, TimeUnit.SECONDS), "SIG needle should see demod audio");
			assertTrue(level.get().doubleValue() > 0.05);
		}
		finally
		{
			engine.stop();
		}
	}

	@Test
	void spectrumKeepsPublishingWhenTheIqQueueIsBusy() throws Exception
	{
		AtomicInteger rf = new AtomicInteger();
		AtomicInteger audio = new AtomicInteger();
		FmListenEngine engine = new FmListenEngine();
		engine.setSettleMs(0);
		engine.setVolume(100);
		engine.setRfSpectrumListener(row -> rf.incrementAndGet());
		engine.setSpectrumListener(row -> audio.incrementAndGet());
		engine.start(new RecordingAudioSink());
		try
		{
			byte[] iq = WfmDemodulatorTest.modulate(1000, WfmDemodulator.DEVIATION_HZ, 0.08);
			int chunk = 262144;
			int offered = 0;
			for (int off = 0; off < iq.length; )
			{
				int n = Math.min(chunk, iq.length - off);
				if ((n & 1) == 1)
					n--;
				byte[] part = new byte[n];
				System.arraycopy(iq, off, part, 0, n);
				if (engine.offerIq(part))
					offered++;
				off += n;
			}
			assertTrue(offered >= 2, "need a backed-up IQ queue, offered " + offered);
			long deadline = System.currentTimeMillis() + 2000;
			while ((rf.get() < 2 || audio.get() < 1) && System.currentTimeMillis() < deadline)
				Thread.sleep(20);
			assertTrue(rf.get() >= 2, "RF waterfall must keep frames when USB is ahead, got " + rf.get());
			assertTrue(audio.get() >= 1, "AUDIO waterfall must keep frames when USB is ahead, got " + audio.get());
		}
		finally
		{
			engine.stop();
		}
	}
}
