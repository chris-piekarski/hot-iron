package hotiron.core;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * Grafana-style frequency zoom: drag a span to zoom in, expand or pop
 * history to zoom out. Integer MHz for the sweep-range window.
 */
public final class SpectrumZoom
{
	public static final int MIN_MHZ = 1;
	public static final int MAX_MHZ = 7250;
	public static final int MIN_SPAN_MHZ = 1;
	public static final int MIN_DRAG_PX = 8;
	public static final double ZOOM_IN_FACTOR = 0.5;
	public static final double ZOOM_OUT_FACTOR = 2.0;

	private SpectrumZoom()
	{
	}

	public static FrequencyRange clamp(int startMHz, int endMHz)
	{
		int start = Math.max(MIN_MHZ, Math.min(MAX_MHZ - MIN_SPAN_MHZ, startMHz));
		int end = Math.max(start + MIN_SPAN_MHZ, Math.min(MAX_MHZ, endMHz));
		if (end - start < MIN_SPAN_MHZ)
			end = Math.min(MAX_MHZ, start + MIN_SPAN_MHZ);
		if (end - start < MIN_SPAN_MHZ)
		{
			start = Math.max(MIN_MHZ, end - MIN_SPAN_MHZ);
			end = Math.min(MAX_MHZ, start + MIN_SPAN_MHZ);
		}
		return new FrequencyRange(start, end);
	}

	/**
	 * Horizontal pixel drag inside the plot {@code area} to an integer-MHz
	 * window. Empty if the drag is too short or the geometry is unusable.
	 */
	public static Optional<FrequencyRange> fromDrag(double pixelA, double pixelB, Rectangle2D area,
			double axisStartMHz, double axisEndMHz)
	{
		if (area == null || area.getWidth() < 1 || axisEndMHz <= axisStartMHz)
			return Optional.empty();
		if (Math.abs(pixelB - pixelA) < MIN_DRAG_PX)
			return Optional.empty();
		double f1 = pixelToMHz(pixelA, area, axisStartMHz, axisEndMHz);
		double f2 = pixelToMHz(pixelB, area, axisStartMHz, axisEndMHz);
		int start = (int) Math.floor(Math.min(f1, f2));
		int end = (int) Math.ceil(Math.max(f1, f2));
		FrequencyRange zoomed = clamp(start, end);
		if (zoomed.getEndMHz() - zoomed.getStartMHz() < MIN_SPAN_MHZ)
			return Optional.empty();
		return Optional.of(zoomed);
	}

	/** Shrink or grow {@code current} around {@code centerMHz}. */
	public static FrequencyRange around(FrequencyRange current, double centerMHz, double factor)
	{
		if (current == null || factor <= 0)
			return clamp(MIN_MHZ, MAX_MHZ);
		double span = (current.getEndMHz() - current.getStartMHz()) * factor;
		if (span < MIN_SPAN_MHZ)
			span = MIN_SPAN_MHZ;
		double mid = centerMHz;
		if (!Double.isFinite(mid))
			mid = (current.getStartMHz() + current.getEndMHz()) / 2.0;
		int start = (int) Math.floor(mid - span / 2.0);
		int end = (int) Math.ceil(mid + span / 2.0);
		return clamp(start, end);
	}

	/** Slide the window by a fraction of its span (positive = higher MHz). */
	public static FrequencyRange pan(FrequencyRange current, double fractionOfSpan)
	{
		if (current == null)
			return clamp(MIN_MHZ, MAX_MHZ);
		int span = current.getEndMHz() - current.getStartMHz();
		int shift = (int) Math.round(span * fractionOfSpan);
		if (shift == 0)
			shift = fractionOfSpan >= 0 ? 1 : -1;
		return clamp(current.getStartMHz() + shift, current.getEndMHz() + shift);
	}

	public static FrequencyRange expand(FrequencyRange current)
	{
		if (current == null)
			return clamp(MIN_MHZ, MAX_MHZ);
		double mid = (current.getStartMHz() + current.getEndMHz()) / 2.0;
		return around(current, mid, ZOOM_OUT_FACTOR);
	}

	static double pixelToMHz(double pixelX, Rectangle2D area, double axisStartMHz, double axisEndMHz)
	{
		return FrequencyAxis.fromArea(area, axisStartMHz, axisEndMHz).xToMhz(pixelX);
	}
}
