package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.FrequencyAxis;
import hotiron.core.NfcActivity;
import hotiron.core.NfcBandLayer;

/**
 * NFC / 13.56 MHz ticks on the spectrum plot. Policy lives in
 * {@link NfcBandLayer}; paint is {@link BandHeaderPainter}.
 */
public final class NfcChannelOverlay
{
	private NfcChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz, NfcActivity activity)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, NfcBandLayer.marks(axis, activity));
	}
}
