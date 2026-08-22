package hotiron.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Quick Select buttons. Labels and MHz ranges live in {@link QuickSelectPreset}.
 * Hover range is an in-panel line so X11 cannot stack leftover tooltip windows.
 */
public class QuickFrequencySelectorPanel extends JPanel
{
	private static final long	serialVersionUID	= -4830755053319335365L;
	static final String			HOVER_HINT_NAME		= "quickSelectHoverHint";
	private String			value			= QuickSelectPreset.WIFI_2.label;
	private final JPanel		grid;
	private final JLabel		hoverHint;

	public QuickFrequencySelectorPanel()
	{
		AnalyzerLookAndFeel.install();

		QuickSelectPreset[] presets = QuickSelectPreset.values();
		int cols = 3;
		int rows = (presets.length + cols - 1) / cols;
		setLayout(new BorderLayout(0, 2));

		grid = new JPanel(new GridLayout(rows, cols, 0, 0));
		for (QuickSelectPreset preset : presets)
		{
			JButton button = new JButton(preset.label);
			button.addActionListener(addListener(preset.label));
			ExclusiveToolTip.install(button, preset.tooltip());
			button.addMouseListener(new HoverHintListener(preset.tooltip()));
			grid.add(button);
		}
		add(grid, BorderLayout.CENTER);

		hoverHint = new JLabel(" ");
		hoverHint.setName(HOVER_HINT_NAME);
		hoverHint.setFont(hoverHint.getFont().deriveFont(Font.PLAIN, 11f));
		hoverHint.setHorizontalAlignment(SwingConstants.CENTER);
		add(hoverHint, BorderLayout.SOUTH);

		Dimension d = new Dimension(300, 25 * rows + 16);
		setPreferredSize(d);
		setMaximumSize(d);
		setMinimumSize(d);
	}

	public String getValue()
	{
		return value;
	}

	String hoverHintText()
	{
		return hoverHint.getText();
	}

	JButton findButton(String label)
	{
		return findButton(this, label);
	}

	static JButton findButton(Container root, String label)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JButton && label.equals(((JButton) child).getText()))
				return (JButton) child;
			if (child instanceof Container)
			{
				JButton nested = findButton((Container) child, label);
				if (nested != null)
					return nested;
			}
		}
		return null;
	}

	private ActionListener addListener(String type)
	{
		return e -> {
			System.out.println("quick link click: " + type);
			try
			{
				// Re-apply even if this button is already selected so a
				// second click restores the FCC/ITU envelope after digit edits.
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
		// VetoableChangeSupport drops equal old/new; pass null so a second
		// click on the same button still reapplies the range.
		fireVetoableChange("value", null, newValue);
		QuickFrequencySelectorPanel.this.value = newValue;
		firePropertyChange("value", oldValue, newValue);
	}

	@Override
	public void removeNotify()
	{
		hoverHint.setText(" ");
		ExclusiveToolTip.hide();
		super.removeNotify();
	}

	private final class HoverHintListener extends MouseAdapter
	{
		private final String text;

		HoverHintListener(String text)
		{
			this.text = text;
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
			hoverHint.setText(text);
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			if (text.equals(hoverHint.getText()))
				hoverHint.setText(" ");
		}
	}
}
