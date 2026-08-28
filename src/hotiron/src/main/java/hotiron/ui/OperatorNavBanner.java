package hotiron.ui;

import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;

/**
 * Top-of-window frequency navigation: grouped Quick Select plus the
 * sweep-range readout. Lives above the plots so the tools column can
 * stay a fixed width.
 */
public final class OperatorNavBanner extends JPanel
{
	private static final long serialVersionUID = 1L;
	static final int RANGE_WIDTH = OperatorLayout.RANGE_WIDTH;

	private final QuickFrequencySelectorPanel quick;
	private final FrequencyRangePanel range;

	public OperatorNavBanner()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 6 8 4 8, fillx", "[grow,fill]", "[grow]"));
		setOpaque(true);
		setBorder(new MatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
		quick = new QuickFrequencySelectorPanel();
		range = new FrequencyRangePanel();
		quick.installRangeControls(range);
		add(quick, "grow");
	}

	public QuickFrequencySelectorPanel quickSelector()
	{
		return quick;
	}

	public FrequencyRangePanel rangePanel()
	{
		return range;
	}
}
