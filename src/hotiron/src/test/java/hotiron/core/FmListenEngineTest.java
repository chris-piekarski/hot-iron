package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
}
