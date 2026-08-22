package hotiron.core;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * One linear map from frequency (MHz) to plot X (pixels). Chart overlays,
 * zoom-drag, and the waterfall use this instead of each inventing a scale.
 */
public final class FrequencyAxis
{
	public static final double MIN_USABLE_WIDTH_PX = 8;

	public final double startMHz;
	public final double endMHz;
	public final double minX;
	public final double width;

	public FrequencyAxis(double startMHz, double endMHz, double minX, double width)
	{
		this.startMHz = startMHz;
		this.endMHz = endMHz;
		this.minX = minX;
		this.width = width;
	}

	public static FrequencyAxis fromArea(Rectangle2D area, double startMHz, double endMHz)
	{
		if (area == null)
			return new FrequencyAxis(startMHz, endMHz, 0, 0);
		return new FrequencyAxis(startMHz, endMHz, area.getMinX(), area.getWidth());
	}

	public static FrequencyAxis of(double startMHz, double endMHz, double widthPx)
	{
		return new FrequencyAxis(startMHz, endMHz, 0, widthPx);
	}

	public double spanMHz()
	{
		return endMHz - startMHz;
	}

	public double maxX()
	{
		return minX + width;
	}

	public boolean usable()
	{
		return width >= MIN_USABLE_WIDTH_PX && spanMHz() > 0;
	}

	public double pxPerMHz()
	{
		double span = spanMHz();
		if (span <= 0 || width <= 0)
			return 0;
		return width / span;
	}

	public double mhzToX(double mhz)
	{
		double span = spanMHz();
		if (span <= 0)
			return minX;
		return minX + ((mhz - startMHz) / span) * width;
	}

	public int mhzToXInt(double mhz)
	{
		return (int) Math.round(mhzToX(mhz));
	}

	public double xToMhz(double x)
	{
		if (width <= 0)
			return startMHz;
		double u = (x - minX) / width;
		if (u < 0)
			u = 0;
		if (u > 1)
			u = 1;
		return startMHz + u * spanMHz();
	}

	public boolean containsMhz(double mhz)
	{
		return mhz >= startMHz && mhz <= endMHz;
	}

	public double clipLow(double bandLowMHz)
	{
		return Math.max(bandLowMHz, startMHz);
	}

	public double clipHigh(double bandHighMHz)
	{
		return Math.min(bandHighMHz, endMHz);
	}

	public boolean occupancyVisible(double bandLowMHz, double bandHighMHz)
	{
		return clipHigh(bandHighMHz) > clipLow(bandLowMHz);
	}

	public int occupancyWidthPx(double bandLowMHz, double bandHighMHz)
	{
		double lo = clipLow(bandLowMHz);
		double hi = clipHigh(bandHighMHz);
		if (hi <= lo)
			return 0;
		return Math.max(1, Math.abs(mhzToXInt(hi) - mhzToXInt(lo)));
	}

	public Optional<FrequencyRange> zoomFromDrag(double pixelA, double pixelB)
	{
		return SpectrumZoom.fromDrag(pixelA, pixelB, new Rectangle2D.Double(minX, 0, width, 1), startMHz, endMHz);
	}
}
