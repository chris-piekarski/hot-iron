package hotiron.ui;

import java.awt.Dimension;

/**
 * Shared chrome sizes. HotIron, the tools column, and the band slot
 * read these so a width or split change is one edit.
 */
public final class OperatorLayout
{
	public static final int TOOLS_WIDTH = 400;
	public static final int BAND_SLOT_HEIGHT = 340;
	/** Sweep-range column beside the survey wave (digits + ◀ − + ▶). */
	public static final int RANGE_WIDTH = 248;
	public static final double PLOT_SPLIT = 0.55;
	/** Listen dual strip: parked RF waterfall | AUDIO waterfall. */
	public static final double LISTEN_WATERFALL_SPLIT = 0.5;
	public static final int MIN_FRAME_WIDTH = 1100;
	public static final int MIN_FRAME_HEIGHT = 640;

	private OperatorLayout()
	{
	}

	public static Dimension minFrame()
	{
		return new Dimension(MIN_FRAME_WIDTH, MIN_FRAME_HEIGHT);
	}

	public static Dimension toolsColumn(int heightHint)
	{
		return new Dimension(TOOLS_WIDTH, Math.max(200, heightHint));
	}

	public static Dimension bandSlot()
	{
		return new Dimension(TOOLS_WIDTH - 16, BAND_SLOT_HEIGHT);
	}

	public static int plotDivider(int splitHeight)
	{
		if (splitHeight <= 80)
			return -1;
		return (int) Math.round(splitHeight * PLOT_SPLIT);
	}
}
