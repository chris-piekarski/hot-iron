package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.FrequencyAxis;

/**
 * Quick Select ranges as vertical bands when the plot is zoomed out past
 * a single preset. Policy lives in {@link QuickSelectBandLayer}; paint is
 * {@link BandHeaderPainter}.
 */
public final class QuickSelectBandOverlay
{
	private QuickSelectBandOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, QuickSelectBandLayer.marks(axis));
	}
}
