package hotiron.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class SpectrumSweepEngineTest {

	@Test
	void syntheticBinsFillQueueAndUpdateDataset() throws Exception {
		FakeHackRFSettings settings = new FakeHackRFSettings();
		settings.getFrequency().setValue(new FrequencyRange(2400, 2410));
		settings.getFFTBinHz().setValue(1_000_000);
		settings.getPeakFallRate().setValue(15);

		SpectrumSweepEngine engine = new SpectrumSweepEngine(settings);
		Thread processing = new Thread(new Runnable() {
			@Override
			public void run() {
				engine.runProcessingLoop();
			}
		}, "test-process");
		processing.start();

		float binHz = 1_000_000f;
		engine.accept(false, new double[] { 2400e6, 2401e6, 2402e6 }, binHz, new float[] { -80f, -81f, -79f });
		engine.accept(false, new double[] { 2403e6, 2404e6, 2405e6 }, binHz, new float[] { -70f, -71f, -72f });
		engine.accept(true, new double[] { 2406e6, 2407e6, 2408e6 }, binHz, new float[] { -65f, -66f, -67f });

		long deadline = System.currentTimeMillis() + 2000;
		while (System.currentTimeMillis() < deadline
				&& (engine.processedPackets() < 3 || engine.getDataset() == null)) {
			Thread.sleep(10);
		}

		assertEquals(3, engine.acceptedPackets(), "queue should have accepted every packet");
		assertEquals(0, engine.droppedPackets());
		assertTrue(engine.processedPackets() >= 2, "processing loop should consume bins after the sizing packet");
		DatasetSpectrumPeak dataset = engine.getDataset();
		assertNotNull(dataset, "datasetSpectrum should be created from the first queued packet");

		int updated = 0;
		float[] spectrum = dataset.getSpectrumArray();
		for (int i = 0; i < spectrum.length; i++) {
			if (spectrum[i] > -150f && Float.isFinite(spectrum[i]))
				updated++;
		}
		assertTrue(updated >= 6, "datasetSpectrum only updated " + updated + " bins");

		engine.requestStop();
		processing.join(1000);
	}
}
