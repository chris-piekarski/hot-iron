package hotiron.ui;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import hotiron.core.McpStatus;
import hotiron.core.RadioIdentity;
import net.miginfocom.swing.MigLayout;

/**
 * Always-on radio chrome: identity, MCP, picker, Restart / Stop / Pause / ⋯.
 * Band tuners stay in the tools column; gain lives on the plot rail.
 */
public final class RadioSessionStrip extends JPanel
{
	private static final long serialVersionUID = 1L;
	static final String FIRST_RADIO = "First radio";

	private final JLabel connected = new JLabel();
	private final JLabel mcp = new JLabel();
	private final JComboBox<String> radio = new JComboBox<>(new String[] { FIRST_RADIO });
	private final JButton restart = new JButton("Restart");
	private final JButton stop = new JButton("Stop");
	private final JButton pause = new JButton("Pause");
	private final JButton more = new JButton("⋯");
	private JPanel overflow;

	public RadioSessionStrip()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 0, wrap 1, gapy 4", "[grow,fill]", ""));
		setOpaque(false);
		connected.setText(RadioIdentity.ABSENT.statusHtml());
		ExclusiveToolTip.setText(connected, RadioIdentity.ABSENT.tooltip(false));
		connected.setVerticalAlignment(SwingConstants.TOP);
		connected.setBorder(null);
		mcp.setText(McpStatus.OFF.statusHtml());
		ExclusiveToolTip.setText(mcp, McpStatus.OFF.tooltip(System.currentTimeMillis()));
		mcp.setVerticalAlignment(SwingConstants.TOP);
		mcp.setBorder(null);
		ExclusiveToolTip.setText(radio, "Which HackRF to open. First radio = libhackrf default.");
		ExclusiveToolTip.setText(restart, "Stop and start the sweep again. Use this if the plot freezes after a setting change.");
		ExclusiveToolTip.setText(stop, "Halt the native sweep and release USB so other tools can open the radio.");
		ExclusiveToolTip.setText(pause, "Freeze the display. The radio keeps sweeping; click Resume to show live data again.");
		ExclusiveToolTip.setText(more, "Hardware: FFT bin, samples, bias tee, CLKOUT, About.");
		ExclusiveToolTip.install(connected);
		ExclusiveToolTip.install(mcp);
		ExclusiveToolTip.install(radio);
		ExclusiveToolTip.install(restart);
		ExclusiveToolTip.install(stop);
		ExclusiveToolTip.install(pause);
		ExclusiveToolTip.install(more);
		more.addActionListener(e -> showOverflow());
		JPanel buttons = new JPanel(new GridLayout(1, 4, 4, 0));
		buttons.add(restart);
		buttons.add(stop);
		buttons.add(pause);
		buttons.add(more);
		add(connected);
		add(mcp);
		add(radio);
		add(buttons);
	}

	JLabel connectedLabel()
	{
		return connected;
	}

	JLabel mcpStatusLabel()
	{
		return mcp;
	}

	JComboBox<String> radioCombo()
	{
		return radio;
	}

	JButton restartButton()
	{
		return restart;
	}

	JButton stopButton()
	{
		return stop;
	}

	JButton pauseButton()
	{
		return pause;
	}

	void setOverflow(JPanel pane)
	{
		overflow = pane;
	}

	JButton moreButton()
	{
		return more;
	}

	private void showOverflow()
	{
		if (overflow == null)
			return;
		javax.swing.JPopupMenu pop = new javax.swing.JPopupMenu();
		if (overflow.getParent() != null)
			overflow.getParent().remove(overflow);
		pop.add(overflow);
		pop.show(more, 0, more.getHeight());
	}
}
