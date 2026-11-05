package hotiron.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import hotiron.core.GpsSvHit;
import net.miginfocom.swing.MigLayout;

/**
 * Park GPS L1 like NFC Sniff: same IQ drives the chart and this PRN list.
 */
public final class GnssSniffPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(140, 200, 255);
	private static final Color LCD_DIM = new Color(90, 130, 170);
	private static final Color CHIP_BG = new Color(36, 48, 64);
	private static final Color CHIP_FG = new Color(220, 230, 240);

	private final JLabel title = new JLabel("GPS L1  1575.42", SwingConstants.CENTER);
	private final JLabel hint = new JLabel("C/A PRNs · not a position fix", SwingConstants.CENTER);
	private final JLabel status = new JLabel("idle", SwingConstants.CENTER);
	private final JButton sniff = new JButton("Sniff");
	private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
	private final Map<Integer, JLabel> chips = new LinkedHashMap<>();
	private final JLabel empty = new JLabel("Waiting for C/A peaks");
	private Runnable onSniff;

	public GnssSniffPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill", "[grow,fill]", "[][][][][grow]"));
		setBorder(BorderFactory.createTitledBorder("GNSS sniff"));
		title.setFont(OperatorTheme.ui(Font.BOLD, 18f));
		title.setForeground(LCD_DIM);
		hint.setForeground(LCD_DIM);
		status.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(sniff,
				"Park the HackRF at GPS L1 1575.42 MHz / 4 MS/s and list C/A PRN peaks (C/N0, Doppler). Last-known chips stay until a newer hit or Stop. SMA bias on for an active patch. Not a PVT fix.");
		grid.setOpaque(false);
		empty.setForeground(LCD_DIM);
		grid.add(empty);
		sniff.addActionListener(e -> {
			if (onSniff != null)
				onSniff.run();
		});
		add(title);
		add(hint);
		add(status);
		add(sniff, "growx");
		add(grid, "grow");
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
			setHits(List.of());
		}
	}

	public void setStatus(String text)
	{
		status.setText(text == null || text.isBlank() ? "idle" : text);
	}

	public void setHits(List<GpsSvHit> hits)
	{
		if (hits == null || hits.isEmpty())
		{
			if (chips.isEmpty() && grid.getComponentCount() == 1 && grid.getComponent(0) == empty)
				return;
			chips.clear();
			grid.removeAll();
			grid.add(empty);
			grid.revalidate();
			grid.repaint();
			return;
		}
		boolean layout = false;
		if (empty.getParent() == grid)
		{
			grid.remove(empty);
			layout = true;
		}
		java.util.HashSet<Integer> keep = new java.util.HashSet<>();
		for (GpsSvHit hit : hits)
		{
			keep.add(hit.prn);
			JLabel chip = chips.get(hit.prn);
			if (chip == null)
			{
				chip = newChip(hit.line());
				chips.put(hit.prn, chip);
				grid.add(chip);
				layout = true;
			}
			else if (!hit.line().equals(chip.getText()))
				chip.setText(hit.line());
		}
		java.util.Iterator<java.util.Map.Entry<Integer, JLabel>> it = chips.entrySet().iterator();
		while (it.hasNext())
		{
			java.util.Map.Entry<Integer, JLabel> e = it.next();
			if (keep.contains(e.getKey()))
				continue;
			grid.remove(e.getValue());
			it.remove();
			layout = true;
		}
		if (layout)
		{
			grid.revalidate();
			grid.repaint();
		}
	}

	private static JLabel newChip(String line)
	{
		JLabel chip = new JLabel(line);
		chip.setOpaque(true);
		chip.setBackground(CHIP_BG);
		chip.setForeground(CHIP_FG);
		chip.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		chip.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		return chip;
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
