package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import hotiron.core.TvChannel;
import hotiron.core.TvChannelPlan;
import net.miginfocom.swing.MigLayout;

/**
 * ATSC channel face: Tune (one FCC channel), Scan (fill Seek), Seek
 * (occupied 6 MHz), Watch.
 */
public final class TvTunerPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(120, 200, 255);
	private static final Color LCD_DIM = new Color(80, 120, 150);

	private final JLabel ch = new JLabel("33", SwingConstants.CENTER);
	private final JLabel unit = new JLabel("UHF  ·  ATSC 1.0", SwingConstants.CENTER);
	private final JButton tuneDown = new JButton("−");
	private final JButton tuneUp = new JButton("+");
	private final JButton seekDown = new JButton("◀◀");
	private final JButton seekUp = new JButton("▶▶");
	private final JButton scan = new JButton("Scan");
	private final JButton watch = new JButton("Watch");
	private final TvVideoPanel preview = new TvVideoPanel();
	private final JSlider volume = new JSlider(0, 100, 80);
	private IntConsumer onTune;
	private IntConsumer onSeek;
	private Runnable onScan;
	private int fcc = 33;

	public TvTunerPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill", "[grow,fill]", "[][][][][][grow][]"));
		setBorder(BorderFactory.createTitledBorder("TV tuner"));
		ch.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
		ch.setForeground(LCD_DIM);
		unit.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(tuneDown, "Previous US TV channel (skips the FM/aviation gaps)");
		ExclusiveToolTip.setText(tuneUp, "Next US TV channel (skips the FM/aviation gaps)");
		ExclusiveToolTip.setText(seekDown, "Seek previous channel from the last Scan");
		ExclusiveToolTip.setText(seekUp, "Seek next channel from the last Scan");
		ExclusiveToolTip.setText(scan, "Sweep VHF then UHF TV and set the channels Seek jumps between");
		ExclusiveToolTip.setText(watch, "Park the radio on this ATSC channel. Stops the sweep.");
		ExclusiveToolTip.setText(volume, "Volume");
		add(ch);
		add(unit);
		JPanel tuneRow = new JPanel(new MigLayout("insets 0", "[grow][grow]", "[]"));
		tuneRow.setOpaque(false);
		tuneRow.add(tuneDown, "growx");
		tuneRow.add(tuneUp, "growx");
		add(tuneRow, "growx");
		JLabel tuneLabel = new JLabel("Tune", SwingConstants.CENTER);
		add(tuneLabel);
		JPanel seekRow = new JPanel(new MigLayout("insets 0", "[grow][grow]", "[]"));
		seekRow.setOpaque(false);
		seekRow.add(seekDown, "growx");
		seekRow.add(seekUp, "growx");
		add(seekRow, "growx");
		JLabel seekLabel = new JLabel("Seek", SwingConstants.CENTER);
		add(seekLabel);
		JPanel actionRow = new JPanel(new MigLayout("insets 0", "[grow][grow]", "[]"));
		actionRow.setOpaque(false);
		actionRow.add(scan, "growx");
		actionRow.add(watch, "growx");
		add(actionRow, "growx");
		preview.setPreferredSize(new Dimension(360, 180));
		preview.setMinimumSize(new Dimension(160, 90));
		preview.setStatus("no picture");
		ExclusiveToolTip.setText(preview, "Decoded ATSC video. IQ spectrogram until MPEG-2 locks.");
		add(preview, "grow, hmin 90");
		add(volume, "growx");
		tuneDown.addActionListener(e -> tune(-1));
		tuneUp.addActionListener(e -> tune(+1));
		seekDown.addActionListener(e -> seek(-1));
		seekUp.addActionListener(e -> seek(+1));
		scan.addActionListener(e -> {
			if (onScan != null)
				onScan.run();
		});
	}

	public JButton watchButton()
	{
		return watch;
	}

	public JSlider volumeSlider()
	{
		return volume;
	}

	public JButton tuneUpButton()
	{
		return tuneUp;
	}

	public JButton seekUpButton()
	{
		return seekUp;
	}

	public JButton scanButton()
	{
		return scan;
	}

	public void setOnTune(IntConsumer onTune)
	{
		this.onTune = onTune;
	}

	public void setOnSeek(IntConsumer onSeek)
	{
		this.onSeek = onSeek;
	}

	public void setOnScan(Runnable onScan)
	{
		this.onScan = onScan;
	}

	public void setScanning(boolean on)
	{
		scan.setText(on ? "Scanning…" : "Scan");
	}

	public void setChannel(int fccChannel)
	{
		TvChannel c = TvChannelPlan.clamp(fccChannel);
		fcc = c.fccChannel;
		ch.setText(c.label());
		unit.setText((c.vhf() ? "VHF" : "UHF") + "  ·  ATSC 1.0");
	}

	public int getChannel()
	{
		return fcc;
	}

	public void setWatching(boolean on)
	{
		Color col = on ? LCD : LCD_DIM;
		ch.setForeground(col);
		unit.setForeground(col);
		watch.setText(on ? "Watching ch " + fcc : "Watch");
		if (!on)
			preview.clear();
		else if (preview.currentFrame() == null)
			preview.setStatus("WATCH ch " + fcc + " — no picture");
	}

	public void setPreviewFrame(BufferedImage img)
	{
		preview.setFrame(img);
	}

	public void setPreviewStatus(String s)
	{
		preview.setStatus(s);
	}

	TvVideoPanel previewPanel()
	{
		return preview;
	}

	private void tune(int direction)
	{
		if (onTune != null)
			onTune.accept(direction);
	}

	private void seek(int direction)
	{
		if (onSeek != null)
			onSeek.accept(direction);
	}
}
