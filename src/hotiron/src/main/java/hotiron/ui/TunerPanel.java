package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import hotiron.core.FmChannel;
import hotiron.core.FmChannelPlan;
import hotiron.core.FmStationHit;
import net.miginfocom.swing.MigLayout;

/**
 * Car-radio face: big frequency, Tune (one 200 kHz click), Seek (next
 * detected station), rotary knob, listen, volume.
 */
public final class TunerPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(255, 186, 64);
	private static final Color LCD_DIM = new Color(160, 120, 50);

	private final JLabel freq = new JLabel("97.3", SwingConstants.CENTER);
	private final JLabel unit = new JLabel("MHz  FM", SwingConstants.CENTER);
	private final JButton tuneDown = new JButton("−");
	private final JButton tuneUp = new JButton("+");
	private final JButton seekDown = new JButton("◀◀");
	private final JButton seekUp = new JButton("▶▶");
	private final JLabel tuneLabel = new JLabel("Tune", SwingConstants.CENTER);
	private final JLabel seekLabel = new JLabel("Seek", SwingConstants.CENTER);
	private final StationKnob knob = new StationKnob();
	private final JButton listen = new JButton("Listen");
	private final JSlider volume = new JSlider(0, 100, 80);
	private IntConsumer onTune;
	private IntConsumer onSeek;

	public TunerPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 6, wrap 1", "[grow,fill]", "[][][][][]"));
		setBorder(BorderFactory.createTitledBorder("FM tuner"));
		freq.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
		freq.setForeground(LCD_DIM);
		unit.setForeground(LCD_DIM);
		tuneDown.setToolTipText("Tune down one channel (200 kHz)");
		tuneUp.setToolTipText("Tune up one channel (200 kHz)");
		seekDown.setToolTipText("Seek previous detected station");
		seekUp.setToolTipText("Seek next detected station");
		listen.setToolTipText("Park the radio on this station. Stops the sweep.");
		volume.setToolTipText("Volume");
		add(freq);
		add(unit);
		JPanel knobRow = new JPanel(new MigLayout("insets 0", "[][grow][]", "[grow]"));
		knobRow.setOpaque(false);
		knobRow.add(tuneDown, "w 36!, h 72!");
		knobRow.add(knob, "center");
		knobRow.add(tuneUp, "w 36!, h 72!");
		add(knobRow, "grow");
		add(tuneLabel);
		JPanel seekRow = new JPanel(new MigLayout("insets 0", "[grow][grow]", "[]"));
		seekRow.setOpaque(false);
		seekRow.add(seekDown, "growx");
		seekRow.add(seekUp, "growx");
		add(seekRow, "growx");
		add(seekLabel);
		add(listen, "growx");
		add(volume, "growx");
		tuneDown.addActionListener(e -> tune(-1));
		tuneUp.addActionListener(e -> tune(+1));
		seekDown.addActionListener(e -> seek(-1));
		seekUp.addActionListener(e -> seek(+1));
		knob.setOnStep(this::seek);
	}

	public StationKnob knob()
	{
		return knob;
	}

	public JButton listenButton()
	{
		return listen;
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

	/** @deprecated knob seek; use {@link #setOnSeek} */
	public void setOnStep(IntConsumer onStep)
	{
		setOnSeek(onStep);
	}

	public void tune(int direction)
	{
		if (onTune != null)
			onTune.accept(direction < 0 ? -1 : 1);
	}

	public void seek(int direction)
	{
		if (onSeek != null)
			onSeek.accept(direction < 0 ? -1 : 1);
	}

	public void setKHz(int kHz)
	{
		FmChannel ch = FmChannelPlan.clamp(kHz / 1000.0);
		freq.setText(ch.label());
		knob.setKHz(ch.centerKHz);
	}

	public void setListening(boolean on)
	{
		Color c = on ? LCD : LCD_DIM;
		freq.setForeground(c);
		unit.setForeground(c);
		listen.setText(on ? "Listening " + freq.getText() : "Listen");
	}

	public void setStations(List<FmStationHit> hits)
	{
		List<Integer> kHz = new ArrayList<Integer>();
		if (hits != null)
		{
			for (FmStationHit hit : hits)
			{
				if (hit != null && hit.channel != null)
					kHz.add(hit.channel.centerKHz);
			}
		}
		knob.setDetents(kHz);
	}
}
