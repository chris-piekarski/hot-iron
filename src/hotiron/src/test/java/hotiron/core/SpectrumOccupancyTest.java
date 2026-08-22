package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpectrumOccupancyTest {

	@Test
	void emptyOrNoiseOnlyHasNoEmitters() {
		assertEquals(0, SpectrumOccupancy.from(null, null, -80f, 100_000f, 88, 108).emitters.size());
		float[] mhz = { 88f, 88.1f, 88.2f };
		float[] dbm = { -80f, -79f, -81f };
		SpectrumOccupancy.Result r = SpectrumOccupancy.from(mhz, dbm, -80f, 100_000f, 88, 108);
		assertEquals(0, r.emitters.size());
		assertEquals(0f, r.occupiedFraction, 0.001f);
		assertNull(r.emitters.isEmpty() ? null : r.emitters.get(0).label);
	}

	@Test
	void wifiBumpIsOneEmitterWithChannelLabel() {
		DatasetSpectrum ds = wifiNoise();
		fillBump(ds, 2427f, 2447f, -40f);
		float[] axis = ds.frequencyAxisMHz();
		for (int i = 0; i < axis.length; i++)
			if (Math.abs(axis[i] - 2437f) < 0.06f)
				ds.getSpectrumArray()[i] = -30f;
		float[] mhz = ds.frequencyAxisMHz();
		float[] dbm = ds.getSpectrumArray();
		int n = 0;
		float[] m = new float[mhz.length];
		float[] d = new float[mhz.length];
		for (int i = 0; i < mhz.length; i++)
		{
			if (DatasetSpectrum.isChartHole(dbm[i]))
				continue;
			m[n] = mhz[i];
			d[n] = dbm[i];
			n++;
		}
		m = java.util.Arrays.copyOf(m, n);
		d = java.util.Arrays.copyOf(d, n);
		float noise = -80f;
		SpectrumOccupancy.Result r = SpectrumOccupancy.from(m, d, noise, 100_000f, 2402, 2472);
		assertEquals(1, r.emitters.size(), r.toJson());
		SpectrumOccupancy.Emitter e = r.emitters.get(0);
		assertEquals(-30f, e.peakDbm, 0.5f);
		assertTrue(e.occupiedMhz > 10f, "20 MHz-ish Wi-Fi bump");
		assertTrue(e.occupiedMhz < 25f);
		assertEquals("ch 6", e.label);
		assertTrue(r.occupiedFraction > 0.1f && r.occupiedFraction < 0.5f);
		assertTrue(r.toJson().contains("ch 6"));
	}

	@Test
	void twoSeparatedPeaksAreTwoEmittersStrongestFirst() {
		float[] mhz = new float[20];
		float[] dbm = new float[20];
		for (int i = 0; i < 20; i++)
		{
			mhz[i] = 2402f + i;
			dbm[i] = -80f;
		}
		dbm[2] = -40f;
		dbm[3] = -35f;
		dbm[4] = -42f;
		dbm[15] = -50f;
		dbm[16] = -48f;
		SpectrumOccupancy.Result r = SpectrumOccupancy.from(mhz, dbm, -80f, 1_000_000f, 2402, 2472);
		assertEquals(2, r.emitters.size());
		assertEquals(-35f, r.emitters.get(0).peakDbm, 0.01f);
		assertEquals(-48f, r.emitters.get(1).peakDbm, 0.01f);
	}

	@Test
	void closeRunsMergeAcrossAOneBinGap() {
		float[] mhz = { 100f, 100.1f, 100.2f, 100.3f, 100.4f };
		float[] dbm = { -40f, -80f, -40f, -41f, -80f };
		SpectrumOccupancy.Result r = SpectrumOccupancy.from(mhz, dbm, -80f, 100_000f, 88, 108);
		assertEquals(1, r.emitters.size(), "gap of 0.1 MHz at 100 kHz bins is 1 bin — merge");
		assertNull(r.emitters.get(0).label);
	}

	@Test
	void emitterCapKeepsTheStrongest() {
		float[] mhz = new float[40];
		float[] dbm = new float[40];
		for (int i = 0; i < 40; i++)
		{
			mhz[i] = 100f + i * 2f;
			dbm[i] = i % 2 == 0 ? -30f - i : -90f;
		}
		SpectrumOccupancy.Result r = SpectrumOccupancy.from(mhz, dbm, -90f, 100_000f, 88, 200);
		assertEquals(SpectrumOccupancy.MAX_EMITTERS, r.emitters.size());
		assertTrue(r.emitters.get(0).peakDbm >= r.emitters.get(r.emitters.size() - 1).peakDbm);
	}

	private static DatasetSpectrum wifiNoise() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2402, 2472, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -80f;
		return ds;
	}

	private static void fillBump(DatasetSpectrum ds, float startMHz, float endMHz, float dbm) {
		float[] mhz = ds.frequencyAxisMHz();
		for (int i = 0; i < mhz.length; i++)
			if (mhz[i] >= startMHz && mhz[i] <= endMHz)
				ds.getSpectrumArray()[i] = dbm;
	}
}
