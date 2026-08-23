package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class QuickSelectBandOverlayTest {

	@Test
	void paintOnAWideViewMarksTheImage() {
		BufferedImage img = new BufferedImage(900, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle2D area = new Rectangle2D.Double(20, 10, 860, 270);
		assertDoesNotThrow(() -> QuickSelectBandOverlay.paint(g, area, 1, 7250));
		g.dispose();
		boolean painted = false;
		for (int x = 20; x < 880 && !painted; x++)
			painted = ((img.getRGB(x, 20) >>> 24) & 0xFF) > 0;
		assertTrue(painted, "expected Quick Select band fills on a full-span view");
	}

	@Test
	void paintSkipsWhenASinglePresetFillsTheView() {
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		QuickSelectBandOverlay.paint(g, new Rectangle2D.Double(0, 0, 400, 200), 88, 108);
		g.dispose();
		assertEquals(0, img.getRGB(200, 20));
	}
}
