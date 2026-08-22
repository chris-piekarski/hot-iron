package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * ATSC video surface (TV-tuner preview, or unused bottom card).
 */
public final class TvVideoPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private volatile BufferedImage frame;
	private volatile String status = "WATCH — waiting for ATSC lock";

	public TvVideoPanel()
	{
		setBackground(Color.BLACK);
		setOpaque(true);
		setPreferredSize(new Dimension(100, 200));
		setMinimumSize(new Dimension(100, 200));
	}

	public void setFrame(BufferedImage img)
	{
		if (img == null)
		{
			frame = null;
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
		repaint();
	}

	BufferedImage currentFrame()
	{
		return frame;
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
		if (img != null && img.getWidth() > 0)
		{
			g.drawImage(img, 0, 0, w, h, null);
		}
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		float fontPx = h < 140 ? 11f : 14f;
		g.setFont(getFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, (int) fontPx)
				: getFont().deriveFont(Font.BOLD, fontPx));
		g.setColor(new Color(255, 186, 64));
		g.drawString(status, 8, h < 140 ? 16 : 22);
	}
}
