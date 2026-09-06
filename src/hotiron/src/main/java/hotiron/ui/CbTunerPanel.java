package hotiron.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import hotiron.core.CbChannel;
import hotiron.core.CbChannelHit;
import hotiron.core.CbChannelPlan;
import net.miginfocom.swing.MigLayout;

/**
 * CB 11 m face: channel LCD, wrapping 1–40 grid, Tune / Seek / Scan / Listen.
 */
public final class CbTunerPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(255, 186, 64);
	private static final Color LCD_DIM = new Color(160, 120, 50);
	private static final Color LIVE_BG = new Color(255, 186, 64);
	private static final Color LIVE_FG = new Color(20, 16, 8);
	private static final Color IDLE_BG = new Color(42, 46, 52);
	private static final Color IDLE_FG = new Color(210, 214, 220);
	private static final Color SELECTED = new Color(120, 200, 255);

	private final JLabel title = new JLabel("19", SwingConstants.CENTER);
	private final JLabel hint = new JLabel("27.185 MHz  AM", SwingConstants.CENTER);
	private final AnalogGauge sig = new AnalogGauge("SIG", new String[] { "0", "2", "4", "6", "8", "10" });
	private final JButton tuneDown = new JButton("−");
	private final JButton tuneUp = new JButton("+");
	private final JButton seekDown = new JButton("◀◀");
	private final JButton seekUp = new JButton("▶▶");
	private final JButton scan = new JButton("Scan");
	private final JButton listen = new JButton("Listen");
	private final JSlider volume = new JSlider(0, 100, 80);
	private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 3, 3));
	private final JButton[] chips = new JButton[CbChannelPlan.LAST_CHANNEL + 1];
	private IntConsumer onTune;
	private IntConsumer onSeek;
	private IntConsumer onSelect;
	private Runnable onScan;
	private List<CbChannelHit> stations = List.of();
	private int channel = CbChannelPlan.HIGHWAY_CHANNEL;
	private boolean listening;

	public CbTunerPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill", "[grow,fill]", "[][][][][][grow]"));
		setBorder(BorderFactory.createTitledBorder("CB 11 m"));
		title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 36));
		title.setForeground(LCD_DIM);
		hint.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(tuneDown, "Previous CB channel (1–40 wrap)");
		ExclusiveToolTip.setText(tuneUp, "Next CB channel (1–40 wrap)");
		ExclusiveToolTip.setText(seekDown, "Seek previous occupied / chatter channel");
		ExclusiveToolTip.setText(seekUp, "Seek next occupied / chatter channel");
		ExclusiveToolTip.setText(scan,
				"Park AM and hop channels 1–40 until chatter. Stops on voice, not a dead carrier. Press again to stop hopping.");
		ExclusiveToolTip.setText(listen, "Park 4 MS/s AM on this channel. Stops the sweep. Channel changes mix in software.");
		ExclusiveToolTip.setText(sig, "Incoming signal. While Listening the needle follows demodulated audio.");
		grid.setOpaque(false);
		for (int n = 1; n <= CbChannelPlan.LAST_CHANNEL; n++)
		{
			final int ch = n;
			JButton b = new JButton(Integer.toString(n));
			b.setMargin(new Insets(2, 4, 2, 4));
			b.setFocusable(false);
			b.addActionListener(e -> select(ch));
			chips[n] = b;
			grid.add(b);
		}
		JPanel row = new JPanel(new MigLayout("insets 0, gap 4", "[grow][grow][grow][grow][grow][grow]", "[]"));
		row.setOpaque(false);
		row.add(tuneDown, "growx");
		row.add(tuneUp, "growx");
		row.add(seekDown, "growx");
		row.add(seekUp, "growx");
		row.add(scan, "growx");
		row.add(listen, "growx");
		add(title);
		add(hint);
		add(sig, "growx, h " + AnalogGauge.PREF_H + "!");
		add(row, "growx");
		add(volume, "growx");
		add(grid, "grow");
		tuneDown.addActionListener(e -> tune(-1));
		tuneUp.addActionListener(e -> tune(+1));
		seekDown.addActionListener(e -> seek(-1));
		seekUp.addActionListener(e -> seek(+1));
		scan.addActionListener(e -> {
			if (onScan != null)
				onScan.run();
		});
		paintChips();
	}

	public JButton listenButton()
	{
		return listen;
	}

	public JSlider volumeSlider()
	{
		return volume;
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

	public void setChannel(int number)
	{
		channel = CbChannelPlan.clamp(number).number;
		CbChannel ch = CbChannelPlan.find(channel);
		title.setText(Integer.toString(channel));
		hint.setText(ch.freqLabel() + " MHz  AM");
		paintChips();
	}

	public void setListening(boolean on)
	{
		listening = on;
		title.setForeground(on ? LCD : LCD_DIM);
		hint.setForeground(on ? LCD : LCD_DIM);
		listen.setText(on ? "Stop" : "Listen");
		if (!on)
			sig.setValue01(0f);
	}

	public void setScanning(boolean on)
	{
		scan.setText(on ? "Stop" : "Scan");
	}

	public void setStations(List<CbChannelHit> hits)
	{
		stations = hits == null ? List.of() : hits;
		paintChips();
	}

	public void setLiveLevel(float level01)
	{
		if (!listening)
			return;
		sig.setValue01(level01);
	}

	private void tune(int dir)
	{
		if (onTune != null)
			onTune.accept(dir);
	}

	private void seek(int dir)
	{
		if (onSeek != null)
			onSeek.accept(dir);
	}

	private void select(int ch)
	{
		if (onSelect != null)
			onSelect.accept(ch);
		else
			setChannel(ch);
	}

	private void paintChips()
	{
		boolean[] live = new boolean[CbChannelPlan.LAST_CHANNEL + 1];
		for (CbChannelHit hit : stations)
		{
			if (hit != null && hit.channel != null)
				live[hit.channel.number] = true;
		}
		for (int n = 1; n <= CbChannelPlan.LAST_CHANNEL; n++)
		{
			JButton b = chips[n];
			boolean on = live[n];
			boolean sel = n == channel;
			b.setBackground(on ? LIVE_BG : IDLE_BG);
			b.setForeground(on ? LIVE_FG : IDLE_FG);
			if (sel)
				b.setBorder(BorderFactory.createLineBorder(SELECTED, 2));
			else
				b.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		}
	}

	static final class WrapLayout extends FlowLayout
	{
		private static final long serialVersionUID = 1L;

		WrapLayout(int align, int hgap, int vgap)
		{
			super(align, hgap, vgap);
		}

		@Override
		public Dimension preferredLayoutSize(Container target)
		{
			return layoutSize(target, true);
		}

		@Override
		public Dimension minimumLayoutSize(Container target)
		{
			Dimension min = layoutSize(target, false);
			min.width -= (getHgap() + 1);
			return min;
		}

		private Dimension layoutSize(Container target, boolean preferred)
		{
			synchronized (target.getTreeLock())
			{
				int maxW = wrapWidth(target);
				int hgap = getHgap();
				int vgap = getVgap();
				Insets ins = target.getInsets();
				int inner = maxW - ins.left - ins.right - hgap * 2;
				int x = 0;
				int y = ins.top + vgap;
				int rowH = 0;
				int n = target.getComponentCount();
				for (int i = 0; i < n; i++)
				{
					Component m = target.getComponent(i);
					if (!m.isVisible())
						continue;
					Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
					if (x > 0 && x + d.width > inner)
					{
						x = 0;
						y += vgap + rowH;
						rowH = 0;
					}
					x += d.width + hgap;
					rowH = Math.max(rowH, d.height);
				}
				y += rowH + vgap + ins.bottom;
				return new Dimension(maxW, Math.max(y, ins.top + ins.bottom + rowH));
			}
		}

		static int wrapWidth(Container target)
		{
			int w = target.getWidth();
			if (w <= 0 && target.getParent() != null)
				w = target.getParent().getWidth();
			if (w <= 0)
				w = 240;
			return w;
		}
	}
}
