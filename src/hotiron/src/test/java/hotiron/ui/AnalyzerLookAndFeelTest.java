package hotiron.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnalyzerLookAndFeelTest {

	@Test
	void installIsIdempotentAndMarksInstalled() {
		assertDoesNotThrow(AnalyzerLookAndFeel::install);
		assertDoesNotThrow(AnalyzerLookAndFeel::install);
		assertTrue(AnalyzerLookAndFeel.isInstalled());
	}
}
