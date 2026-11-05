package hotiron.ui;

import java.awt.BasicStroke;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Live and peak spectrum strokes. The chart panel stays the interaction
 * surface: the waterfall and band overlays share its data area.
 */
public final class SpectrumTraceRenderer extends XYLineAndShapeRenderer
{
	private static final long serialVersionUID = 1L;

	public SpectrumTraceRenderer()
	{
		super(true, false);
		setDefaultShapesVisible(false);
		setDefaultStroke(new BasicStroke(OperatorLayout.SPECTRUM_LINE_WIDTH, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		setAutoPopulateSeriesStroke(false);
		setAutoPopulateSeriesPaint(false);
	}
}
