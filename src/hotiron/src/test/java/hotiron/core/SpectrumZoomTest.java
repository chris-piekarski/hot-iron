package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SpectrumZoomTest {

	private static final Rectangle2D AREA = new Rectangle2D.Double(40, 20, 800, 300);

	@Test
	void clampKeepsIntegerMegahertzBounds() {
		FrequencyRange r = SpectrumZoom.clamp(0, 10000);
		assertEquals(1, r.getStartMHz());
		assertEquals(7250, r.getEndMHz());
		assertEquals(1, SpectrumZoom.clamp(88, 88).getEndMHz() - SpectrumZoom.clamp(88, 88).getStartMHz());
	}

	@Test
	void dragOnFullFmBandSelectsThePixelsSpan() {
		// 88–108 MHz across 800 px → 40 px = 1 MHz
		Optional<FrequencyRange> zoomed = SpectrumZoom.fromDrag(40 + 40, 40 + 200, AREA, 88, 108);
		assertTrue(zoomed.isPresent());
		assertEquals(89, zoomed.get().getStartMHz());
		assertEquals(93, zoomed.get().getEndMHz());
	}

	@Test
	void tinyDragIsIgnored() {
		assertTrue(SpectrumZoom.fromDrag(100, 104, AREA, 88, 108).isEmpty());
		assertTrue(SpectrumZoom.fromDrag(100, 100, AREA, 88, 108).isEmpty());
	}

	@Test
	void dragWorksRightToLeft() {
		Optional<FrequencyRange> zoomed = SpectrumZoom.fromDrag(40 + 200, 40 + 40, AREA, 88, 108);
		assertEquals(89, zoomed.get().getStartMHz());
		assertEquals(93, zoomed.get().getEndMHz());
	}

	@Test
	void aroundCursorHalvesAndDoublesTheSpan() {
		FrequencyRange current = new FrequencyRange(88, 108);
		FrequencyRange in = SpectrumZoom.around(current, 98, SpectrumZoom.ZOOM_IN_FACTOR);
		assertEquals(10, in.getEndMHz() - in.getStartMHz());
		assertTrue(in.getStartMHz() <= 98 && 98 <= in.getEndMHz());
		FrequencyRange out = SpectrumZoom.around(current, 98, SpectrumZoom.ZOOM_OUT_FACTOR);
		assertEquals(40, out.getEndMHz() - out.getStartMHz());
	}

	@Test
	void panShiftsByAQuarterSpan() {
		FrequencyRange moved = SpectrumZoom.pan(new FrequencyRange(88, 108), 0.25);
		assertEquals(93, moved.getStartMHz());
		assertEquals(113, moved.getEndMHz());
		assertEquals(20, moved.spanMHz());
	}

	@Test
	void expandDoublesAroundTheMidpoint() {
		FrequencyRange expanded = SpectrumZoom.expand(new FrequencyRange(88, 108));
		assertEquals(40, expanded.getEndMHz() - expanded.getStartMHz());
		assertEquals(78, expanded.getStartMHz());
		assertEquals(118, expanded.getEndMHz());
	}
}
