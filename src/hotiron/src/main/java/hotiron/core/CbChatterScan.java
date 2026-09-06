package hotiron.core;

/**
 * Classic CB scan: dwell each AM channel, stop on chatter. Mix hops are
 * software; USB stays parked. Loops 1–40 until a hit or the operator stops.
 */
public final class CbChatterScan
{
	public static final int SETTLE_MS = 180;
	public static final int DWELL_MS = 280;
	public static final int HIT_MS = 80;

	public enum Result
	{
		IDLE, CONTINUE, NEXT, HIT
	}

	private boolean active;
	private int current = CbChannelPlan.HIGHWAY_CHANNEL;
	private long enteredMs;
	private long chatterSinceMs;

	public synchronized void start(int channel, long nowMs)
	{
		active = true;
		current = CbChannelPlan.clamp(channel).number;
		enteredMs = nowMs;
		chatterSinceMs = 0;
	}

	public synchronized void stop()
	{
		active = false;
		chatterSinceMs = 0;
	}

	public synchronized boolean active()
	{
		return active;
	}

	public synchronized int current()
	{
		return current;
	}

	public synchronized Result consider(int channel, boolean chatter, long nowMs)
	{
		if (!active)
			return Result.IDLE;
		if (channel != current)
			return Result.CONTINUE;
		if (nowMs - enteredMs < SETTLE_MS)
			return Result.CONTINUE;
		if (chatter)
		{
			if (chatterSinceMs <= 0)
				chatterSinceMs = nowMs;
			if (nowMs - chatterSinceMs >= HIT_MS)
			{
				active = false;
				return Result.HIT;
			}
			return Result.CONTINUE;
		}
		chatterSinceMs = 0;
		if (nowMs - enteredMs >= SETTLE_MS + DWELL_MS)
			return Result.NEXT;
		return Result.CONTINUE;
	}

	public synchronized int advance(long nowMs)
	{
		current = CbStationDial.tune(current, +1).number;
		enteredMs = nowMs;
		chatterSinceMs = 0;
		return current;
	}
}
