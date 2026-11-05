package hotiron.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Font;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

class OperatorThemeTest
{
	@Test
	void faceIsASansAndControlsUseAnEightPixelCorner()
	{
		AnalyzerLookAndFeel.install();
		Font ui = OperatorTheme.ui(13f);
		assertFalse(ui.getFamily().toLowerCase().contains("mono"), ui.getFamily());
		assertEquals(13, ui.getSize());
		assertEquals(OperatorTheme.ARC, UIManager.get("Button.arc"));
		assertEquals(OperatorTheme.ARC, UIManager.get("Component.arc"));
		SpectrumTraceRenderer trace = new SpectrumTraceRenderer();
		assertFalse(trace.getDefaultShapesVisible());
		assertEquals(OperatorLayout.SPECTRUM_LINE_WIDTH, ((BasicStroke) trace.getDefaultStroke()).getLineWidth(), 0.01f);
	}
}
