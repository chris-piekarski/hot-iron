package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class WatchPreviewTest {

	@Test
	void pushDbFillsAVisible640x360Frame() {
		WatchPreview preview = new WatchPreview();
		float[] db = new float[IqSpectrum.FFT_N];
		Arrays.fill(db, -50f);
		db[IqSpectrum.FFT_N / 2] = -8f;
		BufferedImage a = preview.pushDb(db);
		assertEquals(WatchPreview.WIDTH, a.getWidth());
		assertEquals(WatchPreview.HEIGHT, a.getHeight());
		assertEquals(1, preview.frames());
		int mid = luminance(a, WatchPreview.WIDTH / 2, WatchPreview.HEIGHT * 2 / 3);
		int edge = luminance(a, 12, WatchPreview.HEIGHT * 2 / 3);
		assertTrue(mid > edge + 40, "center peak should fill more of the frame than the edge");
		preview.pushDb(db);
		assertEquals(2, preview.frames());
	}

	@Test
	void rgbForDbIsColdAtFloorAndHotAtZero() {
		int cold = WatchPreview.rgbForDb(-80f);
		int hot = WatchPreview.rgbForDb(0f);
		int coldB = cold & 0xff;
		int hotR = (hot >> 16) & 0xff;
		assertTrue(hotR > 200, "hot is red/white");
		assertTrue(coldB > ((cold >> 16) & 0xff), "floor is bluish");
	}

	private static int luminance(BufferedImage img, int x, int y) {
		int rgb = img.getRGB(x, y);
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;
		return r + g + b;
	}
}
