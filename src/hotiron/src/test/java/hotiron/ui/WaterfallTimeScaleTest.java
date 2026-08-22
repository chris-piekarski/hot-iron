package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class WaterfallTimeScaleTest {

	@Test
	void formatAgeUsesCompactUnits() {
		assertEquals("now", WaterfallTimeScale.formatAge(0));
		assertEquals("now", WaterfallTimeScale.formatAge(0.02));
		assertEquals("0.5s", WaterfallTimeScale.formatAge(0.5));
		assertEquals("12s", WaterfallTimeScale.formatAge(12));
		assertEquals("1m", WaterfallTimeScale.formatAge(60));
		assertEquals("1:30", WaterfallTimeScale.formatAge(90));
		assertEquals("1h", WaterfallTimeScale.formatAge(3600));
		assertEquals("1h05", WaterfallTimeScale.formatAge(3900));
	}

	@Test
	void niceStepPicksAReadableInterval() {
		assertEquals(2.0, WaterfallTimeScale.niceStep(10, 6), 1e-9);
		assertEquals(0.2, WaterfallTimeScale.niceStep(1, 6), 1e-9);
		assertEquals(10.0, WaterfallTimeScale.niceStep(40, 6), 1e-9);
		assertEquals(1.0, WaterfallTimeScale.niceStep(5, 6), 1e-9);
		assertTrue(WaterfallTimeScale.niceStep(0, 6) > 0);
	}

	@Test
	void filledCountStopsAtTheFirstEmptyRow() {
		assertEquals(0, WaterfallTimeScale.filledCount(null));
		assertEquals(0, WaterfallTimeScale.filledCount(new long[] { 0, 1, 2 }));
		assertEquals(3, WaterfallTimeScale.filledCount(new long[] { 30, 20, 10, 0, 0 }));
	}

	@Test
	void yForAgeIsMonotonicAndNewestIsTop() {
		long[] rows = WaterfallTimeScale.rowsAtRate(300, 30, 1_000_000L);
		assertEquals(0, WaterfallTimeScale.yForAge(rows, 300, 0));
		int y2 = WaterfallTimeScale.yForAge(rows, 300, 2);
		int y4 = WaterfallTimeScale.yForAge(rows, 300, 4);
		assertTrue(y2 > 10, "2s should be well below the top at 30 fps");
		assertTrue(y4 > y2);
		assertEquals(0, WaterfallTimeScale.ageAtY(rows, 300, 0), 0.05);
		assertEquals(2.0, WaterfallTimeScale.ageAtY(rows, 300, y2), 0.1);
	}

	@Test
	void ticksFromRatePlaceNowAtTheTopAndOlderDownThePanel() {
		List<WaterfallTimeScale.Tick> ticks = WaterfallTimeScale.ticksFromRate(300, 30, 6);
		assertFalse(ticks.isEmpty());
		assertEquals("now", ticks.get(0).label);
		assertEquals(0, ticks.get(0).y);
		assertTrue(ticks.size() >= 4);
		for (int i = 1; i < ticks.size(); i++) {
			assertTrue(ticks.get(i).y > ticks.get(i - 1).y);
			assertTrue(ticks.get(i).ageSec > ticks.get(i - 1).ageSec);
		}
		WaterfallTimeScale.Tick last = ticks.get(ticks.size() - 1);
		assertTrue(last.ageSec >= 6, "300 px at 30 fps is 10 s of history");
		assertTrue(last.y < 300);
	}

	@Test
	void emptyOrTinyHistoryIsJustNowOrNothing() {
		assertTrue(WaterfallTimeScale.ticks(null, 200, 6).isEmpty());
		assertTrue(WaterfallTimeScale.ticks(new long[200], 200, 6).isEmpty());
		long[] one = new long[200];
		one[0] = 50;
		List<WaterfallTimeScale.Tick> justNow = WaterfallTimeScale.ticks(one, 200, 6);
		assertEquals(1, justNow.size());
		assertEquals("now", justNow.get(0).label);
	}

	@Test
	void pauseDoesNotDriftAgesOffTheNewestRow() {
		long[] rows = WaterfallTimeScale.rowsAtRate(100, 20, 5_000L);
		double span = WaterfallTimeScale.spanSeconds(rows);
		assertEquals(99 / 20.0, span, 0.05);
		assertEquals(span, WaterfallTimeScale.ageAtRow(rows, 99), 0.05);
	}

	@Test
	void stretchedBufferMapsLastRowToTheBottom() {
		assertEquals(0, WaterfallTimeScale.rowToY(0, 200, 100));
		assertEquals(99, WaterfallTimeScale.rowToY(199, 200, 100));
		assertEquals(50, WaterfallTimeScale.rowToY(100, 200, 100), 1);
	}
}
