package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.BandScan;
import hotiron.core.NfcActivity;

/**
 * One-line NFC classification on the spectrum plot.
 */
public final class NfcHud
{
	private static final Color FILL = new Color(20, 20, 20, 160);
	private static final Color TEXT = new Color(230, 230, 230, 240);

	private NfcHud()
	{
	}

	public static String text(NfcActivity activity, BandScan scan)
	{
		if (scan == BandScan.NFC)
			return "NFC scan — 13.56 then 27.12 / 40.68 harmonics · click a header tick to stop";
		if (activity == null || !activity.visible)
			return "";
		return activity.summary();
	}

	public static void paint(Graphics2D g0, Rectangle2D area, NfcActivity activity, BandScan scan)
	{
		if (g0 == null || area == null || area.getWidth() < 8)
			return;
		String s = text(activity, scan);
		if (s == null || s.isEmpty())
			return;
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			int w = Math.min(g.getFontMetrics().stringWidth(s) + 16, (int) area.getWidth() - 16);
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
