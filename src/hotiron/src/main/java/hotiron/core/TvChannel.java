package hotiron.core;

/**
 * One US TV channel (47 CFR 73.603), 6 MHz. Same raster as ATSC 1.0.
 */
public final class TvChannel
{
	public final int fccChannel;
	public final int lowMHz;

	public TvChannel(int fccChannel, int lowMHz)
	{
		this.fccChannel = fccChannel;
		this.lowMHz = lowMHz;
	}

	public int highMHz()
	{
		return lowMHz + TvChannelPlan.WIDTH_MHZ;
	}

	public double centerMHz()
	{
		return lowMHz + TvChannelPlan.WIDTH_MHZ / 2.0;
	}

	/** ATSC A/53 pilot, 310 kHz above the lower edge. */
	public double pilotMHz()
	{
		return lowMHz + TvChannelPlan.PILOT_OFFSET_MHZ;
	}

	public long centerHz()
	{
		return Math.round(centerMHz() * 1_000_000d);
	}

	public boolean occupancyOverlaps(double startMHz, double endMHz)
	{
		return highMHz() > startMHz && lowMHz < endMHz;
	}

	public boolean vhf()
	{
		return fccChannel <= 13;
	}

	public String label()
	{
		return Integer.toString(fccChannel);
	}

	@Override
	public String toString()
	{
		return "tv" + fccChannel + "@" + lowMHz + "-" + highMHz();
	}
}
