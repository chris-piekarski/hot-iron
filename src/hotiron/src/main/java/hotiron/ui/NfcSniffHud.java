package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.NfcFrame;

/**
 * One-line parked NFC HUD. Overlay only — same FrequencyAxis as the ticks.
 */
public final class NfcSniffHud
{
	private static final Color INK = new Color(255, 220, 140, 235);
	private static final Color BACK = new Color(18, 16, 10, 180);

	private NfcSniffHud()
	{
	}

	public static String text(NfcFrame last, boolean fieldOn)
	{
		if (last == null)
			return fieldOn ? "NFC sniff · field on · waiting for frames (loop antenna)"
					: "NFC sniff · parked 11.56 / 10 MS/s · no field yet";
		return "NFC sniff · " + last.line();
	}

	public static void paint(Graphics2D g, Rectangle2D area, NfcFrame last, boolean fieldOn)
	{
		if (g == null || area == null)
			return;
		String line = text(last, fieldOn);
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			int x = (int) area.getX() + 8;
			int y = (int) area.getY() + 18;
			int w = g2.getFontMetrics().stringWidth(line) + 12;
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
