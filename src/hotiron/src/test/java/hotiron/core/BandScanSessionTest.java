package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class BandScanSessionTest
{
	@Test
	void fmIsOneWindowAndFinishesAfterDwell()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.FM, 1000);
		assertEquals(FmChannelPlan.VIEW_START_MHZ, s.currentWindow().getStartMHz());
		assertEquals(FmChannelPlan.VIEW_END_MHZ, s.currentWindow().getEndMHz());
		assertTrue(s.nextWindowIfDue(1000 + BandScanSession.DWELL_MS - 1).isEmpty());
		assertFalse(s.shouldFinish(1000 + BandScanSession.DWELL_MS - 1));
		assertTrue(s.shouldFinish(1000 + BandScanSession.DWELL_MS));
		assertTrue(s.nextWindowIfDue(1000 + BandScanSession.DWELL_MS).isEmpty());
	}

	@Test
	void tvAdvancesFromVhfToUhfThenFinishes()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.TV, 0);
		assertEquals(TvChannelPlan.VHF_VIEW_START_MHZ, s.currentWindow().getStartMHz());
		assertEquals(TvChannelPlan.VHF_VIEW_END_MHZ, s.currentWindow().getEndMHz());
		assertFalse(s.shouldFinish(BandScanSession.DWELL_MS));
		Optional<FrequencyRange> uhf = s.nextWindowIfDue(BandScanSession.DWELL_MS);
		assertTrue(uhf.isPresent());
		assertEquals(TvChannelPlan.UHF_VIEW_START_MHZ, uhf.get().getStartMHz());
		assertEquals(TvChannelPlan.UHF_VIEW_END_MHZ, uhf.get().getEndMHz());
		assertEquals(uhf.get(), s.currentWindow());
		assertTrue(s.shouldFinish(2L * BandScanSession.DWELL_MS));
	}

	@Test
	void stopClearsTheSession()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.FM, 0);
		s.stop();
		assertFalse(s.active());
		assertEquals(BandScan.OFF, s.kind());
		assertNull(s.currentWindow());
	}
}
