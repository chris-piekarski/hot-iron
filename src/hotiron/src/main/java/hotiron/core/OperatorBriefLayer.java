package hotiron.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent mark ticks. Policy only; paint is BandHeaderPainter.
 */
public final class OperatorBriefLayer
{
	private OperatorBriefLayer()
	{
	}

	public static List<BandMark> marks(FrequencyAxis axis, OperatorBrief brief)
	{
		if (axis == null || !axis.usable() || brief == null || brief.marks.isEmpty())
			return List.of();
		List<BandMark> out = new ArrayList<>();
		for (OperatorBrief.Mark m : brief.marks)
		{
			if (!axis.containsMhz(m.mhz))
				continue;
			String label = m.label.isEmpty() ? String.format(java.util.Locale.US, "%.3f", m.mhz) : m.label;
			out.add(new BandMark(m.mhz - 0.05, m.mhz + 0.05, m.mhz, label, BandMark.Style.SURVEY, false, false, true,
					BandMark.LabelFit.DROP_IF_OVERLAP, 0.85f));
		}
		return out;
	}
}
