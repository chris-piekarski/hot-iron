package hotiron.ui;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

/**
 * Turns off Swing {@link ToolTipManager} for a widget. The manager opens a
 * new X11 window per hover and does not destroy the previous one, so a
 * button grid stacks leftover tips. Quick Select shows range in-panel
 * instead; this class only prevents the manager from also firing.
 */
final class ExclusiveToolTip
{
	private ExclusiveToolTip()
	{
	}

	/** Keep {@link JComponent#getToolTipText()} for tests, but do not let Swing show it. */
	static void setText(JComponent c, String text)
	{
		if (c == null)
			return;
		c.setToolTipText(text);
		ToolTipManager.sharedInstance().unregisterComponent(c);
	}

	static void install(JComponent c)
	{
		if (c == null)
			return;
		ToolTipManager.sharedInstance().unregisterComponent(c);
	}

	static void install(JComponent c, String text)
	{
		if (c == null)
			return;
		c.setToolTipText(null);
		ToolTipManager.sharedInstance().unregisterComponent(c);
	}

	static void hide()
	{
		/* no window to hide */
	}

	static void hideIfOwner(JComponent source)
	{
		/* no window to hide */
	}

	static void show(JComponent source, String text)
	{
		/* no floating window */
	}

	static boolean isShowing()
	{
		return false;
	}
}
