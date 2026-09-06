package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.CbChannel;
import hotiron.core.CbChannelPlan;

/**
 * One-line CB channel HUD while parked or scanning.
 */
public final class CbHud
{
	private static final Color FILL = new Color(20, 20, 20, 160);
	private static final Color TEXT = new Color(255, 210, 120, 240);

	private CbHud()
	{
	}

	public static String text(int channel, boolean listening, boolean scanning)
	{
		CbChannel ch = CbChannelPlan.clamp(channel);
		if (scanning)
			return "CB scan — ch " + ch.number + " · hopping AM · stops on chatter";
		if (listening)
			return "CB " + ch.number + "  " + ch.freqLabel() + " AM";
		return "CB 11 m  ·  Scan hops AM and stops on chatter";
	}

	public static void paint(Graphics2D g0, Rectangle2D area, int channel, boolean listening, boolean scanning)
	{
		if (g0 == null || area == null || area.getWidth() < 8)
			return;
		String s = text(channel, listening, scanning);
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
