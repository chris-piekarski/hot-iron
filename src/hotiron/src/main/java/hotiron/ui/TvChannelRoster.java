package hotiron.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import hotiron.core.TvChannelGrade;
import hotiron.core.TvStationDial;
import hotiron.core.TvStationHit;
import net.miginfocom.swing.MigLayout;

/**
 * One button per Scan/Watch hit. Occupancy is not a decoded frame.
 * Buttons wrap to the next row inside the tools column; they do not
 * run off the side.
 */
public final class TvChannelRoster extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color PICTURE_BG = new Color(255, 186, 64);
	private static final Color PICTURE_FG = new Color(20, 16, 8);
	private static final Color ATSC_BG = new Color(70, 110, 150);
	private static final Color RF_BG = new Color(48, 52, 60);
	private static final Color NOLOCK_BG = new Color(36, 32, 32);
	private static final Color FG = new Color(220, 224, 230);
	private static final Color MUTED = new Color(110, 118, 128);
	private static final Color SELECTED = new Color(120, 200, 255);

	private final JLabel summary = new JLabel("Scan to fill Seek — occupancy is not a picture",
			SwingConstants.LEFT);
	private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
	private final List<TvStationHit> rows = new ArrayList<>();
	private final List<JButton> buttons = new ArrayList<>();
	private IntConsumer onSelect;
	private int selectedFcc;

	public TvChannelRoster()
	{
		setOpaque(true);
		setBackground(new Color(22, 24, 28));
		setLayout(new MigLayout("insets 0, wrap 1, fillx", "[grow,fill]", "[][]"));
		summary.setForeground(MUTED);
		summary.setFont(summary.getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 11)
				: summary.getFont().deriveFont(Font.PLAIN, 11f));
		grid.setOpaque(false);
		add(summary, "growx");
		add(grid, "growx");
		setMinimumSize(new Dimension(120, 56));
		addComponentListener(new java.awt.event.ComponentAdapter()
		{
			@Override
			public void componentResized(java.awt.event.ComponentEvent e)
			{
				grid.invalidate();
				revalidate();
			}
		});
	}

	public void setOnSelect(IntConsumer onSelect)
	{
		this.onSelect = onSelect;
	}

	public void setSelectedFcc(int fcc)
	{
		selectedFcc = fcc;
		paintButtons();
	}

	public void setHits(List<TvStationHit> hits)
	{
		rows.clear();
		rows.addAll(sorted(hits));
		int pic = TvStationDial.pictureCount(rows);
		int rf = rows.size() - pic;
		if (rows.isEmpty())
			summary.setText("Scan to fill Seek — occupancy is not a picture");
		else
			summary.setText(pic + " picture · " + rf + " RF only");
		rebuild();
	}

	List<TvStationHit> rows()
	{
		return List.copyOf(rows);
	}

	String summaryText()
	{
		return summary.getText();
	}

	List<JButton> buttons()
	{
		return List.copyOf(buttons);
	}

	JPanel grid()
	{
		return grid;
	}

	private void rebuild()
	{
		grid.removeAll();
		buttons.clear();
		for (TvStationHit hit : rows)
		{
			if (hit == null || hit.channel == null)
				continue;
			int fcc = hit.channel.fccChannel;
			JButton b = new JButton(hit.label());
			b.setFocusable(false);
			b.setFont(b.getFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, 12)
					: b.getFont().deriveFont(Font.BOLD, 12f));
			b.setToolTipText(tip(hit));
			b.addActionListener(e -> {
				if (onSelect != null)
					onSelect.accept(fcc);
			});
			style(b, hit, fcc == selectedFcc);
			buttons.add(b);
			grid.add(b);
		}
		grid.invalidate();
		grid.revalidate();
		grid.repaint();
		revalidate();
		repaint();
	}

	private void paintButtons()
	{
		for (int i = 0; i < buttons.size() && i < rows.size(); i++)
			style(buttons.get(i), rows.get(i), rows.get(i).channel.fccChannel == selectedFcc);
	}

	private static void style(JButton b, TvStationHit hit, boolean selected)
	{
		Color bg;
		Color fg = FG;
		if (hit.grade == TvChannelGrade.PICTURE)
		{
			bg = PICTURE_BG;
			fg = PICTURE_FG;
		}
		else if (hit.grade == TvChannelGrade.ATSC_LIKE)
			bg = ATSC_BG;
		else if (hit.grade == TvChannelGrade.NO_LOCK)
			bg = NOLOCK_BG;
		else
			bg = RF_BG;
		if (selected)
		{
			b.setBackground(SELECTED);
			b.setForeground(Color.BLACK);
		}
		else
		{
			b.setBackground(bg);
			b.setForeground(fg);
		}
		b.setOpaque(true);
		b.setContentAreaFilled(true);
	}

	private static String tip(TvStationHit hit)
	{
		String band = hit.channel.vhf() ? "VHF" : "UHF";
		String extra = hit.grade == TvChannelGrade.NO_LOCK && !hit.stage.isEmpty() ? " · " + hit.stage : "";
		return "ch " + hit.label() + "  " + band + "  " + hit.grade.rosterLabel() + extra;
	}

	private static List<TvStationHit> sorted(List<TvStationHit> hits)
	{
		List<TvStationHit> out = new ArrayList<>();
		if (hits != null)
		{
			for (TvStationHit hit : hits)
			{
				if (hit != null && hit.channel != null)
					out.add(hit);
			}
		}
		out.sort((a, b) -> {
			int g = Integer.compare(a.grade.seekRank(), b.grade.seekRank());
			if (g != 0)
				return g;
			return Integer.compare(a.channel.fccChannel, b.channel.fccChannel);
		});
		return out;
	}

	/**
	 * FlowLayout that wraps and reports the wrapped height so a 520 px
	 * tools column grows extra rows instead of clipping off the side.
	 */
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
