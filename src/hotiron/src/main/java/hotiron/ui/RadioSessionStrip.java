package hotiron.ui;

import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import hotiron.core.RadioIdentity;

/**
 * Footer session chrome: identity, Restart / Stop / Pause / ⋯.
 * Click the identity to pick a radio. MCP lives on the plot telemetry.
 */
public final class RadioSessionStrip extends JPanel
{
	private static final long serialVersionUID = 1L;
	static final String FIRST_RADIO = "First radio";

	private final JLabel connected = new JLabel();
	private final JComboBox<String> radio = new JComboBox<>(new String[] { FIRST_RADIO });
	private final JButton restart = new JButton("Restart");
	private final JButton stop = new JButton("Stop");
	private final JButton pause = new JButton("Pause");
	private final JButton more = new JButton("⋯");
	private JPanel overflow;

	public RadioSessionStrip()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new net.miginfocom.swing.MigLayout("insets 0, fillx, gapx 8", "[][][][][]", "[]"));
		setOpaque(false);
		connected.setText(RadioIdentity.ABSENT.statusLine());
		connected.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		ExclusiveToolTip.setText(connected, RadioIdentity.ABSENT.tooltip(false));
		ExclusiveToolTip.setText(radio, "Which HackRF to open. First radio = libhackrf default.");
		ExclusiveToolTip.setText(restart, "Stop and start the sweep again. Use this if the plot freezes after a setting change.");
		ExclusiveToolTip.setText(stop, "Halt the native sweep and release USB so other tools can open the radio.");
		ExclusiveToolTip.setText(pause, "Freeze the display. The radio keeps sweeping; click Resume to show live data again.");
		ExclusiveToolTip.setText(more, "Hardware: FFT bin, samples, bias tee, CLKOUT, About.");
		ExclusiveToolTip.install(connected);
		ExclusiveToolTip.install(radio);
		ExclusiveToolTip.install(restart);
		ExclusiveToolTip.install(stop);
		ExclusiveToolTip.install(pause);
		ExclusiveToolTip.install(more);
		connected.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				showPicker();
			}
		});
		more.addActionListener(e -> showOverflow());
		JPanel buttons = new JPanel(new GridLayout(1, 4, 4, 0));
		buttons.setOpaque(false);
		buttons.add(restart);
		buttons.add(stop);
		buttons.add(pause);
		buttons.add(more);
		add(connected);
		add(buttons);
	}

	JLabel connectedLabel()
	{
		return connected;
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

	private void showPicker()
	{
		JPopupMenu pop = new JPopupMenu();
		if (radio.getParent() != null)
			radio.getParent().remove(radio);
		pop.add(radio);
		pop.show(connected, 0, connected.getHeight());
	}

	private void showOverflow()
	{
		if (overflow == null)
			return;
		JPopupMenu pop = new JPopupMenu();
		if (overflow.getParent() != null)
			overflow.getParent().remove(overflow);
		pop.add(overflow);
		pop.show(more, 0, more.getHeight());
	}
}
