package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import hotiron.core.NfcEnvelopeTrace;

class NfcEnvelopeScopeTest
{
	@Test
	void bannerNamesTheCarrierScope()
	{
		assertEquals("13.56  |IQ|  500 ms", NfcEnvelopeScope.banner());
	}

	@Test
	void yWindowKeepsDecoderFloorInView()
	{
		float[] db = new float[32];
		Arrays.fill(db, -36f);
		float[] yr = NfcEnvelopeScope.yWindow(db);
		assertTrue(yr[0] <= NfcEnvelopeTrace.DECODER_FLOOR_DB);
		assertTrue(yr[1] > -36f);
	}

	@Test
	void hoverIsAgeAndDbfsFromTheRight()
	{
		float[] db = new float[11];
		Arrays.fill(db, -30f);
		assertTrue(NfcEnvelopeScope.hover(db, 0.50f, 0, 100).startsWith("−500.0 ms"));
		String now = NfcEnvelopeScope.hover(db, 0.10f, 100, 100);
		assertTrue(now.startsWith("−0.0 ms"));
		assertTrue(now.contains("30.0 dBFS"));
	}

	@Test
	void paintDoesNotThrowOnEmpty()
	{
		BufferedImage img = new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		assertDoesNotThrow(() -> NfcEnvelopeScope.paint(g, new Rectangle2D.Double(40, 0, 160, 80), null, 0.05f));
		float[] live = new float[NfcEnvelopeTrace.WINDOW_SAMPLES];
		Arrays.fill(live, 0, 2000, NfcEnvelopeTrace.EMPTY_DB);
		Arrays.fill(live, 2000, live.length, -36f);
		assertDoesNotThrow(() -> NfcEnvelopeScope.paint(g, new Rectangle2D.Double(40, 0, 160, 80), live, 0.05f));
		g.dispose();
	}
}
