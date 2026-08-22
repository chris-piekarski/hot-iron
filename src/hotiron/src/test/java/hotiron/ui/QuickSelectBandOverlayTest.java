package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.junit.jupiter.api.Test;

class QuickSelectBandOverlayTest {

	@Test
	void paintOnAWideViewMarksTheImage() {
		BufferedImage img = new BufferedImage(900, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		NumberAxis axis = new NumberAxis();
		axis.setRange(1, 7250);
		Rectangle2D area = new Rectangle2D.Double(20, 10, 860, 270);
		assertDoesNotThrow(
				() -> QuickSelectBandOverlay.paint(g, area, axis, RectangleEdge.BOTTOM, 1, 7250));
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
		NumberAxis axis = new NumberAxis();
		axis.setRange(88, 108);
		QuickSelectBandOverlay.paint(g, new Rectangle2D.Double(0, 0, 400, 200), axis, RectangleEdge.BOTTOM, 88, 108);
		g.dispose();
		assertEquals(0, img.getRGB(200, 20));
	}

	@Test
	void labelsSitInTheTopHeaderLikeChannelDividers() {
		java.awt.Font font = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 10);
		BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		java.awt.FontMetrics fm = img.createGraphics().getFontMetrics(font);
		Rectangle2D area = new Rectangle2D.Double(20, 10, 860, 270);
		float y = QuickSelectBandOverlay.labelBaselineY(area, fm);
		assertTrue(y < area.getMinY() + QuickSelectBandOverlay.HEADER_H);
		assertTrue(y < area.getCenterY());
		assertTrue(y > area.getMinY());
	}

	@Test
	void overlapsDetectsGap() {
		List<double[]> placed = new ArrayList<>();
		placed.add(new double[] { 10, 20 });
		assertTrue(QuickSelectBandOverlay.overlaps(placed, 15, 25));
		assertFalse(QuickSelectBandOverlay.overlaps(placed, 40, 50));
	}
}
