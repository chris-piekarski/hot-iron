package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.BleBandLayer;
import hotiron.core.FrequencyAxis;

/**
 * BLE advertising / ANT+ ticks. Policy is {@link BleBandLayer}.
 */
public final class BleChannelOverlay
{
	private BleChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, BleBandLayer.marks(axis));
	}
}
