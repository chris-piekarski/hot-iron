package hotiron.ui;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import hotiron.core.FrequencyAllocationTable;
import net.miginfocom.swing.MigLayout;

/**
 * Display toggles on the spectrum: peaks, persist, spurs, auto dB,
 * allocation overlay. Line width and waterfall-on are not settings.
 */
public final class ChartToggleBar extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final JCheckBox peaks = new JCheckBox("Peaks");
	private final JCheckBox persist = new JCheckBox("Persist");
	private final JCheckBox spurs = new JCheckBox("Spurs");
	private final JCheckBox autoDb = new JCheckBox("Auto dB");
	private final JComboBox<FrequencyAllocationTable> alloc = new JComboBox<>();

	public ChartToggleBar()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 2 8 2 8, fillx, gapx 10", "[][][][][grow][][]", "[]"));
		setOpaque(false);
		peaks.setSelected(true);
		persist.setSelected(true);
		autoDb.setSelected(true);
		ExclusiveToolTip.setText(peaks, "Peak hold trace. Half-life is 15 s.");
		ExclusiveToolTip.setText(persist, "Heat overlay of earlier sweeps. Half-life is 30 s.");
		ExclusiveToolTip.setText(spurs, "Hide repeating HackRF artifacts. May distort real signals.");
		ExclusiveToolTip.setText(autoDb, "Fit the live band on a 10 dB axis. Off is −100…+20. Waterfall follows.");
		ExclusiveToolTip.setText(alloc, "EU/USA allocation labels on the spectrum.");
		ExclusiveToolTip.install(peaks);
		ExclusiveToolTip.install(persist);
		ExclusiveToolTip.install(spurs);
		ExclusiveToolTip.install(autoDb);
		ExclusiveToolTip.install(alloc);
		add(peaks);
		add(persist);
		add(spurs);
		add(autoDb);
		add(new JLabel(""), "growx");
		add(new JLabel("Alloc"));
		add(alloc, "w 120!");
	}

	JCheckBox peaksCheckbox()
	{
		return peaks;
	}

	JCheckBox persistCheckbox()
	{
		return persist;
	}

	JCheckBox spursCheckbox()
	{
		return spurs;
	}

	JCheckBox autoDbCheckbox()
	{
		return autoDb;
	}

	JComboBox<FrequencyAllocationTable> allocationCombo()
	{
		return alloc;
	}
}
