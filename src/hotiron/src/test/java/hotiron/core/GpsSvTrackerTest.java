package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class GpsSvTrackerTest
{
	@Test
	void firstPeakIsShownAndEmptyDoesNotClear()
	{
		GpsSvTracker t = new GpsSvTracker();
		assertFalse(t.update(List.of()));
		assertTrue(t.last().isEmpty());
		assertTrue(t.update(List.of(new GpsSvHit(7, 42f, 500, 10, 12f))));
		assertEquals(1, t.last().size());
		assertEquals(7, t.last().get(0).prn);
		assertEquals(1, t.last().get(0).dwell);
		assertFalse(t.update(List.of()));
		assertFalse(t.update(null));
		assertEquals(7, t.last().get(0).prn);
		assertEquals(42f, t.last().get(0).cn0DbHz);
	}

	@Test
	void laterHitUpdatesThatPrnAndKeepsOthers()
	{
		GpsSvTracker t = new GpsSvTracker();
		assertTrue(t.update(List.of(new GpsSvHit(7, 42f, 500, 10, 12f))));
		assertTrue(t.update(List.of(new GpsSvHit(12, 38f, -250, 4, 9f))));
		assertEquals(2, t.last().size());
		assertEquals(7, t.last().get(0).prn);
		assertTrue(t.update(List.of(new GpsSvHit(7, 45f, 1000, 11, 14f))));
		assertEquals(2, t.last().size());
		GpsSvHit seven = t.last().stream().filter(h -> h.prn == 7).findFirst().orElseThrow();
		assertEquals(45f, seven.cn0DbHz);
		assertEquals(1000, seven.dopplerHz);
		assertEquals(2, seven.dwell);
		assertTrue(t.last().stream().anyMatch(h -> h.prn == 12 && h.cn0DbHz == 38f));
	}

	@Test
	void identicalDisplayDoesNotCountAsAnUpdate()
	{
		GpsSvTracker t = new GpsSvTracker();
		assertTrue(t.update(List.of(new GpsSvHit(5, 40f, 0, 1, 9f))));
		assertFalse(t.update(List.of(new GpsSvHit(5, 40f, 0, 1, 9f))));
		assertEquals(1, t.last().size());
	}

	@Test
	void resetClearsHeldHits()
	{
		GpsSvTracker t = new GpsSvTracker();
		t.update(List.of(new GpsSvHit(3, 41f, 0, 0, 10f)));
		t.reset();
		assertTrue(t.last().isEmpty());
	}
}
