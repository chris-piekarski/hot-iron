package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class BandScanSessionTest
{
	@Test
	void fmIsOneWindowAndFinishesAfterLiveDwell()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.FM, 1000);
		assertEquals(FmChannelPlan.VIEW_START_MHZ, s.currentWindow().getStartMHz());
		assertEquals(FmChannelPlan.VIEW_END_MHZ, s.currentWindow().getEndMHz());
		assertFalse(s.windowLive());
		assertTrue(s.nextWindowIfDue(1000 + BandScanSession.DWELL_MS).isEmpty(),
				"USB retune time must not count as dwell");
		assertFalse(s.shouldFinish(1000 + BandScanSession.DWELL_MS));
		s.markLive(4000);
		assertTrue(s.windowLive());
		assertFalse(s.shouldFinish(4000 + BandScanSession.DWELL_MS - 1));
		assertTrue(s.shouldFinish(4000 + BandScanSession.DWELL_MS));
		assertTrue(s.nextWindowIfDue(4000 + BandScanSession.DWELL_MS).isEmpty());
	}

	@Test
	void tvAdvancesFromVhfToUhfAfterEachWindowIsLive()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.TV, 0);
		assertEquals(TvChannelPlan.VHF_VIEW_START_MHZ, s.currentWindow().getStartMHz());
		assertEquals(TvChannelPlan.VHF_VIEW_END_MHZ, s.currentWindow().getEndMHz());
		assertTrue(s.nextWindowIfDue(BandScanSession.DWELL_MS).isEmpty());
		s.markLive(100);
		assertFalse(s.shouldFinish(100 + BandScanSession.DWELL_MS));
		Optional<FrequencyRange> uhf = s.nextWindowIfDue(100 + BandScanSession.DWELL_MS);
		assertTrue(uhf.isPresent());
		assertEquals(TvChannelPlan.UHF_VIEW_START_MHZ, uhf.get().getStartMHz());
		assertEquals(TvChannelPlan.UHF_VIEW_END_MHZ, uhf.get().getEndMHz());
		assertEquals(uhf.get(), s.currentWindow());
		assertFalse(s.windowLive(), "UHF dwell waits for the first UHF sweep");
		assertFalse(s.shouldFinish(100 + 2L * BandScanSession.DWELL_MS));
		s.markLive(100 + BandScanSession.DWELL_MS + 50);
		assertTrue(s.shouldFinish(100 + 2L * BandScanSession.DWELL_MS + 50));
	}

	@Test
	void markLiveIsIdempotentOnTheSameWindow()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.FM, 0);
		s.markLive(10);
		s.markLive(999);
		assertFalse(s.shouldFinish(10 + BandScanSession.DWELL_MS - 1));
		assertTrue(s.shouldFinish(10 + BandScanSession.DWELL_MS));
	}

	@Test
	void stopClearsTheSession()
	{
		BandScanSession s = new BandScanSession();
		s.start(BandScan.FM, 0);
		s.markLive(0);
		s.stop();
		assertFalse(s.active());
		assertFalse(s.windowLive());
		assertEquals(BandScan.OFF, s.kind());
		assertNull(s.currentWindow());
	}
}
