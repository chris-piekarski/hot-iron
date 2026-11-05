package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.OperatorBrief;

/**
 * One-line agent title at the bottom of the spectrum plot. Overlay only.
 */
public final class OperatorBriefHud
{
	private static final Color INK = new Color(230, 220, 180, 235);
	private static final Color BACK = new Color(18, 16, 12, 180);

	private OperatorBriefHud()
	{
	}

	public static void paint(Graphics2D g, Rectangle2D area, OperatorBrief brief)
	{
		if (g == null || area == null || brief == null || brief.isEmpty())
			return;
		String line = brief.title.isEmpty() ? clip(brief.body, 80) : brief.title;
		if (line.isEmpty())
			return;
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setFont(OperatorTheme.ui(Font.BOLD, 12f));
			int w = Math.min(g2.getFontMetrics().stringWidth(line) + 12, (int) area.getWidth() - 16);
			int h = 18;
			int x = (int) area.getX() + 8;
			int y = (int) (area.getMaxY() - h - 6);
			g2.setColor(BACK);
			g2.fillRoundRect(x - 4, y, w, h, 6, 6);
			g2.setColor(INK);
			g2.drawString(line, x, y + 13);
		}
		finally
		{
			g2.dispose();
		}
	}

	static String clip(String s, int max)
	{
		if (s == null)
			return "";
		String t = s.strip();
		if (t.length() <= max)
			return t;
		return t.substring(0, max);
	}
}
