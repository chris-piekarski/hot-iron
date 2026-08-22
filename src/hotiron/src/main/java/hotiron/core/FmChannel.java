package hotiron.core;

import java.util.Locale;

/**
 * One US FM broadcast channel (47 CFR 73.201). Centers are odd tenths
 * from 88.1 to 107.9 MHz; occupied width is 200 kHz.
 */
public final class FmChannel
{
	/** FCC channel number 201 (88.1) through 300 (107.9). */
	public final int fccChannel;
	/** Center frequency in kHz so 97.3 stays exact. */
	public final int centerKHz;

	public FmChannel(int fccChannel, int centerKHz)
	{
		this.fccChannel = fccChannel;
		this.centerKHz = centerKHz;
	}

	public double centerMHz()
	{
		return centerKHz / 1000.0;
	}

	public double lowMHz()
	{
		return (centerKHz - FmChannelPlan.HALF_KHZ) / 1000.0;
	}

	public double highMHz()
	{
		return (centerKHz + FmChannelPlan.HALF_KHZ) / 1000.0;
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

	/** Dial label, e.g. {@code 97.3}. */
	public String label()
	{
		return String.format(Locale.US, "%.1f", centerMHz());
	}

	@Override
	public String toString()
	{
		return "fm" + fccChannel + "@" + label();
	}
}
