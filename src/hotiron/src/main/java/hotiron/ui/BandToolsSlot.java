package hotiron.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import hotiron.core.BandContext;
import hotiron.core.BandToolKind;

/**
 * Host for one band face at a time. Fills the column between the
 * Spectrum tools title and the MCP log; the selected tool stacks
 * vertically and scrolls if it is taller than that space.
 */
public final class BandToolsSlot extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final java.util.List<BandTool> tools;
	private final JComponent idle;
	private final FillHost host;
	private final JScrollPane scroll;
	private BandContext shown = BandContext.none();

	public BandToolsSlot(BandTool... tools)
	{
		this(defaultIdle(), tools);
	}

	public BandToolsSlot(JComponent idle, BandTool... tools)
	{
		AnalyzerLookAndFeel.install();
		this.idle = idle == null ? defaultIdle() : idle;
		this.tools = new java.util.ArrayList<BandTool>();
		if (tools != null)
		{
			for (BandTool tool : tools)
			{
				if (tool != null)
					this.tools.add(tool);
			}
		}
		host = new FillHost();
		scroll = new JScrollPane(host);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		lockSize();
		apply(BandContext.none());
	}

	public void apply(BandContext next)
	{
		BandContext ctx = next == null ? BandContext.none() : next;
		BandToolKind face = ctx.face();
		if (ctx.equals(shown) && host.getComponentCount() > 0)
			return;
		shown = ctx;
		JComponent view = null;
		if (face != null)
		{
			for (BandTool tool : tools)
			{
				if (tool.kind == face)
				{
					view = tool.view;
					break;
				}
			}
		}
		host.removeAll();
		host.setLayout(new BorderLayout());
		host.add(view == null ? idle : view, BorderLayout.CENTER);
		lockSize();
		revalidate();
		repaint();
	}

	public BandContext shown()
	{
		return shown;
	}

	public boolean hosts(JComponent view)
	{
		return view != null && view.getParent() == host;
	}

	private void lockSize()
	{
		Dimension pref = OperatorLayout.bandSlot();
		setPreferredSize(pref);
		setMinimumSize(new Dimension(120, 120));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
	}

	static JLabel defaultIdle()
	{
		JLabel idle = new JLabel(
				"<html><center>Spectrum tools<br>Pick FM, V-TV, U-TV, NFC, or BLE.</center></html>",
				SwingConstants.CENTER);
		idle.setEnabled(false);
		return idle;
	}

	/**
	 * Track viewport width (and height when the face is shorter) so the
	 * selected tool uses the whole slot; scroll only when it overflows.
	 */
	static final class FillHost extends JPanel implements Scrollable
	{
		private static final long serialVersionUID = 1L;

		FillHost()
		{
			setLayout(new BorderLayout());
			setOpaque(false);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
		{
			return Math.max(80, visible.height - 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			if (getParent() instanceof JViewport viewport)
				return getPreferredSize().height <= viewport.getExtentSize().height;
			return true;
		}
	}
}
