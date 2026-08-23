package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class SweepFramePolicyTest
{
	@Test
	void paintUsesChartFpsNotSweepRate()
	{
		assertFalse(SweepFramePolicy.shouldPaint(0, 1000 / SweepFramePolicy.CHART_FPS));
		assertTrue(SweepFramePolicy.shouldPaint(0, 1000 / SweepFramePolicy.CHART_FPS + 1));
	}

	@Test
	void axisChangeIsSameAxisAsNegated()
	{
		DatasetSpectrum a = new DatasetSpectrum(20_000f, 2402, 2472, -150f);
		DatasetSpectrum b = new DatasetSpectrum(20_000f, 2402, 2472, -150f);
		DatasetSpectrum c = new DatasetSpectrum(20_000f, 88, 108, -150f);
		assertTrue(SweepFramePolicy.axisChanged(null, a));
		assertFalse(SweepFramePolicy.axisChanged(a, b), "gain-only restart keeps waterfall history");
		assertTrue(SweepFramePolicy.axisChanged(a, c));
	}

	@Test
	void autoGainIsDisplayPolicyAndOffWhenParkedOrScanning()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		assertTrue(SweepFramePolicy.maySeedAutoGain(s, false));
		assertTrue(SweepFramePolicy.mayConsiderAutoGain(s, false));
		s.startListen();
		assertFalse(SweepFramePolicy.maySeedAutoGain(s, false));
		assertFalse(SweepFramePolicy.mayConsiderAutoGain(s, false));
		s.stopListen();
		assertFalse(SweepFramePolicy.maySeedAutoGain(s, true));
		s.isCapturingPaused().setValue(true);
		assertTrue(SweepFramePolicy.maySeedAutoGain(s, false), "pause still allows a band-shift seed");
		assertFalse(SweepFramePolicy.mayConsiderAutoGain(s, false));
		s.isCapturingPaused().setValue(false);
		s.releaseRadio();
		assertTrue(SweepFramePolicy.maySeedAutoGain(s, false));
		assertFalse(SweepFramePolicy.mayConsiderAutoGain(s, false));
		s.restartSweep();
		s.isAutoGain().setValue(false);
		assertFalse(SweepFramePolicy.maySeedAutoGain(s, false));
	}
}
