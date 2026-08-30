package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VuMeterTest
{
	@Test
	void silenceStaysOnTheFloor()
	{
		VuMeter m = new VuMeter();
		short[] z = new short[480];
		assertEquals(0f, m.accept(z, z.length, 1_000L), 0.001f);
		assertEquals(0f, m.accept(z, z.length, 1_040L), 0.001f);
	}

	@Test
	void loudPeakReadsHigh()
	{
		assertTrue(VuMeter.fromPeak(8000) > 0.7f);
		assertTrue(VuMeter.fromPeak(200) > 0.05f);
		assertTrue(VuMeter.fromPeak(200) < VuMeter.fromPeak(8000));
		assertEquals(1f, VuMeter.fromPeak(32767), 0.001f);
		assertEquals(0f, VuMeter.fromPeak(0), 0.001f);
	}

	@Test
	void attackIsFasterThanRelease()
	{
		VuMeter m = new VuMeter();
		short[] loud = tone(12_000);
		short[] quiet = new short[loud.length];
		float up = m.accept(loud, loud.length, 10_000L);
		assertTrue(up > 0.7f, "first loud block should pin the needle up, got " + up);
		float after20 = m.accept(quiet, quiet.length, 10_020L);
		assertTrue(after20 > up * 0.7f, "20 ms of silence must not dump the needle");
		float later = after20;
		for (int i = 1; i <= 12; i++)
			later = m.accept(quiet, quiet.length, 10_020L + i * 40L);
		assertTrue(later < after20 * 0.5f, "release should sag over a few hundred ms");
	}

	private static short[] tone(int peak)
	{
		short[] pcm = new short[480];
		for (int i = 0; i < pcm.length; i++)
			pcm[i] = (short) ((i & 1) == 0 ? peak : -peak);
		return pcm;
	}
}
