package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import hotiron.core.FmBandLayer;
import hotiron.core.FmStationHit;
import hotiron.core.FrequencyAxis;

/**
 * Live US FM stations on the spectrum plot. Policy lives in
 * {@link FmBandLayer}; paint is {@link BandHeaderPainter}.
 */
public final class FmChannelOverlay
{
	private FmChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz,
			double endMHz, List<FmStationHit> stations, Integer selectedKHz)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, FmBandLayer.marks(axis, stations, selectedKHz));
	}
}
