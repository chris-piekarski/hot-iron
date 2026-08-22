package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutoGainPolicyTest {

	@Test
	void seedIsLowerOnWifiAndHigherOnFm() {
		assertEquals(32, AutoGainPolicy.seedGain(2402, 2472));
		assertEquals(48, AutoGainPolicy.seedGain(88, 108));
		assertEquals(56, AutoGainPolicy.seedGain(3, 30));
		assertEquals(40, AutoGainPolicy.seedGain(5170, 5895));
	}

	@Test
	void bandShiftIgnoresASmallZoom() {
		assertFalse(AutoGainPolicy.bandShifted(88, 108, 92, 99));
		assertTrue(AutoGainPolicy.bandShifted(88, 108, 2402, 2472));
	}

	@Test
	void clipDropsGainImmediately() {
		assertEquals(32, AutoGainPolicy.decide(40, -1f, -1f, -50f));
		assertEquals(40, AutoGainPolicy.decide(40, -6f, -6f, -50f),
				"a single −6 dBm Wi-Fi bin is not hard clip");
	}

	@Test
	void weakPeakRaisesTowardTheHoldWindow() {
		int next = AutoGainPolicy.decide(32, -70f, -70f, -85f);
		assertTrue(next >= 40, "a −70 dBm peak at 32 dB gain should step toward the target");
		assertEquals(32, AutoGainPolicy.decide(32, -30f, -30f, -70f), "already in the hold window");
	}

	@Test
	void holdWindowDoesNotChatter() {
		assertEquals(40, AutoGainPolicy.decide(40, -35f, -35f, -80f));
		assertEquals(40, AutoGainPolicy.decide(40, -20f, -20f, -70f));
	}

	@Test
	void afterRaiseBacksOffWhenThePeakDidNotFollow() {
		assertEquals(48, AutoGainPolicy.afterRaise(56, 16, 3f));
		assertEquals(56, AutoGainPolicy.afterRaise(56, 16, 15f));
		assertEquals(56, AutoGainPolicy.afterRaise(56, 16, -20f),
				"a disappeared burst is not compression");
	}

	@Test
	void rememberedBurstDoesNotDropGain() {
		assertEquals(40, AutoGainPolicy.decide(40, -35f, -10f, -70f),
				"peak-hold of a Wi-Fi packet must not yank gain down");
	}

	@Test
	void observeSkipsHopHoles() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = i % 4 == 0 ? -150f : -80f;
		ds.getSpectrumArray()[20] = -55f;
		AutoGainPolicy.Observation o = AutoGainPolicy.observe(ds, 40, 88, 108);
		assertTrue(o.usable());
		assertEquals(-55f, o.peakDbm, 0.01f);
		assertTrue(o.noiseDbm > -140f);
		assertTrue(o.noiseDbm < -70f);
	}

	@Test
	void loopSeedsOnFirstBandThenSettles() {
		AutoGainPolicy.Loop loop = new AutoGainPolicy.Loop();
		DatasetSpectrum ds = fmLike(-70f, -85f);
		AutoGainPolicy.Observation o = AutoGainPolicy.observe(ds, 32, 88, 108);
		Integer seed = loop.consider(o, 1_000L);
		assertEquals(48, seed.intValue());
		loop.markSettling(1_000L);
		assertNull(loop.consider(AutoGainPolicy.observe(ds, 48, 88, 108), 1_200L), "must wait for settle");
		Integer next = loop.consider(AutoGainPolicy.observe(ds, 48, 88, 108), 1_000L + AutoGainPolicy.SETTLE_MS + 20);
		assertNotNull(next);
		assertTrue(next.intValue() > 48, "FM at −70 dBm should keep raising after the seed");
	}

	@Test
	void loopClipOverridesSettle() {
		AutoGainPolicy.Loop loop = new AutoGainPolicy.Loop();
		loop.seedIfBandShifted(2402, 2472, 40);
		loop.markSettling(5_000L);
		DatasetSpectrum ds = fmLike(-1f, -40f);
		assertNull(loop.consider(AutoGainPolicy.observe(ds, 40, 2402, 2472), 5_100L),
				"one hard-clip frame must not restart USB");
		assertNull(loop.consider(AutoGainPolicy.observe(ds, 40, 2402, 2472), 5_130L));
		Integer next = loop.consider(AutoGainPolicy.observe(ds, 40, 2402, 2472), 5_160L);
		assertNotNull(next);
		assertTrue(next.intValue() < 40);
	}

	@Test
	void wifiPeakHoldDoesNotPumpDuringAQuietGap() {
		AutoGainPolicy.Loop loop = new AutoGainPolicy.Loop();
		assertEquals(32, loop.seedIfBandShifted(2402, 2472, 40));
		loop.markSettling(0);
		DatasetSpectrum loud = fmLike(-30f, -80f);
		assertNull(loop.consider(AutoGainPolicy.observe(loud, 32, 2402, 2472), AutoGainPolicy.SETTLE_MS + 10),
				"−30 dBm is already in the hold window");
		DatasetSpectrum quiet = fmLike(-82f, -88f);
		assertNull(loop.consider(AutoGainPolicy.observe(quiet, 32, 2402, 2472), AutoGainPolicy.SETTLE_MS + 200),
				"a 200 ms quiet gap must not raise gain after a Wi-Fi packet");
	}

	@Test
	void wifiLoudQuietLoudDoesNotOscillateAroundTheSeed() {
		AutoGainPolicy.Loop loop = new AutoGainPolicy.Loop();
		assertEquals(32, loop.seedIfBandShifted(2402, 2472, 40));
		loop.markSettling(0);
		Integer up = loop.consider(AutoGainPolicy.observe(fmLike(-70f, -85f), 32, 2402, 2472),
				AutoGainPolicy.SETTLE_MS + 10);
		assertNotNull(up);
		assertTrue(up.intValue() > 32);
		loop.markSettling(AutoGainPolicy.SETTLE_MS + 10);
		long t = AutoGainPolicy.SETTLE_MS + 10 + AutoGainPolicy.SETTLE_MS + 20;
		Integer afterQuiet = loop.consider(AutoGainPolicy.observe(fmLike(-75f, -88f), up.intValue(), 2402, 2472), t);
		assertTrue(afterQuiet == null || afterQuiet.intValue() >= up.intValue(),
				"quiet after a raise must not reverse toward the seed");
		int held = afterQuiet == null ? up.intValue() : afterQuiet.intValue();
		if (afterQuiet != null)
			loop.markSettling(t);
		assertNull(loop.consider(AutoGainPolicy.observe(fmLike(-28f, -80f), held, 2402, 2472),
				t + AutoGainPolicy.SETTLE_MS + 20), "a normal Wi-Fi packet after the raise must hold");
	}

	@Test
	void smallZoomDoesNotReseed() {
		AutoGainPolicy.Loop loop = new AutoGainPolicy.Loop();
		assertEquals(48, loop.seedIfBandShifted(88, 108, 32));
		assertNull(loop.seedIfBandShifted(92, 99, 48));
	}

	@Test
	void peakHoldDecaysTowardTheLivePeak() {
		float hold = AutoGainPolicy.decayPeakHold(-20f, -80f, AutoGainPolicy.PEAK_HOLD_HALF_LIFE_SEC);
		assertEquals(-50f, hold, 1f);
		assertEquals(-10f, AutoGainPolicy.decayPeakHold(-20f, -10f, 1.0), 0.01f);
	}

	private static DatasetSpectrum fmLike(float peak, float noise) {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = noise;
		ds.getSpectrumArray()[40] = peak;
		return ds;
	}
}
