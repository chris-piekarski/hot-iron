package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import hotiron.core.FrequencyAxis;
import hotiron.core.WifiBandLayer;

/**
 * 20 MHz 802.11 occupancy on the spectrum plot. Policy lives in
 * {@link WifiBandLayer}; paint is {@link BandHeaderPainter}.
 */
public final class WifiChannelOverlay
{
	private WifiChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, WifiBandLayer.marks(axis));
	}

	static boolean overlaps(List<double[]> placed, double left, double right)
	{
		return BandHeaderPainter.overlaps(placed, left, right);
	}
}
