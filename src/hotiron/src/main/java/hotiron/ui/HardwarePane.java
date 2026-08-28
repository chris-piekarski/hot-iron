package hotiron.ui;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.ListEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;

import hotiron.Version;
import net.miginfocom.swing.MigLayout;

/**
 * Overflow Hardware menu: FFT bin/samples, bias-tee, CLKOUT, debug, About.
 * Daily gain and chart toggles live on the plots.
 */
public final class HardwarePane extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final JSpinner fftBin = new JSpinner();
	private final JSpinner samples = new JSpinner();
	private final JCheckBox antennaPower = new JCheckBox("Antenna power (bias tee)");
	private final JCheckBox clkout = new JCheckBox("CLKOUT 10 MHz");
	private final JCheckBox debug = new JCheckBox("Debug display");
	private final JSpinner peakFall = new JSpinner(new SpinnerNumberModel(15, 0, 500, 1));
	private final JComboBox<Integer> persistHalfLife = new JComboBox<>();
	private final JButton homepage = new JButton("GitHub");

	public HardwarePane()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 10, wrap 1, fillx, gapy 6", "[grow,fill]", ""));
		Font mono = new Font(Font.MONOSPACED, Font.BOLD, 14);
		fftBin.setFont(mono);
		samples.setFont(mono);
		samples.setModel(new SpinnerListModel(
				new String[] { "8192", "16384", "32768", "65536", "131072", "262144" }));
		((ListEditor) samples.getEditor()).getTextField().setHorizontalAlignment(JTextField.RIGHT);
		((ListEditor) samples.getEditor()).getTextField().setEditable(false);
		ExclusiveToolTip.setText(fftBin, "Resolution bandwidth. Locked while FFT Auto is on.");
		ExclusiveToolTip.setText(samples, "8192 = one FFT block (the Auto default). Higher averages more dwells.");
		ExclusiveToolTip.setText(antennaPower, "DC on the antenna port for a powered preamp. Do not enable into a short.");
		ExclusiveToolTip.setText(clkout, "Drive CLKOUT so another radio can lock. CLKIN is used when 10 MHz is present.");
		ExclusiveToolTip.setText(debug, "Perf overlay under the plots.");
		ExclusiveToolTip.setText(peakFall, "Peak hold half-life in seconds. 0 follows live. Frozen default is 15.");
		add(new JLabel("FFT Bin [Hz]"));
		add(fftBin, "growx");
		add(new JLabel("Samples / hop"));
		add(samples, "growx");
		add(antennaPower, "growx");
		add(clkout, "growx");
		add(new JLabel("Peak half-life [s]"));
		add(peakFall, "w 80!");
		add(new JLabel("Persist half-life [s]"));
		add(persistHalfLife, "w 80!");
		add(debug, "growx");
		JPanel about = new JPanel(new MigLayout("insets 8 0 0 0", "[grow][]", "[]"));
		about.setOpaque(false);
		about.add(new JLabel("HotIron " + Version.version), "growx");
		about.add(homepage);
		add(about, "growx");
		homepage.addActionListener(e -> {
			try
			{
				DesktopBrowse.open(Version.url);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		});
	}

	JSpinner fftBinSpinner()
	{
		return fftBin;
	}

	JSpinner samplesSpinner()
	{
		return samples;
	}

	JCheckBox antennaPowerCheckbox()
	{
		return antennaPower;
	}

	JCheckBox clkoutCheckbox()
	{
		return clkout;
	}

	JCheckBox debugCheckbox()
	{
		return debug;
	}

	JSpinner peakFallSpinner()
	{
		return peakFall;
	}

	JComboBox persistHalfLifeCombo()
	{
		return persistHalfLife;
	}
}
