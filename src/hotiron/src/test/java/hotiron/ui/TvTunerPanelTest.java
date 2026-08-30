package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import hotiron.core.TvChannelGrade;
import hotiron.core.TvChannelPlan;
import hotiron.core.TvStationHit;

class TvTunerPanelTest
{
	@Test
	void rosterGroupsPictureAheadOfRfOnly()
	{
		TvTunerPanel p = new TvTunerPanel();
		p.setStations(List.of(
				hit(18, TvChannelGrade.OCCUPIED),
				hit(33, TvChannelGrade.PICTURE)));
		List<TvStationHit> rows = p.roster().rows();
		assertEquals(2, rows.size());
		assertEquals(33, rows.get(0).channel.fccChannel);
		assertEquals(18, rows.get(1).channel.fccChannel);
		assertTrue(p.roster().summaryText().contains("1 picture"));
		assertTrue(p.roster().summaryText().contains("1 RF only"));
		assertEquals(2, p.roster().buttons().size());
		assertEquals("33", p.roster().buttons().get(0).getText());
		assertEquals("18", p.roster().buttons().get(1).getText());
	}

	@Test
	void scanButtonShowsQualifyingChannel()
	{
		TvTunerPanel p = new TvTunerPanel();
		assertEquals("Scan", p.scanButton().getText());
		p.setScanning(true);
		assertEquals("Scanning…", p.scanButton().getText());
		p.setScanning(false);
		p.setQualifying(true, 33);
		assertEquals("Qualifying ch 33…", p.scanButton().getText());
		p.setQualifying(false, 0);
		assertEquals("Scan", p.scanButton().getText());
	}

	@Test
	void channelButtonsWrapInsideThePanel()
	{
		TvTunerPanel p = new TvTunerPanel();
		java.util.ArrayList<TvStationHit> hits = new java.util.ArrayList<>();
		int[] chs = { 4, 6, 14, 15, 17, 18, 29, 31, 32, 33, 34, 35, 36 };
		for (int ch : chs)
			hits.add(hit(ch, TvChannelGrade.OCCUPIED));
		p.setStations(hits);
		p.setSize(240, 900);
		p.doLayout();
		TvChannelRoster roster = p.roster();
		roster.setSize(220, 400);
		roster.doLayout();
		roster.grid().setSize(220, 400);
		roster.grid().doLayout();
		java.util.TreeSet<Integer> ys = new java.util.TreeSet<>();
		int maxRight = 0;
		for (javax.swing.JButton b : roster.buttons())
		{
			ys.add(Integer.valueOf(b.getY()));
			maxRight = Math.max(maxRight, b.getX() + b.getWidth());
		}
		assertTrue(ys.size() >= 2, "13 buttons wrap to a second row in a 220 px strip");
		assertTrue(maxRight <= 220, "nothing runs off the right edge");
	}

	@Test
	void picturePaneDoesNotCoverChannelButtons()
	{
		TvTunerPanel p = new TvTunerPanel();
		java.util.ArrayList<TvStationHit> hits = new java.util.ArrayList<>();
		int[] chs = { 4, 6, 14, 15, 17, 18, 29, 31, 32, 33, 34, 35, 36 };
		for (int ch : chs)
			hits.add(hit(ch, TvChannelGrade.OCCUPIED));
		p.setStations(hits);
		p.setSize(500, 900);
		p.doLayout();
		java.awt.Component stack = p.getComponent(0);
		stack.setSize(500, Math.max(400, stack.getPreferredSize().height));
		stack.doLayout();
		java.awt.Rectangle roster = javax.swing.SwingUtilities.convertRectangle(p.roster().getParent(),
				p.roster().getBounds(), p);
		java.awt.Rectangle picture = javax.swing.SwingUtilities.convertRectangle(p.previewPanel().getParent(),
				p.previewPanel().getBounds(), p);
		assertFalse(roster.intersects(picture), "channel buttons must not sit under the picture");
		assertTrue(roster.y + roster.height <= picture.y);
		assertTrue(roster.height > 56, "wrapped roster is taller than one clipped row");
	}

	@Test
	void clickingARosterRowSelectsTheChannel()
	{
		TvTunerPanel p = new TvTunerPanel();
		AtomicInteger got = new AtomicInteger();
		p.setOnSelect(got::set);
		p.setStations(List.of(hit(28, TvChannelGrade.OCCUPIED)));
		assertEquals(1, p.roster().buttons().size());
		p.roster().buttons().get(0).doClick();
		assertEquals(28, got.get());
	}

	private static TvStationHit hit(int ch, TvChannelGrade grade)
	{
		return new TvStationHit(TvChannelPlan.findByFccChannel(ch), -40f, 1f, grade, "", 0, Float.NaN,
				Float.NaN);
	}
}
