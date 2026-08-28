package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import hotiron.core.BleFrame;
import net.miginfocom.swing.MigLayout;

/**
 * Parallel nRF BLE sniff. HackRF keeps sweeping 2400–2484.
 */
public final class BleSniffPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(120, 200, 255);
	private static final Color LCD_DIM = new Color(70, 110, 140);

	private final JLabel title = new JLabel("2.4 BLE", SwingConstants.CENTER);
	private final JLabel hint = new JLabel("nRF ACM · HackRF stays on sweep", SwingConstants.CENTER);
	private final JLabel status = new JLabel("idle", SwingConstants.CENTER);
	private final JButton sniff = new JButton("Sniff");
	private final DefaultListModel<String> frames = new DefaultListModel<String>();
	private final JList<String> list = new JList<String>(frames);
	private Runnable onSniff;

	public BleSniffPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill", "[grow,fill]", "[][][][][grow]"));
		setBorder(BorderFactory.createTitledBorder("BLE sniff"));
		title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
		title.setForeground(LCD_DIM);
		hint.setForeground(LCD_DIM);
		status.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(sniff,
				"Open the nRF J-Link CDC and decode Nordic sniffer UART frames. Sets sweep to 2400–2484 MHz. Does not park the HackRF.");
		list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		list.setVisibleRowCount(6);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sniff.addActionListener(e -> {
			if (onSniff != null)
				onSniff.run();
		});
		add(title);
		add(hint);
		add(status);
		add(sniff, "growx");
		add(new JScrollPane(list), "grow");
	}

	public JButton sniffButton()
	{
		return sniff;
	}

	public void setOnSniff(Runnable onSniff)
	{
		this.onSniff = onSniff;
	}

	public void setSniffing(boolean on)
	{
		title.setForeground(on ? LCD : LCD_DIM);
		status.setForeground(on ? LCD : LCD_DIM);
		sniff.setText(on ? "Stop" : "Sniff");
		if (!on)
			status.setText("idle");
	}

	public void setStatus(String text)
	{
		status.setText(text == null || text.isBlank() ? "idle" : text);
	}

	public void setFrames(List<BleFrame> next)
	{
		frames.clear();
		if (next == null)
			return;
		int from = Math.max(0, next.size() - 40);
		for (int i = next.size() - 1; i >= from; i--)
			frames.addElement(next.get(i).line());
	}
}
