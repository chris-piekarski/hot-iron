package hotiron.ui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Hover text without a popup window and without growing the sidebar.
 * Swing's {@link ToolTipManager} / {@code JWindow} open a new X11 window
 * per hover (WSL stacks leftovers). In-panel labels were rejected because
 * they reflow the settings column. This paints one {@link JLabel} on the
 * frame {@link JLayeredPane} — same window, setBounds only, no pack.
 */
final class ExclusiveToolTip
{
	static final Object HINT_KEY = "hotiron.hint";
	static final Object COLUMN_KEY = "hotiron.hintColumn";
	private static final Object LISTENER_KEY = "hotiron.hintListener";
	private static final int PAD = 4;
	private static final int MAX_OVERLAY_H = 72;
	private static final AWTEventListener HOVER = ExclusiveToolTip::onAwtEvent;
	private static final MouseAdapter MOUSE = new MouseAdapter()
	{
		@Override
		public void mouseEntered(MouseEvent e)
		{
			if (e.getSource() instanceof JComponent c)
				show(c);
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			if (e.getSource() instanceof JComponent c)
				hideIfOwner(c);
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			hide();
		}
	};

	private static volatile boolean shared;
	private static JComponent owner;
	private static final JLabel overlay = new JLabel();

	private ExclusiveToolTip()
	{
	}

	static void installShared()
	{
		if (shared)
			return;
		synchronized (ExclusiveToolTip.class)
		{
			if (shared)
				return;
			ToolTipManager tm = ToolTipManager.sharedInstance();
			tm.setEnabled(false);
			tm.setInitialDelay(Integer.MAX_VALUE);
			tm.setReshowDelay(Integer.MAX_VALUE);
			tm.setDismissDelay(0);
			overlay.setOpaque(true);
			overlay.setVisible(false);
			overlay.setFocusable(false);
			styleOverlay();
			Toolkit.getDefaultToolkit().addAWTEventListener(HOVER,
					AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK
							| AWTEvent.CONTAINER_EVENT_MASK);
			shared = true;
		}
	}

	static void bindColumn(JComponent column)
	{
		if (column != null)
			column.putClientProperty(COLUMN_KEY, Boolean.TRUE);
	}

	/**
	 * Store the hint for our hover handler. Never call
	 * {@link JComponent#setToolTipText(String)} with a non-null value —
	 * that re-registers the widget with {@link ToolTipManager}.
	 */
	static void setText(JComponent c, String text)
	{
		if (c == null)
			return;
		c.putClientProperty(HINT_KEY, text);
		c.setToolTipText(null);
		disarm(c);
		attachHover(c);
	}

	static void install(JComponent c)
	{
		if (c == null)
			return;
		c.setToolTipText(null);
		disarm(c);
		attachHover(c);
	}

	static void install(JComponent c, String text)
	{
		if (c == null)
			return;
		c.setToolTipText(null);
		disarm(c);
		attachHover(c);
	}

	static String hintOf(JComponent c)
	{
		return hintFrom(c);
	}

	static void disarm(JComponent c)
	{
		if (c == null)
			return;
		ToolTipManager.sharedInstance().unregisterComponent(c);
	}

	static void disarmTree(Component root)
	{
		if (root instanceof JComponent jc)
		{
			jc.setToolTipText(null);
			disarm(jc);
		}
		if (root instanceof Container box)
		{
			for (Component child : box.getComponents())
				disarmTree(child);
		}
	}

	static void hide()
	{
		owner = null;
		overlay.setText("");
		overlay.setVisible(false);
		overlay.setBounds(0, 0, 0, 0);
	}

	static void hideIfOwner(JComponent source)
	{
		if (source != null && owner == source)
			hide();
	}

	static void show(JComponent source)
	{
		String text = hintFrom(source);
		if (text == null)
		{
			hide();
			return;
		}
		owner = source;
		styleOverlay();
		overlay.setText(plain(text));
		if (!source.isShowing())
			return;
		JLayeredPane layered = layeredPaneOf(source);
		if (layered == null)
			return;
		attach(layered);
		place(source, layered, text);
		overlay.setVisible(true);
	}

	static boolean isShowing()
	{
		return owner != null;
	}

	static JComponent owner()
	{
		return owner;
	}

	static String overlayText()
	{
		return overlay.getText() == null ? "" : overlay.getText();
	}

	static boolean opensAWindow()
	{
		return false;
	}

	private static void attachHover(JComponent c)
	{
		if (c.getClientProperty(LISTENER_KEY) != null)
			return;
		c.putClientProperty(LISTENER_KEY, Boolean.TRUE);
		c.addMouseListener(MOUSE);
	}

	private static String hintFrom(Component start)
	{
		for (Component p = start; p != null; p = p.getParent())
		{
			if (p instanceof JComponent jc)
			{
				Object v = jc.getClientProperty(HINT_KEY);
				if (v instanceof String s && !s.isBlank())
					return s;
			}
		}
		return null;
	}

	private static JComponent findColumn(Component start)
	{
		for (Component p = start; p != null; p = p.getParent())
		{
			if (p instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty(COLUMN_KEY)))
				return jc;
		}
		return null;
	}

	private static JLayeredPane layeredPaneOf(JComponent source)
	{
		Window w = SwingUtilities.getWindowAncestor(source);
		if (w instanceof JFrame frame)
			return frame.getLayeredPane();
		return null;
	}

	private static void attach(JLayeredPane layered)
	{
		if (overlay.getParent() != layered)
		{
			if (overlay.getParent() != null)
				overlay.getParent().remove(overlay);
			layered.add(overlay, Integer.valueOf(JLayeredPane.POPUP_LAYER));
		}
	}

	private static void place(JComponent source, JLayeredPane layered, String text)
	{
		Rectangle clip = clip(source, layered);
		int maxW = Math.max(48, clip.width - PAD * 2);
		overlay.setText(plain(text));
		Dimension d = overlay.getPreferredSize();
		if (d.width > maxW || text.indexOf('\n') >= 0)
		{
			overlay.setText("<html><body style='width:" + Math.max(40, maxW - 16) + "px'>"
					+ escape(text).replace("\n", "<br>") + "</body></html>");
			d = overlay.getPreferredSize();
		}
		int w = Math.min(d.width, maxW);
		int h = Math.min(d.height, MAX_OVERLAY_H);
		Point below = SwingUtilities.convertPoint(source, PAD, source.getHeight() + 2, layered);
		int x = Math.max(clip.x + PAD, Math.min(below.x, clip.x + clip.width - w - PAD));
		int y = below.y;
		if (y + h > clip.y + clip.height - PAD)
			y = Math.max(clip.y + PAD, below.y - source.getHeight() - h - 4);
		overlay.setBounds(x, y, w, h);
	}

	private static Rectangle clip(JComponent source, JLayeredPane layered)
	{
		JComponent column = findColumn(source);
		if (column != null && column.isShowing())
			return SwingUtilities.convertRectangle(column,
					new Rectangle(0, 0, column.getWidth(), column.getHeight()), layered);
		return new Rectangle(0, 0, Math.max(1, layered.getWidth()), Math.max(1, layered.getHeight()));
	}

	private static void styleOverlay()
	{
		Color bg = UIManager.getColor("ToolTip.background");
		Color fg = UIManager.getColor("ToolTip.foreground");
		overlay.setBackground(bg != null ? bg : new Color(64, 64, 64));
		overlay.setForeground(fg != null ? fg : Color.WHITE);
		Border tip = UIManager.getBorder("ToolTip.border");
		Border pad = BorderFactory.createEmptyBorder(2, 6, 2, 6);
		overlay.setBorder(tip != null ? BorderFactory.createCompoundBorder(tip, pad) : pad);
		overlay.setFont(UIManager.getFont("ToolTip.font"));
	}

	private static String plain(String text)
	{
		return text.indexOf('\n') >= 0 ? text.replace('\n', ' ') : text;
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static void onAwtEvent(AWTEvent event)
	{
		if (event instanceof ContainerEvent ce && ce.getID() == ContainerEvent.COMPONENT_ADDED)
		{
			disarmTree(ce.getChild());
			return;
		}
		if (!(event instanceof MouseEvent))
			return;
		MouseEvent e = (MouseEvent) event;
		int id = e.getID();
		if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_WHEEL)
		{
			hide();
			return;
		}
		if (!(e.getSource() instanceof JComponent))
			return;
		JComponent c = (JComponent) e.getSource();
		if (c == overlay)
			return;
		if (id == MouseEvent.MOUSE_ENTERED)
		{
			if (hintFrom(c) != null)
				show(c);
			return;
		}
		if (id == MouseEvent.MOUSE_EXITED)
			hideIfOwner(c);
	}

	static void dispatchForTest(JComponent c, int eventId)
	{
		if (c == null)
			return;
		onAwtEvent(new MouseEvent(c, eventId, 0, 0, 1, 1, 0, false));
	}
}
