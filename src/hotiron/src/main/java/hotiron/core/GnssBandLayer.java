package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * RNSS L1 catalog ticks plus live occupancy.
 */
public final class GnssBandLayer
{
	public static final double MAX_VIEW_SPAN_MHZ = GnssBandPlan.MAX_CLASSIFY_SPAN_MHZ;

	private GnssBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		return GnssBandPlan.viewIsGnss(startMHz, endMHz);
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis, GnssActivity activity)
	{
		if (!tagsReadable(axis))
			return List.of();
		List<BandMark> out = new ArrayList<>();
		for (GnssBandPlan.GnssSignal s : GnssBandPlan.visibleSignals(axis.startMHz, axis.endMHz))
		{
			boolean live = activity != null && activity.visible && live(s, activity);
			BandMark.Style style = live ? BandMark.Style.TUNED : BandMark.Style.PRIMARY;
			float intensity = live ? Math.max(0.6f, activity.confidence) : 0.8f;
			out.add(new BandMark(s.lowMHz(), s.highMHz(), s.centerMhz, s.label, style, false, false, true,
					BandMark.LabelFit.DROP_IF_OVERLAP, intensity));
		}
		return out;
	}

	private static boolean live(GnssBandPlan.GnssSignal s, GnssActivity activity)
	{
		if (s == GnssBandPlan.GPS_L1)
			return activity.gpsL1;
		if (s == GnssBandPlan.BDS_B1I)
			return activity.bdsB1;
		if (s == GnssBandPlan.GLO_L1)
			return activity.gloL1;
		return false;
	}
}
