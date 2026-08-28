package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import hotiron.core.BandMark;
import hotiron.core.FmChannel;
import hotiron.core.FmChannelPlan;
import hotiron.core.FrequencyAxis;
import hotiron.core.WifiBandLayer;

class BandHeaderPainterTest {

	@Test
	void overlapsUsesTheSharedGap() {
		List<double[]> placed = new ArrayList<>();
		placed.add(new double[] { 10, 20 });
		assertTrue(BandHeaderPainter.overlaps(placed, 15, 25));
		assertFalse(BandHeaderPainter.overlaps(placed, 40, 50));
	}

	@Test
	void intensityScalesAlpha() {
		Color base = new Color(80, 160, 230, 200);
		assertTrue(BandHeaderPainter.withIntensity(base, 1f).getAlpha() > BandHeaderPainter
				.withIntensity(base, 0.2f).getAlpha());
	}

	@Test
	void paintWifi2MarksTheHeaderAndNoopsOnEmpty() {
		BufferedImage img = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle2D area = new Rectangle2D.Double(40, 20, 720, 250);
		FrequencyAxis axis = FrequencyAxis.fromArea(area, 2402, 2472);
		assertDoesNotThrow(() -> BandHeaderPainter.paint(g, area, axis, List.of()));
		assertDoesNotThrow(() -> BandHeaderPainter.paint(g, area, axis, WifiBandLayer.marks(axis)));
		g.dispose();
		boolean painted = false;
		for (int x = 40; x < 760 && !painted; x++)
			painted = ((img.getRGB(x, 30) >>> 24) & 0xFF) > 0;
		assertTrue(painted);
	}

	@Test
	void hitTestFindsPrimaryHeaderMarkAndIgnoresPlotBody() {
		Rectangle2D area = new Rectangle2D.Double(40, 20, 720, 250);
		FrequencyAxis axis = FrequencyAxis.fromArea(area, 88, 108);
		FmChannel ch = FmChannelPlan.nearest(97.3);
		BandMark mark = new BandMark(ch.lowMHz(), ch.highMHz(), ch.centerMHz(), ch.label(), BandMark.Style.PRIMARY,
				false, false, true, BandMark.LabelFit.DROP_IF_OVERLAP, 1f);
		int x = axis.mhzToXInt(97.3);
		assertEquals(ch.label(), BandHeaderPainter.hitTest(x, area.getMinY() + 4, area, axis, List.of(mark)).label);
		assertNull(BandHeaderPainter.hitTest(x, area.getMinY() + BandHeaderPainter.HEADER_H + 20, area, axis,
				List.of(mark)));
	}

	@Test
	void labelsSitInTheTopHeader() {
		java.awt.Font font = BandHeaderPainter.labelFont(false);
		BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		java.awt.FontMetrics fm = img.createGraphics().getFontMetrics(font);
		Rectangle2D area = new Rectangle2D.Double(20, 10, 860, 270);
		float y = BandHeaderPainter.labelBaselineY(area, fm);
		assertTrue(y < area.getMinY() + BandHeaderPainter.HEADER_H);
		assertTrue(y < area.getCenterY());
		assertTrue(BandHeaderPainter.HEADER_H >= fm.getHeight() + 4,
				"header must fit the bold channel labels");
		assertEquals(13, BandHeaderPainter.LABEL_SIZE);
	}
}
