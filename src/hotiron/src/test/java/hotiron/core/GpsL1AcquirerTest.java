package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class GpsL1AcquirerTest
{
	@Test
	void findsSyntheticPrnAtZeroDoppler()
	{
		GpsL1Acquirer acq = new GpsL1Acquirer(1000, 500);
		byte[] iq = GpsL1Acquirer.syntheticMs(7, 0, 0, 0.4f);
		List<GpsSvHit> hits = acq.search(iq);
		assertFalse(hits.isEmpty(), "expected PRN 7");
		GpsSvHit hit = hits.stream().filter(h -> h.prn == 7).findFirst().orElseThrow();
		assertEquals(0, hit.dopplerHz);
		assertEquals(0, hit.codePhase, 8);
		assertTrue(hit.cn0DbHz > 35f, "C/N0 " + hit.cn0DbHz);
		assertTrue(hit.peakRatio >= GpsL1Acquirer.DETECT_RATIO);
	}

	@Test
	void findsDopplerAndDoesNotInventMissingPrns()
	{
		GpsL1Acquirer acq = new GpsL1Acquirer(2000, 500);
		byte[] iq = GpsL1Acquirer.syntheticMs(12, 1500, 0, 0.45f);
		List<GpsSvHit> hits = acq.search(iq);
		GpsSvHit hit = hits.stream().filter(h -> h.prn == 12).findFirst().orElseThrow();
		assertEquals(1500, hit.dopplerHz);
		assertTrue(hits.stream().noneMatch(h -> h.prn == 1), "PRN 1 was not in the IQ");
	}

	@Test
	void cn0FromPeakRatioIsAbout39dBAtRatio10()
	{
		assertEquals(39.5f, GpsL1Acquirer.cn0(10f), 0.2f);
	}
}
