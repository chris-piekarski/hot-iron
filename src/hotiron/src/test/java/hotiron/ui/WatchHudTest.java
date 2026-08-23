package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class WatchHudTest {

	@Test
	void textNamesTheChannel() {
		assertEquals("WATCH ch 14 — parked IQ · 8VSB weak  0 dB (no picture)",
				WatchHud.text(14, true, 0f, 0, 0, 0));
		assertEquals("WATCH ch 14 — parked IQ · ATSC lock  16 dB", WatchHud.text(14, true, 16.2f, 0, 0, 0));
		assertEquals("WATCH ch 14 — parked IQ · no ATSC lock", WatchHud.text(14, false, 0f, 0, 0, 0));
		assertEquals("WATCH ch 18 — parked IQ · 8VSB  900 TS pkt (no picture)",
				WatchHud.text(18, false, -30f, 900, 0, 0));
		assertEquals("WATCH ch 18 — parked IQ · live", WatchHud.text(18, true, 16f, 900, 3, 0));
		assertEquals("WATCH ch 18 — parked IQ · IQ video", WatchHud.text(18, false, -30f, 900, 0, 12));
	}

	@Test
	void paintDoesNotThrow() {
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		assertDoesNotThrow(() -> WatchHud.paint(g, new Rectangle2D.Double(20, 10, 360, 160), 14, false,
				0f, 0, 0, 0));
		g.dispose();
	}
}
