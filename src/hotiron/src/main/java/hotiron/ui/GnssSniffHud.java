package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import hotiron.core.GpsSvHit;

/**
 * One-line parked GPS L1 HUD. Overlay only.
 */
public final class GnssSniffHud
{
	private static final Color INK = new Color(180, 220, 255, 235);
	private static final Color BACK = new Color(12, 16, 22, 180);

	private GnssSniffHud()
	{
	}

	public static String text(List<GpsSvHit> hits)
	{
		if (hits == null || hits.isEmpty())
			return "GPS L1 sniff · parked 1575.42 / 4 MS/s · waiting for C/A peaks (not a fix)";
		StringBuilder sb = new StringBuilder("GPS L1 sniff · ");
		sb.append(hits.size()).append(hits.size() == 1 ? " PRN" : " PRNs");
		int n = Math.min(hits.size(), 6);
		for (int i = 0; i < n; i++)
		{
			sb.append(" · ").append(hits.get(i).prn);
			sb.append(String.format(java.util.Locale.US, " %.0f", hits.get(i).cn0DbHz));
		}
		if (hits.size() > n)
			sb.append(" · …");
		return sb.toString();
	}

	public static void paint(Graphics2D g, Rectangle2D area, List<GpsSvHit> hits)
	{
		if (g == null || area == null)
			return;
		String line = text(hits);
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setFont(OperatorTheme.ui(Font.BOLD, 12f));
			int x = (int) area.getX() + 8;
			int y = (int) area.getY() + 18;
			int w = Math.min(g2.getFontMetrics().stringWidth(line) + 12, (int) area.getWidth() - 16);
			int h = 18;
			g2.setColor(BACK);
			g2.fillRoundRect(x - 4, y - 13, w, h, 6, 6);
			g2.setColor(INK);
			g2.drawString(line, x, y);
		}
		finally
		{
			g2.dispose();
		}
	}
}
