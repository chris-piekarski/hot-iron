package hotiron.hw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;

import hotiron.FakeHackRFSettings;
import hotiron.core.DatasetSpectrumPeak;
import hotiron.core.FFTBins;
import hotiron.core.FrequencyRange;
import hotiron.core.GainPolicy;
import hotiron.core.SpectrumSweepEngine;
import hotiron.core.jfc.XYSeriesImmutable;
import hotiron.nativebridge.HackRFDeviceQuery;

/**
 * Hardware / application-integration tests. Tagged {@code hardware} and named
 * {@code *IT} so default {@code make test} / Surefire does not run them.
 *
 * Covers firmware/USB API health and the analyzer data path (settings →
 * native sweep → {@link FFTBins} → {@link DatasetSpectrumPeak}).
 *
 * Run with: {@code make test-hw}
 */
@Tag("hardware")
@EnabledIf("hotiron.hw.HardwareConditions#hackrfUsbPresent")
class HackRFSweepHardwareIT {

	private static final int FREQ_START_MHZ = 2400;
	private static final int FREQ_END_MHZ = 2500;
	private static final int FFT_BIN_HZ = 1_000_000;

	@BeforeAll
	static void configureNativeLibraryPath() {
		File lib = HardwareConditions.findNativeLibrary();
		if (lib != null) {
			System.setProperty("hackrf.sweep.lib.dir", lib.getParentFile().getAbsolutePath() + File.separator);
		}
	}

	@HardwareTest
	void usbDeviceIsHackrfOne() {
		assertTrue(HardwareConditions.hackrfUsbPresent(), "HackRF USB 1d50:6089 not enumerated");
	}

	@HardwareTest
	void firmwareAndUsbApiAreSupported() {
		HardwareSweepSession.assumeSweepReady();
		HackRFDeviceQuery.Info info = HackRFDeviceQuery.query();
		if (!info.opened()) {
			int code = info.openResult != -1 ? info.openResult : info.initResult;
			fail("libhackrf open failed: " + HackRFDeviceQuery.errorName(code) + " (" + code + ")");
		}
		assertTrue(info.firmware != null && info.firmware.length() > 0, "firmware version string was empty");
		assertTrue(HackRFDeviceQuery.meetsMinimumFirmware(info.firmware, HackRFDeviceQuery.MIN_FIRMWARE),
				"firmware " + info.firmware + " is older than required " + HackRFDeviceQuery.MIN_FIRMWARE);
		assertTrue(info.usbApi >= HackRFDeviceQuery.MIN_USB_API,
				"USB API " + info.usbApiString() + " is below 1.00");
		assertTrue(HackRFDeviceQuery.isKnownHackrfBoard(info.boardId),
				"unexpected board id " + info.boardId + " (" + info.boardName + ")");
	}

	@HardwareTest
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void liveSweepFillsAnalyzerDatasetFromSettings() throws Exception {
		HardwareSweepSession.assumeSweepReady();

		final FakeHackRFSettings settings = new FakeHackRFSettings();
		settings.getFFTBinHz().setValue(500000);
		FrequencyRange range = settings.getFrequency().getValue();
		int fftBinHz = settings.getFFTBinHz().getValue();
		int lna = GainPolicy.lnaGain(settings.getGain().getValue());
		int vga = GainPolicy.vgaGain(settings.getGain().getValue());
		settings.getGainLNA().setValue(lna);
		settings.getGainVGA().setValue(vga);

		final float initPower = -150f;
		final DatasetSpectrumPeak dataset = new DatasetSpectrumPeak(fftBinHz, range.getStartMHz(), range.getEndMHz(),
				initPower, 15, settings.getPeakFallRate().getValue() * 1000L);
		final AtomicInteger hwEvents = new AtomicInteger();
		settings.registerListener(new hotiron.core.HackRFSettings.HackRFEventListener() {
			@Override
			public void captureStateChanged(boolean isCapturing) {
			}

			@Override
			public void hardwareStatusChanged(boolean hardwareSendingData) {
				if (hardwareSendingData)
					hwEvents.incrementAndGet();
			}
		});

		HardwareSweepSession.Result result = HardwareSweepSession.runUntilFullSweep(range.getStartMHz(),
				range.getEndMHz(), fftBinHz, settings.getSamples().getValue(), lna, vga,
				settings.getAntennaPowerEnable().getValue(), settings.getAntennaLNA().getValue(),
				new HardwareSweepSession.SpectrumSink() {
					@Override
					public void onSpectrum(boolean fullSweepDone, double[] frequencyStart, float fftBinWidthHz,
							float[] signalPowerdBm) {
						settings.fireHardwareStatusChanged(true);
						dataset.addNewData(new FFTBins(fullSweepDone, frequencyStart, fftBinWidthHz, signalPowerdBm));
					}
				});
		result.assertHealthy("analyzer data path");
		assertTrue(result.sawFullSweep, "did not observe a full sweep (sweepStarted/fullSweepDone)");
		assertTrue(hwEvents.get() >= 1, "HackRFSettings hardware listener was not notified");

		float[] spectrum = dataset.getSpectrumArray();
		int updated = 0;
		for (int i = 0; i < spectrum.length; i++) {
			if (spectrum[i] != initPower && Float.isFinite(spectrum[i]))
				updated++;
		}
		assertTrue(updated > 10, "DatasetSpectrumPeak only updated " + updated + " of " + spectrum.length + " bins");

		XYSeriesImmutable series = dataset.createSpectrumDataset("spectrum");
		assertEquals(spectrum.length, series.getItemCount());
		assertTrue(series.getXX(0) >= range.getStartMHz() - 1,
				"chart series start " + series.getXX(0) + " MHz outside requested range");
	}

	@HardwareTest
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void realSweepEngineFillsQueueAndDatasetSpectrum() throws Exception {
		HardwareSweepSession.assumeSweepReady();

		final FakeHackRFSettings settings = new FakeHackRFSettings();
		settings.getFFTBinHz().setValue(500000);
		int totalGain = settings.getGain().getValue();
		settings.getGainLNA().setValue(GainPolicy.lnaGain(totalGain));
		settings.getGainVGA().setValue(GainPolicy.vgaGain(totalGain));

		SpectrumSweepEngine engine = new SpectrumSweepEngine(settings);
		try {
			engine.startBackgroundThreads();
			long deadline = System.currentTimeMillis() + 15000;
			while (System.currentTimeMillis() < deadline) {
				if (engine.acceptedPackets() >= 1 && engine.processedPackets() >= 1 && engine.getDataset() != null)
					break;
				Thread.sleep(50);
			}

			assertTrue(engine.acceptedPackets() >= 1,
					"processing queue never received bins (accepted=" + engine.acceptedPackets() + ")");
			assertTrue(engine.processedPackets() >= 1,
					"processingThread never applied bins (processed=" + engine.processedPackets() + ")");
			DatasetSpectrumPeak dataset = engine.getDataset();
			assertTrue(dataset != null, "datasetSpectrum was never created");

			float init = -150f;
			int updated = 0;
			float[] spectrum = dataset.getSpectrumArray();
			for (int i = 0; i < spectrum.length; i++) {
				if (spectrum[i] != init && Float.isFinite(spectrum[i]))
					updated++;
			}
			assertTrue(updated > 10, "datasetSpectrum only updated " + updated + " of " + spectrum.length + " bins");
		} finally {
			engine.stopAndJoin(8000);
		}
	}

	@HardwareTest
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void startProducesSpectrumCallbacksThenStopAllowsRestart() throws Exception {
		HardwareSweepSession.assumeSweepReady();

		HardwareSweepSession.Result first = HardwareSweepSession.run(FREQ_START_MHZ, FREQ_END_MHZ, FFT_BIN_HZ, false,
				false);
		first.assertHealthy("first start");

		HardwareSweepSession.Result second = HardwareSweepSession.run(FREQ_START_MHZ, FREQ_END_MHZ, FFT_BIN_HZ, false,
				false);
		second.assertHealthy("restart after stop");
	}

	@HardwareTest
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void antennaPowerAndLnaStartStopDoesNotCrash() throws Exception {
		HardwareSweepSession.assumeSweepReady();

		HardwareSweepSession.Result on = HardwareSweepSession.run(FREQ_START_MHZ, FREQ_END_MHZ, FFT_BIN_HZ, true, true);
		on.assertHealthy("antenna power + LNA enabled");

		HardwareSweepSession.Result off = HardwareSweepSession.run(FREQ_START_MHZ, FREQ_END_MHZ, FFT_BIN_HZ, false,
				false);
		off.assertHealthy("antenna power + LNA disabled after stop");
	}

	@HardwareTest
	@Timeout(value = 40, unit = TimeUnit.SECONDS)
	void restartAfterFftBinAndFreqChangeStillProducesData() throws Exception {
		HardwareSweepSession.assumeSweepReady();

		HardwareSweepSession.Result wide = HardwareSweepSession.run(FREQ_START_MHZ, FREQ_END_MHZ, FFT_BIN_HZ, false,
				false);
		wide.assertHealthy("2400-2500 MHz @ 1 MHz bins");

		final int fmStartMHz = 88;
		final int fmEndMHz = 108;
		final int fineBinHz = 100_000;
		HardwareSweepSession.Result fm = HardwareSweepSession.run(fmStartMHz, fmEndMHz, fineBinHz, false, false);
		fm.assertHealthy("88-108 MHz @ 100 kHz bins after restart");

		assertTrue(fm.fftBinWidthHz > 0f, "second sweep reported no FFT bin width");
		assertTrue(Math.abs(fm.fftBinWidthHz - fineBinHz) < Math.abs(fm.fftBinWidthHz - FFT_BIN_HZ),
				"second sweep FFT bin should be nearer 100 kHz than 1 MHz, got " + fm.fftBinWidthHz);
	}

	@HardwareTest
	@Timeout(value = 40, unit = TimeUnit.SECONDS)
	void listenModeProducesIqThenSweepResumes() throws Exception {
		HardwareSweepSession.assumeSweepReady();
		long loHz = 97_300_000L - hotiron.core.WfmDemodulator.OFFSET_HZ;
		int iq = HardwareFmSession.runUntilIq(loHz, 24, 20, 12);
		assertTrue(iq >= 1, "expected at least one IQ callback while listening");
		HardwareSweepSession.Result after = HardwareSweepSession.run(FREQ_START_MHZ, FREQ_END_MHZ, FFT_BIN_HZ, false,
				false);
		after.assertHealthy("sweep after FM listen stop");
	}
}
