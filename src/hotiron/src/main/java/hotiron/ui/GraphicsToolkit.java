package hotiron.ui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

public class GraphicsToolkit {

	public static BufferedImage createAcceleratedImageTransparent(int width, int height) {
		return createAcceleratedImage(width, height, Transparency.TRANSLUCENT);
	}
	
	public static BufferedImage createAcceleratedImageOpaque(int width, int height) {
		return createAcceleratedImage(width, height, Transparency.OPAQUE);
	}
	
	private static BufferedImage createAcceleratedImage(int width, int height, int transparency) {
		int w = Math.max(width, 1);
		int h = Math.max(height, 1);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (ge.isHeadlessInstance()) {
			int type = (transparency == Transparency.OPAQUE)
					? BufferedImage.TYPE_INT_RGB
					: BufferedImage.TYPE_INT_ARGB;
			return new BufferedImage(w, h, type);
		}
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage image = gc.createCompatibleImage(w, h, transparency);
		image.setAccelerationPriority(1);
		return image;
	}
	
}
