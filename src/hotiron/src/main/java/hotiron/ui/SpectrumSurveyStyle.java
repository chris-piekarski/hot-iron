package hotiron.ui;

import java.awt.Color;

/**
 * Group colors for the survey banner. Wave fills and chip borders share
 * these so a human can match a button to a slice. ITU envelopes (HF /
 * VHF / UHF / All) have their own tints so they do not disappear on a
 * gray banner.
 */
public final class SpectrumSurveyStyle
{
	static final Color BROADCAST = new Color(255, 186, 64);
	static final Color ISM = new Color(80, 196, 230);
	static final Color CELLULAR = new Color(188, 132, 255);
	static final Color AVIATION = new Color(150, 176, 255);
	static final Color HAM = new Color(96, 210, 130);
	static final Color SURVEY = new Color(186, 186, 190);
	static final Color HF = new Color(72, 148, 255);
	static final Color VHF = new Color(48, 204, 176);
	static final Color UHF = new Color(232, 148, 72);
	static final Color ALL = new Color(232, 214, 150);
	static final Color TEXT = new Color(255, 244, 214);

	private SpectrumSurveyStyle()
	{
	}

	public static Color color(QuickSelectPreset preset)
	{
		if (preset == null)
			return SURVEY;
		if (preset == QuickSelectPreset.HF)
			return HF;
		if (preset == QuickSelectPreset.VHF)
			return VHF;
		if (preset == QuickSelectPreset.UHF)
			return UHF;
		if (preset == QuickSelectPreset.ALL)
			return ALL;
		return color(preset.group);
	}

	/**
	 * Opaque chip fill. Envelopes keep more of the hue so HF/VHF/UHF/All
	 * read as tinted buttons, not charcoal on charcoal.
	 */
	public static Color chipBackground(QuickSelectPreset preset, boolean selected)
	{
		Color c = color(preset);
		boolean envelope = preset != null && (preset.surveyEnvelope() || preset == QuickSelectPreset.ALL);
		double mix = selected ? (envelope ? 0.52 : 0.38) : (envelope ? 0.36 : 0.22);
		int floor = 18;
		return new Color(blend(c.getRed(), floor, mix), blend(c.getGreen(), floor, mix),
				blend(c.getBlue(), floor, mix));
	}

	private static int blend(int channel, int floor, double mix)
	{
		int v = floor + (int) Math.round((channel - floor) * mix);
		if (v < 0)
			return 0;
		if (v > 255)
			return 255;
		return v;
	}

	public static Color color(QuickSelectPreset.Group group)
	{
		if (group == null)
			return SURVEY;
		switch (group)
		{
		case BROADCAST:
			return BROADCAST;
		case ISM:
			return ISM;
		case CELLULAR:
			return CELLULAR;
		case AVIATION:
			return AVIATION;
		case HAM:
			return HAM;
		case SURVEY:
		default:
			return SURVEY;
		}
	}

	public static Color fill(QuickSelectPreset preset, boolean selected)
	{
		Color c = color(preset);
		int a = selected ? 110 : (preset != null && preset.surveyEnvelope() ? 28 : 55);
		if (preset == QuickSelectPreset.ALL)
			a = selected ? 50 : 18;
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}

	public static Color line(QuickSelectPreset preset, boolean selected)
	{
		Color c = color(preset);
		int a = selected ? 240 : 170;
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}
}
