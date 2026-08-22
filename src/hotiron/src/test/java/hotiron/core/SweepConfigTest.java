package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class SweepConfigTest {

	@Test
	void fromSettingsCapturesRadioFieldsNotDisplayOptions() {
		FakeHackRFSettings settings = new FakeHackRFSettings();
		settings.getFrequency().setValue(new FrequencyRange(88, 108));
		settings.getFFTBinHz().setValue(50_000);
		settings.getSamples().setValue(16384);
		settings.getGainLNA().setValue(16);
		settings.getGainVGA().setValue(20);
		settings.getAntennaLNA().setValue(true);
		settings.getSelectedSerial().setValue("abc");
		settings.isChartsPeaksVisible().setValue(false);
		settings.isPowerAutoScale().setValue(true);

		SweepConfig a = SweepConfig.from(settings);
		assertEquals(88, a.startMHz);
		assertEquals(108, a.endMHz);
		assertEquals(50_000, a.fftBinHz);
		assertEquals(16384, a.samples);
		assertEquals(16, a.lnaGain);
		assertEquals(20, a.vgaGain);
		assertTrue(a.antennaLna);
		assertEquals("abc", a.serial);

		SweepConfig b = SweepConfig.from(settings);
		assertEquals(a, b);
		settings.isChartsPeaksVisible().setValue(true);
		assertEquals(a, SweepConfig.from(settings), "peaks / auto-scale must not change the radio config");
		settings.getFrequency().setValue(new FrequencyRange(88, 109));
		assertNotEquals(a, SweepConfig.from(settings));
	}

	@Test
	void rejectsSamplesThatNativeSweepCannotHonor() {
		assertThrows(IllegalArgumentException.class,
				() -> new SweepConfig(88, 108, 100_000, 10000, 16, 20, false, false, false, ""));
	}

	@Test
	void shouldStartAfterStopSkipsStaleAndReleased() {
		assertTrue(SweepConfig.shouldStartAfterStop(false, false));
		assertFalse(SweepConfig.shouldStartAfterStop(true, false));
		assertFalse(SweepConfig.shouldStartAfterStop(false, true), "a newer apply is already queued");
		assertFalse(SweepConfig.shouldStartAfterStop(true, true));
	}
}
