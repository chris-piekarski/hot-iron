package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NfcEnvelopeTraceTest
{
	@Test
	void carrierAt2MHzIfIsAboutMinusSixDbfs()
	{
		float[] db = NfcEnvelopeTrace.mixCarrierDb(cis(1000, 0.2, 64), NfcSniffEngine.IQ_RATE_HZ,
				NfcEnvelopeTrace.IF_HZ, NfcEnvelopeTrace.SAMPLE_HZ);
		assertEquals(2, db.length);
		assertEquals(-6.02f, db[0], 1.0f);
	}

	@Test
	void hashAt12p96IsRejectedVsCarrier()
	{
		double hashCps = (12.956e6 - NfcSniffEngine.LO_HZ) / NfcSniffEngine.IQ_RATE_HZ;
		float[] carrier = NfcEnvelopeTrace.mixCarrierDb(cis(5000, 0.2, 64), NfcSniffEngine.IQ_RATE_HZ,
				NfcEnvelopeTrace.IF_HZ, NfcEnvelopeTrace.SAMPLE_HZ);
		float[] hash = NfcEnvelopeTrace.mixCarrierDb(cis(5000, hashCps, 64), NfcSniffEngine.IQ_RATE_HZ,
				NfcEnvelopeTrace.IF_HZ, NfcEnvelopeTrace.SAMPLE_HZ);
		assertTrue(mean(carrier) > mean(hash) + 12f, "carrier " + mean(carrier) + " hash " + mean(hash));
	}

	@Test
	void askHoleOnCarrierBecomesADip()
	{
		byte[] iq = cisHole(4000, 0.2, 64, 1600, 2400);
		float[] db = NfcEnvelopeTrace.mixCarrierDb(iq, NfcSniffEngine.IQ_RATE_HZ, NfcEnvelopeTrace.IF_HZ,
				NfcEnvelopeTrace.SAMPLE_HZ);
		assertTrue(db.length >= 6);
		assertTrue(db[0] > -12f, "carrier " + db[0]);
		assertTrue(db[db.length / 2] < db[0] - 12f, "hole " + db[db.length / 2] + " vs " + db[0]);
	}

	@Test
	void latestLiveReadsTheNewestFilledSample()
	{
		assertEquals(NfcEnvelopeTrace.EMPTY_DB, NfcEnvelopeTrace.latestLive(null), 0.01f);
		assertEquals(NfcEnvelopeTrace.EMPTY_DB, NfcEnvelopeTrace.latestLive(new float[0]), 0.01f);
		float[] snap = new float[8];
		java.util.Arrays.fill(snap, NfcEnvelopeTrace.EMPTY_DB);
		snap[7] = -12.5f;
		assertEquals(-12.5f, NfcEnvelopeTrace.latestLive(snap), 0.01f);
	}

	@Test
	void snapshotFillsFromTheRight()
	{
		NfcEnvelopeTrace trace = new NfcEnvelopeTrace();
		assertEquals(0, trace.filled());
		trace.acceptIq(cis(500, 0.2, 64));
		assertEquals(1, trace.filled());
		float[] snap = trace.snapshot();
		assertEquals(NfcEnvelopeTrace.WINDOW_SAMPLES, snap.length);
		assertEquals(NfcEnvelopeTrace.EMPTY_DB, snap[0], 0.01f);
		assertEquals(-6.02f, snap[snap.length - 1], 1.5f);
	}

	@Test
	void engineEnvelopeIsTheCarrierMix()
	{
		byte[] iq = cis(500, 0.2, 64);
		float[] a = NfcSniffEngine.envelope(iq);
		float[] b = NfcEnvelopeTrace.mixCarrierDb(iq, NfcSniffEngine.IQ_RATE_HZ, NfcEnvelopeTrace.IF_HZ,
				NfcEnvelopeTrace.SAMPLE_HZ);
		assertArrayEquals(b, a, 1e-4f);
	}

	@Test
	void widebandDecimateStillMeansMagnitude()
	{
		byte[] iq = new byte[800];
		for (int i = 0; i < 400; i++)
		{
			iq[2 * i] = 64;
			iq[2 * i + 1] = 0;
		}
		float[] db = NfcEnvelopeTrace.decimateIq(iq, NfcSniffEngine.IQ_RATE_HZ, 50_000f);
		assertEquals(2, db.length);
		assertEquals(-6.02f, db[0], 0.2f);
	}

	private static float mean(float[] db)
	{
		double s = 0;
		for (float v : db)
			s += v;
		return (float) (s / db.length);
	}

	private static byte[] cis(int pairs, double cyclesPerSample, int amp)
	{
		return cisHole(pairs, cyclesPerSample, amp, 0, 0);
	}

	private static byte[] cisHole(int pairs, double cyclesPerSample, int amp, int holeFrom, int holeTo)
	{
		byte[] iq = new byte[pairs * 2];
		for (int n = 0; n < pairs; n++)
		{
			int a = (n >= holeFrom && n < holeTo) ? 0 : amp;
			double ph = 2.0 * Math.PI * cyclesPerSample * n;
			iq[2 * n] = (byte) Math.round(a * Math.cos(ph));
			iq[2 * n + 1] = (byte) Math.round(a * Math.sin(ph));
		}
		return iq;
	}
}
