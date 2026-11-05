package hotiron.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.formdev.flatlaf.FlatLaf;

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
		assertEquals(OperatorTheme.face(), FlatLaf.getPreferredFontFamily());
		Font button = UIManager.getFont("Button.font");
		Font label = UIManager.getFont("Label.font");
		Font field = UIManager.getFont("TextField.font");
		assertTrue(button instanceof FontUIResource, button.getClass().getName());
		assertTrue(label instanceof FontUIResource, label.getClass().getName());
		assertTrue(field instanceof FontUIResource, field.getClass().getName());
		assertFalse(button.getFamily().toLowerCase().contains("mono"), button.getFamily());
		Insets thin = OperatorTheme.roundBorder(Color.BLACK, 1).getBorderInsets(null);
		Insets thick = OperatorTheme.roundBorder(Color.BLACK, 2).getBorderInsets(null);
		assertEquals(new Insets(3, 6, 3, 6), thin);
		assertEquals(thin, thick);
		SpectrumTraceRenderer trace = new SpectrumTraceRenderer();
		assertFalse(trace.getDefaultShapesVisible());
		assertEquals(OperatorLayout.SPECTRUM_LINE_WIDTH, ((BasicStroke) trace.getDefaultStroke()).getLineWidth(), 0.01f);
	}
}
