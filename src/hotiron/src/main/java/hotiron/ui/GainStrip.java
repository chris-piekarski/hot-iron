package hotiron.ui;

import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

/**
 * Always-on gain: Auto plus LNA/VGA sliders. FFT/antenna stay on the
 * HackRF Settings tab so a gain tweak does not retouch band tools.
 */
public final class GainStrip extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final JCheckBox autoGain = new JCheckBox("Auto");
	private final JSlider gain = new JSlider(JSlider.HORIZONTAL, 0, 100, 2);
	private final JSlider lna = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 2);
	private final JSlider vga = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 2);
	private final JLabel caption = new JLabel();

	public GainStrip()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 0, wrap 1, fillx, gapy 2", "[grow,fill]", ""));
		setOpaque(false);
		autoGain.setSelected(true);
		ExclusiveToolTip.setText(autoGain, "Pick LNA/VGA for this band so the plot is not all blue or all red.");
		gain.setFont(new Font("Monospaced", Font.BOLD, 16));
		lna.setFont(new Font("Monospaced", Font.BOLD, 16));
		vga.setFont(new Font("Monospaced", Font.BOLD, 16));
		JPanel head = new JPanel(new MigLayout("insets 0", "[grow][]", "[]"));
		head.setOpaque(false);
		head.add(new JLabel("Gain [dB]"), "growx");
		head.add(autoGain);
		add(head, "growx");
		add(gain, "growx");
		add(caption, "alignx right");
		add(new JLabel("LNA Gain [dB]"), "growx");
		add(lna, "growx");
		add(new JLabel("VGA Gain [dB]"), "growx");
		add(vga, "growx");
	}

	void setCaption(String text)
	{
		caption.setText(text == null ? "" : text);
	}

	JCheckBox autoGainCheckbox()
	{
		return autoGain;
	}

	JSlider gainSlider()
	{
		return gain;
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
