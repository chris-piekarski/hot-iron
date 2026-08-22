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

	public static String text(int fccChannel, boolean locked)
	{
		return text(fccChannel, locked, 0f);
	}

	public static String text(int fccChannel, boolean locked, float snrDb)
	{
		return text(fccChannel, locked, snrDb, 0, 0);
	}

	public static String text(int fccChannel, boolean locked, float snrDb, int packets, int frames)
	{
		return text(fccChannel, locked, snrDb, packets, frames, 0);
	}

	public static String text(int fccChannel, boolean locked, float snrDb, int packets, int mpegFrames,
			int previewFrames)
	{
		if (mpegFrames > 0)
			return String.format(Locale.US, "WATCH ch %d — live", fccChannel);
		if (locked && snrDb >= 5f)
			return String.format(Locale.US, "WATCH ch %d — ATSC lock  %.0f dB", fccChannel, snrDb);
		if (locked)
			return String.format(Locale.US, "WATCH ch %d — 8VSB weak  %.0f dB (no picture)", fccChannel,
					snrDb);
		if (previewFrames > 0)
			return String.format(Locale.US, "WATCH ch %d — IQ video", fccChannel);
		if (packets > 0)
			return String.format(Locale.US, "WATCH ch %d — 8VSB  %d TS pkt (no picture)", fccChannel,
					packets);
		return String.format(Locale.US, "WATCH ch %d — no ATSC lock", fccChannel);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, int fccChannel, boolean locked)
	{
		paint(g0, area, fccChannel, locked, 0f);
	}

	public static void paint(Graphics2D g0, Rectangle2D area, int fccChannel, boolean locked, float snrDb)
	{
		paint(g0, area, fccChannel, locked, snrDb, 0, 0, 0);
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
