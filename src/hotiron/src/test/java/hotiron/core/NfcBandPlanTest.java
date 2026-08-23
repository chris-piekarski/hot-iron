package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class NfcBandPlanTest
{
	@Test
	void phyWindowCoversTypeAbSidebands()
	{
		FrequencyRange phy = NfcBandPlan.phyWindow();
		assertEquals(12, phy.getStartMHz());
		assertEquals(15, phy.getEndMHz());
		assertTrue(NfcBandPlan.AB_LSB.overlaps(phy.getStartMHz(), phy.getEndMHz()));
		assertTrue(NfcBandPlan.AB_USB.overlaps(phy.getStartMHz(), phy.getEndMHz()));
		assertTrue(NfcBandPlan.CARRIER.overlaps(phy.getStartMHz(), phy.getEndMHz()));
	}

	@Test
	void viewIsNfcOnlyOnTightPhyOrHarmonicWindows()
	{
		assertTrue(NfcBandPlan.viewIsNfc(12, 15));
		assertTrue(NfcBandPlan.viewIsNfc(26, 28));
		assertTrue(NfcBandPlan.viewIsNfc(40, 42));
		assertFalse(NfcBandPlan.viewIsNfc(3, 30), "HF survey is too wide");
		assertFalse(NfcBandPlan.viewIsNfc(1, 7250));
		assertFalse(NfcBandPlan.viewIsNfc(88, 108));
		assertFalse(NfcBandPlan.viewIsNfc(2402, 2472));
	}

	@Test
	void labelsMatchCarrierSidebandsAndHarmonics()
	{
		assertEquals("13.56", NfcBandPlan.labelForPeak(13.56, 12, 15));
		assertEquals("NFC-A/B", NfcBandPlan.labelForPeak(12.7125, 12, 15));
		assertEquals("NFC-A/B", NfcBandPlan.labelForPeak(14.4075, 12, 15));
		assertEquals("NFC-F", NfcBandPlan.labelForPeak(13.348, 12, 15));
		assertEquals("NFC-V", NfcBandPlan.labelForPeak(13.13625, 12, 15));
		assertEquals("NFC×2", NfcBandPlan.labelForPeak(27.12, 26, 28));
		assertEquals("NFC×3", NfcBandPlan.labelForPeak(40.68, 40, 42));
		assertNull(NfcBandPlan.labelForPeak(13.56, 3, 30));
		assertNull(NfcBandPlan.labelForPeak(97.3, 88, 108));
	}

	@Test
	void visibleFeaturesOnPhyIncludeCarrierAndAb()
	{
		List<NfcFeature> marks = NfcBandPlan.visibleFeatures(12, 15);
		assertTrue(marks.stream().anyMatch(f -> f == NfcBandPlan.CARRIER));
		assertTrue(marks.stream().anyMatch(f -> f == NfcBandPlan.AB_LSB));
		assertTrue(marks.stream().anyMatch(f -> f == NfcBandPlan.AB_USB));
		assertTrue(NfcBandPlan.visibleFeatures(26, 28).stream().anyMatch(f -> f == NfcBandPlan.H2));
		assertTrue(NfcBandPlan.visibleFeatures(3, 30).isEmpty());
	}

	@Test
	void observeFindsCarrierAndTypeAbSidebands()
	{
		DatasetSpectrum ds = nfcSpectrum();
		NfcObservation quiet = NfcBandPlan.observe(ds, 12, 15);
		assertFalse(quiet.anyEnergy());
		spike(ds, 13.56, -40f);
		NfcObservation field = NfcBandPlan.observe(ds, 12, 15);
		assertTrue(field.carrierOn);
		assertTrue(field.narrowCarrier);
		assertEquals(13.56, field.carrierMhz, 0.02);
		spike(ds, 12.7125, -55f);
		spike(ds, 14.4075, -54f);
		NfcObservation card = NfcBandPlan.observe(ds, 12, 15);
		assertTrue(card.sidebandAb);
		assertFalse(card.sidebandF);
	}

	@Test
	void observeFindsSecondHarmonicOnItsWindow()
	{
		DatasetSpectrum ds = new DatasetSpectrum(10_000f, 26, 28, -90f);
		spike(ds, 27.12, -35f);
		NfcObservation obs = NfcBandPlan.observe(ds, 26, 28);
		assertFalse(obs.carrierOn);
		assertTrue(obs.harmonic2);
		assertEquals(27.12, obs.carrierMhz, 0.02);
	}

	static DatasetSpectrum nfcSpectrum()
	{
		return new DatasetSpectrum(10_000f, 12, 15, -90f);
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
