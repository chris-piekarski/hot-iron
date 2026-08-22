package hotiron.ui;

import java.util.ArrayList;
import java.util.List;

import hotiron.core.BandMark;
import hotiron.core.FrequencyAxis;

/**
 * Quick Select presets as {@link BandMark}s when the view is wider than
 * a single button.
 */
public final class QuickSelectBandLayer
{
	public static final double MIN_VISIBLE_WIDTH_PX = 3;

	private QuickSelectBandLayer()
	{
	}

	public static List<BandMark> marks(FrequencyAxis axis)
	{
		if (axis == null || !axis.usable())
			return List.of();
		List<QuickSelectPreset> bands = QuickSelectPreset.visibleInView(axis.startMHz, axis.endMHz);
		if (bands.isEmpty())
			return List.of();
		List<BandMark> out = new ArrayList<>();
		for (QuickSelectPreset band : QuickSelectPreset.labelPriority(bands))
		{
			double low = band.visibleLowMHz(axis.startMHz, axis.endMHz);
			double high = band.visibleHighMHz(axis.startMHz, axis.endMHz);
			if (high <= low)
				continue;
			if ((high - low) * axis.pxPerMHz() < MIN_VISIBLE_WIDTH_PX)
				continue;
			BandMark.Style style = band.surveyEnvelope() ? BandMark.Style.SURVEY : BandMark.Style.PRIMARY;
			out.add(new BandMark(low, high, (low + high) / 2.0, band.label, style, true, true, false,
					BandMark.LabelFit.FIT_OCCUPANCY, 1f));
		}
		return out;
	}
}
