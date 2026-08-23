package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutoSweepPolicyTest {

	@Test
	void zoomedInUsesFineBinsAndOneHardwareBlock() {
		assertEquals(2445, AutoSweepPolicy.chooseBinHz(1));
		assertEquals(2445, AutoSweepPolicy.chooseBinHz(4));
		assertEquals(5_000, AutoSweepPolicy.chooseBinHz(20));
		assertEquals(AutoSweepPolicy.SAMPLES, AutoSweepPolicy.choose(20, 5_000).samples);
		assertEquals(SweepSamples.SAMPLES_PER_BLOCK, AutoSweepPolicy.SAMPLES);
	}

	@Test
	void defaultWifiWindowIsTwentyKilohertz() {
		int span = WifiChannelPlan.WIFI_24_VIEW_END_MHZ - WifiChannelPlan.WIFI_24_VIEW_START_MHZ;
		assertEquals(70, span);
		assertEquals(20_000, AutoSweepPolicy.chooseBinHz(span));
		assertTrue(AutoSweepPolicy.datasetBins(span, 20_000) <= AutoSweepPolicy.TARGET_BINS);
	}

	@Test
	void wideAllScanStaysCoarseSoTheWaterfallCanMove() {
		int all = FrequencyRange.MAX_MHZ - FrequencyRange.MIN_MHZ;
		assertEquals(2_000_000, AutoSweepPolicy.chooseBinHz(all));
		assertTrue(AutoSweepPolicy.datasetBins(all, 2_000_000) <= AutoSweepPolicy.TARGET_BINS);
		assertTrue(AutoSweepPolicy.datasetBins(all, 100_000) > AutoSweepPolicy.KEEP_MAX_BINS,
				"100 kHz on All would thrash the plot");
	}

	@Test
	void typicalPresetsStayInsideTheTargetBand() {
		assertEquals(5_000, AutoSweepPolicy.chooseBinHz(108 - 88));
		assertEquals(10_000, AutoSweepPolicy.chooseBinHz(30 - 3));
		assertEquals(100_000, AutoSweepPolicy.chooseBinHz(300 - 30));
		assertEquals(1_000_000, AutoSweepPolicy.chooseBinHz(3000 - 300));
		assertEquals(200_000, AutoSweepPolicy.chooseBinHz(2200 - 1695));
	}

	@Test
	void hysteresisIgnoresASmallSpanNudge() {
		int fm = AutoSweepPolicy.chooseBinHz(20);
		assertEquals(5_000, fm);
		assertEquals(5_000, AutoSweepPolicy.chooseBinHz(22, fm), "a 2 MHz pan must not retune FFT");
		assertEquals(5_000, AutoSweepPolicy.chooseBinHz(18, fm));
	}

	@Test
	void aRealZoomOutCoarsensAndARealZoomInRefines() {
		int fm = AutoSweepPolicy.chooseBinHz(20);
		assertEquals(10_000, AutoSweepPolicy.chooseBinHz(40, fm));
		int wifi = AutoSweepPolicy.chooseBinHz(70);
		assertEquals(20_000, wifi);
		assertEquals(20_000, AutoSweepPolicy.chooseBinHz(35, wifi), "first 2× zoom stays put");
		assertEquals(5_000, AutoSweepPolicy.chooseBinHz(17, wifi), "crossing KEEP_MIN picks a fresh fine bin");
	}

	@Test
	void unknownCurrentBinTakesAFreshPick() {
		assertEquals(20_000, AutoSweepPolicy.chooseBinHz(70, 12345));
	}

	@Test
	void binLabelsMatchTheOperatorSpinner() {
		assertEquals("2 445", AutoSweepPolicy.binLabel(2445));
		assertEquals("100 000", AutoSweepPolicy.binLabel(100_000));
		assertEquals("1 000 000", AutoSweepPolicy.binLabel(1_000_000));
		String[] labels = AutoSweepPolicy.binLabels();
		assertEquals(AutoSweepPolicy.BIN_HZ.length, labels.length);
		assertEquals("5 000 000", labels[labels.length - 1]);
	}

	@Test
	void rangeNullAndEmptySpanStillPickTheFloor() {
		assertEquals(2445, AutoSweepPolicy.choose(null, 100_000).fftBinHz);
		assertEquals(2445, AutoSweepPolicy.choose(new FrequencyRange(88, 88), 100_000).fftBinHz);
	}

	@Test
	void datasetLengthMatchesDatasetSpectrum() {
		DatasetSpectrum wifi = new DatasetSpectrum(20_000f, 2402, 2472, -150f);
		assertEquals(wifi.spectrumLength(), AutoSweepPolicy.datasetBins(70, 20_000));
		DatasetSpectrum fm = new DatasetSpectrum(5_000f, 88, 108, -150f);
		assertEquals(fm.spectrumLength(), AutoSweepPolicy.datasetBins(20, 5_000));
	}

	@Test
	void hysteresisBandIsInclusiveAtBothEdges() {
		assertEquals(5_000, AutoSweepPolicy.chooseBinHz(30, 5_000), "KEEP_MAX 6000 bins must stay");
		assertEquals(10_000, AutoSweepPolicy.chooseBinHz(31, 5_000), "past KEEP_MAX coarsens");
		assertEquals(20_000, AutoSweepPolicy.chooseBinHz(30, 20_000), "KEEP_MIN 1500 bins must stay");
		assertEquals(10_000, AutoSweepPolicy.chooseBinHz(29, 20_000), "under KEEP_MIN refines");
	}

	@Test
	void everySpinnerLabelRoundTripsToAListBin() {
		String[] labels = AutoSweepPolicy.binLabels();
		for (int i = 0; i < AutoSweepPolicy.BIN_HZ.length; i++)
		{
			assertEquals(AutoSweepPolicy.BIN_HZ[i],
					Integer.parseInt(labels[i].replace(" ", "")));
			assertTrue(AutoSweepPolicy.isBinChoice(AutoSweepPolicy.BIN_HZ[i]));
		}
		assertFalse(AutoSweepPolicy.isBinChoice(12345));
		assertFalse(AutoSweepPolicy.isBinChoice(0));
	}

	@Test
	void choiceMatchesAndRejectsPartialSampleBlocks() {
		AutoSweepPolicy.Choice c = new AutoSweepPolicy.Choice(20_000, 8192);
		assertTrue(c.matches(20_000, 8192));
		assertFalse(c.matches(10_000, 8192));
		assertFalse(c.matches(20_000, 16384));
		assertThrows(IllegalArgumentException.class, () -> new AutoSweepPolicy.Choice(20_000, 10000));
	}

	@Test
	void applyWritesRadioSettingsWithoutClearingAuto() {
		AnalyzerSettings s = new AnalyzerSettings();
		assertFalse(AutoSweepPolicy.apply(s, s.getFrequency().getValue()), "default Wi-Fi is already the auto pick");
		assertEquals(20_000, s.getFFTBinHz().getValue());

		s.getSamples().setValue(262144);
		assertTrue(AutoSweepPolicy.apply(s, s.getFrequency().getValue()), "auto pins dwell to one hardware block");
		assertEquals(8192, s.getSamples().getValue());
		assertEquals(20_000, s.getFFTBinHz().getValue());
		assertTrue(s.isAutoSweep().getValue());

		FrequencyRange all = new FrequencyRange(FrequencyRange.MIN_MHZ, FrequencyRange.MAX_MHZ);
		assertTrue(AutoSweepPolicy.apply(s, all));
		assertEquals(2_000_000, s.getFFTBinHz().getValue());
		assertEquals(8192, s.getSamples().getValue());
		assertTrue(s.isAutoSweep().getValue(), "apply is not a manual override");
	}

	@Test
	void applyNoopsWhenAutoIsOffOrRadioIsParked() {
		AnalyzerSettings s = new AnalyzerSettings();
		s.isAutoSweep().setValue(false);
		s.getFrequency().setValue(new FrequencyRange(1, 7250));
		assertFalse(AutoSweepPolicy.apply(s, s.getFrequency().getValue()));
		assertEquals(20_000, s.getFFTBinHz().getValue(), "manual override must keep the armed bin");

		s.isAutoSweep().setValue(true);
		s.startListen();
		assertFalse(AutoSweepPolicy.apply(s, new FrequencyRange(88, 108)));
		assertEquals(20_000, s.getFFTBinHz().getValue());
		assertTrue(s.isListening().getValue());
	}

	@Test
	void applyPanDoesNotRetuneFft() {
		AnalyzerSettings s = new AnalyzerSettings();
		FrequencyRange fm = new FrequencyRange(88, 108);
		assertTrue(AutoSweepPolicy.apply(s, fm));
		assertEquals(5_000, s.getFFTBinHz().getValue());
		assertFalse(AutoSweepPolicy.apply(s, new FrequencyRange(90, 110)), "same 20 MHz span after a pan");
		assertEquals(5_000, s.getFFTBinHz().getValue());
	}

	@Test
	void applyNullsAreNoops() {
		assertFalse(AutoSweepPolicy.apply(null, new FrequencyRange(88, 108)));
		assertFalse(AutoSweepPolicy.apply(new AnalyzerSettings(), null));
	}
}
