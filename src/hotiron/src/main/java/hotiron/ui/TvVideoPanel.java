package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import hotiron.core.MpegTsPlayer;

/**
 * ATSC video surface. Sized to the frame aspect (16:9 until a picture
 * arrives); the picture is letterboxed, not stretched.
 */
public final class TvVideoPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	public static final float DEFAULT_ASPECT = MpegTsPlayer.WIDTH / (float) MpegTsPlayer.HEIGHT;
	private static final int MIN_W = 160;
	private volatile BufferedImage frame;
	private volatile String status = "WATCH — waiting for ATSC lock";
	private volatile float aspect = DEFAULT_ASPECT;

	public TvVideoPanel()
	{
		setBackground(new Color(10, 14, 24));
		setOpaque(true);
	}

	public void setFrame(BufferedImage img)
	{
		if (img == null)
		{
			frame = null;
			setAspect(DEFAULT_ASPECT);
			repaint();
			return;
		}
		BufferedImage dst = frame;
		if (dst == null || dst.getWidth() != img.getWidth() || dst.getHeight() != img.getHeight()
				|| dst.getType() != img.getType())
			dst = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
		Graphics g = dst.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		frame = dst;
		if (img.getHeight() > 0)
			setAspect(img.getWidth() / (float) img.getHeight());
		repaint();
	}

	public void setStatus(String s)
	{
		status = s == null ? "" : s;
		repaint();
	}

	public void clear()
	{
		frame = null;
		status = "no picture";
		setAspect(DEFAULT_ASPECT);
		repaint();
	}

	BufferedImage currentFrame()
	{
		return frame;
	}

	float aspect()
	{
		return aspect;
	}

	private void setAspect(float next)
	{
		float a = next > 0.1f && Float.isFinite(next) ? next : DEFAULT_ASPECT;
		if (Math.abs(a - aspect) < 0.001f)
			return;
		aspect = a;
		revalidate();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(layoutWidth(), heightForWidth(layoutWidth(), aspect));
	}

	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(MIN_W, heightForWidth(MIN_W, aspect));
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, heightForWidth(layoutWidth(), aspect));
	}

	private int layoutWidth()
	{
		int w = getWidth();
		if (w <= 0 && getParent() != null)
			w = getParent().getWidth();
		if (w <= 0)
			w = OperatorLayout.TOOLS_WIDTH - 24;
		return Math.max(MIN_W, w);
	}

	/** Height of a {@code width × aspect} rectangle. */
	public static int heightForWidth(int width, float aspect)
	{
		if (width <= 0 || !(aspect > 0) || !Float.isFinite(aspect))
			return 1;
		return Math.max(1, Math.round(width / aspect));
	}

	/**
	 * Destination rectangle that fits {@code frameW×frameH} inside the
	 * panel (letterbox). Uses expected 16:9 when there is no frame.
	 */
	public static Rectangle destRect(int panelW, int panelH, int frameW, int frameH)
	{
		float a = frameW > 0 && frameH > 0 ? frameW / (float) frameH : DEFAULT_ASPECT;
		if (panelW <= 0 || panelH <= 0 || !(a > 0))
			return new Rectangle(0, 0, Math.max(0, panelW), Math.max(0, panelH));
		int dw = panelW;
		int dh = heightForWidth(dw, a);
		if (dh > panelH)
		{
			dh = panelH;
			dw = Math.max(1, Math.round(panelH * a));
		}
		int x = (panelW - dw) / 2;
		int y = (panelH - dh) / 2;
		return new Rectangle(x, y, dw, dh);
	}

	@Override
	protected void paintComponent(Graphics g0)
	{
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int w = getWidth();
		int h = getHeight();
		g.setColor(new Color(10, 14, 24));
		g.fillRect(0, 0, w, h);
		BufferedImage img = frame;
		Rectangle dest = destRect(w, h, img != null ? img.getWidth() : MpegTsPlayer.WIDTH,
				img != null ? img.getHeight() : MpegTsPlayer.HEIGHT);
		if (img != null && img.getWidth() > 0 && dest.width > 0 && dest.height > 0)
			g.drawImage(img, dest.x, dest.y, dest.width, dest.height, null);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		float fontPx = dest.height < 140 ? 11f : 14f;
		g.setFont(getFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, (int) fontPx)
				: getFont().deriveFont(Font.BOLD, fontPx));
		g.setColor(new Color(255, 186, 64));
		int ty = dest.y + (dest.height < 140 ? 16 : 22);
		g.drawString(status, dest.x + 8, ty);
	}
}
