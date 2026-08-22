package hotiron.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 640×360 RGB “video” of parked IQ: a short spectrogram strip plus a
 * full-frame, auto-scaled spectrum so the panel is never a black card.
 */
public final class WatchPreview
{
	public static final int WIDTH = MpegTsPlayer.WIDTH;
	public static final int HEIGHT = MpegTsPlayer.HEIGHT;
	public static final float PALETTE_START_DB = -80f;
	public static final float PALETTE_SIZE_DB = 80f;
	static final int WATER_H = 80;
	private static final Color BG = new Color(10, 14, 24);
	private static final Color FILL = new Color(255, 186, 64);
	private static final Color GRID = new Color(60, 70, 90);
	private static final Color TEXT = new Color(230, 230, 230);

	private final BufferedImage image;
	private int frames;

	public WatchPreview()
	{
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		reset();
	}

	public int frames()
	{
		return frames;
	}

	public BufferedImage image()
	{
		return image;
	}

	public synchronized void reset()
	{
		Graphics2D g = image.createGraphics();
		g.setColor(BG);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.dispose();
		frames = 0;
	}

	/**
	 * Paint {@code db} (fftshifted) as a new spectrogram line and a
	 * filled spectrum. Returns the same image instance.
	 */
	public synchronized BufferedImage pushDb(float[] db)
	{
		if (db == null || db.length < 2)
			return image;
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		scrollWater(g);
		paintWaterRow(g, db);
		paintSpectrum(g, db);
		g.dispose();
		frames++;
		return image;
	}

	private void scrollWater(Graphics2D g)
	{
		if (WATER_H < 4)
			return;
		g.copyArea(0, 0, WIDTH, WATER_H - 3, 0, 3);
	}

	private void paintWaterRow(Graphics2D g, float[] db)
	{
		int n = db.length;
		for (int x = 0; x < WIDTH; x++)
		{
			int i = (int) ((long) x * n / WIDTH);
			if (i >= n)
				i = n - 1;
			g.setColor(new Color(rgbForDb(db[i])));
			g.fillRect(x, 0, 1, 3);
		}
	}

	private void paintSpectrum(Graphics2D g, float[] db)
	{
		int top = WATER_H;
		int plotH = HEIGHT - top;
		g.setColor(BG);
		g.fillRect(0, top, WIDTH, plotH);
		g.setColor(GRID);
		g.drawLine(WIDTH / 2, top, WIDTH / 2, HEIGHT);
		g.drawLine(0, HEIGHT - 1, WIDTH, HEIGHT - 1);

		int n = db.length;
		float lo = db[0];
		float hi = db[0];
		for (int i = 1; i < n; i++)
		{
			if (db[i] < lo)
				lo = db[i];
			if (db[i] > hi)
				hi = db[i];
		}
		if (hi - lo < 12f)
			hi = lo + 12f;

		int base = HEIGHT - 4;
		int usable = plotH - 28;
		if (usable < 8)
			usable = 8;
		int[] xs = new int[n + 2];
		int[] ys = new int[n + 2];
		xs[0] = 0;
		ys[0] = base;
		for (int i = 0; i < n; i++)
		{
			int x = (int) ((long) i * (WIDTH - 1) / (n - 1));
			float u = (db[i] - lo) / (hi - lo);
			if (u < 0)
				u = 0;
			if (u > 1)
				u = 1;
			int y = base - (int) Math.round(u * usable);
			xs[i + 1] = x;
			ys[i + 1] = y;
		}
		xs[n + 1] = WIDTH - 1;
		ys[n + 1] = base;
		g.setColor(FILL);
		g.fillPolygon(xs, ys, n + 2);
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(FILL.brighter());
		g.drawPolyline(java.util.Arrays.copyOfRange(xs, 1, n + 1), java.util.Arrays.copyOfRange(ys, 1, n + 1),
				n);

		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		g.setColor(TEXT);
		g.drawString("IQ VIDEO", 12, top + 20);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		g.setColor(new Color(180, 190, 210));
		g.drawString(String.format("peak %.0f dBFS", hi), 12, top + 38);
	}

	static int rgbForDb(float db)
	{
		double u = (db - PALETTE_START_DB) / PALETTE_SIZE_DB;
		if (u < 0)
			u = 0;
		if (u > 1)
			u = 1;
		return heatBgr(u);
	}

	/** Blue → cyan → yellow → white, packed as 0xRRGGBB. */
	static int heatBgr(double u)
	{
		int r, g, b;
		if (u < 0.33)
		{
			double t = u / 0.33;
			r = 0;
			g = (int) Math.round(180 * t);
			b = (int) Math.round(80 + 140 * t);
		}
		else if (u < 0.66)
		{
			double t = (u - 0.33) / 0.33;
			r = (int) Math.round(255 * t);
			g = (int) Math.round(180 + 75 * t);
			b = (int) Math.round(220 * (1 - t));
		}
		else
		{
			double t = (u - 0.66) / 0.34;
			r = 255;
			g = (int) Math.round(255 * (0.7 + 0.3 * t));
			b = (int) Math.round(255 * t);
		}
		return (r << 16) | (g << 8) | b;
	}
}
