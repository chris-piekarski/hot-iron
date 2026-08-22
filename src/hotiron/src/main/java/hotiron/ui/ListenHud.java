package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

/**
 * Spectrum overlay while the radio is parked as a WFM receiver.
 */
public final class ListenHud
{
	private static final Color FILL = new Color(20, 20, 20, 160);
	private static final Color TEXT = new Color(230, 230, 230, 240);

	private ListenHud()
	{
	}

	public static String text(double mhz)
	{
		return text(mhz, true);
	}

	public static String text(double mhz, boolean audioOk)
	{
		if (audioOk)
			return String.format(Locale.US, "Listening %.1f FM — sweep paused", mhz);
		return String.format(Locale.US, "Listening %.1f FM — sweep paused (no speakers)", mhz);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double mhz)
	{
		paint(g0, area, mhz, true);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, double mhz, boolean audioOk)
	{
		if (g0 == null || area == null || area.getWidth() < 8)
			return;
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
			String s = text(mhz, audioOk);
			int w = g.getFontMetrics().stringWidth(s) + 16;
			int h = g.getFontMetrics().getHeight() + 6;
			int x = (int) Math.round(area.getMinX() + 8);
			int y = (int) Math.round(area.getMinY() + BandHeaderPainter.HEADER_H + 8);
			g.setColor(FILL);
			g.fillRoundRect(x, y, w, h, 8, 8);
			g.setColor(TEXT);
			g.drawString(s, x + 8, y + g.getFontMetrics().getAscent() + 2);
		}
		finally
		{
			g.dispose();
		}
	}
}
