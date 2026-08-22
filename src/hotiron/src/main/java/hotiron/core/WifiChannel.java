package hotiron.core;

/**
 * One IEEE 802.11 20 MHz channel. {@link #centerMHz} is the beacon/center
 * frequency used on-air; occupied width is {@link #widthMHz} (typically 20).
 */
public final class WifiChannel
{
	public final String band;
	public final int number;
	public final double centerMHz;
	public final double widthMHz;
	public final boolean primary;

	public WifiChannel(String band, int number, double centerMHz, double widthMHz, boolean primary)
	{
		this.band = band;
		this.number = number;
		this.centerMHz = centerMHz;
		this.widthMHz = widthMHz;
		this.primary = primary;
	}

	public double lowMHz()
	{
		return centerMHz - widthMHz / 2.0;
	}

	public double highMHz()
	{
		return centerMHz + widthMHz / 2.0;
	}

	/**
	 * Non-overlapping column used on the plot. 2.4 GHz channels overlap in RF
	 * (20 MHz) but are numbered every 5 MHz; the column is {@code center ± 2.5}.
	 * 5 GHz 20 MHz channels do not overlap, so the column is the occupancy.
	 */
	public double slotHalfMHz()
	{
		return WifiChannelPlan.BAND_24.equals(band) ? 2.5 : widthMHz / 2.0;
	}

	public double slotLowMHz()
	{
		return centerMHz - slotHalfMHz();
	}

	public double slotHighMHz()
	{
		return centerMHz + slotHalfMHz();
	}

	public boolean centerIn(double startMHz, double endMHz)
	{
		return centerMHz >= startMHz && centerMHz <= endMHz;
	}

	/** True if the 20 MHz occupied slice overlaps {@code [startMHz, endMHz]}. */
	public boolean occupancyOverlaps(double startMHz, double endMHz)
	{
		return highMHz() > startMHz && lowMHz() < endMHz;
	}

	public String label()
	{
		return Integer.toString(number);
	}

	@Override
	public String toString()
	{
		return "ch" + number + "@" + centerMHz;
	}
}
