package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class SpectrumZoomOverlayTest {

	@Test
	void paintMarksTheDragBandAndClearRemovesIt() {
		SpectrumZoomOverlay overlay = new SpectrumZoomOverlay();
		assertFalse(overlay.isActive());
		overlay.setSelection(50, 200);
		assertTrue(overlay.isActive());
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		assertDoesNotThrow(() -> overlay.paint(g, new Rectangle2D.Double(0, 0, 400, 200)));
		g.dispose();
		boolean painted = false;
		for (int x = 50; x < 200 && !painted; x++)
			painted = ((img.getRGB(x, 20) >>> 24) & 0xFF) > 0;
		assertTrue(painted);
		overlay.clear();
		assertFalse(overlay.isActive());
	}
}
