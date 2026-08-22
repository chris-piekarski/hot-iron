package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FrequencyRangeParseTest {

	@Test
	void spanIsEndMinusStart() {
		assertEquals(20, new FrequencyRange(88, 108).spanMHz());
	}

	@Test
	void parseAcceptsDashSpaceAndMhz() {
		FrequencyRange a = FrequencyRange.parse("88-108", 20).orElseThrow();
		assertEquals(88, a.getStartMHz());
		assertEquals(108, a.getEndMHz());
		FrequencyRange b = FrequencyRange.parse("2402 – 2472 MHz", 20).orElseThrow();
		assertEquals(2402, b.getStartMHz());
		assertEquals(2472, b.getEndMHz());
		FrequencyRange c = FrequencyRange.parse("88 108", 20).orElseThrow();
		assertEquals(88, c.getStartMHz());
		assertEquals(108, c.getEndMHz());
	}

	@Test
	void parseSingleNumberCentersWithSpan() {
		FrequencyRange r = FrequencyRange.parse("97", 20).orElseThrow();
		assertEquals(87, r.getStartMHz());
		assertEquals(107, r.getEndMHz());
	}

	@Test
	void parseRejectsGarbage() {
		assertTrue(FrequencyRange.parse("", 20).isEmpty());
		assertTrue(FrequencyRange.parse("nope", 20).isEmpty());
	}
}
