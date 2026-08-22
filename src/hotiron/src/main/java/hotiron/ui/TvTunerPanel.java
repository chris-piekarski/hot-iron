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
 * ATSC channel face: Tune (one FCC channel), Seek (occupied 6 MHz), Watch.
 */
public final class TvTunerPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(120, 200, 255);
	private static final Color LCD_DIM = new Color(80, 120, 150);

	private final JLabel ch = new JLabel("14", SwingConstants.CENTER);
	private final JLabel unit = new JLabel("UHF  ·  ATSC 1.0", SwingConstants.CENTER);
	private final JButton tuneDown = new JButton("−");
	private final JButton tuneUp = new JButton("+");
	private final JButton seekDown = new JButton("◀◀");
	private final JButton seekUp = new JButton("▶▶");
	private final JButton watch = new JButton("Watch");
	private final TvVideoPanel preview = new TvVideoPanel();
	private final JSlider volume = new JSlider(0, 100, 80);
	private IntConsumer onTune;
	private IntConsumer onSeek;
	private int fcc = 14;

	public TvTunerPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 6, wrap 1", "[grow,fill]", "[][][][][][][]"));
		setBorder(BorderFactory.createTitledBorder("TV tuner"));
		ch.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
		ch.setForeground(LCD_DIM);
		unit.setForeground(LCD_DIM);
		tuneDown.setToolTipText("Previous US TV channel (skips the FM/aviation gaps)");
		tuneUp.setToolTipText("Next US TV channel (skips the FM/aviation gaps)");
		seekDown.setToolTipText("Seek previous occupied 6 MHz channel");
		seekUp.setToolTipText("Seek next occupied 6 MHz channel");
		watch.setToolTipText("Park the radio on this ATSC channel. Stops the sweep.");
		volume.setToolTipText("Volume");
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
		add(watch, "growx");
		preview.setPreferredSize(new Dimension(220, 124));
		preview.setMinimumSize(new Dimension(160, 90));
		preview.setStatus("no picture");
		preview.setToolTipText("Decoded ATSC video. IQ spectrogram until MPEG-2 locks.");
		add(preview, "growx, h 90:124:160");
		add(volume, "growx");
		tuneDown.addActionListener(e -> tune(-1));
		tuneUp.addActionListener(e -> tune(+1));
		seekDown.addActionListener(e -> seek(-1));
		seekUp.addActionListener(e -> seek(+1));
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

	public void setOnTune(IntConsumer onTune)
	{
		this.onTune = onTune;
	}

	public void setOnSeek(IntConsumer onSeek)
	{
		this.onSeek = onSeek;
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
