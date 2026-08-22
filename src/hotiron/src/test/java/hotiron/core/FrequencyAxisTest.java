package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

class FrequencyAxisTest {

	@Test
	void mhzAndPixelsRoundTripOnFmBand() {
		FrequencyAxis axis = FrequencyAxis.fromArea(new Rectangle2D.Double(40, 20, 800, 300), 88, 108);
		assertEquals(20, axis.spanMHz(), 0.001);
		assertEquals(40, axis.pxPerMHz(), 0.001);
		assertEquals(40, axis.mhzToX(88), 0.001);
		assertEquals(840, axis.mhzToX(108), 0.001);
		assertEquals(440, axis.mhzToX(98), 0.001);
		assertEquals(98, axis.xToMhz(440), 0.001);
		assertEquals(88, axis.xToMhz(0), 0.001);
		assertEquals(108, axis.xToMhz(900), 0.001);
	}

	@Test
	void wifiChannelElevenIsTwentyMegahertzWide() {
		FrequencyAxis axis = FrequencyAxis.of(2402, 2472, 700);
		assertEquals(200, axis.occupancyWidthPx(2452, 2472));
		assertEquals(axis.occupancyWidthPx(2402, 2422), axis.occupancyWidthPx(2452, 2472));
	}

	@Test
	void unusableWhenTooNarrowOrEmptySpan() {
		assertFalse(FrequencyAxis.of(88, 108, 4).usable());
		assertFalse(FrequencyAxis.of(88, 88, 800).usable());
		assertTrue(FrequencyAxis.of(88, 108, 800).usable());
	}

	@Test
	void occupancyVisibleAndClip() {
		FrequencyAxis axis = FrequencyAxis.of(90, 100, 200);
		assertTrue(axis.occupancyVisible(88, 108));
		assertFalse(axis.occupancyVisible(2402, 2472));
		assertEquals(90, axis.clipLow(88), 0.001);
		assertEquals(100, axis.clipHigh(108), 0.001);
		assertTrue(axis.containsMhz(95));
		assertFalse(axis.containsMhz(80));
	}

	@Test
	void zoomFromDragMatchesSpectrumZoom() {
		Rectangle2D area = new Rectangle2D.Double(40, 20, 800, 300);
		FrequencyAxis axis = FrequencyAxis.fromArea(area, 88, 108);
		assertEquals(SpectrumZoom.fromDrag(80, 240, area, 88, 108), axis.zoomFromDrag(80, 240));
	}
}
