package hotiron.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import hotiron.core.FrequencyAxis;
import hotiron.core.TvBandLayer;
import hotiron.core.TvStationHit;

/**
 * Live US ATSC 6 MHz occupants. Policy is {@link TvBandLayer}.
 */
public final class TvChannelOverlay
{
	private TvChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double startMHz, double endMHz,
			List<TvStationHit> stations, Integer selectedFcc)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, TvBandLayer.marks(axis, stations, selectedFcc));
	}
}
