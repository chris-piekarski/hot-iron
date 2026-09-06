package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class CbChannelPlanTest
{
	@Test
	void fortyFccChannelsCoverElevenMeters()
	{
		assertEquals(40, CbChannelPlan.CHANNELS.size());
		assertEquals(26965, CbChannelPlan.find(1).centerKHz);
		assertEquals(27065, CbChannelPlan.find(9).centerKHz);
		assertEquals(27185, CbChannelPlan.find(19).centerKHz);
		assertEquals(27405, CbChannelPlan.find(40).centerKHz);
		assertEquals(27255, CbChannelPlan.find(23).centerKHz);
		assertEquals(27235, CbChannelPlan.find(24).centerKHz);
		assertNull(CbChannelPlan.find(0));
		assertNull(CbChannelPlan.find(41));
	}

	@Test
	void nearestSnapsToTenKilohertzCenters()
	{
		assertEquals(19, CbChannelPlan.nearest(27.185).number);
		assertEquals(1, CbChannelPlan.nearest(26.965).number);
		assertNull(CbChannelPlan.nearest(26.995));
		assertNull(CbChannelPlan.nearest(97.3));
	}

	@Test
	void viewIsCbOnTheTwoMegahertzWindowNotHfSurvey()
	{
		assertTrue(CbChannelPlan.viewIsCb(26, 28));
		assertFalse(CbChannelPlan.viewIsCb(3, 30));
		assertFalse(CbChannelPlan.viewIsCb(88, 108));
		assertEquals("CB 19", CbChannelPlan.labelForPeak(27.185, 26, 28));
		assertNull(CbChannelPlan.labelForPeak(27.185, 3, 30));
	}

	@Test
	void detectFindsACarrierOnChannel19()
	{
		DatasetSpectrum ds = new DatasetSpectrum(2445f, 26, 28, -90f);
		assertTrue(CbChannelPlan.detect(ds, 26, 28, 8f).isEmpty());
		spike(ds, 27.185, -60f);
		List<CbChannelHit> hits = CbChannelPlan.detect(ds, 26, 28, 8f);
		assertEquals(1, hits.size());
		assertEquals(19, hits.get(0).channel.number);
	}

	static void spike(DatasetSpectrum ds, double mhz, float dbm)
	{
		double targetHz = mhz * 1_000_000d;
		int best = 0;
		double bestErr = Double.POSITIVE_INFINITY;
		for (int i = 0; i < ds.spectrumLength(); i++)
		{
			double err = Math.abs(ds.getFrequency(i) - targetHz);
			if (err < bestErr)
			{
				bestErr = err;
				best = i;
			}
		}
		ds.getSpectrumArray()[best] = dbm;
	}
}
