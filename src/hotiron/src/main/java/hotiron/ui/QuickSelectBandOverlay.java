package hotiron.ui;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import hotiron.core.FrequencyAxis;

/**
 * Quick Select ranges as vertical bands when the plot is zoomed out past
 * a single preset. Policy lives in {@link QuickSelectBandLayer}; paint is
 * {@link BandHeaderPainter}.
 */
public final class QuickSelectBandOverlay
{
	static final int HEADER_H = BandHeaderPainter.HEADER_H;
	static final int MIN_LABEL_GAP_PX = BandHeaderPainter.MIN_LABEL_GAP_PX;

	private QuickSelectBandOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, QuickSelectBandLayer.marks(axis));
	}

	static float labelBaselineY(Rectangle2D area, FontMetrics fm)
	{
		return BandHeaderPainter.labelBaselineY(area, fm);
	}

	static boolean overlaps(List<double[]> placed, double left, double right)
	{
		return BandHeaderPainter.overlaps(placed, left, right);
	}
}
