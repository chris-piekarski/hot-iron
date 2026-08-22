package hotiron.hw;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import hotiron.nativebridge.HackRFSweepDataCallback;
import hotiron.nativebridge.HackRFSweepNativeBridge;

/**
 * Runs one blocking {@code hackrf_sweep_lib_start} on a worker thread, waits for
 * the first spectrum callback, then {@code stop}s. Used only by hardware ITs.
 */
final class HardwareSweepSession {
	static final int DEFAULT_SAMPLES = 8192;
	static final int DEFAULT_LNA_GAIN = 16;
	static final int DEFAULT_VGA_GAIN = 16;
	static final long WAIT_SEC = 12;
	static final long SETTLE_MS = 400;

	private HardwareSweepSession() {
	}

	static void assumeSweepReady() {
		File lib = HardwareConditions.findNativeLibrary();
		assumeTrue(lib != null,
				"libhackrf-sweep.so not found; run `make build` (Linux .so) then retry make test-hw");
		assumeTrue(HardwareConditions.hackrfUsbNodeWritable(),
				"HackRF usbfs node is not writable (WSL usbipd nodes are often root:root). "
						+ "Fix udev or: sudo chmod a+rw "
						+ String.valueOf(HardwareConditions.findHackrfUsbDeviceNode()));
		System.setProperty("hackrf.sweep.lib.dir", lib.getParentFile().getAbsolutePath() + File.separator);
	}

	interface SpectrumSink {
		void onSpectrum(boolean fullSweepDone, double[] frequencyStart, float fftBinWidthHz, float[] signalPowerdBm);
	}

	static Result run(int freqStartMHz, int freqEndMHz, int fftBinHz, boolean antennaPower,
			boolean antennaLna) throws Exception {
		return run(freqStartMHz, freqEndMHz, fftBinHz, DEFAULT_SAMPLES, DEFAULT_LNA_GAIN, DEFAULT_VGA_GAIN,
				antennaPower, antennaLna, WAIT_SEC, null, false);
	}

	static Result runUntilFullSweep(int freqStartMHz, int freqEndMHz, int fftBinHz, int numSamples, int lnaGain,
			int vgaGain, boolean antennaPower, boolean antennaLna, SpectrumSink sink) throws Exception {
		return run(freqStartMHz, freqEndMHz, fftBinHz, numSamples, lnaGain, vgaGain, antennaPower, antennaLna, WAIT_SEC,
				sink, true);
	}

	static Result run(int freqStartMHz, int freqEndMHz, int fftBinHz, int numSamples, int lnaGain, int vgaGain,
			boolean antennaPower, boolean antennaLna, long timeoutSec, SpectrumSink sink, boolean untilFullSweep)
			throws Exception {
		final AtomicInteger callbacks = new AtomicInteger();
		final AtomicInteger binsSeen = new AtomicInteger();
		final AtomicBoolean sawInRangeHz = new AtomicBoolean();
		final AtomicBoolean sawFinitePower = new AtomicBoolean();
		final AtomicBoolean sawFullSweep = new AtomicBoolean();
		final AtomicReference<Float> fftBinWidthHz = new AtomicReference<Float>();
		final AtomicReference<Throwable> startError = new AtomicReference<Throwable>();
		final CountDownLatch done = new CountDownLatch(1);
		final double minHz = freqStartMHz * 1_000_000d;
		final double maxHz = freqEndMHz * 1_000_000d;

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HackRFSweepNativeBridge.start(new HackRFSweepDataCallback() {
						@Override
						public void newSpectrumData(boolean sweepStarted, double[] frequencyStart, float fftBinWidth,
								float[] signalPowerdBm) {
							if (frequencyStart == null || signalPowerdBm == null || frequencyStart.length == 0)
								return;
							callbacks.incrementAndGet();
							binsSeen.addAndGet(frequencyStart.length);
							fftBinWidthHz.set(Float.valueOf(fftBinWidth));
							if (sweepStarted)
								sawFullSweep.set(true);
							for (int i = 0; i < frequencyStart.length; i++) {
								double hz = frequencyStart[i];
								if (hz >= minHz && hz <= maxHz)
									sawInRangeHz.set(true);
							}
							for (int i = 0; i < signalPowerdBm.length; i++) {
								float p = signalPowerdBm[i];
								if (Float.isFinite(p) && p > -140f && p < 20f)
									sawFinitePower.set(true);
							}
							if (sink != null)
								sink.onSpectrum(sweepStarted, frequencyStart, fftBinWidth, signalPowerdBm);
							if (!untilFullSweep || sweepStarted)
								done.countDown();
						}
					}, freqStartMHz, freqEndMHz, fftBinHz, numSamples, lnaGain, vgaGain, antennaPower, antennaLna);
				} catch (Throwable err) {
					startError.set(err);
					done.countDown();
				}
			}
		}, "hackrf-hw-sweep");

		boolean gotData = false;
		try {
			t.start();
			gotData = done.await(timeoutSec, TimeUnit.SECONDS);
		} finally {
			try {
				HackRFSweepNativeBridge.stop();
			} catch (Throwable stopErr) {
				if (startError.get() == null)
					startError.set(stopErr);
			}
			t.join(5000);
			Thread.sleep(SETTLE_MS);
		}

		float width = fftBinWidthHz.get() == null ? 0f : fftBinWidthHz.get().floatValue();
		return new Result(gotData, callbacks.get(), binsSeen.get(), sawInRangeHz.get(), sawFinitePower.get(),
				sawFullSweep.get(), width, startError.get());
	}

	static final class Result {
		final boolean gotData;
		final int callbacks;
		final int bins;
		final boolean sawInRangeHz;
		final boolean sawFinitePower;
		final boolean sawFullSweep;
		final float fftBinWidthHz;
		final Throwable startError;

		Result(boolean gotData, int callbacks, int bins, boolean sawInRangeHz, boolean sawFinitePower,
				boolean sawFullSweep, float fftBinWidthHz, Throwable startError) {
			this.gotData = gotData;
			this.callbacks = callbacks;
			this.bins = bins;
			this.sawInRangeHz = sawInRangeHz;
			this.sawFinitePower = sawFinitePower;
			this.sawFullSweep = sawFullSweep;
			this.fftBinWidthHz = fftBinWidthHz;
			this.startError = startError;
		}

		void assertHealthy(String label) {
			if (startError != null)
				fail(label + ": hackrf_sweep_lib_start/stop failed: " + startError);
			assertTrue(gotData, label + ": no spectrum callback within timeout");
			assertTrue(callbacks >= 1, label + ": expected at least one callback, got " + callbacks);
			assertTrue(bins > 0, label + ": callback bins were empty");
			assertTrue(sawInRangeHz, label + ": freqStart values were not in requested Hz range");
			assertTrue(sawFinitePower, label + ": no finite power sample in [-140, 20] dBm");
		}
	}
}
