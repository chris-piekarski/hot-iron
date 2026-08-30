package hotiron.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.DoubleConsumer;

import javax.swing.JComponent;

/**
 * 1970s Simpson / VU meter: dark housing, recessed cream D-window, arc
 * scale, pivoting needle. 0 sits left, 1 sits right. The cream is the
 * window, not the whole component.
 */
public final class AnalogGauge extends JComponent
{
	static final int PREF_W = 230;
	static final int PREF_H = 88;
	/** Kept for callers that still size by height. */
	static final int PREF = PREF_H;
	/** Mathematical degrees: left rest → right full. */
	public static final double START_DEG = 155;
	public static final double SWEEP_DEG = 130;

	private static final Color HOUSING = new Color(36, 32, 28);
	private static final Color RING = new Color(150, 144, 128);
	private static final Color RECESS = new Color(10, 8, 6);
	private static final Color FACE = new Color(236, 220, 176);
	private static final Color INK = new Color(28, 22, 16);
	private static final Color NEEDLE = new Color(196, 28, 22);
	private static final Color NEEDLE_SHADOW = new Color(40, 16, 12, 110);
	private static final Color HUB = new Color(28, 28, 28);
	private static final Color TITLE = new Color(214, 204, 184);
	private static final Color GLASS = new Color(255, 255, 255, 38);

	private final String title;
	private final String[] ticks;
	private float value01;
	private DoubleConsumer onSelect01;

	public AnalogGauge(String title, String[] ticks)
	{
		this.title = title == null ? "" : title;
		this.ticks = ticks == null ? new String[0] : ticks.clone();
		setOpaque(false);
		setPreferredSize(new Dimension(PREF_W, PREF_H));
		setMinimumSize(new Dimension(110, PREF_H));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, PREF_H));
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (javax.swing.SwingUtilities.isLeftMouseButton(e))
					pick(e);
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (javax.swing.SwingUtilities.isLeftMouseButton(e))
					pick(e);
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	public void setOnSelect01(DoubleConsumer onSelect01)
	{
		this.onSelect01 = onSelect01;
	}

	public void setValue01(float value01)
	{
		float v = value01;
		if (v < 0f)
			v = 0f;
		if (v > 1f)
			v = 1f;
		if (Math.abs(this.value01 - v) < 0.001f)
			return;
		this.value01 = v;
		repaint();
	}

	public float getValue01()
	{
		return value01;
	}

	public static double needleDeg(float value01)
	{
		float t = value01;
		if (t < 0f)
			t = 0f;
		if (t > 1f)
			t = 1f;
		return START_DEG - SWEEP_DEG * t;
	}

	public static float valueForDeg(double deg)
	{
		double t = (START_DEG - deg) / SWEEP_DEG;
		if (t < 0)
			t = 0;
		if (t > 1)
			t = 1;
		return (float) t;
	}

	static int windowX(int width)
	{
		return Math.max(8, width / 18);
	}

	static int windowY(int height)
	{
		return Math.max(6, height / 14);
	}

	static int windowW(int width)
	{
		return width - 2 * windowX(width);
	}

	static int windowH(int height)
	{
		int title = Math.max(14, height / 7);
		return Math.max(24, height - windowY(height) - title - 4);
	}

	static int pivotX(int width)
	{
		return width / 2;
	}

	static int pivotY(int height)
	{
		return windowY(height) + windowH(height) - 4;
	}

	static int arcRadius(int width, int height)
	{
		int cxPad = windowW(width) / 2 - 14;
		int cyPad = pivotY(height) - windowY(height) - 8;
		int r = Math.min(cxPad, cyPad);
		return Math.max(18, r);
	}

	private void pick(MouseEvent e)
	{
		if (onSelect01 == null)
			return;
		int w = getWidth();
		int h = getHeight();
		int cx = pivotX(w);
		int cy = pivotY(h);
		double dx = e.getX() - cx;
		double dy = cy - e.getY();
		double deg = Math.toDegrees(Math.atan2(dy, dx));
		if (deg < 0)
			deg += 360;
		/* Meter lives in the upper arc; wrap so 155…25 is monotonic. */
		double a = deg;
		if (a > 180)
			a -= 360;
		onSelect01.accept(valueForDeg(a));
	}

	@Override
	protected void paintComponent(Graphics g0)
	{
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			if (w < 16 || h < 16)
				return;
			int margin = 3;
			g.setColor(HOUSING);
			g.fillRoundRect(margin, margin, w - 2 * margin, h - 2 * margin, 12, 12);
			g.setColor(RING);
			g.setStroke(new BasicStroke(2.0f));
			g.drawRoundRect(margin + 1, margin + 1, w - 2 * margin - 2, h - 2 * margin - 2, 10, 10);
			int wx = windowX(w);
			int wy = windowY(h);
			int ww = windowW(w);
			int wh = windowH(h);
			g.setColor(RECESS);
			g.fillRoundRect(wx - 2, wy - 2, ww + 4, wh + 4, 8, 8);
			Shape window = new RoundRectangle2D.Float(wx, wy, ww, wh, 6, 6);
			g.setColor(FACE);
			g.fill(window);
			int cx = pivotX(w);
			int cy = pivotY(h);
			int arcR = arcRadius(w, h);
			g.setClip(window);
			g.setColor(INK);
			g.setStroke(new BasicStroke(1.8f));
			g.draw(new Arc2D.Double(cx - arcR, cy - arcR, arcR * 2, arcR * 2, START_DEG - SWEEP_DEG, SWEEP_DEG,
					Arc2D.OPEN));
			int n = Math.max(2, ticks.length);
			g.setFont(faceFont(Font.BOLD, 9f));
			for (int i = 0; i < n; i++)
			{
				float t = i / (float) (n - 1);
				strokeTick(g, cx, cy, arcR, t, i == 0 || i == n - 1 ? 2.0f : 1.3f, 8);
				if (i < ticks.length && ticks[i] != null)
				{
					double rad = Math.toRadians(needleDeg(t));
					int lx = cx + (int) Math.round(Math.cos(rad) * (arcR - 16));
					int ly = cy - (int) Math.round(Math.sin(rad) * (arcR - 16));
					int tw = g.getFontMetrics().stringWidth(ticks[i]);
					g.drawString(ticks[i], lx - tw / 2, ly + 3);
				}
				if (i < n - 1)
				{
					for (int k = 1; k < 5; k++)
						strokeTick(g, cx, cy, arcR, t + (k / 5f) / (n - 1), 0.9f, 4);
				}
			}
			double nrad = Math.toRadians(needleDeg(value01));
			int nlen = arcR - 2;
			int nx = cx + (int) Math.round(Math.cos(nrad) * nlen);
			int ny = cy - (int) Math.round(Math.sin(nrad) * nlen);
			g.setColor(NEEDLE_SHADOW);
			g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(cx + 1, cy + 1, nx + 1, ny + 1);
			g.setColor(NEEDLE);
			g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(cx, cy, nx, ny);
			g.setColor(GLASS);
			g.fillRoundRect(wx + 4, wy + 3, ww / 2, Math.max(10, wh / 3), 8, 8);
			g.setClip(null);
			g.setColor(HUB);
			g.fillOval(cx - 6, cy - 6, 12, 12);
			g.setColor(RING);
			g.fillOval(cx - 2, cy - 2, 4, 4);
			g.setColor(TITLE);
			g.setFont(faceFont(Font.BOLD, 11f));
			int tw = g.getFontMetrics().stringWidth(title);
			g.drawString(title, cx - tw / 2, h - 7);
		}
		finally
		{
			g.dispose();
		}
	}

	private Font faceFont(int style, float size)
	{
		Font base = getFont();
		if (base != null)
			return base.deriveFont(style, size);
		return new Font(Font.SANS_SERIF, style, Math.round(size));
	}

	private static void strokeTick(Graphics2D g, int cx, int cy, int arcR, float t01, float width, int length)
	{
		double rad = Math.toRadians(needleDeg(t01));
		int inner = arcR - 1;
		int outer = arcR + length;
		int x0 = cx + (int) Math.round(Math.cos(rad) * inner);
		int y0 = cy - (int) Math.round(Math.sin(rad) * inner);
		int x1 = cx + (int) Math.round(Math.cos(rad) * outer);
		int y1 = cy - (int) Math.round(Math.sin(rad) * outer);
		g.setStroke(new BasicStroke(width));
		g.drawLine(x0, y0, x1, y1);
	}
}
