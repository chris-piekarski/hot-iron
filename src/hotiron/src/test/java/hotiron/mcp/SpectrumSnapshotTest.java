package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.core.DatasetSpectrum;

class SpectrumSnapshotTest {

	@Test
	void emptyDatasetIsEmpty() {
		SpectrumSnapshot snap = SpectrumSnapshot.fromDataset(null, 1L, 100, null);
		assertTrue(snap.isEmpty());
		assertTrue(snap.toJson().contains("\"points\":[]"));
	}

	@Test
	void holesAreOmittedAndPeakIsTheStrongestFilledBin() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = i % 3 == 0 ? -150f : -60f;
		ds.getSpectrumArray()[10] = -35f;
		SpectrumSnapshot snap = SpectrumSnapshot.fromDataset(ds, 42L, 10_000, null);
		assertEquals(42L, snap.timestampMs);
		assertEquals(88, snap.startMHz);
		assertEquals(108, snap.endMHz);
		assertTrue(snap.filledBins > 0);
		assertTrue(snap.omittedHoles > 0);
		assertEquals(snap.filledBins, snap.mhz.length);
		assertEquals(-35f, snap.peakDbm, 0.01f);
		assertTrue(snap.noiseDbm < -50f);
		for (int i = 0; i < snap.dbm.length; i++)
			assertTrue(snap.dbm[i] > -140f, "hole leaked into points");
	}

	@Test
	void downsampleKeepsThePeakInItsBucket() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2400, 2500, -150f);
		ds.getSpectrumArray()[0] = -80f;
		ds.getSpectrumArray()[1] = -20f;
		ds.getSpectrumArray()[2] = -90f;
		SpectrumSnapshot snap = SpectrumSnapshot.fromDataset(ds, 1L, 50, null);
		assertTrue(snap.mhz.length <= 50);
		assertEquals(-20f, snap.peakDbm, 0.01f);
		boolean saw = false;
		for (float y : snap.dbm)
			if (Math.abs(y + 20f) < 0.01f)
				saw = true;
		assertTrue(saw, "peak bin must survive downsample");
	}

	@Test
	void minDbmDropsWeakBins() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 89, -150f);
		ds.getSpectrumArray()[0] = -80f;
		ds.getSpectrumArray()[1] = -30f;
		SpectrumSnapshot snap = SpectrumSnapshot.fromDataset(ds, 1L, 100, Float.valueOf(-40f));
		assertEquals(1, snap.mhz.length);
		assertEquals(-30f, snap.dbm[0], 0.01f);
	}
}
