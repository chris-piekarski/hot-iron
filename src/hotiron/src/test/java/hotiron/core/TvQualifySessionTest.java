package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TvQualifySessionTest
{
	@Test
	void queueIsStrongestUhfAtscLikeCapped()
	{
		List<Integer> q = TvQualifySession.queue(List.of(
				hit(7, TvChannelGrade.ATSC_LIKE, -20f),
				hit(18, TvChannelGrade.OCCUPIED, -10f),
				hit(28, TvChannelGrade.ATSC_LIKE, -40f),
				hit(33, TvChannelGrade.ATSC_LIKE, -25f),
				hit(36, TvChannelGrade.PICTURE, -15f)));
		assertEquals(List.of(33, 28), q);
	}

	@Test
	void advancesOnPictureOrDwellThenFinishes()
	{
		TvQualifySession s = new TvQualifySession(List.of(33, 28));
		s.start(0);
		assertTrue(s.active());
		assertEquals(33, s.currentFcc());
		assertFalse(s.shouldAdvance(1_000, 0));
		assertTrue(s.shouldAdvance(1_000, 2));
		assertTrue(s.advance(1_000));
		assertEquals(28, s.currentFcc());
		assertTrue(s.shouldAdvance(1_000 + TvQualifySession.DWELL_MS, 0));
		assertFalse(s.advance(1_000 + TvQualifySession.DWELL_MS));
		assertFalse(s.active());
	}

	@Test
	void cancelStopsTheQueue()
	{
		TvQualifySession s = new TvQualifySession(List.of(33));
		s.start(0);
		s.cancel();
		assertFalse(s.active());
		assertFalse(s.shouldAdvance(TvQualifySession.DWELL_MS, 0));
	}

	private static TvStationHit hit(int ch, TvChannelGrade grade, float dbm)
	{
		return new TvStationHit(TvChannelPlan.findByFccChannel(ch), dbm, 1f, grade, "", 0, Float.NaN,
				Float.NaN);
	}
}
