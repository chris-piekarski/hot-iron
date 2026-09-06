package hotiron.core;

import java.util.Locale;

/**
 * One US CB AM channel (47 CFR 95 Subpart D). 10 kHz occupancy around
 * the published center.
 */
public final class CbChannel
{
	public final int number;
	public final int centerKHz;

	public CbChannel(int number, int centerKHz)
	{
		this.number = number;
		this.centerKHz = centerKHz;
	}

	public double centerMHz()
	{
		return centerKHz / 1000.0;
	}

	public long centerHz()
	{
		return centerKHz * 1000L;
	}

	public double lowMHz()
	{
		return (centerKHz - CbChannelPlan.HALF_KHZ) / 1000.0;
	}

	public double highMHz()
	{
		return (centerKHz + CbChannelPlan.HALF_KHZ) / 1000.0;
	}

	public boolean centerIn(double startMHz, double endMHz)
	{
		double c = centerMHz();
		return c >= startMHz && c <= endMHz;
	}

	public boolean occupancyOverlaps(double startMHz, double endMHz)
	{
		return highMHz() > startMHz && lowMHz() < endMHz;
	}

	public String label()
	{
		return Integer.toString(number);
	}

	public String freqLabel()
	{
		return String.format(Locale.US, "%.3f", centerMHz());
	}

	@Override
	public String toString()
	{
		return "cb" + number + "@" + freqLabel();
	}
}
