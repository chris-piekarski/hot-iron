package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.junit.jupiter.api.Test;

class OperatorShellTest
{
	@Test
	void placePutsBannerPlotsToolsAndStatus()
	{
		JPanel root = new JPanel(new BorderLayout());
		JLabel banner = new JLabel("banner");
		JPanel plots = new JPanel();
		JLabel tools = new JLabel("tools");
		JLabel status = new JLabel("status");
		OperatorShell.place(root, banner, plots, tools, status);
		assertSame(banner, borderChild(root, BorderLayout.NORTH));
		assertSame(plots, borderChild(root, BorderLayout.CENTER));
		Component east = borderChild(root, BorderLayout.EAST);
		assertTrue(east instanceof JPanel);
		assertSame(status, borderChild(root, BorderLayout.SOUTH));
		assertEquals(OperatorLayout.MIN_FRAME_WIDTH, root.getMinimumSize().width);
		assertEquals(OperatorLayout.TOOLS_WIDTH, east.getPreferredSize().width);
	}

	@Test
	void verticalPlotsUseTheSharedSplitWeight()
	{
		JSplitPane split = OperatorShell.verticalPlots(new JPanel(), new JPanel());
		assertEquals(JSplitPane.VERTICAL_SPLIT, split.getOrientation());
		assertEquals(OperatorLayout.PLOT_SPLIT, split.getResizeWeight(), 1e-9);
		assertEquals(-1, OperatorLayout.plotDivider(80));
		assertEquals(55, OperatorLayout.plotDivider(100));
	}

	@Test
	void listenWaterfallsSitSideBySide()
	{
		JPanel rf = new JPanel();
		JPanel audio = new JPanel();
		JSplitPane dual = OperatorShell.listenWaterfalls(rf, audio);
		assertEquals(JSplitPane.HORIZONTAL_SPLIT, dual.getOrientation());
		assertSame(rf, dual.getLeftComponent());
		assertSame(audio, dual.getRightComponent());
		assertEquals(OperatorLayout.LISTEN_WATERFALL_SPLIT, dual.getResizeWeight(), 1e-9);
	}

	@Test
	void fieldOfPlayPutsGainOnThePlot()
	{
		JPanel plots = new JPanel();
		JPanel rail = new JPanel();
		JComponent field = OperatorShell.fieldOfPlay(plots, rail);
		BorderLayout layout = (BorderLayout) ((JPanel) field).getLayout();
		assertSame(plots, layout.getLayoutComponent(BorderLayout.CENTER));
		assertSame(rail, layout.getLayoutComponent(BorderLayout.WEST));
		JPanel chips = new JPanel();
		JPanel chart = new JPanel();
		JComponent stack = OperatorShell.spectrumStack(chips, chart);
		BorderLayout sl = (BorderLayout) ((JPanel) stack).getLayout();
		assertSame(chips, sl.getLayoutComponent(BorderLayout.NORTH));
		assertSame(chart, sl.getLayoutComponent(BorderLayout.CENTER));
	}

	@Test
	void showBottomSwapsTheWaterfallStrip()
	{
		JPanel chart = new JPanel();
		JPanel wf = new JPanel();
		JPanel dual = new JPanel();
		JSplitPane plots = OperatorShell.verticalPlots(chart, wf);
		OperatorShell.showBottom(plots, dual);
		assertSame(dual, plots.getBottomComponent());
		OperatorShell.showBottom(plots, wf);
		assertSame(wf, plots.getBottomComponent());
	}

	private static Component borderChild(JPanel root, String constraint)
	{
		BorderLayout layout = (BorderLayout) root.getLayout();
		return layout.getLayoutComponent(constraint);
	}
}
