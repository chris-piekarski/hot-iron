package hotiron.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import hotiron.core.FrequencyRange;
import hotiron.core.SpectrumSurveyAxis;
import hotiron.ui.SurveyChipLayout.Chip;
import hotiron.ui.SurveyChipLayout.Side;

/**
 * Full-span survey: chirp HackRF min→max, color fills/dividers matching
 * chips above and below.
 */
public final class SpectrumWavePainter
{
	static final Color BG = new Color(10, 12, 16);
	static final Color WINDOW = new Color(255, 186, 64, 120);
	static final Color WINDOW_EDGE = new Color(255, 214, 80, 240);
	/** Wi‑Fi 2 (70 MHz) occupies this fraction of the strip so −/+ is visible. */
	static final double READABLE_WINDOW_FRACTION = 0.08;
	static final double READABLE_WINDOW_SPAN_MHZ = 70;
	static final Color TICK = new Color(255, 244, 214, 200);
	static final Color TICK_DIM = new Color(170, 170, 180, 140);
	static final Color RULE = new Color(40, 36, 32);

	private SpectrumWavePainter()
	{
	}

	public static void paint(Graphics2D g, Rectangle2D area, FrequencyRange window, Iterable<Chip> chips,
			QuickSelectPreset selected)
	{
		if (g == null || area == null || area.getWidth() < 8 || area.getHeight() < 8)
			return;
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(BG);
			g2.fill(area);
			int x0 = (int) Math.floor(area.getX());
			int y0 = (int) Math.floor(area.getY());
			int w = (int) Math.floor(area.getWidth());
			int h = (int) Math.floor(area.getHeight());
			if (chips != null)
				paintSpans(g2, x0, y0, w, h, chips, selected);
			if (window != null)
				paintWindow(g2, x0, y0, w, h, window);
			paintWave(g2, x0, y0, w, h);
			if (chips != null)
				paintDividers(g2, x0, y0, w, h, chips, selected);
			paintTicks(g2, x0, y0, w, h);
			g2.setColor(RULE);
			g2.drawLine(x0, y0, x0 + w, y0);
			g2.drawLine(x0, y0 + h - 1, x0 + w, y0 + h - 1);
		}
		finally
		{
			g2.dispose();
		}
	}

	/**
	 * Gold sweep-window pixels {@code [x0, x1)} on a strip of {@code width}.
	 * Uses the true log span when that is already readable (FM, HF, All).
	 * Narrow microwave windows (Wi‑Fi 2 is a few pixels on the log axis)
	 * get a floor that grows with {@code sqrt(span)} so − / + is visible.
	 */
	public static int[] windowPixels(FrequencyRange window, int width)
	{
		int w = Math.max(1, width);
		if (window == null)
			return new int[] { 0, 0 };
		double start = window.getStartMHz();
		double end = window.getEndMHz();
		int a = SpectrumSurveyAxis.mhzToX(start, w);
		int b = SpectrumSurveyAxis.mhzToX(end, w);
		if (b < a)
		{
			int t = a;
			a = b;
			b = t;
		}
		int trueW = Math.max(2, b - a);
		int visW = Math.max(trueW, readableWidth(window.spanMHz(), w));
		int mid = SpectrumSurveyAxis.mhzToX(SpectrumSurveyAxis.bandCenterMHz(start, end), w);
		int va = mid - visW / 2;
		int vb = va + visW;
		if (va < 0)
		{
			vb = Math.min(w, vb - va);
			va = 0;
		}
		if (vb > w)
		{
			va = Math.max(0, va - (vb - w));
			vb = w;
		}
		if (vb - va < 2)
			vb = Math.min(w, va + 2);
		return new int[] { va, vb };
	}

	static int readableWidth(int spanMHz, int width)
	{
		double span = Math.max(1, spanMHz);
		double frac = READABLE_WINDOW_FRACTION * Math.sqrt(span / READABLE_WINDOW_SPAN_MHZ);
		return Math.max(8, (int) Math.round(frac * Math.max(1, width)));
	}

	private static void paintWindow(Graphics2D g2, int x0, int y0, int w, int h, FrequencyRange window)
	{
		int[] x = windowPixels(window, w);
		int a = x0 + x[0];
		int b = x0 + x[1];
		if (b - a < 2)
			b = a + 2;
		g2.setColor(WINDOW);
		g2.fillRect(a, y0, b - a, h);
		g2.setColor(WINDOW_EDGE);
		g2.setStroke(new BasicStroke(2.0f));
		g2.drawLine(a, y0, a, y0 + h);
		g2.drawLine(b, y0, b, y0 + h);
		g2.setStroke(new BasicStroke(1.4f));
		g2.drawLine(a, y0, b, y0);
		g2.drawLine(a, y0 + h - 1, b, y0 + h - 1);
	}

	private static void paintSpans(Graphics2D g2, int x0, int y0, int w, int h, Iterable<Chip> chips,
			QuickSelectPreset selected)
	{
		for (Chip chip : chips)
		{
			if (chip == null || chip.preset == QuickSelectPreset.ALL)
				continue;
			boolean on = chip.preset == selected;
			int a = x0 + Math.max(0, Math.min(w - 1, chip.spanX0));
			int b = x0 + Math.max(0, Math.min(w, chip.spanX1));
			if (b - a < 2)
				b = a + 2;
			g2.setColor(SpectrumSurveyStyle.fill(chip.preset, on));
			int y = y0;
			int hh = h;
			if (chip.side == Side.TOP)
			{
				y = y0;
				hh = Math.max(8, h / 2);
			}
			else
			{
				hh = Math.max(8, h / 2);
				y = y0 + h - hh;
			}
			g2.fillRect(a, y, b - a, hh);
		}
	}

	private static void paintDividers(Graphics2D g2, int x0, int y0, int w, int h, Iterable<Chip> chips,
			QuickSelectPreset selected)
	{
		g2.setStroke(new BasicStroke(1.2f));
		for (Chip chip : chips)
		{
			if (chip == null || chip.preset == QuickSelectPreset.ALL)
				continue;
			boolean on = chip.preset == selected;
			g2.setColor(SpectrumSurveyStyle.line(chip.preset, on));
			int a = x0 + Math.max(0, Math.min(w - 1, chip.spanX0));
			int b = x0 + Math.max(0, Math.min(w - 1, chip.spanX1));
			int y1 = y0;
			int y2 = y0 + h;
			if (chip.side == Side.TOP)
				y2 = y0 + h / 2 + 4;
			else
				y1 = y0 + h / 2 - 4;
			if (on)
				g2.setStroke(new BasicStroke(2.2f));
			else
				g2.setStroke(new BasicStroke(1.2f));
			g2.drawLine(a, y1, a, y2);
			g2.drawLine(b, y1, b, y2);
			g2.drawLine(a, chip.side == Side.TOP ? y0 : y0 + h - 1, b,
					chip.side == Side.TOP ? y0 : y0 + h - 1);
		}
	}

	private static void paintWave(Graphics2D g2, int x0, int y0, int w, int h)
	{
		double cy = y0 + h * 0.50;
		double amp = Math.max(6, h * 0.22);
		g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		double phase = 0;
		int prevX = x0;
		double prevY = cy;
		for (int i = 0; i < w; i++)
		{
			double frac = w <= 1 ? 0 : i / (double) (w - 1);
			phase += 2 * Math.PI * (0.035 + 0.62 * frac * frac);
			double y = cy + amp * Math.sin(phase);
			int r = (int) Math.round(90 + frac * (255 - 90));
			int g = (int) Math.round(170 + frac * (210 - 170));
			int b = (int) Math.round(255 + frac * (90 - 255));
			g2.setColor(new Color(clampRgb(r), clampRgb(g), clampRgb(b), 210));
			if (i > 0)
				g2.drawLine(prevX, (int) Math.round(prevY), x0 + i, (int) Math.round(y));
			prevX = x0 + i;
			prevY = y;
		}
	}

	private static int clampRgb(int v)
	{
		if (v < 0)
			return 0;
		if (v > 255)
			return 255;
		return v;
	}

	private static void paintTicks(Graphics2D g2, int x0, int y0, int w, int h)
	{
		g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		FontMetrics fm = g2.getFontMetrics();
		int base = y0 + h / 2 + fm.getAscent() / 2 - 1;
		for (SpectrumSurveyAxis.Tick tick : SpectrumSurveyAxis.ticks())
		{
			int x = x0 + SpectrumSurveyAxis.mhzToX(tick.mhz, w);
			g2.setColor(TICK_DIM);
			g2.drawLine(x, y0 + h / 2 - 10, x, y0 + h / 2 + 10);
			g2.setColor(TICK);
			int tw = fm.stringWidth(tick.label);
			int tx = x - tw / 2;
			if (tx < x0 + 2)
				tx = x0 + 2;
			if (tx + tw > x0 + w - 2)
				tx = x0 + w - 2 - tw;
			g2.drawString(tick.label, tx, base + 14);
		}
	}
}
