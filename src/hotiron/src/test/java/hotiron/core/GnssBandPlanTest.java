package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class GnssBandPlanTest
{
	@Test
	void viewWindowsCoverL1FamilyAndGpsCaLobe()
	{
		FrequencyRange l1 = GnssBandPlan.viewWindow();
		assertEquals(1559, l1.getStartMHz());
		assertEquals(1610, l1.getEndMHz());
		FrequencyRange gps = GnssBandPlan.gpsL1Window();
		assertEquals(1573, gps.getStartMHz());
		assertEquals(1578, gps.getEndMHz());
		assertTrue(GnssBandPlan.GPS_L1.overlaps(gps.getStartMHz(), gps.getEndMHz()));
		assertTrue(GnssBandPlan.BDS_B1I.overlaps(l1.getStartMHz(), l1.getEndMHz()));
		assertTrue(GnssBandPlan.GLO_L1.overlaps(l1.getStartMHz(), l1.getEndMHz()));
	}

	@Test
	void viewIsGnssOnL1NotAWideSurvey()
	{
		assertTrue(GnssBandPlan.viewIsGnss(1559, 1610));
		assertTrue(GnssBandPlan.viewIsGnss(1573, 1578));
		assertFalse(GnssBandPlan.viewIsGnss(1, 7250));
		assertFalse(GnssBandPlan.viewIsGnss(88, 108));
		assertFalse(GnssBandPlan.viewIsGnss(2402, 2472));
	}

	@Test
	void labelsMatchConstellationCenters()
	{
		assertEquals("GPS", GnssBandPlan.labelForPeak(1575.42, 1559, 1610));
		assertEquals("BDS", GnssBandPlan.labelForPeak(1561.1, 1559, 1610));
		assertEquals("GLO", GnssBandPlan.labelForPeak(1602, 1559, 1610));
		assertNull(GnssBandPlan.labelForPeak(1575.42, 1, 7250));
		assertNull(GnssBandPlan.labelForPeak(97.3, 88, 108));
	}

	@Test
	void observeFindsAGpsL1Brick()
	{
		DatasetSpectrum ds = gnssSpectrum();
		assertFalse(GnssBandPlan.observe(ds, 1559, 1610).any());
		brick(ds, GnssBandPlan.GPS_L1, -70f);
		GnssBandPlan.GnssObservation obs = GnssBandPlan.observe(ds, 1559, 1610);
		assertTrue(obs.gpsL1);
		assertFalse(obs.bdsB1);
		assertFalse(obs.gloL1);
	}

	@Test
	void visibleSignalsOnGpsWindowAreGpsOnly()
	{
		List<GnssBandPlan.GnssSignal> gps = GnssBandPlan.visibleSignals(1573, 1578);
		assertEquals(1, gps.size());
		assertEquals(GnssBandPlan.GPS_L1, gps.get(0));
		assertTrue(GnssBandPlan.visibleSignals(1559, 1610).size() >= 3);
		assertTrue(GnssBandPlan.visibleSignals(3, 30).isEmpty());
	}

	static DatasetSpectrum gnssSpectrum()
	{
		return new DatasetSpectrum(50_000f, 1559, 1610, -90f);
	}

	static void brick(DatasetSpectrum ds, GnssBandPlan.GnssSignal s, float dbm)
	{
		long lo = Math.round(s.lowMHz() * 1_000_000d);
		long hi = Math.round(s.highMHz() * 1_000_000d);
		for (int i = 0; i < ds.spectrumLength(); i++)
		{
			double f = ds.getFrequency(i);
			if (f >= lo && f < hi)
				ds.getSpectrumArray()[i] = dbm;
		}
	}
}
