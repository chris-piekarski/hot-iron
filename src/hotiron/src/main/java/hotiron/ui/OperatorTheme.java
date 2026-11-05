package hotiron.ui;

import com.formdev.flatlaf.FlatLaf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

/**
 * Shared type, color, and control shape for the operator window.
 * Install once, before {@code FlatDarkLaf.setup()}.
 */
public final class OperatorTheme
{
	public static final int ARC = 8;
	public static final Color ACCENT = new Color(0x4C8DFF);
	public static final Color BUTTON = new Color(0x2C313A);
	public static final Color BUTTON_HOVER = new Color(0x363C48);
	public static final Color BUTTON_PRESSED = new Color(0x3A74DB);
	public static final Color TEXT = new Color(0xE7EAF0);
	public static final Color TEXT_MUTED = new Color(0x9AA3B2);
	public static final Color SURFACE = new Color(0x101218);
	/** Live spectrum trace. */
	public static final Color TRACE = new Color(0x4DA3FF);
	/** Peak-hold trace. */
	public static final Color PEAK = new Color(0xC5CAD3);

	private static final String FACE = pickFace();

	private OperatorTheme()
	{
	}

	public static String face()
	{
		return FACE;
	}

	public static Font ui(float size)
	{
		return ui(Font.PLAIN, size);
	}

	public static Font ui(int style, float size)
	{
		return new Font(FACE, style, Math.max(1, Math.round(size))).deriveFont(style, size);
	}

	/** Large frequency and channel figures. */
	public static Font readout(float size)
	{
		return ui(Font.BOLD, size);
	}

	public static AbstractBorder roundBorder(Color color, int thickness)
	{
		return new RoundBorder(color, Math.max(1, thickness), ARC);
	}

	public static void installDefaults()
	{
		// Family only. A plain Font in UIManager is an absolute override, so
		// FlatLaf cannot apply -Dflatlaf.uiScale and labels stay 13 pt while
		// the controls grow. FlatLaf builds FontUIResource sizes from this face.
		FlatLaf.setPreferredFontFamily(FACE);
		FlatLaf.setPreferredLightFontFamily(FACE);
		FlatLaf.setPreferredSemiboldFontFamily(FACE);
		UIManager.put("Button.arc", ARC);
		UIManager.put("Component.arc", ARC);
		UIManager.put("TextComponent.arc", ARC);
		UIManager.put("ProgressBar.arc", ARC);
		UIManager.put("ScrollBar.thumbArc", ARC);
		UIManager.put("Button.borderWidth", 1);
		UIManager.put("Button.margin", new Insets(6, 12, 6, 12));
		UIManager.put("Button.background", BUTTON);
		UIManager.put("Button.foreground", TEXT);
		UIManager.put("Button.hoverBackground", BUTTON_HOVER);
		UIManager.put("Button.pressedBackground", BUTTON_PRESSED);
		UIManager.put("Button.selectedBackground", ACCENT);
		UIManager.put("Button.selectedForeground", Color.WHITE);
		UIManager.put("Button.borderColor", new Color(0x3A, 0x40, 0x4A));
		UIManager.put("Button.focusedBorderColor", ACCENT);
		UIManager.put("Button.default.borderColor", ACCENT);
		UIManager.put("Component.focusColor", ACCENT);
		UIManager.put("Component.focusedBorderColor", ACCENT);
		UIManager.put("ToggleButton.selectedBackground", ACCENT);
		UIManager.put("ToggleButton.selectedForeground", Color.WHITE);
		UIManager.put("TabbedPane.selectedBackground", ACCENT);
	}

	private static String pickFace()
	{
		String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (String name : names)
		{
			if ("Segoe UI".equals(name))
				return name;
		}
		for (String name : names)
		{
			if ("Inter".equals(name))
				return name;
		}
		return Font.SANS_SERIF;
	}

	private static final class RoundBorder extends AbstractBorder
	{
		private static final long serialVersionUID = 1L;
		private final Color color;
		private final int thickness;
		private final int arc;

		RoundBorder(Color color, int thickness, int arc)
		{
			this.color = color;
			this.thickness = thickness;
			this.arc = arc;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			try
			{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(color);
				g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				// Stroke sits inside a fixed inset. A thicker selected ring must
				// not grow the insets or Quick Select labels clip.
				int inset = Math.max(1, (thickness + 1) / 2);
				g2.drawRoundRect(x + inset, y + inset, Math.max(0, w - inset * 2 - 1),
						Math.max(0, h - inset * 2 - 1), arc, arc);
			}
			finally
			{
				g2.dispose();
			}
		}

		@Override
		public Insets getBorderInsets(Component c)
		{
			return new Insets(3, 6, 3, 6);
		}

		@Override
		public boolean isBorderOpaque()
		{
			return false;
		}
	}
}
