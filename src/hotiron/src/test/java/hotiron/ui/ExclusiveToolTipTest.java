package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExclusiveToolTipTest {

	@BeforeEach
	void reset() {
		ExclusiveToolTip.hide();
		AnalyzerLookAndFeel.install();
		ExclusiveToolTip.installShared();
	}

	@Test
	void hideIsSafeWhenNothingIsShowing() {
		ExclusiveToolTip.hide();
		assertFalse(ExclusiveToolTip.isShowing());
		JButton button = new JButton("FM");
		ExclusiveToolTip.hideIfOwner(button);
		assertFalse(ExclusiveToolTip.isShowing());
	}

	@Test
	void setTextNeverRegistersASwingTooltip() {
		JButton scan = new JButton("Scan");
		scan.setToolTipText("leftover");
		ExclusiveToolTip.setText(scan, "Sweep 88–108 MHz");
		assertNull(scan.getToolTipText());
		assertEquals("Sweep 88–108 MHz", ExclusiveToolTip.hintOf(scan));
		assertFalse(ExclusiveToolTip.opensAWindow());
	}

	@Test
	void installClearsSwingTooltipTextSoQuickSelectStaysInPanel() {
		JButton button = new JButton("FM");
		button.setToolTipText("88–108 MHz. long citation");
		ExclusiveToolTip.install(button, "88–108 MHz");
		assertNull(button.getToolTipText());
		ExclusiveToolTip.dispatchForTest(button, MouseEvent.MOUSE_ENTERED);
		assertFalse(ExclusiveToolTip.isShowing(), "install without hint must not open a tip");
	}

	@Test
	void hoveringASecondButtonReplacesTheFirstHint() {
		JButton scan = new JButton("Scan");
		JButton listen = new JButton("Listen");
		ExclusiveToolTip.setText(scan, "Sweep 88–108 MHz");
		ExclusiveToolTip.setText(listen, "Park the radio");
		assertNull(scan.getToolTipText());
		assertNull(listen.getToolTipText());
		ExclusiveToolTip.dispatchForTest(scan, MouseEvent.MOUSE_ENTERED);
		assertTrue(ExclusiveToolTip.isShowing());
		assertEquals(scan, ExclusiveToolTip.owner());
		assertTrue(ExclusiveToolTip.overlayText().contains("88–108"));
		ExclusiveToolTip.dispatchForTest(listen, MouseEvent.MOUSE_ENTERED);
		ExclusiveToolTip.dispatchForTest(scan, MouseEvent.MOUSE_EXITED);
		assertTrue(ExclusiveToolTip.isShowing());
		assertEquals(listen, ExclusiveToolTip.owner(), "exit of the old button must not clear the new hint");
		assertTrue(ExclusiveToolTip.overlayText().contains("Park the radio"));
		ExclusiveToolTip.dispatchForTest(listen, MouseEvent.MOUSE_EXITED);
		assertFalse(ExclusiveToolTip.isShowing());
	}

	@Test
	void tunerHoverDoesNotChangePanelSize() {
		TunerPanel tuner = new TunerPanel();
		Dimension before = tuner.getPreferredSize();
		assertNull(tuner.scanButton().getToolTipText());
		ExclusiveToolTip.dispatchForTest(tuner.scanButton(), MouseEvent.MOUSE_ENTERED);
		assertTrue(ExclusiveToolTip.isShowing());
		assertTrue(ExclusiveToolTip.overlayText().contains("88–108"));
		assertEquals(before, tuner.getPreferredSize());
		assertNull(tuner.scanButton().getToolTipText());
	}
}
