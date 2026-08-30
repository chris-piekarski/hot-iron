package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class TvVideoPanelTest
{
	@Test
	void paintsWithoutThrowing()
	{
		TvVideoPanel p = new TvVideoPanel();
		p.setSize(320, 180);
		p.setStatus("WATCH ch 33");
		BufferedImage img = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try
		{
			p.paint(g);
		}
		finally
		{
			g.dispose();
		}
	}

	@Test
	void heightFollowsSixteenByNineOfThePanelWidth()
	{
		assertEquals(16f / 9f, TvVideoPanel.DEFAULT_ASPECT, 1e-4);
		assertEquals(180, TvVideoPanel.heightForWidth(320, TvVideoPanel.DEFAULT_ASPECT));
		assertEquals(270, TvVideoPanel.heightForWidth(480, TvVideoPanel.DEFAULT_ASPECT));
		TvVideoPanel p = new TvVideoPanel();
		p.setSize(480, 800);
		assertEquals(270, p.getPreferredSize().height);
		assertEquals(270, p.getMaximumSize().height);
	}

	@Test
	void letterboxKeepsTheFrameAspectInATallPanel()
	{
		java.awt.Rectangle r = TvVideoPanel.destRect(480, 800, 640, 360);
		assertEquals(480, r.width);
		assertEquals(270, r.height);
		assertEquals(0, r.x);
		assertEquals((800 - 270) / 2, r.y);
		java.awt.Rectangle wide = TvVideoPanel.destRect(800, 180, 640, 360);
		assertEquals(180, wide.height);
		assertEquals(320, wide.width);
		assertEquals((800 - 320) / 2, wide.x);
	}

	@Test
	void incomingFrameAspectReplacesTheDefault()
	{
		TvVideoPanel p = new TvVideoPanel();
		assertEquals(TvVideoPanel.DEFAULT_ASPECT, p.aspect(), 1e-4);
		BufferedImage sd = new BufferedImage(704, 480, BufferedImage.TYPE_INT_RGB);
		p.setFrame(sd);
		assertEquals(704 / 480f, p.aspect(), 1e-3);
		p.clear();
		assertEquals(TvVideoPanel.DEFAULT_ASPECT, p.aspect(), 1e-4);
	}
}
