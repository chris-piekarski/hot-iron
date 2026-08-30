package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class AnalogGaugeTest
{
	@Test
	void needleSweepsLeftToRightAsValueRises()
	{
		assertEquals(AnalogGauge.START_DEG, AnalogGauge.needleDeg(0f), 0.01);
		assertEquals(AnalogGauge.START_DEG - AnalogGauge.SWEEP_DEG, AnalogGauge.needleDeg(1f), 0.01);
		assertTrue(AnalogGauge.needleDeg(0f) > AnalogGauge.needleDeg(0.5f));
		assertTrue(AnalogGauge.needleDeg(0.5f) > AnalogGauge.needleDeg(1f));
		assertEquals(0.5f, AnalogGauge.valueForDeg(AnalogGauge.needleDeg(0.5f)), 0.002f);
	}

	@Test
	void valueClamps()
	{
		AnalogGauge g = new AnalogGauge("GAIN", new String[] { "0", "10" });
		g.setValue01(-1f);
		assertEquals(0f, g.getValue01(), 0.001f);
		g.setValue01(2f);
		assertEquals(1f, g.getValue01(), 0.001f);
	}

	@Test
	void dragReports01()
	{
		AnalogGauge g = new AnalogGauge("FREQ", new String[] { "88", "108" });
		AtomicReference<Double> got = new AtomicReference<>();
		g.setOnSelect01(v -> got.set(Double.valueOf(v)));
		g.setSize(AnalogGauge.PREF_W, AnalogGauge.PREF_H);
		/* Click the right side of the window (high value). */
		java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(g,
				java.awt.event.MouseEvent.MOUSE_PRESSED, 0, java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
				200, 36, 1, false, java.awt.event.MouseEvent.BUTTON1);
		g.dispatchEvent(e);
		assertNotNull(got.get());
		assertTrue(got.get().doubleValue() > 0.4, "right-of-pivot should be a high gauge reading");
	}

	@Test
	void faceIsARecessedWindowNotACreamSlab()
	{
		BufferedImage img = paint(0.5f);
		int w = img.getWidth();
		int h = img.getHeight();
		int housing = img.getRGB(4, 4);
		int wx = AnalogGauge.windowX(w);
		int wy = AnalogGauge.windowY(h);
		int ww = AnalogGauge.windowW(w);
		int wh = AnalogGauge.windowH(h);
		int cream = 0;
		int creamRgb = 0;
		for (int x = wx + 10; x < wx + ww / 3; x++)
		{
			for (int y = wy + 12; y < wy + wh / 2; y++)
			{
				int p = img.getRGB(x, y);
				int L = luma(p);
				if (L > cream)
				{
					cream = L;
					creamRgb = p;
				}
			}
		}
		assertTrue(luma(housing) < 80, "housing is a dark bezel, not cream");
		assertTrue(cream > luma(housing) + 60, "cream lives in the window, not the whole face");
		assertTrue(red(creamRgb) > 180 && green(creamRgb) > 160, "window is cream");
	}

	@Test
	void midscaleNeedleIsRedInTheWindow()
	{
		BufferedImage img = paint(0.5f);
		int w = img.getWidth();
		int h = img.getHeight();
		int cx = AnalogGauge.pivotX(w);
		int cy = AnalogGauge.pivotY(h);
		int arcR = AnalogGauge.arcRadius(w, h);
		double rad = Math.toRadians(AnalogGauge.needleDeg(0.5f));
		int nx = cx + (int) Math.round(Math.cos(rad) * arcR * 0.62);
		int ny = cy - (int) Math.round(Math.sin(rad) * arcR * 0.62);
		boolean found = false;
		for (int dx = -3; dx <= 3 && !found; dx++)
		{
			for (int dy = -3; dy <= 3; dy++)
			{
				int x = nx + dx;
				int y = ny + dy;
				if (x < 0 || y < 0 || x >= w || y >= h)
					continue;
				int p = img.getRGB(x, y);
				if (red(p) > 140 && red(p) > green(p) + 20 && red(p) > blue(p) + 20)
					found = true;
			}
		}
		assertTrue(found, "mid-scale needle should show as red inside the window");
	}

	private static BufferedImage paint(float value01)
	{
		AnalogGauge g = new AnalogGauge("GAIN", new String[] { "0", "2", "4", "6", "8", "10" });
		g.setSize(AnalogGauge.PREF_W, AnalogGauge.PREF_H);
		g.setValue01(value01);
		BufferedImage img = new BufferedImage(AnalogGauge.PREF_W, AnalogGauge.PREF_H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();
		try
		{
			g.paint(g2);
		}
		finally
		{
			g2.dispose();
		}
		return img;
	}

	private static int luma(int rgb)
	{
		return (red(rgb) * 3 + green(rgb) * 6 + blue(rgb)) / 10;
	}

	private static int red(int rgb)
	{
		return (rgb >> 16) & 0xff;
	}

	private static int green(int rgb)
	{
		return (rgb >> 8) & 0xff;
	}

	private static int blue(int rgb)
	{
		return rgb & 0xff;
	}
}
