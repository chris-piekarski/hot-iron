package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpectrumZoomHistoryTest {

	@Test
	void pushPopRestoresPreviousWindow() {
		SpectrumZoomHistory history = new SpectrumZoomHistory();
		assertFalse(history.canZoomOut());
		history.push(new FrequencyRange(88, 108));
		history.push(new FrequencyRange(96, 100));
		assertEquals(2, history.size());
		assertEquals(new FrequencyRange(96, 100), history.pop().orElseThrow());
		assertEquals(new FrequencyRange(88, 108), history.pop().orElseThrow());
		assertTrue(history.pop().isEmpty());
	}

	@Test
	void pushDoesNotDuplicateTheTop() {
		SpectrumZoomHistory history = new SpectrumZoomHistory();
		FrequencyRange fm = new FrequencyRange(88, 108);
		history.push(fm);
		history.push(new FrequencyRange(88, 108));
		assertEquals(1, history.size());
	}

	@Test
	void clearDropsTheStack() {
		SpectrumZoomHistory history = new SpectrumZoomHistory();
		history.push(new FrequencyRange(88, 108));
		history.clear();
		assertFalse(history.canZoomOut());
	}

	@Test
	void depthIsCapped() {
		SpectrumZoomHistory history = new SpectrumZoomHistory();
		for (int i = 0; i < SpectrumZoomHistory.MAX_DEPTH + 5; i++)
			history.push(new FrequencyRange(1 + i, 10 + i));
		assertEquals(SpectrumZoomHistory.MAX_DEPTH, history.size());
	}
}
