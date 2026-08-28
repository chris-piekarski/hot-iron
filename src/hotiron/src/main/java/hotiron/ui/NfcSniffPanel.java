package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
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

import hotiron.core.NfcEnvelopeTrace;
import hotiron.core.NfcFrame;
import net.miginfocom.swing.MigLayout;

/**
 * Park NFC like Listen/Watch: same IQ drives the chart and this frame list.
 */
public final class NfcSniffPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(255, 186, 64);
	private static final Color LCD_DIM = new Color(160, 120, 50);

	private final JLabel title = new JLabel("13.56 NFC", SwingConstants.CENTER);
	private final JLabel hint = new JLabel("loop antenna · receive only", SwingConstants.CENTER);
	private final JLabel status = new JLabel("idle", SwingConstants.CENTER);
	private final JButton sniff = new JButton("Sniff");
	private final EnvelopeStrip envelope = new EnvelopeStrip();
	private final DefaultListModel<String> frames = new DefaultListModel<String>();
	private final JList<String> list = new JList<String>(frames);
	private Runnable onSniff;

	public NfcSniffPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill", "[grow,fill]", "[][][][][][grow]"));
		setBorder(BorderFactory.createTitledBorder("NFC sniff"));
		title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
		title.setForeground(LCD_DIM);
		hint.setForeground(LCD_DIM);
		status.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(sniff,
				"Park the HackRF at 11.56 MHz / 10 MS/s and decode NFC-A/B frames. Stops the sweep. Loop antenna, not a Wi-Fi whip.");
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
		add(envelope, "growx, h 80!");
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
		{
			status.setText("idle");
			envelope.setDb(new float[0]);
		}
	}

	public void setStatus(String text)
	{
		status.setText(text == null || text.isBlank() ? "idle" : text);
	}

	public void setFrames(List<NfcFrame> next)
	{
		frames.clear();
		if (next == null)
			return;
		int from = Math.max(0, next.size() - 40);
		for (int i = next.size() - 1; i >= from; i--)
			frames.addElement(next.get(i).line());
	}

	public void setEnvelope(float[] db)
	{
		envelope.setDb(db);
	}

	private static final class EnvelopeStrip extends JPanel
	{
		private static final long serialVersionUID = 1L;
		private float[] db = new float[0];

		EnvelopeStrip()
		{
			setOpaque(true);
			setBackground(new Color(18, 16, 10));
			setPreferredSize(new Dimension(10, 108));
			setMinimumSize(new Dimension(10, 80));
		}

		void setDb(float[] next)
		{
			db = next == null ? new float[0] : next;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			NfcEnvelopeScope.paint(g2,
					new Rectangle2D.Double(36, 2, Math.max(8, getWidth() - 40), Math.max(8, getHeight() - 4)), db,
					NfcEnvelopeTrace.WINDOW_S);
			float live = NfcEnvelopeTrace.latestLive(db);
			g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
			g2.setColor(LCD);
			String lab = live <= NfcEnvelopeTrace.EMPTY_DB + 1f ? NfcEnvelopeScope.banner()
					: String.format("%s  %+.1f", NfcEnvelopeScope.banner(), live);
			g2.drawString(lab, 6, 12);
		}
	}
}
