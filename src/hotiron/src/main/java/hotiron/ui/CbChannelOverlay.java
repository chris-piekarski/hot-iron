package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import hotiron.core.CbBandLayer;
import hotiron.core.CbChannelHit;
import hotiron.core.FrequencyAxis;

/**
 * US CB 11 m ticks. Policy lives in {@link CbBandLayer}.
 */
public final class CbChannelOverlay
{
	private CbChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz, List<CbChannelHit> live,
			Integer selected)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, CbBandLayer.marks(axis, live, selected));
	}
}
