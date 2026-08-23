package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class RadioSessionTest
{
	private static final class Rec implements RadioSession.Driver, RadioSession.Debounce
	{
		int stops;
		int aborts;
		int prepares;
		int debounceRestarts;
		int debounceStops;
		final List<RadioMode> started = new ArrayList<>();

		@Override
		public void stopAndJoin()
		{
			stops++;
		}

		@Override
		public void abort()
		{
			aborts++;
		}

		@Override
		public void prepareSweep()
		{
			prepares++;
		}

		@Override
		public void startExclusive(RadioMode mode)
		{
			started.add(mode);
		}

		@Override
		public void restart()
		{
			debounceRestarts++;
		}

		@Override
		public void stop()
		{
			debounceStops++;
		}
	}

	@Test
	void applyNowStartsSweepAfterStopAndPreparesAutoSweep()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.applyNow();
		assertTrue(session.drainOne());
		assertEquals(1, rec.stops);
		assertEquals(1, rec.prepares);
		assertEquals(List.of(RadioMode.SWEEP), rec.started);
		assertFalse(session.drainOne());
	}

	@Test
	void applyNowCoalescesToOneStart()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.applyNow();
		session.applyNow();
		session.applyNow();
		assertTrue(session.drainOne());
		assertFalse(session.drainOne());
		assertEquals(1, rec.stops);
		assertEquals(1, rec.started.size());
	}

	@Test
	void listenDoesNotPrepareSweep()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		s.startListen();
		session.applyNow();
		session.drainOne();
		assertEquals(0, rec.prepares);
		assertEquals(List.of(RadioMode.LISTEN), rec.started);
	}

	@Test
	void watchStartsWatchMode()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		s.startWatch();
		session.applyNow();
		session.drainOne();
		assertEquals(0, rec.prepares);
		assertEquals(List.of(RadioMode.WATCH), rec.started);
	}

	@Test
	void releasedApplyIsIgnored()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		s.releaseRadio();
		session.applyNow();
		session.applyDebounced();
		assertFalse(session.drainOne());
		assertEquals(0, rec.stops);
		assertEquals(0, rec.debounceRestarts);
		assertTrue(rec.started.isEmpty());
	}

	@Test
	void releaseStopsImmediatelyAndDropsQueue()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.applyNow();
		s.releaseRadio();
		session.release();
		assertEquals(1, rec.aborts);
		assertEquals(0, rec.stops, "Stop must not go through the launcher join loop");
		assertEquals(1, rec.debounceStops);
		assertFalse(session.drainOne(), "Stop must not start after join");
		assertTrue(rec.started.isEmpty());
	}

	@Test
	void newerQueuedApplySkipsStaleStartThenRuns()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		AtomicInteger phase = new AtomicInteger();
		List<RadioMode> started = new ArrayList<>();
		RadioSession[] box = new RadioSession[1];
		RadioSession.Driver driver = new RadioSession.Driver()
		{
			@Override
			public void stopAndJoin()
			{
				if (phase.getAndIncrement() == 0)
					box[0].applyNow();
			}

			@Override
			public void abort()
			{
			}

			@Override
			public void prepareSweep()
			{
			}

			@Override
			public void startExclusive(RadioMode mode)
			{
				started.add(mode);
			}
		};
		RadioSession session = new RadioSession(s, driver, RadioSession.Debounce.NOOP);
		box[0] = session;
		session.applyNow();
		session.drainOne();
		assertTrue(started.isEmpty(), "a newer apply is already queued");
		session.drainOne();
		assertEquals(List.of(RadioMode.SWEEP), started);
	}

	@Test
	void debounceDoesNotStartUntilFired()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.applyDebounced();
		assertEquals(1, rec.debounceRestarts);
		assertFalse(session.drainOne());
		session.applyNow();
		session.drainOne();
		assertEquals(1, rec.started.size());
	}

	@Test
	void cancelDebounceStopsTimer()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.applyDebounced();
		session.cancelDebounce();
		assertEquals(1, rec.debounceStops);
	}

	@Test
	void constructorRejectsNullSettings()
	{
		assertThrows(IllegalArgumentException.class,
				() -> new RadioSession(null, RadioSession.Driver.NOOP, RadioSession.Debounce.NOOP));
	}

	@Test
	void startLauncherRunsQueuedApply() throws Exception
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.startLauncher();
		session.startLauncher();
		session.applyNow();
		long deadline = System.currentTimeMillis() + 2000;
		while (System.currentTimeMillis() < deadline && rec.started.isEmpty())
			Thread.sleep(10);
		try
		{
			assertEquals(List.of(RadioMode.SWEEP), rec.started);
			assertEquals(1, rec.prepares);
			assertEquals(1, rec.stops);
		}
		finally
		{
			session.stopLauncher();
		}
	}

	@Test
	void stopLauncherThenStartLauncherRunsAgain() throws Exception
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		Rec rec = new Rec();
		RadioSession session = new RadioSession(s, rec, rec);
		session.stopLauncher();
		session.startLauncher();
		session.applyNow();
		long deadline = System.currentTimeMillis() + 2000;
		while (System.currentTimeMillis() < deadline && rec.started.isEmpty())
			Thread.sleep(10);
		session.stopLauncher();
		assertEquals(List.of(RadioMode.SWEEP), rec.started);
		rec.started.clear();
		session.startLauncher();
		session.applyNow();
		deadline = System.currentTimeMillis() + 2000;
		while (System.currentTimeMillis() < deadline && rec.started.isEmpty())
			Thread.sleep(10);
		session.stopLauncher();
		assertEquals(List.of(RadioMode.SWEEP), rec.started);
	}
}
