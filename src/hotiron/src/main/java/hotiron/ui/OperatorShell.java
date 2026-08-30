package hotiron.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Frame chrome: banner north, plots center, fixed-width tools east,
 * status south. HotIron stays the composition root; sizes live in
 * {@link OperatorLayout}.
 */
public final class OperatorShell
{
	private OperatorShell()
	{
	}

	public static JSplitPane verticalPlots(JComponent chart, JComponent waterfall)
	{
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, waterfall);
		split.setResizeWeight(OperatorLayout.PLOT_SPLIT);
		split.setBorder(null);
		return split;
	}

	public static JComponent spectrumStack(JComponent chips, JComponent chart)
	{
		javax.swing.JPanel stack = new javax.swing.JPanel(new BorderLayout());
		stack.setOpaque(false);
		if (chips != null)
			stack.add(chips, BorderLayout.NORTH);
		if (chart != null)
			stack.add(chart, BorderLayout.CENTER);
		return stack;
	}

	public static JComponent fieldOfPlay(JComponent plots, JComponent gainRail)
	{
		javax.swing.JPanel field = new javax.swing.JPanel(new BorderLayout());
		field.setOpaque(false);
		if (plots != null)
			field.add(plots, BorderLayout.CENTER);
		if (gainRail != null)
			field.add(gainRail, BorderLayout.WEST);
		return field;
	}

	/** Side-by-side parked RF + AUDIO waterfalls (Listen and Watch). */
	public static JSplitPane listenWaterfalls(JComponent rf, JComponent audio)
	{
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rf, audio);
		split.setResizeWeight(OperatorLayout.LISTEN_WATERFALL_SPLIT);
		split.setContinuousLayout(true);
		split.setBorder(null);
		return split;
	}

	public static void showBottom(JSplitPane plots, JComponent bottom)
	{
		if (plots == null || bottom == null || plots.getBottomComponent() == bottom)
			return;
		int loc = plots.getDividerLocation();
		plots.setBottomComponent(bottom);
		if (loc > 0)
			plots.setDividerLocation(loc);
	}

	public static void applyPlotSplit(JSplitPane split)
	{
		if (split == null)
			return;
		int loc = OperatorLayout.plotDivider(split.getHeight());
		if (loc > 0)
			split.setDividerLocation(loc);
	}

	/**
	 * Fixed-width east column. The band slot inside {@code tools} scrolls
	 * above the MCP log; this wrapper does not steal that scrollbar.
	 */
	public static JComponent toolsColumn(JComponent tools)
	{
		JPanel col = new JPanel(new BorderLayout());
		col.setOpaque(true);
		if (tools != null)
			col.add(tools, BorderLayout.CENTER);
		Dimension size = OperatorLayout.toolsColumn(200);
		col.setMinimumSize(size);
		col.setPreferredSize(new Dimension(size.width, size.height));
		col.setMaximumSize(new Dimension(size.width, Integer.MAX_VALUE));
		return col;
	}

	public static void place(Container root, JComponent banner, JComponent plots, JComponent tools,
			JComponent status)
	{
		if (root == null)
			throw new IllegalArgumentException("root");
		if (banner != null)
			root.add(banner, BorderLayout.NORTH);
		if (plots != null)
			root.add(plots, BorderLayout.CENTER);
		if (tools != null)
			root.add(toolsColumn(tools), BorderLayout.EAST);
		if (status != null)
			root.add(status, BorderLayout.SOUTH);
		if (root instanceof JFrame frame)
			frame.setMinimumSize(OperatorLayout.minFrame());
		else if (root instanceof JComponent jc)
			jc.setMinimumSize(OperatorLayout.minFrame());
	}
}
