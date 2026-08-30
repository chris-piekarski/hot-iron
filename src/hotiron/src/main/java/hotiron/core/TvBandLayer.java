package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Live ATSC 6 MHz occupants as {@link BandMark}s. Hidden on a wide survey.
 */
public final class TvBandLayer
{
	/** U-TV is 138 MHz; V-TV envelope is 162 MHz. */
	public static final double MAX_VIEW_SPAN_MHZ = 180;

	private TvBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz)
			return false;
		if (endMHz - startMHz > MAX_VIEW_SPAN_MHZ)
			return false;
		return !TvChannelPlan.visibleOccupancy(startMHz, endMHz).isEmpty();
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis, List<TvStationHit> stations, Integer selectedFcc)
	{
		if (!tagsReadable(axis))
			return List.of();
		List<BandMark> out = new ArrayList<>();
		boolean haveTuned = false;
		if (stations != null)
		{
			for (TvStationHit hit : stations)
			{
				if (hit == null || hit.channel == null)
					continue;
				TvChannel ch = hit.channel;
				if (!ch.occupancyOverlaps(axis.startMHz, axis.endMHz))
					continue;
				boolean tuned = selectedFcc != null && ch.fccChannel == selectedFcc.intValue();
				haveTuned |= tuned;
				BandMark.Style style = tuned ? BandMark.Style.TUNED : styleOf(hit.grade);
				float intensity = tuned ? 1f
						: hit.grade == TvChannelGrade.PICTURE ? 1f
								: hit.grade == TvChannelGrade.NO_LOCK ? 0.35f : hit.confidence;
				out.add(new BandMark(ch.lowMHz, ch.highMHz(), ch.centerMHz(), ch.label(), style, false,
						false, true, BandMark.LabelFit.DROP_IF_OVERLAP, intensity));
			}
		}
		if (!haveTuned && selectedFcc != null)
		{
			TvChannel ch = TvChannelPlan.findByFccChannel(selectedFcc.intValue());
			if (ch != null && ch.occupancyOverlaps(axis.startMHz, axis.endMHz))
				out.add(0, new BandMark(ch.lowMHz, ch.highMHz(), ch.centerMHz(), ch.label(), BandMark.Style.TUNED,
						false, false, true, BandMark.LabelFit.DROP_IF_OVERLAP, 1f));
		}
		return out;
	}

	static BandMark.Style styleOf(TvChannelGrade grade)
	{
		if (grade == TvChannelGrade.PICTURE || grade == TvChannelGrade.ATSC_LIKE)
			return BandMark.Style.PRIMARY;
		return BandMark.Style.SECONDARY;
	}
}
