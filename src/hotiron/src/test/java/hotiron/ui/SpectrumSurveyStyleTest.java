package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class SpectrumSurveyStyleTest
{
	@Test
	void envelopePresetsAreNotTheSameGray()
	{
		Color hf = SpectrumSurveyStyle.color(QuickSelectPreset.HF);
		Color vhf = SpectrumSurveyStyle.color(QuickSelectPreset.VHF);
		Color uhf = SpectrumSurveyStyle.color(QuickSelectPreset.UHF);
		Color all = SpectrumSurveyStyle.color(QuickSelectPreset.ALL);
		assertNotEquals(hf, vhf);
		assertNotEquals(vhf, uhf);
		assertNotEquals(uhf, all);
		assertTrue(chroma(hf) > 40, "HF must be blue, not gray");
		assertTrue(chroma(vhf) > 40, "VHF must be teal, not gray");
		assertTrue(chroma(uhf) > 40, "UHF must be copper, not gray");
	}

	@Test
	void envelopeChipFillsKeepTheHue()
	{
		Color hf = SpectrumSurveyStyle.chipBackground(QuickSelectPreset.HF, false);
		Color vhf = SpectrumSurveyStyle.chipBackground(QuickSelectPreset.VHF, false);
		Color uhf = SpectrumSurveyStyle.chipBackground(QuickSelectPreset.UHF, false);
		assertTrue(hf.getBlue() > hf.getRed() + 20, "HF fill stays blue");
		assertTrue(vhf.getGreen() > vhf.getRed() + 20, "VHF fill stays teal");
		assertTrue(uhf.getRed() > uhf.getBlue() + 20, "UHF fill stays copper");
		Color selected = SpectrumSurveyStyle.chipBackground(QuickSelectPreset.HF, true);
		assertTrue(brightness(selected) > brightness(hf), "selected envelope is brighter");
	}

	private static int chroma(Color c)
	{
		return Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()))
				- Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
	}

	private static int brightness(Color c)
	{
		return c.getRed() + c.getGreen() + c.getBlue();
	}
}
