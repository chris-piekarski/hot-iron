package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * BLE advertising / ANT+ ticks. Policy only; paint is BandHeaderPainter.
 */
public final class BleBandLayer
{
	private BleBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		return BleBandPlan.viewShowsOverlay(startMHz, endMHz);
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis)
	{
		if (!tagsReadable(axis))
			return List.of();
		List<BandMark> out = new ArrayList<>();
		for (BleBandPlan.BleFeature f : BleBandPlan.visibleOverlay(axis.startMHz, axis.endMHz))
		{
			BandMark.Style style = f.advertising ? BandMark.Style.PRIMARY : BandMark.Style.SECONDARY;
			out.add(new BandMark(f.lowMHz(), f.highMHz(), f.centerMhz, f.label, style, false, false, true,
					BandMark.LabelFit.DROP_IF_OVERLAP, f.advertising ? 0.9f : 0.55f));
		}
		return out;
	}
}
