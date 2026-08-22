package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import hotiron.core.AnalyzerSettings;
import hotiron.core.DatasetSpectrum;
import hotiron.core.FmChannelPlan;
import hotiron.core.FmStationHit;
import hotiron.core.FrequencyRange;
import hotiron.core.RadioIdentity;

class SpectrumSnapshotStoreTest {

	@Test
	void latestOverwriteAndRingCap() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore(3);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		ds.getSpectrumArray()[4] = -40f;
		for (int i = 0; i < 8; i++)
			store.publishSweep(SpectrumSnapshot.fromDataset(ds, i, 100, null), i);
		assertEquals(3, store.ringSize());
		assertEquals(7L, store.latest().timestampMs);
	}

	@Test
	void shouldPublishRespectsInterval() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		assertTrue(store.shouldPublish(1000));
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 89, -150f);
		ds.getSpectrumArray()[0] = -50f;
		store.publishSweep(SpectrumSnapshot.fromDataset(ds, 1000, 10, null), 1000);
		assertFalse(store.shouldPublish(1050));
		assertTrue(store.shouldPublish(1000 + SpectrumSnapshotStore.MIN_PUBLISH_INTERVAL_MS));
	}

	@Test
	void concurrentReadersSeeACompleteSnapshot() throws Exception {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore(8);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2400, 2410, -150f);
		ds.getSpectrumArray()[1] = -33f;
		AtomicInteger bad = new AtomicInteger();
		Thread writer = new Thread(() -> {
			for (int i = 0; i < 200; i++)
				store.publishSweep(SpectrumSnapshot.fromDataset(ds, i, 50, null), i);
		});
		List<Thread> readers = new ArrayList<Thread>();
		for (int r = 0; r < 4; r++)
		{
			readers.add(new Thread(() -> {
				for (int i = 0; i < 200; i++)
				{
					SpectrumSnapshot s = store.latest();
					if (s.mhz.length != s.dbm.length)
						bad.incrementAndGet();
				}
			}));
		}
		writer.start();
		for (Thread t : readers)
			t.start();
		writer.join();
		for (Thread t : readers)
			t.join();
		assertEquals(0, bad.get());
	}

	@Test
	void contextKeepsRadioAndDisplaySeparateAndHidesFmWhenZoomedOut() {
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getFrequency().setValue(new FrequencyRange(1, 7250));
		settings.getRadioIdentity().setValue(RadioIdentity.of("HackRF One", "aabbccddeeff0011", "v2026.01.3", "1.16"));
		settings.isPowerAutoScale().setValue(true);
		settings.isChartsPeaksVisible().setValue(false);
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f, 0.8f);
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		store.publishContext(settings, List.of(hit), 12.5);
		assertTrue(store.context().autoScale);
		assertTrue(store.context().autoGain);
		assertFalse(store.context().peaks);
		assertTrue(store.context().sweepConfigJson().contains("\"autoScale\":true"));
		assertTrue(store.context().sweepConfigJson().contains("\"autoGain\":true"));
		assertTrue(store.context().sweepConfigJson().contains("\"radio\""));
		assertTrue(store.context().sweepConfigJson().contains("radioMode"));
		assertEquals("[]", store.context().fmStationsJson());
		assertEquals("HackRF One", store.context().board);
		assertEquals("eeff0011", store.context().serial);

		settings.getFrequency().setValue(new FrequencyRange(88, 108));
		store.publishContext(settings, List.of(hit), 12.5);
		assertTrue(store.context().fmStationsJson().contains("97.3"));
	}

	@Test
	void historyUsesSummariesAndDropsADifferentAxis() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore(10);
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getGainLNA().setValue(16);
		settings.getGainVGA().setValue(8);
		store.publishContext(settings, List.of(), 10);
		DatasetSpectrum wifi = new DatasetSpectrum(100_000f, 2402, 2472, -150f);
		for (int i = 0; i < wifi.spectrumLength(); i++)
			wifi.getSpectrumArray()[i] = -80f;
		wifi.getSpectrumArray()[50] = -40f;
		for (int t = 1000; t <= 1500; t += 100)
			store.publishSweep(SpectrumSnapshot.fromDataset(wifi, t, 400, null), t);
		DatasetSpectrum fm = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < fm.spectrumLength(); i++)
			fm.getSpectrumArray()[i] = -80f;
		fm.getSpectrumArray()[10] = -50f;
		store.publishSweep(SpectrumSnapshot.fromDataset(fm, 2000, 400, null), 2000);
		String hist = store.historyJson(5.0, 50);
		assertTrue(hist.contains("\"startMHz\":88"));
		assertFalse(hist.contains("2402"), "Wi-Fi samples must not stitch onto FM");
		assertTrue(hist.contains("\"sampleCount\":1"));
		assertTrue(hist.contains("occupiedFraction"));
		assertTrue(hist.contains("lnaGain"));
		assertTrue(hist.contains("16"));
	}

	@Test
	void historyHonorsMaxSamplesAndTimeWindow() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore(20);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		ds.getSpectrumArray()[5] = -40f;
		for (int t = 0; t < 10; t++)
			store.publishSweep(SpectrumSnapshot.fromDataset(ds, t * 1000L, 200, null), t * 1000L);
		String few = store.historyJson(30.0, 3);
		assertTrue(few.contains("\"sampleCount\":3"));
		String recent = store.historyJson(1.5, 50);
		assertTrue(recent.contains("\"sampleCount\":2") || recent.contains("\"sampleCount\":1"));
	}
}
