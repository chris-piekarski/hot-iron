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

import hotiron.core.FmChannelPlan;
import hotiron.core.FmStationHit;

class FmChannelOverlayTest {

	@Test
	void paintDrawsDetectedStationsAndSkipsEmpty() {
		BufferedImage img = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		NumberAxis axis = new NumberAxis();
		axis.setRange(88, 108);
		Rectangle2D area = new Rectangle2D.Double(40, 20, 720, 250);
		assertDoesNotThrow(() -> FmChannelOverlay.paint(g, area, axis, RectangleEdge.BOTTOM, 88, 108, List.of()));
		assertEquals(0, img.getRGB(400, 30));
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f);
		assertDoesNotThrow(() -> FmChannelOverlay.paint(g, area, axis, RectangleEdge.BOTTOM, 88, 108, List.of(hit)));
		g.dispose();
		boolean painted = false;
		for (int x = 40; x < 760 && !painted; x++) {
			int argb = img.getRGB(x, 30);
			painted = ((argb >>> 24) & 0xFF) > 0;
		}
		assertTrue(painted, "expected a divider on the detected station");
	}

	@Test
	void tagsReadableOnlyAtFmScale() {
		assertTrue(FmChannelOverlay.tagsReadable(88, 108));
		assertTrue(FmChannelOverlay.tagsReadable(96, 102));
		assertTrue(FmChannelOverlay.tagsReadable(80, 110));
		assertFalse(FmChannelOverlay.tagsReadable(1, 7250));
		assertFalse(FmChannelOverlay.tagsReadable(30, 300));
		assertFalse(FmChannelOverlay.tagsReadable(80, 200));
		assertFalse(FmChannelOverlay.tagsReadable(2402, 2472));
	}

	@Test
	void paintSkipsWhenZoomedOutPastTheFmBand() {
		BufferedImage img = new BufferedImage(900, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		NumberAxis axis = new NumberAxis();
		axis.setRange(1, 7250);
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f);
		FmChannelOverlay.paint(g, new Rectangle2D.Double(0, 0, 900, 200), axis, RectangleEdge.BOTTOM, 1, 7250,
				List.of(hit));
		g.dispose();
		assertEquals(0, img.getRGB(450, 10), "station tags must not paint on a multi-band view");
	}

	@Test
	void paintSkipsWhenRangeHasNoFm() {
		BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		NumberAxis axis = new NumberAxis();
		axis.setRange(2402, 2472);
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f);
		FmChannelOverlay.paint(g, new Rectangle2D.Double(0, 0, 200, 100), axis, RectangleEdge.BOTTOM, 2402, 2472,
				List.of(hit));
		g.dispose();
		assertEquals(0, img.getRGB(100, 10));
	}

	@Test
	void higherConfidenceIsMoreOpaque() {
		java.awt.Color base = new java.awt.Color(80, 160, 230, 200);
		assertTrue(FmChannelOverlay.withConfidence(base, 1f).getAlpha() > FmChannelOverlay
				.withConfidence(base, 0.5f).getAlpha());
	}

	@Test
	void overlapsDetectsGap() {
		List<double[]> placed = new ArrayList<>();
		placed.add(new double[] { 10, 20 });
		assertTrue(FmChannelOverlay.overlaps(placed, 15, 25));
		assertFalse(FmChannelOverlay.overlaps(placed, 40, 50));
	}
}
