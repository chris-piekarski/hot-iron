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
 * 1970s FM face: digital station on top, SIG needle (live audio while
 * Listening), horizontal slide-rule selector, Tune / Seek / Scan.
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
	private final JButton scan = new JButton("Scan");
	private final JLabel tuneLabel = new JLabel("Tune", SwingConstants.CENTER);
	private final JLabel seekLabel = new JLabel("Seek", SwingConstants.CENTER);
	private final AnalogGauge gainGauge = new AnalogGauge("SIG",
			new String[] { "0", "2", "4", "6", "8", "10" });
	private final FmTunerScale scale = new FmTunerScale();
	private final JButton listen = new JButton("Listen");
	private final JSlider volume = new JSlider(0, 100, 80);
	private IntConsumer onTune;
	private IntConsumer onSeek;
	private IntConsumer onSelect;
	private Runnable onScan;
	private List<FmStationHit> stations = List.of();
	private boolean listening;

	public TunerPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 6 8 8 8, fillx, wrap 1, aligny top", "[grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("FM tuner"));
		freq.setFont(new Font(Font.MONOSPACED, Font.BOLD, 44));
		freq.setForeground(LCD_DIM);
		unit.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
		unit.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(tuneDown, "Fine tune down one channel (200 kHz)");
		ExclusiveToolTip.setText(tuneUp, "Fine tune up one channel (200 kHz)");
		ExclusiveToolTip.setText(seekDown, "Seek previous strong station from the last Scan");
		ExclusiveToolTip.setText(seekUp, "Seek next strong station from the last Scan");
		ExclusiveToolTip.setText(scan, "Sweep 88–108 MHz and pin strong stations for Seek");
		ExclusiveToolTip.setText(listen, "Park the radio on this station. Stops the sweep.");
		ExclusiveToolTip.setText(volume, "Volume");
		add(freq, "growx");
		add(unit, "growx");
		ExclusiveToolTip.setText(gainGauge,
				"Incoming signal. While Listening the needle follows demodulated audio.");
		ExclusiveToolTip.install(gainGauge);
		add(gainGauge, "growx, h " + AnalogGauge.PREF_H + "!");
		add(scale, "growx, h " + FmTunerScale.PREF_H + "!");
		JPanel tuneRow = new JPanel(new MigLayout("insets 0, gap 4", "[grow][grow][grow][grow][grow][grow]", "[]"));
		tuneRow.setOpaque(false);
		tuneRow.add(tuneDown, "growx");
		tuneRow.add(tuneUp, "growx");
		tuneRow.add(seekDown, "growx");
		tuneRow.add(seekUp, "growx");
		tuneRow.add(scan, "growx");
		tuneRow.add(listen, "growx");
		add(tuneRow, "growx");
		JPanel capRow = new JPanel(new MigLayout("insets 0", "[grow][grow][grow][grow][grow][grow]", "[]"));
		capRow.setOpaque(false);
		capRow.add(tuneLabel, "span 2, growx");
		capRow.add(seekLabel, "span 2, growx");
		capRow.add(new JLabel(" "), "growx");
		capRow.add(new JLabel(" "), "growx");
		add(capRow, "growx");
		add(volume, "growx");
		tuneDown.addActionListener(e -> tune(-1));
		tuneUp.addActionListener(e -> tune(+1));
		seekDown.addActionListener(e -> seek(-1));
		seekUp.addActionListener(e -> seek(+1));
		scan.addActionListener(e -> {
			if (onScan != null)
				onScan.run();
		});
		scale.setOnTune(this::tune);
		scale.setOnSelectKHz(this::selectKHz);
	}

	public FmTunerScale scale()
	{
		return scale;
	}

	public AnalogGauge gainGauge()
	{
		return gainGauge;
	}

	public void setLiveLevel(float level01)
	{
		if (!listening)
			return;
		gainGauge.setValue01(level01);
	}

	private void selectKHz(int kHz)
	{
		if (onSelect != null)
			onSelect.accept(kHz);
		else
			setKHz(kHz);
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

	public JButton seekDownButton()
	{
		return seekDown;
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

	public void setOnSelect(IntConsumer onSelect)
	{
		this.onSelect = onSelect;
	}

	public void setOnScan(Runnable onScan)
	{
		this.onScan = onScan;
	}

	public void setScanning(boolean on)
	{
		scan.setText(on ? "Scanning…" : "Scan");
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
		scale.setKHz(ch.centerKHz);
		refreshGainNeedle();
	}

	public void setListening(boolean on)
	{
		listening = on;
		Color c = on ? LCD : LCD_DIM;
		freq.setForeground(c);
		unit.setForeground(c);
		listen.setText(on ? "Listening " + freq.getText() : "Listen");
		if (!on)
			refreshGainNeedle();
	}

	public void setStations(List<FmStationHit> hits)
	{
		List<Integer> kHz = new ArrayList<Integer>();
		stations = hits == null ? List.of() : hits;
		if (hits != null)
		{
			for (FmStationHit hit : hits)
			{
				if (hit != null && hit.channel != null)
					kHz.add(hit.channel.centerKHz);
			}
		}
		scale.setDetents(kHz);
		refreshGainNeedle();
	}

	private void refreshGainNeedle()
	{
		if (listening)
			return;
		int want = scale.getKHz();
		float db = -90f;
		boolean hit = false;
		for (int i = 0; i < stations.size(); i++)
		{
			FmStationHit s = stations.get(i);
			if (s == null || s.channel == null)
				continue;
			if (s.channel.centerKHz != want)
				continue;
			db = s.powerDbm;
			hit = true;
			break;
		}
		if (!hit)
		{
			gainGauge.setValue01(0f);
			return;
		}
		float t = (db + 90f) / 70f;
		gainGauge.setValue01(t);
	}
}
