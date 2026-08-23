package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * NFC / 13.56 MHz catalog ticks plus the live classification mark.
 */
public final class NfcBandLayer
{
	public static final double MAX_VIEW_SPAN_MHZ = NfcBandPlan.MAX_CLASSIFY_SPAN_MHZ;

	private NfcBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		return NfcBandPlan.viewIsNfc(startMHz, endMHz);
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis, NfcActivity activity)
	{
		if (!tagsReadable(axis))
			return List.of();
		List<BandMark> out = new ArrayList<>();
		for (NfcFeature f : NfcBandPlan.visibleFeatures(axis.startMHz, axis.endMHz))
		{
			BandMark.Style style = f.sideband || f.harmonic ? BandMark.Style.SECONDARY : BandMark.Style.PRIMARY;
			boolean live = activity != null && activity.visible && !activity.label().isEmpty()
					&& Math.abs(f.centerMhz - (Float.isFinite(activity.carrierMhz) ? activity.carrierMhz
							: NfcBandPlan.CARRIER_MHZ)) < 0.08
					&& !f.sideband;
			if (live)
				style = BandMark.Style.TUNED;
			float intensity = live ? Math.max(0.55f, activity.confidence) : (f.sideband ? 0.55f : 0.85f);
			String label = live && !activity.label().isEmpty() ? activity.label() : f.label;
			out.add(new BandMark(f.lowMHz(), f.highMHz(), f.centerMhz, label, style, false, false, true,
					BandMark.LabelFit.DROP_IF_OVERLAP, intensity));
		}
		return out;
	}
}
