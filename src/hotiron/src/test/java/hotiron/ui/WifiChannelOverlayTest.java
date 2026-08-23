package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.junit.jupiter.api.Test;

import hotiron.core.WifiChannel;
import hotiron.core.WifiChannelPlan;

class WifiChannelOverlayTest {

	@Test
	void paintOnWifi2DoesNotThrowAndMarksTheImage() {
		BufferedImage img = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle2D area = new Rectangle2D.Double(40, 20, 720, 250);
		assertDoesNotThrow(() -> WifiChannelOverlay.paint(g, area, 2402, 2472));
		g.dispose();
		boolean painted = false;
		for (int x = 40; x < 760 && !painted; x++) {
			int argb = img.getRGB(x, 30);
			painted = ((argb >>> 24) & 0xFF) > 0;
		}
		assertTrue(painted, "expected channel ticks in the plot area");
	}

	@Test
	void wifi5OccupancyBandsAreEqualWidthOnTheViewAxis() {
		NumberAxis axis = new NumberAxis();
		axis.setAutoRange(false);
		axis.setLowerMargin(0);
		axis.setUpperMargin(0);
		axis.setRange(WifiChannelPlan.WIFI_5_VIEW_START_MHZ, WifiChannelPlan.WIFI_5_VIEW_END_MHZ);
		Rectangle2D area = new Rectangle2D.Double(0, 0, 725, 200);
		WifiChannel first = WifiChannelPlan.find(WifiChannelPlan.BAND_5, 36);
		WifiChannel last = WifiChannelPlan.find(WifiChannelPlan.BAND_5, 177);
		double w36 = Math.abs(axis.valueToJava2D(first.highMHz(), area, RectangleEdge.BOTTOM)
				- axis.valueToJava2D(first.lowMHz(), area, RectangleEdge.BOTTOM));
		double w177 = Math.abs(axis.valueToJava2D(last.highMHz(), area, RectangleEdge.BOTTOM)
				- axis.valueToJava2D(last.lowMHz(), area, RectangleEdge.BOTTOM));
		assertEquals(w36, w177, 0.51);
		assertEquals(20.0, w36, 0.51);
	}

	@Test
	void wifi2OccupancyBandsAreEqualWidthOnTheViewAxis() {
		NumberAxis axis = new NumberAxis();
		axis.setAutoRange(false);
		axis.setLowerMargin(0);
		axis.setUpperMargin(0);
		axis.setRange(WifiChannelPlan.WIFI_24_VIEW_START_MHZ, WifiChannelPlan.WIFI_24_VIEW_END_MHZ);
		Rectangle2D area = new Rectangle2D.Double(0, 0, 700, 200);
		WifiChannel one = WifiChannelPlan.find(WifiChannelPlan.BAND_24, 1);
		WifiChannel eleven = WifiChannelPlan.find(WifiChannelPlan.BAND_24, 11);
		double w1 = Math.abs(axis.valueToJava2D(one.highMHz(), area, RectangleEdge.BOTTOM)
				- axis.valueToJava2D(one.lowMHz(), area, RectangleEdge.BOTTOM));
		double w11 = Math.abs(axis.valueToJava2D(eleven.highMHz(), area, RectangleEdge.BOTTOM)
				- axis.valueToJava2D(eleven.lowMHz(), area, RectangleEdge.BOTTOM));
		assertEquals(w1, w11, 0.51);
		assertEquals(200.0, w1, 0.51);
	}

	@Test
	void paintSkipsWhenRangeHasNoWifi() {
		BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		WifiChannelOverlay.paint(g, new Rectangle2D.Double(0, 0, 200, 100), 88, 108);
		g.dispose();
		assertEquals(0, img.getRGB(100, 10));
	}

	@Test
	void paintNoopsOnBadGeometry() {
		BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		assertDoesNotThrow(() -> WifiChannelOverlay.paint(g, new Rectangle2D.Double(0, 0, 2, 2), 2400, 2484));
		assertDoesNotThrow(() -> WifiChannelOverlay.paint(g, null, 2400, 2484));
		g.dispose();
	}
}
