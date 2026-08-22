package hotiron.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Grafana-style drag-select band on the spectrum plot.
 */
public final class SpectrumZoomOverlay
{
	private static final Color FILL = new Color(130, 200, 255, 55);
	private static final Color EDGE = new Color(130, 200, 255, 200);

	private volatile double x1 = Double.NaN;
	private volatile double x2 = Double.NaN;

	public void setSelection(double pixelA, double pixelB)
	{
		x1 = pixelA;
		x2 = pixelB;
	}

	public void clear()
	{
		x1 = Double.NaN;
		x2 = Double.NaN;
	}

	public boolean isActive()
	{
		return Double.isFinite(x1) && Double.isFinite(x2);
	}

	public void paint(Graphics2D g0, Rectangle2D area)
	{
		if (area == null || !isActive() || area.getWidth() < 2)
			return;
		double left = Math.min(x1, x2);
		double right = Math.max(x1, x2);
		left = Math.max(area.getMinX(), left);
		right = Math.min(area.getMaxX(), right);
		if (right - left < 1)
			return;
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setClip(area);
			int x = (int) Math.round(left);
			int w = Math.max(1, (int) Math.round(right - left));
			int y = (int) Math.round(area.getMinY());
			int h = Math.max(1, (int) Math.round(area.getHeight()));
			g.setColor(FILL);
			g.fillRect(x, y, w, h);
			g.setColor(EDGE);
			g.drawLine(x, y, x, y + h);
			g.drawLine(x + w, y, x + w, y + h);
		}
		finally
		{
			g.dispose();
		}
	}
}
