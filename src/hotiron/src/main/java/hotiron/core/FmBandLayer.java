package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Live FM station hits as {@link BandMark}s. Hidden when the view is
 * wider than a single FM-scale window.
 */
public final class FmBandLayer
{
	public static final double MAX_VIEW_SPAN_MHZ = 30;

	private FmBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz)
			return false;
		if (endMHz - startMHz > MAX_VIEW_SPAN_MHZ)
			return false;
		return Math.min(endMHz, FmChannelPlan.VIEW_END_MHZ) > Math.max(startMHz, FmChannelPlan.VIEW_START_MHZ);
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis, List<FmStationHit> stations)
	{
		return marks(axis, stations, null);
	}

	public static List<BandMark> marks(FrequencyAxis axis, List<FmStationHit> stations, Integer selectedKHz)
	{
		if (!tagsReadable(axis) || stations == null || stations.isEmpty())
			return List.of();
		List<FmStationHit> hits = new ArrayList<>();
		for (FmStationHit hit : stations)
		{
			if (hit == null || hit.channel == null)
				continue;
			if (!hit.channel.occupancyOverlaps(axis.startMHz, axis.endMHz))
				continue;
			hits.add(hit);
		}
		hits.sort((a, b) -> {
			int byConf = Float.compare(b.confidence, a.confidence);
			return byConf != 0 ? byConf : Float.compare(b.powerDbm, a.powerDbm);
		});
		List<BandMark> out = new ArrayList<>();
		boolean haveTuned = false;
		for (FmStationHit hit : hits)
		{
			FmChannel ch = hit.channel;
			boolean tuned = selectedKHz != null && ch.centerKHz == selectedKHz.intValue();
			haveTuned |= tuned;
			out.add(new BandMark(ch.lowMHz(), ch.highMHz(), ch.centerMHz(), hit.label(),
					tuned ? BandMark.Style.TUNED : BandMark.Style.PRIMARY, false, false,
					ch.centerIn(axis.startMHz, axis.endMHz), BandMark.LabelFit.DROP_IF_OVERLAP,
					tuned ? 1f : hit.confidence));
		}
		if (!haveTuned && selectedKHz != null)
		{
			FmChannel ch = FmChannelPlan.findByCenterKHz(selectedKHz.intValue());
			if (ch != null && ch.occupancyOverlaps(axis.startMHz, axis.endMHz))
				out.add(0, new BandMark(ch.lowMHz(), ch.highMHz(), ch.centerMHz(), ch.label(), BandMark.Style.TUNED,
						false, false, ch.centerIn(axis.startMHz, axis.endMHz), BandMark.LabelFit.DROP_IF_OVERLAP, 1f));
		}
		return out;
	}
}
