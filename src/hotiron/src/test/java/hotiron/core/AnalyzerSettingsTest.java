package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class AnalyzerSettingsTest {

	@Test
	void defaultsMatchTheOperatorApp() {
		AnalyzerSettings s = new AnalyzerSettings();
		assertEquals(WifiChannelPlan.WIFI_24_VIEW_START_MHZ, s.getFrequency().getValue().getStartMHz());
		assertEquals(WifiChannelPlan.WIFI_24_VIEW_END_MHZ, s.getFrequency().getValue().getEndMHz());
		assertEquals(20_000, s.getFFTBinHz().getValue());
		assertEquals(8192, s.getSamples().getValue());
		assertTrue(s.isChartsPeaksVisible().getValue());
		assertTrue(s.isPowerAutoScale().getValue());
		assertTrue(s.isAutoGain().getValue());
		assertTrue(s.isAutoSweep().getValue());
		assertTrue(s.isPersistentDisplayVisible().getValue());
		assertTrue(s.isWaterfallVisible().getValue());
		assertEquals(RadioIdentity.ABSENT, s.getRadioIdentity().getValue());
		assertEquals(McpStatus.OFF, s.getMcpStatus().getValue());
	}

	@Test
	void samplesSettingAcceptsOnlyCompleteSupportedHardwareBlocks() {
		AnalyzerSettings s = new AnalyzerSettings();
		s.getSamples().setValue(16384);
		assertEquals(16384, s.getSamples().getValue());
		assertThrows(IllegalArgumentException.class, () -> s.getSamples().setValue(10000));
		assertThrows(IllegalArgumentException.class, () -> s.getSamples().setValue(270336));
	}

	@Test
	void radioVersusDisplaySettings() {
		AnalyzerSettings s = new AnalyzerSettings();
		assertTrue(s.isRadioSetting(s.getFrequency()));
		assertTrue(s.isRadioSetting(s.getFFTBinHz()));
		assertTrue(s.isRadioSetting(s.getGainLNA()));
		assertTrue(s.isRadioSetting(s.getClkoutEnable()));
		assertFalse(s.isRadioSetting(s.isChartsPeaksVisible()));
		assertFalse(s.isRadioSetting(s.isPowerAutoScale()));
		assertFalse(s.isRadioSetting(s.isAutoGain()));
		assertFalse(s.isRadioSetting(s.isAutoSweep()));
		assertFalse(s.isRadioSetting(s.isPersistentDisplayVisible()));
		assertFalse(s.isRadioSetting(s.getSpectrumPaletteStart()));
	}

	@Test
	void hardwareHooksRestartAndReleaseWithoutOwningUsb() {
		AnalyzerSettings s = new AnalyzerSettings();
		AtomicInteger restarts = new AtomicInteger();
		AtomicInteger releases = new AtomicInteger();
		List<String> serials = new ArrayList<String>();
		serials.add("aabbccdd");
		s.setHardware(new AnalyzerSettings.Hardware()
		{
			@Override
			public void restartSweep()
			{
				restarts.incrementAndGet();
			}

			@Override
			public void releaseRadio()
			{
				releases.incrementAndGet();
			}

			@Override
			public void startListen()
			{
			}

			@Override
			public void startWatch()
			{
			}

			@Override
			public List<String> listRadioSerials()
			{
				return serials;
			}
		});
		s.releaseRadio();
		assertTrue(s.isRadioReleased().getValue());
		assertEquals(1, releases.get());
		s.restartSweep();
		assertFalse(s.isRadioReleased().getValue());
		assertEquals(1, restarts.get());
		assertEquals(List.of("aabbccdd"), s.listRadioSerials());
	}

	@Test
	void listenModeIsExclusiveWithSweepAndDoesNotReleaseUsb() {
		AnalyzerSettings s = new AnalyzerSettings();
		AtomicInteger restarts = new AtomicInteger();
		AtomicInteger listens = new AtomicInteger();
		s.setHardware(new AnalyzerSettings.Hardware()
		{
			@Override
			public void restartSweep()
			{
				restarts.incrementAndGet();
			}

			@Override
			public void releaseRadio()
			{
			}

			@Override
			public void startListen()
			{
				listens.incrementAndGet();
			}

			@Override
			public void startWatch()
			{
			}

			@Override
			public List<String> listRadioSerials()
			{
				return List.of();
			}
		});
		assertEquals(RadioMode.SWEEP, s.radioMode());
		assertEquals(97300, s.getListenKHz().getValue());
		s.startListen();
		assertEquals(RadioMode.LISTEN, s.radioMode());
		assertTrue(s.isListening().getValue());
		assertFalse(s.isRadioReleased().getValue());
		assertEquals(1, listens.get());
		s.stopListen();
		assertEquals(RadioMode.SWEEP, s.radioMode());
		assertFalse(s.isListening().getValue());
		assertEquals(1, restarts.get());
		s.startListen();
		s.releaseRadio();
		assertEquals(RadioMode.STOPPED, s.radioMode());
		assertFalse(s.isListening().getValue());
		s.restartSweep();
		assertEquals(RadioMode.SWEEP, s.radioMode());
		assertFalse(s.isRadioSetting(s.isListening()));
		assertFalse(s.isRadioSetting(s.getListenVolume()));
		s.startWatch();
		assertEquals(RadioMode.WATCH, s.radioMode());
		assertEquals(ListenService.TV, s.getListenService().getValue());
		assertEquals(33, s.getTvChannel().getValue());
		s.startSniff();
		assertEquals(RadioMode.NFC, s.radioMode());
		assertEquals(ListenService.NFC, s.getListenService().getValue());
		assertEquals(NfcBandPlan.VIEW_START_MHZ, s.getFrequency().getValue().getStartMHz());
		assertEquals(NfcBandPlan.VIEW_END_MHZ, s.getFrequency().getValue().getEndMHz());
		assertTrue(s.isListening().getValue());
	}

	@Test
	void fmScanQsyzTheBandAndStopsListen() {
		AnalyzerSettings s = new AnalyzerSettings();
		AtomicInteger restarts = new AtomicInteger();
		s.setHardware(new AnalyzerSettings.Hardware()
		{
			@Override
			public void restartSweep()
			{
				restarts.incrementAndGet();
			}

			@Override
			public void releaseRadio()
			{
			}

			@Override
			public void startListen()
			{
			}

			@Override
			public void startWatch()
			{
			}

			@Override
			public List<String> listRadioSerials()
			{
				return List.of();
			}
		});
		s.getDetectedFmStations().setValue(List.of(
				new FmStationHit(FmChannelPlan.nearest(97.3), -30f)));
		s.startListen();
		s.startFmScan();
		assertEquals(BandScan.FM, s.getBandScan().getValue());
		assertFalse(s.isListening().getValue());
		assertEquals(FmChannelPlan.VIEW_START_MHZ, s.getFrequency().getValue().getStartMHz());
		assertEquals(FmChannelPlan.VIEW_END_MHZ, s.getFrequency().getValue().getEndMHz());
		assertTrue(s.getDetectedFmStations().getValue().isEmpty(), "Scan rebuilds the Seek list");
		assertEquals(1, restarts.get(), "leave Listen before the FM survey");
		s.startFmScan();
		assertEquals(BandScan.OFF, s.getBandScan().getValue());
	}

	@Test
	void tvScanStartsOnVhfAndClearsSeekList() {
		AnalyzerSettings s = new AnalyzerSettings();
		s.getDetectedTvStations().setValue(List.of(
				new TvStationHit(TvChannelPlan.findByFccChannel(33), -40f)));
		s.startTvScan();
		assertEquals(BandScan.TV, s.getBandScan().getValue());
		assertEquals(TvChannelPlan.VHF_VIEW_START_MHZ, s.getFrequency().getValue().getStartMHz());
		assertEquals(TvChannelPlan.VHF_VIEW_END_MHZ, s.getFrequency().getValue().getEndMHz());
		assertTrue(s.getDetectedTvStations().getValue().isEmpty());
		s.startListen();
		assertEquals(BandScan.OFF, s.getBandScan().getValue(), "Listen cancels Scan");
	}

	@Test
	void nfcScanStartsOnPhyWindowAndTogglesOff()
	{
		AnalyzerSettings s = new AnalyzerSettings();
		s.startNfcScan();
		assertEquals(BandScan.NFC, s.getBandScan().getValue());
		assertEquals(NfcBandPlan.VIEW_START_MHZ, s.getFrequency().getValue().getStartMHz());
		assertEquals(NfcBandPlan.VIEW_END_MHZ, s.getFrequency().getValue().getEndMHz());
		s.startNfcScan();
		assertEquals(BandScan.OFF, s.getBandScan().getValue());
	}

	@Test
	void hardwareEventsReachRegisteredListeners() {
		AnalyzerSettings s = new AnalyzerSettings();
		AtomicInteger hw = new AtomicInteger();
		AtomicInteger cap = new AtomicInteger();
		s.registerListener(new HackRFSettings.HackRFEventAdapter()
		{
			@Override
			public void hardwareStatusChanged(boolean hardwareSendingData)
			{
				if (hardwareSendingData)
					hw.incrementAndGet();
			}

			@Override
			public void captureStateChanged(boolean isCapturing)
			{
				if (isCapturing)
					cap.incrementAndGet();
			}
		});
		s.fireHardwareStatusChanged(true);
		s.fireCaptureStateChanged(true);
		assertEquals(1, hw.get());
		assertEquals(1, cap.get());
	}

	@Test
	void sweepConfigIgnoresDisplayOnlyChanges() {
		AnalyzerSettings s = new AnalyzerSettings();
		SweepConfig a = SweepConfig.from(s);
		s.isPowerAutoScale().setValue(true);
		s.isChartsPeaksVisible().setValue(false);
		s.isAutoSweep().setValue(false);
		assertEquals(a, SweepConfig.from(s));
		s.getFFTBinHz().setValue(50000);
		assertNotEquals(a, SweepConfig.from(s));
	}
}
