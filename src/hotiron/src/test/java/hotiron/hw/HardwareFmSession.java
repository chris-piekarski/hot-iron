package hotiron.hw;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import hotiron.core.WfmDemodulator;
import hotiron.nativebridge.HackRFFmNativeBridge;

/**
 * Blocking {@code hackrf_fm_lib_start} until the first IQ callback, then stop.
 */
final class HardwareFmSession
{
	private HardwareFmSession()
	{
	}

	static int runUntilIq(long freqHz, int lna, int vga, long timeoutSec) throws Exception
	{
		HardwareSweepSession.assumeSweepReady();
		final AtomicInteger callbacks = new AtomicInteger();
		final AtomicInteger bytes = new AtomicInteger();
		final CountDownLatch first = new CountDownLatch(1);
		final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
		Thread t = new Thread(() -> {
			try
			{
				HackRFFmNativeBridge.configure(null, false);
				HackRFFmNativeBridge.start(iq -> {
					if (iq == null || iq.length < 2)
						return;
					callbacks.incrementAndGet();
					bytes.addAndGet(iq.length);
					first.countDown();
				}, freqHz, WfmDemodulator.IQ_RATE_HZ, lna, vga, false, false);
			}
			catch (Throwable e)
			{
				err.set(e);
				first.countDown();
			}
		}, "hw-fm");
		t.start();
		boolean ok = first.await(timeoutSec, TimeUnit.SECONDS);
		HackRFFmNativeBridge.stop();
		t.join(5000);
		if (err.get() != null)
			throw new RuntimeException(err.get());
		assumeTrue(ok, "no IQ callback within " + timeoutSec + "s");
		return callbacks.get();
	}
}
