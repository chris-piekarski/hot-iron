package hotiron.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import hotiron.core.BandContext;
import net.miginfocom.swing.MigLayout;

/**
 * Fixed-size host for band faces. Applies a {@link BandContext} by
 * showing every registered tool that qualifies. One visible tool fills
 * the slot; several sit in a row. Column width and slot height never
 * follow the spectrum.
 */
public final class BandToolsSlot extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final List<BandTool> tools;
	private final JComponent idle;
	private BandContext shown = BandContext.none();

	public BandToolsSlot(BandTool... tools)
	{
		this(defaultIdle(), tools);
	}

	public BandToolsSlot(JComponent idle, BandTool... tools)
	{
		AnalyzerLookAndFeel.install();
		this.idle = idle == null ? defaultIdle() : idle;
		this.tools = new ArrayList<BandTool>();
		if (tools != null)
		{
			for (BandTool tool : tools)
			{
				if (tool != null)
					this.tools.add(tool);
			}
		}
		lockSize();
		apply(BandContext.none());
	}

	public void apply(BandContext next)
	{
		BandContext ctx = next == null ? BandContext.none() : next;
		if (ctx.equals(shown) && getComponentCount() > 0)
			return;
		shown = ctx;
		List<JComponent> visible = new ArrayList<JComponent>();
		for (BandTool tool : tools)
		{
			if (ctx.shows(tool.kind))
				visible.add(tool.view);
		}
		removeAll();
		if (visible.isEmpty())
		{
			setLayout(new MigLayout("insets 0, fill", "[grow,fill]", "[grow]"));
			add(idle, "grow");
		}
		else if (visible.size() == 1)
		{
			setLayout(new MigLayout("insets 0, fill", "[grow,fill]", "[grow]"));
			add(visible.get(0), "grow");
		}
		else
		{
			setLayout(new GridLayout(1, visible.size(), 4, 0));
			for (JComponent view : visible)
				add(view);
		}
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
		return view != null && view.getParent() == this;
	}

	private void lockSize()
	{
		Dimension size = OperatorLayout.bandSlot();
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, OperatorLayout.BAND_SLOT_HEIGHT));
	}

	static JLabel defaultIdle()
	{
		JLabel idle = new JLabel(
				"<html><center>No band tools for this view.<br>Zoom to FM, V-TV, U-TV, NFC, or BLE.</center></html>",
				SwingConstants.CENTER);
		idle.setEnabled(false);
		return idle;
	}
}
