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
 * Always-on radio chrome: identity, MCP, picker, Restart / Stop / Pause.
 * Band tuners and gain live in sibling panels.
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
		ExclusiveToolTip.install(connected);
		ExclusiveToolTip.install(mcp);
		ExclusiveToolTip.install(radio);
		ExclusiveToolTip.install(restart);
		ExclusiveToolTip.install(stop);
		ExclusiveToolTip.install(pause);
		JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
		buttons.add(restart);
		buttons.add(stop);
		buttons.add(pause);
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
}
