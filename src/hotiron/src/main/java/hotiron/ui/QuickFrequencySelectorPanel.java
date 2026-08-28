package hotiron.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import hotiron.core.FrequencyRange;
import hotiron.ui.SurveyChipLayout.Chip;
import hotiron.ui.SurveyChipLayout.Side;

/**
 * Quick Select on the HackRF 1–7250 MHz survey: services above the wave,
 * HF/VHF/UHF + All below. Chip width follows the band; color matches the
 * dividers.
 */
public class QuickFrequencySelectorPanel extends JPanel
{
	private static final long serialVersionUID = -4830755053319335365L;
	private String value = QuickSelectPreset.WIFI_2.label;
	private final ButtonGroup group = new ButtonGroup();
	private final JPanel topHost = new JPanel(null);
	private final JPanel bottomHost = new JPanel(null);
	private final SpectrumWaveStrip wave = new SpectrumWaveStrip();
	private final Map<QuickSelectPreset, JToggleButton> buttons = new HashMap<>();
	private List<Chip> chips = List.of();
	private FrequencyRange window = new FrequencyRange(QuickSelectPreset.WIFI_2.startMHz,
			QuickSelectPreset.WIFI_2.endMHz);
	private final Font chipFont;

	public QuickFrequencySelectorPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 0, fillx, hidemode 3",
				"[grow,fill][" + OperatorLayout.RANGE_WIDTH + "!]",
				"[][" + SurveyChipLayout.WAVE_H + "!][]"));
		setOpaque(false);
		chipFont = getFont().deriveFont(Font.PLAIN, SurveyChipLayout.FONT_PT);
		topHost.setOpaque(false);
		bottomHost.setOpaque(false);
		for (QuickSelectPreset preset : QuickSelectPreset.values())
		{
			JToggleButton button = new JToggleButton(preset.label);
			button.setFocusPainted(false);
			button.setMargin(new Insets(3, 6, 3, 6));
			button.setFont(chipFont);
			button.setForeground(SpectrumSurveyStyle.TEXT);
			button.addActionListener(addListener(preset.label));
			button.addItemListener(e -> styleButton(preset, button));
			ExclusiveToolTip.setText(button, preset.tooltip());
			ExclusiveToolTip.install(button);
			group.add(button);
			buttons.put(preset, button);
			if (SurveyChipLayout.bottomSide(preset))
				bottomHost.add(button);
			else
				topHost.add(button);
			styleButton(preset, button);
		}
		add(topHost, "cell 0 0, growx");
		add(wave, "cell 0 1, growx, h " + SurveyChipLayout.WAVE_H + "!");
		add(bottomHost, "cell 0 2, growx");
		highlight(QuickSelectPreset.WIFI_2);
		layoutChips();
	}

	void installRangeControls(FrequencyRangePanel range)
	{
		if (range == null)
			return;
		add(range.displayPanel(), "cell 1 0, growx, bottom");
		add(range.keysPanel(), "cell 1 1, grow, h " + SurveyChipLayout.WAVE_H + "!");
		add(range.spanLabel(), "cell 1 2, growx, top");
		setSweepWindow(range.getRange());
		range.addRangeListener(e -> setSweepWindow(range.getRange()));
	}

	JPanel waveStrip()
	{
		return wave;
	}

	public void setSweepWindow(FrequencyRange range)
	{
		if (range == null)
			return;
		window = range;
		wave.setWindow(range);
	}

	public String getValue()
	{
		return value;
	}

	boolean isHighlighted(String label)
	{
		AbstractButton button = findButton(label);
		return button != null && button.isSelected();
	}

	void highlightRange(int startMHz, int endMHz)
	{
		highlight(QuickSelectPreset.findByRange(startMHz, endMHz).orElse(null));
	}

	void highlight(QuickSelectPreset preset)
	{
		if (preset == null)
		{
			group.clearSelection();
			value = "";
			restyleAll();
			wave.repaint();
			return;
		}
		AbstractButton button = findButton(preset.label);
		if (button != null)
			button.setSelected(true);
		value = preset.label;
		restyleAll();
		wave.repaint();
	}

	AbstractButton findButton(String label)
	{
		return findButton(this, label);
	}

	List<Chip> chips()
	{
		return chips;
	}

	static AbstractButton findButton(Container root, String label)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof AbstractButton && label.equals(((AbstractButton) child).getText()))
				return (AbstractButton) child;
			if (child instanceof Container)
			{
				AbstractButton nested = findButton((Container) child, label);
				if (nested != null)
					return nested;
			}
		}
		return null;
	}

	FrequencyRange sweepWindow()
	{
		return window;
	}

	private void layoutChips()
	{
		int width = Math.max(SurveyChipLayout.MIN_BUTTON_W * 4, topHost.getWidth());
		if (width < 8)
			width = Math.max(SurveyChipLayout.MIN_BUTTON_W * 4, getWidth() - OperatorLayout.RANGE_WIDTH);
		if (width < 8)
			width = 800;
		FontMetrics fm = getFontMetrics(chipFont);
		chips = SurveyChipLayout.place(width, preset -> {
			int tw = fm.stringWidth(preset.label) + 20;
			return Math.max(SurveyChipLayout.MIN_BUTTON_W, tw);
		});
		int topH = SurveyChipLayout.rowsHeight(SurveyChipLayout.topRows(chips));
		int botH = SurveyChipLayout.rowsHeight(SurveyChipLayout.bottomRows(chips));
		topHost.setPreferredSize(new Dimension(width, topH));
		topHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, topH));
		bottomHost.setPreferredSize(new Dimension(width, botH));
		bottomHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, botH));
		for (Chip chip : chips)
		{
			JToggleButton b = buttons.get(chip.preset);
			if (b != null)
				b.setBounds(chip.x, chip.y, chip.w, chip.h);
		}
		wave.setChips(chips);
		wave.setWindow(window);
		wave.setSelected(QuickSelectPreset.findByLabel(value).orElse(null));
	}

	@Override
	public void doLayout()
	{
		super.doLayout();
		layoutChips();
	}

	private void restyleAll()
	{
		for (Map.Entry<QuickSelectPreset, JToggleButton> e : buttons.entrySet())
			styleButton(e.getKey(), e.getValue());
	}

	private void styleButton(QuickSelectPreset preset, JToggleButton button)
	{
		boolean on = button.isSelected();
		Color c = SpectrumSurveyStyle.color(preset);
		button.setFont(chipFont.deriveFont(on ? Font.BOLD : Font.PLAIN));
		button.setForeground(SpectrumSurveyStyle.TEXT);
		button.setBackground(SpectrumSurveyStyle.chipBackground(preset, on));
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorder(BorderFactory.createLineBorder(c, on ? 3 : 2));
	}

	private ActionListener addListener(String type)
	{
		return e -> {
			System.out.println("quick link click: " + type);
			try
			{
				fireValueChange(value, type);
			}
			catch (PropertyVetoException ee)
			{
				System.out.println("Failed to set quick selection");
			}
		};
	}

	private void fireValueChange(String oldValue, String newValue) throws PropertyVetoException
	{
		fireVetoableChange("value", null, newValue);
		QuickFrequencySelectorPanel.this.value = newValue;
		firePropertyChange("value", oldValue, newValue);
		restyleAll();
		wave.setSelected(QuickSelectPreset.findByLabel(newValue).orElse(null));
	}

	@Override
	public void removeNotify()
	{
		ExclusiveToolTip.hide();
		super.removeNotify();
	}

	@Override
	public Dimension getPreferredSize()
	{
		int w = Math.max(400, getWidth());
		int h = SurveyChipLayout.heightFor(chips);
		return new Dimension(w, h);
	}

	static final class SpectrumWaveStrip extends JPanel
	{
		private static final long serialVersionUID = 1L;
		private List<Chip> chips = List.of();
		private FrequencyRange window;
		private QuickSelectPreset selected;

		SpectrumWaveStrip()
		{
			setOpaque(true);
			setPreferredSize(new Dimension(100, SurveyChipLayout.WAVE_H));
			setMinimumSize(new Dimension(40, SurveyChipLayout.WAVE_H));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, SurveyChipLayout.WAVE_H));
		}

		void setChips(List<Chip> next)
		{
			chips = next == null ? List.of() : next;
			repaint();
		}

		void setWindow(FrequencyRange range)
		{
			window = range;
			repaint();
		}

		void setSelected(QuickSelectPreset preset)
		{
			selected = preset;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			SpectrumWavePainter.paint(g2, new java.awt.geom.Rectangle2D.Double(0, 0, getWidth(), getHeight()),
					window, chips, selected);
		}
	}
}
