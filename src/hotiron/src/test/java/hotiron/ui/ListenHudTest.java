package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class ListenHudTest {

	@Test
	void textNamesTheDialAndSaysSweepPaused() {
		assertEquals("Listening 97.3 FM — sweep paused", ListenHud.text(97.3));
		assertEquals("Listening 97.3 FM — sweep paused (no speakers)", ListenHud.text(97.3, false));
	}

	@Test
	void paintDoesNotThrow() {
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		assertDoesNotThrow(() -> ListenHud.paint(g, new Rectangle2D.Double(20, 10, 360, 160), 97.3));
		g.dispose();
	}
}
