package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpectrumPowerScaleTest {

	@Test
	void emptyOrInitOnlyUsesTheDefaultWindow() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		SpectrumPowerScale scale = SpectrumPowerScale.fromDataset(ds);
		assertEquals(SpectrumPowerScale.DEFAULT_LOW, scale.lowDb, 0.01f);
		assertEquals(SpectrumPowerScale.DEFAULT_HIGH, scale.highDb, 0.01f);
	}

	@Test
	void hopHolesDoNotPullTheFloorToMinus150() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = i % 3 == 0 ? -150f : -60f;
		ds.getSpectrumArray()[10] = -35f;
		SpectrumPowerScale scale = SpectrumPowerScale.fromDataset(ds);
		assertTrue(scale.lowDb > -90f, "holes must not set the floor to −150");
		assertTrue(scale.highDb > -40f);
		assertTrue(scale.span() < 80f);
	}

	@Test
	void typicalFmPeaksFitATightWindow() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -62f;
		ds.getSpectrumArray()[40] = -38f;
		ds.getSpectrumArray()[80] = -42f;
		SpectrumPowerScale scale = SpectrumPowerScale.fromDataset(ds);
		assertTrue(scale.lowDb < -62f);
		assertTrue(scale.highDb > -38f);
		assertTrue(scale.span() >= SpectrumPowerScale.MIN_SPAN_DB);
		assertTrue(scale.span() < 80f);
	}

	@Test
	void shallowFmBandStillGetsATightDisplayWindow() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -83.5f;
		ds.getSpectrumArray()[50] = -67.5f;
		SpectrumPowerScale scale = SpectrumPowerScale.fromDataset(ds).displayTicks();
		assertTrue(scale.span() <= 60f, "16 dB FM contrast must not sit in a 70+ dB window");
		assertTrue(scale.highDb <= -50f);
		assertTrue(scale.lowDb >= -110f);
	}

	@Test
	void displayTicksSnapToTenDb() {
		SpectrumPowerScale ticks = new SpectrumPowerScale(-63.2f, -31.1f).displayTicks();
		assertEquals(0f, ticks.lowDb % 10f, 0.001f);
		assertEquals(0f, ticks.highDb % 10f, 0.001f);
		assertTrue(ticks.lowDb <= -63.2f);
		assertTrue(ticks.highDb >= -31.1f);
		assertEquals(-70f, ticks.lowDb, 0.001f);
		assertEquals(-30f, ticks.highDb, 0.001f);
	}

	@Test
	void followIgnoresSmallChatterInsideTheWindow() {
		SpectrumPowerScale held = new SpectrumPowerScale(-80f, -30f, 10_000L).displayTicks();
		SpectrumPowerScale wiggle = new SpectrumPowerScale(-78f, -32f);
		SpectrumPowerScale next = held.follow(wiggle, 10_100L);
		assertTrue(held.sameDisplayAs(next), "axis ticks must stay put for a 2 dB wiggle");
	}

	@Test
	void followHoldsThroughPeakWobbleNearATick() {
		SpectrumPowerScale held = new SpectrumPowerScale(-70f, -30f, 10_000L);
		// padded high -28 is 2 dB over the tick — peak is still 6 dB below the top
		SpectrumPowerScale wobble = new SpectrumPowerScale(-70f, -28f);
		assertTrue(held.sameDisplayAs(held.follow(wobble, 10_050L)));
		assertTrue(held.sameDisplayAs(held.follow(wobble, 14_000L)), "wobble must not expand even after the hold");
		// padded high -16 overshoots the −30 tick by more than one 10 dB step
		SpectrumPowerScale clipping = new SpectrumPowerScale(-70f, -16f);
		SpectrumPowerScale expanded = held.follow(clipping, 10_200L);
		assertEquals(-10f, expanded.highDb, 0.001f, "a peak that would clip must open to the next 10 dB tick");
	}

	@Test
	void followExpandsImmediatelyAndShrinksOneTickAfterAQuietHold() {
		SpectrumPowerScale held = new SpectrumPowerScale(-80f, -40f, 0L);
		SpectrumPowerScale hotter = new SpectrumPowerScale(-80f, -10f);
		SpectrumPowerScale expanded = held.follow(hotter, 100L);
		assertTrue(expanded.highDb >= -10f, "a clipping peak must open the top in one step");
		SpectrumPowerScale quieter = new SpectrumPowerScale(-70f, -70f);
		long hold = SpectrumPowerScale.SHRINK_HOLD_MS;
		SpectrumPowerScale duringHold = expanded.follow(quieter, 500L);
		assertTrue(expanded.sameDisplayAs(duringHold), "must not shrink during the hold");
		// Expand's peak is still in the first window — start a fresh watch, do not shrink yet.
		SpectrumPowerScale endFirstWindow = duringHold.follow(quieter, 100L + hold);
		assertTrue(expanded.sameDisplayAs(endFirstWindow));
		SpectrumPowerScale afterQuiet = endFirstWindow.follow(quieter, 100L + 2 * hold);
		assertEquals(expanded.highDb - SpectrumPowerScale.TICK_DB, afterQuiet.highDb, 0.001f);
		SpectrumPowerScale settled = endFirstWindow;
		for (int i = 1; i <= 8; i++)
			settled = settled.follow(quieter, 100L + hold + i * hold);
		assertTrue(settled.highDb < expanded.highDb - 10f);
		assertTrue(settled.span() >= SpectrumPowerScale.MIN_SPAN_DB);
	}

	@Test
	void followDoesNotShrinkIfAPeakOccurredDuringTheHold() {
		SpectrumPowerScale held = new SpectrumPowerScale(-80f, -30f, 0L);
		SpectrumPowerScale burst = new SpectrumPowerScale(-70f, -28f);
		SpectrumPowerScale quiet = new SpectrumPowerScale(-70f, -55f);
		SpectrumPowerScale afterBurst = held.follow(burst, 100L);
		assertTrue(held.sameDisplayAs(afterBurst), "a 2 dB overshoot must not expand");
		SpectrumPowerScale afterQuiet = afterBurst.follow(quiet, SpectrumPowerScale.SHRINK_HOLD_MS + 50L);
		assertTrue(held.sameDisplayAs(afterQuiet), "a burst in the window must keep the ceiling");
	}

	@Test
	void defaultWindowIsMinus100ToPlus20() {
		SpectrumPowerScale scale = SpectrumPowerScale.defaults();
		assertEquals(-100f, scale.lowDb, 0.001f);
		assertEquals(20f, scale.highDb, 0.001f);
		assertTrue(scale.isUnset());
	}
}
