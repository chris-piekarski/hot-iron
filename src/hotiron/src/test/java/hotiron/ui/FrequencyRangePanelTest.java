package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import hotiron.core.FrequencyRange;

class FrequencyRangePanelTest {

	@Test
	void panAndZoomButtonsMoveTheWindow() {
		FrequencyRangePanel panel = new FrequencyRangePanel();
		panel.setRange(new FrequencyRange(88, 108));
		panel.panRightButton().doClick();
		assertEquals(93, panel.getRange().getStartMHz());
		assertEquals(113, panel.getRange().getEndMHz());
		panel.setRange(new FrequencyRange(88, 108));
		panel.zoomInButton().doClick();
		assertEquals(10, panel.getRange().spanMHz());
		panel.setRange(new FrequencyRange(88, 108));
		panel.zoomOutButton().doClick();
		assertEquals(40, panel.getRange().spanMHz());
	}

	@Test
	void setRangeFiresOnce() {
		FrequencyRangePanel panel = new FrequencyRangePanel();
		AtomicInteger n = new AtomicInteger();
		panel.addRangeListener(e -> n.incrementAndGet());
		panel.setRange(new FrequencyRange(88, 108));
		assertEquals(1, n.get());
		panel.setRange(new FrequencyRange(88, 108));
		assertEquals(1, n.get());
	}

	@Test
	void readoutShowsStartEndAndSpan() {
		FrequencyRangePanel panel = new FrequencyRangePanel();
		panel.setRange(new FrequencyRange(2402, 2472));
		assertTrue(panel.readoutLabel().getText().contains("2402"));
		assertTrue(panel.readoutLabel().getText().contains("2472"));
		assertTrue(panel.readoutLabel().getText().contains("MHz"));
	}

	@Test
	void typingARangeCommitsOnEnter() {
		FrequencyRangePanel panel = new FrequencyRangePanel();
		panel.setRange(new FrequencyRange(2402, 2472));
		panel.beginEdit();
		panel.editField().setText("88-108");
		panel.editField().postActionEvent();
		assertEquals(88, panel.getRange().getStartMHz());
		assertEquals(108, panel.getRange().getEndMHz());
	}

	@Test
	void garbageEditKeepsTheCurrentRange() {
		FrequencyRangePanel panel = new FrequencyRangePanel();
		panel.setRange(new FrequencyRange(88, 108));
		panel.beginEdit();
		panel.editField().setText("nope");
		panel.editField().postActionEvent();
		assertEquals(88, panel.getRange().getStartMHz());
		assertEquals(108, panel.getRange().getEndMHz());
	}
}
