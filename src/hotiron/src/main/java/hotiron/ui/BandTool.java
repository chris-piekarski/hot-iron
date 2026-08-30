package hotiron.ui;

import javax.swing.JComponent;

import hotiron.core.BandToolKind;

/**
 * One registered band face. The slot shows {@link hotiron.core.BandContext#face()}
 * only — adding a tool is register-here, not another if/else in the settings
 * column.
 */
public final class BandTool
{
	public final BandToolKind kind;
	public final JComponent view;

	public BandTool(BandToolKind kind, JComponent view)
	{
		if (kind == null || view == null)
			throw new IllegalArgumentException("kind and view");
		this.kind = kind;
		this.view = view;
	}
}
