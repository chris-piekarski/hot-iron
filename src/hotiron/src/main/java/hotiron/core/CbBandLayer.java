package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * All 40 CB ticks; live occupancy is brighter. Selected channel is TUNED.
 */
public final class CbBandLayer
{
	public static final double MAX_VIEW_SPAN_MHZ = CbChannelPlan.MAX_CLASSIFY_SPAN_MHZ;

	private CbBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		return CbChannelPlan.viewIsCb(startMHz, endMHz);
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis, List<CbChannelHit> live, Integer selected)
	{
		if (!tagsReadable(axis))
			return List.of();
		boolean[] active = new boolean[CbChannelPlan.LAST_CHANNEL + 1];
		float[] conf = new float[CbChannelPlan.LAST_CHANNEL + 1];
		if (live != null)
		{
			for (CbChannelHit hit : live)
			{
				if (hit == null || hit.channel == null)
					continue;
				active[hit.channel.number] = true;
				conf[hit.channel.number] = hit.confidence;
			}
		}
		int sel = selected == null ? 0 : selected.intValue();
		List<BandMark> out = new ArrayList<>();
		for (CbChannel ch : CbChannelPlan.CHANNELS)
		{
			if (!ch.occupancyOverlaps(axis.startMHz, axis.endMHz))
				continue;
			boolean tuned = ch.number == sel;
			boolean on = active[ch.number];
			BandMark.Style style = tuned ? BandMark.Style.TUNED : on ? BandMark.Style.PRIMARY : BandMark.Style.SECONDARY;
			float intensity = tuned ? 1f : on ? Math.max(0.55f, conf[ch.number]) : 0.35f;
			out.add(new BandMark(ch.lowMHz(), ch.highMHz(), ch.centerMHz(), ch.label(), style, false, false, true,
					BandMark.LabelFit.DROP_IF_OVERLAP, intensity));
		}
		return out;
	}
}
