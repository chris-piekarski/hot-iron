package hotiron.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import hotiron.core.NfcEnvelopeTrace;

/**
 * Sidebar oscilloscope of the 13.56 MHz carrier after IF mix. X is the last
 * {@link NfcEnvelopeTrace#WINDOW_S} seconds (newest at the right), Y is dBFS.
 */
public final class NfcEnvelopeScope
{
	private static final Color TRACE = new Color(255, 186, 64);
	private static final Color FILL = new Color(255, 186, 64, 55);
	private static final Color FLOOR = new Color(255, 186, 64, 150);
	private static final Color AXIS = new Color(0xBB, 0xBB, 0xBB);
	private static final BasicStroke TRACE_STROKE = new BasicStroke(1.4f);
	private static final BasicStroke FLOOR_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
			1f, new float[] { 4f, 4f }, 0f);

	private NfcEnvelopeScope()
	{
	}

	public static String banner()
	{
		return "13.56  |IQ|  500 ms";
	}

	public static float[] yWindow(float[] db)
	{
		float peak = NfcEnvelopeTrace.EMPTY_DB;
		boolean any = false;
		if (db != null)
		{
			for (int i = 0; i < db.length; i++)
			{
				if (db[i] > NfcEnvelopeTrace.EMPTY_DB + 1f)
				{
					any = true;
					if (db[i] > peak)
						peak = db[i];
				}
			}
		}
		if (!any)
			return new float[] { -80f, 0f };
		float high = Math.min(0f, peak + 4f);
		float low = peak - 28f;
		float floorPad = NfcEnvelopeTrace.DECODER_FLOOR_DB - 8f;
		if (low > floorPad)
			low = floorPad;
		if (low < -90f)
			low = -90f;
		if (high - low < 16f)
			high = Math.min(0f, low + 16f);
		return new float[] { low, high };
	}

	public static int firstLive(float[] db)
	{
		if (db == null)
			return 0;
		for (int i = 0; i < db.length; i++)
		{
			if (db[i] > NfcEnvelopeTrace.EMPTY_DB + 1f)
				return i;
		}
		return db.length;
	}

	public static String hover(float[] db, float windowSec, int x, int plotWidth)
	{
		if (db == null || db.length == 0 || plotWidth <= 0 || !(windowSec > 0))
			return "—";
		double u = x / (double) plotWidth;
		if (u < 0)
			u = 0;
		if (u > 1)
			u = 1;
		int i = (int) Math.round(u * (db.length - 1));
		if (i < 0)
			i = 0;
		if (i >= db.length)
			i = db.length - 1;
		double ageMs = (1.0 - u) * windowSec * 1000.0;
		if (db[i] <= NfcEnvelopeTrace.EMPTY_DB + 1f)
			return String.format("−%.1f ms  —", ageMs);
		return String.format("−%.1f ms  %.1f dBFS", ageMs, db[i]);
	}

	public static void paint(Graphics2D g, Rectangle2D plot, float[] db, float windowSec)
	{
		if (g == null || plot == null || plot.getWidth() < 8 || plot.getHeight() < 8)
			return;
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			float[] yr = yWindow(db);
			float low = yr[0];
			float high = yr[1];
			paintTrace(g2, plot, db, low, high);
			paintFloor(g2, plot, low, high);
			paintTimeAxis(g2, plot, windowSec);
			paintDbScale(g2, plot, low, high);
		}
		finally
		{
			g2.dispose();
		}
	}

	static void paintTrace(Graphics2D g, Rectangle2D plot, float[] db, float low, float high)
	{
		if (db == null || db.length < 2 || high <= low)
			return;
		int first = firstLive(db);
		if (first >= db.length - 1)
			return;
		double x0 = plot.getX();
		double y0 = plot.getY();
		double w = plot.getWidth();
		double h = plot.getHeight() - 18;
		if (h < 8)
			return;
		Path2D.Double line = new Path2D.Double();
		Path2D.Double fill = new Path2D.Double();
		boolean started = false;
		double lastX = 0;
		double baseY = y0 + h;
		for (int i = first; i < db.length; i++)
		{
			double u = i / (double) (db.length - 1);
			double x = x0 + u * w;
			double y = y0 + (high - db[i]) / (high - low) * h;
			if (y < y0)
				y = y0;
			if (y > baseY)
				y = baseY;
			if (!started)
			{
				line.moveTo(x, y);
				fill.moveTo(x, baseY);
				fill.lineTo(x, y);
				started = true;
			}
			else
			{
				line.lineTo(x, y);
				fill.lineTo(x, y);
			}
			lastX = x;
		}
		if (!started)
			return;
		fill.lineTo(lastX, baseY);
		fill.closePath();
		g.setColor(FILL);
		g.fill(fill);
		g.setStroke(TRACE_STROKE);
		g.setColor(TRACE);
		g.draw(line);
	}

	static void paintFloor(Graphics2D g, Rectangle2D plot, float low, float high)
	{
		float floor = NfcEnvelopeTrace.DECODER_FLOOR_DB;
		if (floor <= low || floor >= high)
			return;
		double h = plot.getHeight() - 18;
		if (h < 8)
			return;
		double y = plot.getY() + (high - floor) / (high - low) * h;
		g.setStroke(FLOOR_STROKE);
		g.setColor(FLOOR);
		g.drawLine((int) plot.getX(), (int) y, (int) (plot.getX() + plot.getWidth()), (int) y);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		g.drawString("decode", (int) plot.getX() + 6, (int) y - 3);
	}

	static void paintTimeAxis(Graphics2D g, Rectangle2D plot, float windowSec)
	{
		if (!(windowSec > 0))
			return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		g.setColor(AXIS);
		int y = (int) (plot.getY() + plot.getHeight() - 2);
		int x0 = (int) plot.getX();
		int x1 = (int) (plot.getX() + plot.getWidth());
		g.drawLine(x0, y - 10, x1, y - 10);
		FontMetrics fm = g.getFontMetrics();
		int msMax = Math.round(windowSec * 1000f);
		int step = msMax <= 60 ? 10 : msMax <= 120 ? 20 : 50;
		for (int ms = 0; ms <= msMax; ms += step)
		{
			int x = x0 + (int) Math.round(plot.getWidth() * (ms / (windowSec * 1000.0)));
			g.drawLine(x, y - 14, x, y - 10);
			String lab = ms == msMax ? "now" : (ms == 0 ? ("−" + msMax + " ms") : String.format("−%d", msMax - ms));
			int tw = fm.stringWidth(lab);
			int tx = x - tw / 2;
			if (tx < x0)
				tx = x0;
			if (tx + tw > x1)
				tx = x1 - tw;
			g.drawString(lab, tx, y);
		}
	}

	static void paintDbScale(Graphics2D g, Rectangle2D plot, float low, float high)
	{
		int gutter = (int) plot.getX();
		if (gutter < 28)
			return;
		double h = plot.getHeight() - 18;
		if (h < 16)
			return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		g.setColor(AXIS);
		FontMetrics fm = g.getFontMetrics();
		int axisX = gutter - 1;
		int y0 = (int) plot.getY();
		g.drawLine(axisX, y0, axisX, (int) (y0 + h));
		int step = 10;
		int t0 = (int) Math.ceil(low / step) * step;
		int t1 = (int) Math.floor(high / step) * step;
		for (int db = t0; db <= t1; db += step)
		{
			int y = (int) (y0 + (high - db) / (high - low) * h);
			g.drawLine(axisX - 4, y, axisX, y);
			String lab = Integer.toString(db);
			int tw = fm.stringWidth(lab);
			int ty = y + fm.getAscent() / 2;
			g.drawString(lab, Math.max(2, axisX - 6 - tw), ty);
		}
	}
}
