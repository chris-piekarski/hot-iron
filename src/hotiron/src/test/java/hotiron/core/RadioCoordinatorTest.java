package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class RadioCoordinatorTest
{
	private static final class UsbCount implements RadioCoordinator.Usb
	{
		final AtomicInteger now = new AtomicInteger();
		final AtomicInteger debounce = new AtomicInteger();

		@Override
		public void applyNow()
		{
			now.incrementAndGet();
		}

		@Override
		public void applyDebounced()
		{
			debounce.incrementAndGet();
		}
	}

	private static RadioCoordinator bind(FakeHackRFSettings s, UsbCount usb)
	{
		RadioCoordinator radio = new RadioCoordinator(s, usb, new AutoGainPolicy.Loop());
		radio.bind();
		return radio;
	}

	@Test
	void operatorFftTurnsAutoSweepOffAndAppliesNow()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		assertTrue(s.isAutoSweep().getValue());
		s.getFFTBinHz().setValue(50_000);
		assertFalse(s.isAutoSweep().getValue(), "manual FFT is an operator override");
		assertEquals(1, usb.now.get());
		assertEquals(0, usb.debounce.get());
	}

	@Test
	void operatorSamplesTurnsAutoSweepOffAndAppliesNow()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.getSamples().setValue(16_384);
		assertFalse(s.isAutoSweep().getValue());
		assertEquals(16_384, s.getSamples().getValue());
		assertEquals(1, usb.now.get());
	}

	@Test
	void autoSweepWriteDoesNotClearAutoAndCoalescesUsb()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		s.getFrequency().setValue(new FrequencyRange(1, 7250));
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		assertTrue(radio.applyAutoSweep(s.getFrequency().getValue(), false));
		assertTrue(s.isAutoSweep().getValue(), "Auto policy must not look like a spinner");
		assertEquals(2_000_000, s.getFFTBinHz().getValue());
		assertEquals(8192, s.getSamples().getValue());
		assertEquals(0, usb.now.get(), "FFT/samples listeners coalesce while Auto writes");
		assertEquals(0, usb.debounce.get());
	}

	@Test
	void autoSweepWithRestartAppliesOnceAfterTheWrite()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		s.isAutoSweep().setValue(false);
		s.getSamples().setValue(16_384);
		usb.now.set(0);
		s.isAutoSweep().setValue(true);
		assertEquals(8192, s.getSamples().getValue());
		assertTrue(s.isAutoSweep().getValue());
		assertEquals(1, usb.now.get(), "enable Auto restarts once, not once per bin/samples listener");
	}

	@Test
	void frequencyWhileSweepingDebouncesAndMayRewriteFft()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.getFrequency().setValue(new FrequencyRange(1, 7250));
		assertTrue(s.isAutoSweep().getValue());
		assertEquals(2_000_000, s.getFFTBinHz().getValue());
		assertEquals(0, usb.now.get(), "span change must not also fire FFT applyNow");
		assertEquals(1, usb.debounce.get());
	}

	@Test
	void frequencyWhileListeningDoesNotApplyUsb()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.startListen();
		s.getFrequency().setValue(new FrequencyRange(88, 108));
		assertEquals(0, usb.now.get());
		assertEquals(0, usb.debounce.get());
		assertEquals(20_000, s.getFFTBinHz().getValue(), "parked RX keeps the last sweep FFT");
	}

	@Test
	void listenKhzRetunesOnlyWhenFmIsParked()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.getListenKHz().setValue(95_100);
		assertEquals(0, usb.now.get(), "dial is display until Listen");
		s.startListen();
		s.getListenKHz().setValue(97_300);
		assertEquals(1, usb.now.get());
		s.startWatch();
		usb.now.set(0);
		s.getListenKHz().setValue(95_100);
		assertEquals(0, usb.now.get(), "Watch ignores FM dial");
	}

	@Test
	void tvChannelRetunesOnlyWhenWatchIsParked()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.getTvChannel().setValue(28);
		assertEquals(0, usb.now.get());
		s.startWatch();
		s.getTvChannel().setValue(33);
		assertEquals(1, usb.now.get());
		s.startListen();
		usb.now.set(0);
		s.getTvChannel().setValue(14);
		assertEquals(0, usb.now.get());
	}

	@Test
	void operatorGainTurnsAutoGainOffAndSplitsLnaVga()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = new RadioCoordinator(s, usb, new AutoGainPolicy.Loop());
		radio.recalculateGains(40);
		radio.bind();
		s.getGain().setValue(24);
		assertFalse(s.isAutoGain().getValue());
		assertEquals(GainPolicy.lnaGain(24), s.getGainLNA().getValue());
		assertEquals(GainPolicy.vgaGain(24), s.getGainVGA().getValue());
		assertTrue(usb.now.get() >= 1);
	}

	@Test
	void autoGainWriteDoesNotClearAuto()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		radio.applyAutoGain(32, true);
		assertTrue(s.isAutoGain().getValue());
		assertEquals(32, s.getGain().getValue());
		assertEquals(GainPolicy.lnaGain(32), s.getGainLNA().getValue());
		assertEquals(1, usb.now.get());
	}

	@Test
	void autoGainSeedCoalescesWithFrequencyApply()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		radio.applyAutoGain(48, false);
		assertTrue(s.isAutoGain().getValue());
		assertEquals(48, s.getGain().getValue());
		assertEquals(0, usb.now.get());
	}

	@Test
	void operatorLnaTurnsAutoGainOff()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = new RadioCoordinator(s, usb, new AutoGainPolicy.Loop());
		radio.recalculateGains(40);
		radio.bind();
		usb.now.set(0);
		s.getGainLNA().setValue(16);
		assertFalse(s.isAutoGain().getValue());
		assertEquals(16, s.getGain().getValue().intValue());
		assertEquals(1, usb.now.get());
	}

	@Test
	void releasedRadioIgnoresApply()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.releaseRadio();
		s.getAntennaLNA().setValue(true);
		s.getFFTBinHz().setValue(50_000);
		s.getFrequency().setValue(new FrequencyRange(88, 108));
		assertEquals(0, usb.now.get());
		assertEquals(0, usb.debounce.get());
	}

	@Test
	void antennaAndClkoutApplyNowWhileSweeping()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		bind(s, usb);
		s.getAntennaPowerEnable().setValue(true);
		s.getClkoutEnable().setValue(true);
		assertEquals(2, usb.now.get());
	}

	@Test
	void maybeSeedWritesGainWithoutUsbAndSkipsWhenParked()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		radio.maybeSeedAutoGain(s.getFrequency().getValue(), false, 1_000L);
		assertTrue(s.isAutoGain().getValue());
		assertEquals(AutoGainPolicy.seedGain(s.getFrequency().getValue().getStartMHz(),
				s.getFrequency().getValue().getEndMHz()), s.getGain().getValue());
		assertEquals(0, usb.now.get());
		s.startListen();
		int gain = s.getGain().getValue();
		radio.maybeSeedAutoGain(new FrequencyRange(88, 108), false, 2_000L);
		assertEquals(gain, s.getGain().getValue());
	}

	@Test
	void considerAutoGainSkippedWhenListeningOrScanning()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -80f;
		ds.getSpectrumArray()[10] = -10f;
		s.startListen();
		radio.considerAutoGain(ds, new FrequencyRange(88, 108), false, 10_000L);
		assertEquals(0, usb.now.get());
		s.stopListen();
		int gain = s.getGain().getValue();
		radio.considerAutoGain(ds, new FrequencyRange(88, 108), true, 10_000L);
		assertEquals(gain, s.getGain().getValue(), "scan must not AGC");
	}

	@Test
	void bindIsIdempotent()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		radio.bind();
		s.getFFTBinHz().setValue(50_000);
		assertEquals(1, usb.now.get(), "second bind must not double listeners");
	}

	@Test
	void reenablingAutoGainSeedsAndApplies()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = new RadioCoordinator(s, usb, new AutoGainPolicy.Loop());
		radio.recalculateGains(40);
		radio.bind();
		s.getGain().setValue(24);
		assertFalse(s.isAutoGain().getValue());
		usb.now.set(0);
		s.isAutoGain().setValue(true);
		assertTrue(s.isAutoGain().getValue());
		int seed = AutoGainPolicy.seedGain(s.getFrequency().getValue().getStartMHz(),
				s.getFrequency().getValue().getEndMHz());
		assertEquals(seed, s.getGain().getValue());
		assertEquals(1, usb.now.get());
		assertEquals(RadioCoordinator.Source.OPERATOR, radio.source());
	}

	@Test
	void autoApplyRestoresOperatorSource()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		s.getFrequency().setValue(new FrequencyRange(1, 7250));
		UsbCount usb = new UsbCount();
		RadioCoordinator radio = bind(s, usb);
		assertEquals(RadioCoordinator.Source.OPERATOR, radio.source());
		radio.applyAutoSweep(s.getFrequency().getValue(), false);
		assertEquals(RadioCoordinator.Source.OPERATOR, radio.source());
		assertTrue(s.isAutoSweep().getValue());
		radio.applyAutoGain(32, true);
		assertEquals(RadioCoordinator.Source.OPERATOR, radio.source());
		assertTrue(s.isAutoGain().getValue());
	}
}
