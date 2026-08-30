package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Wi-Fi 20 MHz occupancy as {@link BandMark}s. Hidden when a channel is
 * only a few pixels wide (survey zoom).
 */
public final class WifiBandLayer
{
	public static final double MIN_PX_PER_20MHZ_24 = 8;
	public static final double MIN_PX_PER_20MHZ_5 = 4;

	private WifiBandLayer()
	{
	}

	public static boolean showBand24(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && 20 * axis.pxPerMHz() >= MIN_PX_PER_20MHZ_24;
	}

	public static boolean showBand5(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && 20 * axis.pxPerMHz() >= MIN_PX_PER_20MHZ_5;
	}

	public static boolean showBand6(FrequencyAxis axis)
	{
		return showBand5(axis);
	}

	public static List<BandMark> marks(FrequencyAxis axis)
	{
		if (axis == null || !axis.usable())
			return List.of();
		boolean show24 = showBand24(axis);
		boolean show5 = showBand5(axis);
		boolean show6 = showBand6(axis);
		if (!show24 && !show5 && !show6)
			return List.of();
		List<WifiChannel> visible = new ArrayList<>();
		for (WifiChannel ch : WifiChannelPlan.visibleOccupancy(axis.startMHz, axis.endMHz))
		{
			if (WifiChannelPlan.BAND_24.equals(ch.band) && show24)
				visible.add(ch);
			else if (WifiChannelPlan.BAND_5.equals(ch.band) && show5)
				visible.add(ch);
			else if (WifiChannelPlan.BAND_6.equals(ch.band) && show6)
				visible.add(ch);
		}
		List<BandMark> out = new ArrayList<>();
		for (WifiChannel ch : WifiChannelPlan.labelPriority(visible))
		{
			BandMark.Style style = ch.primary ? BandMark.Style.PRIMARY : BandMark.Style.SECONDARY;
			BandMark.LabelFit fit = WifiChannelPlan.BAND_24.equals(ch.band) ? BandMark.LabelFit.DROP_IF_OVERLAP
					: BandMark.LabelFit.FIT_OCCUPANCY;
			out.add(new BandMark(ch.lowMHz(), ch.highMHz(), ch.centerMHz, ch.label(), style, false, true,
					ch.centerIn(axis.startMHz, axis.endMHz), fit, 1f));
		}
		return out;
	}
}
