package hotiron.ui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

/**
 * Auto / +14 / LNA / VGA on the left of the plots. Combined gain and
 * chart tabs stay out of the tools column.
 */
public final class SpectrumGainRail extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final JCheckBox autoGain = new JCheckBox("Auto");
	private final JCheckBox antennaLna = new JCheckBox("+14");
	private final JSlider lna = new JSlider(SwingConstants.VERTICAL, 0, 100, 2);
	private final JSlider vga = new JSlider(SwingConstants.VERTICAL, 0, 100, 2);
	private final JLabel caption = new JLabel(" ", SwingConstants.CENTER);

	public SpectrumGainRail()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill, gapy 4, alignx center", "[grow,fill]",
				"[][][][grow][][grow][]"));
		setOpaque(false);
		int w = OperatorLayout.GAIN_RAIL_WIDTH;
		setPreferredSize(new Dimension(w, 200));
		setMinimumSize(new Dimension(w, 160));
		autoGain.setSelected(true);
		ExclusiveToolTip.setText(autoGain, "Pick LNA/VGA so the plot is not all blue or all red.");
		ExclusiveToolTip.setText(antennaLna, "Onboard RF amp +14 dB. Restarts USB. Skip next to a broadcast tower.");
		ExclusiveToolTip.install(autoGain);
		ExclusiveToolTip.install(antennaLna);
		Font tick = new Font("Monospaced", Font.BOLD, 11);
		lna.setFont(tick);
		vga.setFont(tick);
		lna.setInverted(false);
		caption.setFont(caption.getFont().deriveFont(Font.PLAIN, 11f));
		add(autoGain, "alignx center");
		add(antennaLna, "alignx center");
		add(new JLabel("LNA", SwingConstants.CENTER), "alignx center");
		add(lna, "grow");
		add(new JLabel("VGA", SwingConstants.CENTER), "alignx center");
		add(vga, "grow");
		add(caption, "growx");
	}

	void setCaption(String text)
	{
		caption.setText(text == null || text.isBlank() ? " " : text);
	}

	JCheckBox autoGainCheckbox()
	{
		return autoGain;
	}

	JCheckBox antennaLnaCheckbox()
	{
		return antennaLna;
	}

	JSlider lnaSlider()
	{
		return lna;
	}

	JSlider vgaSlider()
	{
		return vga;
	}
}
