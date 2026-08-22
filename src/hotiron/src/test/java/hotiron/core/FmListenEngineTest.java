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
}
