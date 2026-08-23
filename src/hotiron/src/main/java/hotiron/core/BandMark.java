package hotiron.core;

/**
 * One labeled interval for the shared plot header painter. Layers (Wi-Fi,
 * FM, Quick Select) produce these; they do not paint.
 */
public final class BandMark
{
	public enum Style
	{
		PRIMARY, SECONDARY, SURVEY, TUNED
	}

	public enum LabelFit
	{
		/** Drop the label if it collides with one already placed. */
		DROP_IF_OVERLAP,
		/** Drop the label if it is wider than the visible occupancy. */
		FIT_OCCUPANCY
	}

	public final double lowMHz;
	public final double highMHz;
	public final double labelMHz;
	public final String label;
	public final Style style;
	public final boolean fullHeightFill;
	public final boolean edgeTicks;
	public final boolean centerTick;
	public final LabelFit labelFit;
	public final float intensity;

	public BandMark(double lowMHz, double highMHz, double labelMHz, String label, Style style,
			boolean fullHeightFill, boolean edgeTicks, boolean centerTick, LabelFit labelFit, float intensity)
	{
		this.lowMHz = lowMHz;
		this.highMHz = highMHz;
		this.labelMHz = labelMHz;
		this.label = label == null ? "" : label;
		this.style = style == null ? Style.SECONDARY : style;
		this.fullHeightFill = fullHeightFill;
		this.edgeTicks = edgeTicks;
		this.centerTick = centerTick;
		this.labelFit = labelFit == null ? LabelFit.DROP_IF_OVERLAP : labelFit;
		this.intensity = Math.max(0f, Math.min(1f, intensity));
	}

	public boolean centerIn(FrequencyAxis axis)
	{
		return axis != null && axis.containsMhz(labelMHz);
	}
}
