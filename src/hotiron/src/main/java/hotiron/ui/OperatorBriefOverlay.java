package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.BleMacRow;
import hotiron.core.FrequencyAxis;
import hotiron.core.BandMark;
import hotiron.core.OperatorBrief;
import hotiron.core.OperatorBriefLayer;

/**
 * Agent marks + HUD, and the selected BLE MAC channel band.
 */
public final class OperatorBriefOverlay
{
	private OperatorBriefOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz, OperatorBrief brief)
	{
		paint(g0, area, startMHz, endMHz, brief, null);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz, OperatorBrief brief,
			BleMacRow bleSelect)
	{
		if (g0 == null || area == null)
			return;
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		java.util.ArrayList<BandMark> marks = new java.util.ArrayList<>();
		if (brief != null && !brief.isEmpty())
			marks.addAll(OperatorBriefLayer.marks(axis, brief));
		if (bleSelect != null && Double.isFinite(bleSelect.mhz()))
		{
			double mhz = bleSelect.mhz();
			String label = "ch" + bleSelect.channel;
			marks.add(new BandMark(mhz - 1, mhz + 1, mhz, label, BandMark.Style.TUNED, true, false, true,
					BandMark.LabelFit.DROP_IF_OVERLAP, 1f));
		}
		if (!marks.isEmpty())
			BandHeaderPainter.paint(g0, area, axis, marks);
		if (brief != null && !brief.isEmpty())
			OperatorBriefHud.paint(g0, area, brief);
	}
}
