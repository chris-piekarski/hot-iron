package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.FrequencyAxis;
import hotiron.core.GnssActivity;
import hotiron.core.GnssBandLayer;

/**
 * RNSS L1 ticks on the spectrum plot. Policy lives in {@link GnssBandLayer}.
 */
public final class GnssChannelOverlay
{
	private GnssChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz, GnssActivity activity)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, GnssBandLayer.marks(axis, activity));
	}
}
