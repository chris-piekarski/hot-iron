package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import hotiron.core.FmBandLayer;
import hotiron.core.FmChannelPlan;
import hotiron.core.FmStationHit;

class FmChannelOverlayTest {

	@Test
	void paintDrawsDetectedStationsAndSkipsEmpty() {
		BufferedImage img = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle2D area = new Rectangle2D.Double(40, 20, 720, 250);
		assertDoesNotThrow(() -> FmChannelOverlay.paint(g, area, 88, 108, List.of(), null));
		assertEquals(0, img.getRGB(400, 30));
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f);
		assertDoesNotThrow(() -> FmChannelOverlay.paint(g, area, 88, 108, List.of(hit), null));
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
		assertTrue(FmBandLayer.tagsReadable(88, 108));
		assertTrue(FmBandLayer.tagsReadable(96, 102));
		assertTrue(FmBandLayer.tagsReadable(80, 110));
		assertFalse(FmBandLayer.tagsReadable(1, 7250));
		assertFalse(FmBandLayer.tagsReadable(30, 300));
		assertFalse(FmBandLayer.tagsReadable(80, 200));
		assertFalse(FmBandLayer.tagsReadable(2402, 2472));
	}

	@Test
	void paintSkipsWhenZoomedOutPastTheFmBand() {
		BufferedImage img = new BufferedImage(900, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f);
		FmChannelOverlay.paint(g, new Rectangle2D.Double(0, 0, 900, 200), 1, 7250, List.of(hit), null);
		g.dispose();
		assertEquals(0, img.getRGB(450, 10), "station tags must not paint on a multi-band view");
	}

	@Test
	void paintSkipsWhenRangeHasNoFm() {
		BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f);
		FmChannelOverlay.paint(g, new Rectangle2D.Double(0, 0, 200, 100), 2402, 2472, List.of(hit), null);
		g.dispose();
		assertEquals(0, img.getRGB(100, 10));
	}
}
