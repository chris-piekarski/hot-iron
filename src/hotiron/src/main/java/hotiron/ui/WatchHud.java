package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

/**
 * Spectrum overlay while the radio is parked as an ATSC receiver.
 */
public final class WatchHud
{
	private static final Color FILL = new Color(20, 20, 20, 160);
	private static final Color TEXT = new Color(230, 230, 230, 240);

	private WatchHud()
	{
	}

	public static String text(int fccChannel, boolean locked, float snrDb, int packets, int mpegFrames,
			int previewFrames)
	{
		if (mpegFrames > 0)
			return parked(fccChannel, "live");
		if (locked && snrDb >= 5f)
			return parked(fccChannel, String.format(Locale.US, "ATSC lock  %.0f dB", snrDb));
		if (locked)
			return parked(fccChannel, String.format(Locale.US, "8VSB weak  %.0f dB (no picture)", snrDb));
		if (previewFrames > 0)
			return parked(fccChannel, "IQ video");
		if (packets > 0)
			return parked(fccChannel, String.format(Locale.US, "8VSB  %d TS pkt (no picture)", packets));
		return parked(fccChannel, "no ATSC lock");
	}

	private static String parked(int fccChannel, String detail)
	{
		return String.format(Locale.US, "WATCH ch %d — parked IQ · %s", fccChannel, detail);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, int fccChannel, boolean locked, float snrDb,
			int packets, int mpegFrames, int previewFrames)
	{
		if (g0 == null || area == null || area.getWidth() < 8)
			return;
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
			String s = text(fccChannel, locked, snrDb, packets, mpegFrames, previewFrames);
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
