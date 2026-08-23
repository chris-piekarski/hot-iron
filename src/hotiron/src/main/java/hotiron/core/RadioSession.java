package hotiron.core;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Exclusive USB session: last-launch-wins apply queue, frequency debounce,
 * stop-then-start. Mode is read from {@link HackRFSettings} at start time
 * (same as today’s launcher). Native start/stop stay on {@link Driver}
 * (the frame); this class does not load JNA.
 */
public final class RadioSession
{
	public interface Driver
	{
		void stopAndJoin();

		/** Stop button: bounded join so the EDT cannot hang. */
		void abort();

		void prepareSweep();

		void startExclusive(RadioMode mode);

		Driver NOOP = new Driver()
		{
			@Override
			public void stopAndJoin()
			{
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
			}
		};
	}

	public interface Debounce
	{
		void restart();

		void stop();

		Debounce NOOP = new Debounce()
		{
			@Override
			public void restart()
			{
			}

			@Override
			public void stop()
			{
			}
		};
	}

	private final HackRFSettings settings;
	private final Driver driver;
	private final Debounce debounce;
	private final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(1);
	private Thread launcher;

	public RadioSession(HackRFSettings settings, Driver driver, Debounce debounce)
	{
		if (settings == null)
			throw new IllegalArgumentException("settings");
		this.settings = settings;
		this.driver = driver == null ? Driver.NOOP : driver;
		this.debounce = debounce == null ? Debounce.NOOP : debounce;
	}

	public synchronized void applyNow()
	{
		if (released())
			return;
		if (!queue.offer(0))
		{
			queue.clear();
			queue.offer(0);
		}
	}

	public void applyDebounced()
	{
		if (released())
			return;
		debounce.restart();
	}

	public void cancelDebounce()
	{
		debounce.stop();
	}

	/**
	 * Drop USB immediately. Cancels debounce and queued applies so the
	 * launcher does not start after Stop.
	 */
	public void release()
	{
		debounce.stop();
		queue.clear();
		driver.abort();
	}

	public void startLauncher()
	{
		if (launcher != null)
			return;
		launcher = new Thread(() -> {
			Thread.currentThread().setName("Launcher-thread");
			while (true)
			{
				try
				{
					queue.take();
					runApply();
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					return;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		launcher.start();
	}

	public void stopLauncher()
	{
		Thread t = launcher;
		if (t == null)
			return;
		t.interrupt();
		try
		{
			t.join(1000);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		launcher = null;
	}

	/**
	 * Tests: consume one queued apply (same as the launcher after
	 * {@code take()}).
	 */
	public boolean drainOne()
	{
		if (queue.poll() == null)
			return false;
		runApply();
		return true;
	}

	boolean queued()
	{
		return queue.peek() != null;
	}

	void runApply()
	{
		driver.stopAndJoin();
		if (!SweepConfig.shouldStartAfterStop(released(), queued()))
			return;
		RadioMode mode = settings.radioMode();
		if (mode == RadioMode.STOPPED)
			return;
		if (mode == RadioMode.SWEEP)
			driver.prepareSweep();
		driver.startExclusive(mode);
	}

	private boolean released()
	{
		return settings.radioMode() == RadioMode.STOPPED;
	}
}
