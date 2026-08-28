package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

import hotiron.core.FrequencyRange;

class SpectrumWavePainterTest
{
	@Test
	void paintsTheFullSpanWithoutThrowing()
	{
		BufferedImage img = new BufferedImage(800, SurveyChipLayout.WAVE_H, BufferedImage.TYPE_INT_RGB);
		SpectrumWavePainter.paint(img.createGraphics(),
				new Rectangle2D.Double(0, 0, 800, SurveyChipLayout.WAVE_H),
				new FrequencyRange(88, 108),
				SurveyChipLayout.place(800, p -> 80), QuickSelectPreset.FM);
		assertTrue(img.getRGB(10, 10) != 0);
	}

	@Test
	void goldWindowGrowsWhenBandwidthDoubles()
	{
		int[] wifi = SpectrumWavePainter.windowPixels(new FrequencyRange(2402, 2472), 1000);
		int[] wider = SpectrumWavePainter.windowPixels(new FrequencyRange(2367, 2507), 1000);
		assertTrue(wifi[1] - wifi[0] >= 60, "WiFi 2 gold must be readable, was " + (wifi[1] - wifi[0]));
		assertTrue(wider[1] - wider[0] > wifi[1] - wifi[0], "doubling span must widen the gold overlay");
	}
}
