package hotiron.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.JComponent;

import hotiron.core.FmChannelPlan;

/**
 * 1970s horizontal analog tuner window. Drag left = lower MHz, right =
 * higher. The red pointer is the frequency needle on the printed scale.
 */
public final class FmTunerScale extends JComponent
{
	static final int PREF_H = 92;
	private static final Color HOUSING = new Color(22, 20, 18);
	private static final Color GLASS = new Color(42, 78, 52);
	private static final Color SCALE = new Color(210, 232, 190);
	private static final Color INK = new Color(16, 28, 16);
	private static final Color POINTER = new Color(220, 36, 28);
	private static final Color POINTER_BASE = new Color(200, 180, 80);
	private static final Color STATION = new Color(255, 210, 80);

	private int kHz = 97300;
	private final List<Integer> detents = new ArrayList<Integer>();
	private IntConsumer onSelectKHz;
	private IntConsumer onTune;
	private boolean dragging;

	public FmTunerScale()
	{
		setOpaque(true);
		setPreferredSize(new Dimension(OperatorLayout.TOOLS_WIDTH - 32, PREF_H));
		setMinimumSize(new Dimension(240, 72));
		setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
		ExclusiveToolTip.setText(this,
				"Analog FM slide-rule. Drag left for a lower frequency, right for higher.");
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (!javax.swing.SwingUtilities.isLeftMouseButton(e))
					return;
				dragging = true;
				selectAt(e.getX());
				requestFocusInWindow();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				dragging = false;
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragging)
					selectAt(e.getX());
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				nudge(e.getWheelRotation() < 0 ? +1 : -1);
				e.consume();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}

	public void setOnSelectKHz(IntConsumer onSelectKHz)
	{
		this.onSelectKHz = onSelectKHz;
	}

	public void setOnTune(IntConsumer onTune)
	{
		this.onTune = onTune;
	}

	public void setKHz(int kHz)
	{
		int next = FmChannelPlan.clamp(kHz / 1000.0).centerKHz;
		if (this.kHz == next)
			return;
		this.kHz = next;
		repaint();
	}

	public int getKHz()
	{
		return kHz;
	}

	public void setDetents(List<Integer> stationKHz)
	{
		List<Integer> next = new ArrayList<Integer>();
		if (stationKHz != null)
		{
			for (Integer k : stationKHz)
			{
				if (k != null && !next.contains(k))
					next.add(k);
			}
			next.sort(Integer::compareTo);
		}
		if (next.equals(detents))
			return;
		detents.clear();
		detents.addAll(next);
		repaint();
	}

	public void nudge(int direction)
	{
		if (onTune != null)
			onTune.accept(direction < 0 ? -1 : 1);
	}

	static int xForKHz(int kHz, int width)
	{
		int lo = FmChannelPlan.FIRST_CENTER_KHZ;
		int hi = FmChannelPlan.LAST_CENTER_KHZ;
		double t = (kHz - lo) / (double) (hi - lo);
		if (t < 0)
			t = 0;
		if (t > 1)
			t = 1;
		int pad = pad(width);
		return pad + (int) Math.round(t * (width - 2 * pad));
	}

	static int kHzForX(int x, int width)
	{
		int pad = pad(width);
		double t = (x - pad) / (double) Math.max(1, width - 2 * pad);
		if (t < 0)
			t = 0;
		if (t > 1)
			t = 1;
		int lo = FmChannelPlan.FIRST_CENTER_KHZ;
		int hi = FmChannelPlan.LAST_CENTER_KHZ;
		int kHz = lo + (int) Math.round(t * (hi - lo));
		return FmChannelPlan.clamp(kHz / 1000.0).centerKHz;
	}

	static float kHzTo01(int kHz)
	{
		int lo = FmChannelPlan.FIRST_CENTER_KHZ;
		int hi = FmChannelPlan.LAST_CENTER_KHZ;
		float t = (kHz - lo) / (float) (hi - lo);
		if (t < 0f)
			return 0f;
		if (t > 1f)
			return 1f;
		return t;
	}

	static int kHzFrom01(float t01)
	{
		float t = t01;
		if (t < 0f)
			t = 0f;
		if (t > 1f)
			t = 1f;
		int lo = FmChannelPlan.FIRST_CENTER_KHZ;
		int hi = FmChannelPlan.LAST_CENTER_KHZ;
		int kHz = lo + Math.round(t * (hi - lo));
		return FmChannelPlan.clamp(kHz / 1000.0).centerKHz;
	}

	private static int pad(int width)
	{
		return Math.max(14, Math.min(32, width / 16));
	}

	private void selectAt(int x)
	{
		int next = kHzForX(x, getWidth());
		if (onSelectKHz != null)
			onSelectKHz.accept(next);
		else
			setKHz(next);
	}

	@Override
	protected void paintComponent(Graphics g0)
	{
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			g.setColor(HOUSING);
			g.fillRoundRect(0, 0, w, h, 10, 10);
			g.setColor(new Color(90, 86, 78));
			g.setStroke(new BasicStroke(2.5f));
			g.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
			int gx = 8;
			int gy = 8;
			int gw = w - 16;
			int gh = h - 16;
			g.setColor(GLASS);
			g.fillRoundRect(gx, gy, gw, gh, 6, 6);
			g.setColor(SCALE);
			g.fillRect(gx + 4, gy + 18, gw - 8, gh - 28);
			g.setColor(INK);
			g.setFont(getFont().deriveFont(Font.BOLD, 9f));
			g.drawString("FM MHz", gx + 8, gy + 14);
			int baseline = gy + gh - 8;
			int tickTop = gy + 22;
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			for (int mhz = 88; mhz <= 108; mhz += 2)
			{
				int k = mhz == 88 ? FmChannelPlan.FIRST_CENTER_KHZ
						: (mhz == 108 ? FmChannelPlan.LAST_CENTER_KHZ : mhz * 1000);
				int tx = xForKHz(k, w);
				boolean major = mhz == 88 || mhz == 98 || mhz == 108;
				g.setStroke(new BasicStroke(major ? 2.0f : 1.2f));
				g.drawLine(tx, tickTop, tx, baseline - 16);
				String label = Integer.toString(mhz);
				int tw = g.getFontMetrics().stringWidth(label);
				g.drawString(label, tx - tw / 2, baseline);
			}
			g.setStroke(new BasicStroke(1.0f));
			for (int mhz = 89; mhz <= 107; mhz += 2)
			{
				int tx = xForKHz(mhz * 1000 + 100, w);
				g.drawLine(tx, tickTop + 10, tx, baseline - 20);
			}
			g.setColor(STATION);
			g.setStroke(new BasicStroke(2.4f));
			for (int i = 0; i < detents.size(); i++)
			{
				int tx = xForKHz(detents.get(i).intValue(), w);
				g.drawLine(tx, tickTop - 2, tx, tickTop + 8);
			}
			int fx = xForKHz(kHz, w);
			g.setColor(POINTER);
			g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(fx, gy + 6, fx, gy + gh - 6);
			int[] xs = { fx - 7, fx + 7, fx };
			int[] ys = { gy + 4, gy + 4, gy + 16 };
			g.fillPolygon(xs, ys, 3);
			g.setColor(POINTER_BASE);
			g.fillRect(fx - 5, gy + gh - 12, 10, 8);
		}
		finally
		{
			g.dispose();
		}
	}
}
