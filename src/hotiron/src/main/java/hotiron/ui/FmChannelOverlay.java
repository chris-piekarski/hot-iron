package hotiron.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import hotiron.core.FmBandLayer;
import hotiron.core.FmStationHit;
import hotiron.core.FrequencyAxis;

/**
 * Live US FM stations on the spectrum plot. Policy lives in
 * {@link FmBandLayer}; paint is {@link BandHeaderPainter}.
 */
public final class FmChannelOverlay
{
	static final double MAX_VIEW_SPAN_MHZ = FmBandLayer.MAX_VIEW_SPAN_MHZ;

	private FmChannelOverlay()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		return FmBandLayer.tagsReadable(startMHz, endMHz);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz, List<FmStationHit> stations)
	{
		paint(g0, area, domain, edge, startMHz, endMHz, stations, null);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz, List<FmStationHit> stations, Integer selectedKHz)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, FmBandLayer.marks(axis, stations, selectedKHz));
	}

	static Color withConfidence(Color base, float confidence)
	{
		return BandHeaderPainter.withIntensity(base, confidence);
	}

	static boolean overlaps(List<double[]> placed, double left, double right)
	{
		return BandHeaderPainter.overlaps(placed, left, right);
	}
}
