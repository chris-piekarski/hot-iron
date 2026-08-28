package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
		assertTrue(borderChild(root, BorderLayout.EAST) instanceof JScrollPane);
		assertSame(status, borderChild(root, BorderLayout.SOUTH));
		assertEquals(OperatorLayout.MIN_FRAME_WIDTH, root.getMinimumSize().width);
		assertEquals(OperatorLayout.TOOLS_WIDTH,
				((JScrollPane) borderChild(root, BorderLayout.EAST)).getPreferredSize().width);
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
