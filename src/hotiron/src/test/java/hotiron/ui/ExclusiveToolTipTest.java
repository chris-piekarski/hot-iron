package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

class ExclusiveToolTipTest {

	@Test
	void hideIsSafeWhenNothingIsShowing() {
		ExclusiveToolTip.hide();
		assertFalse(ExclusiveToolTip.isShowing());
		JButton button = new JButton("FM");
		ExclusiveToolTip.hideIfOwner(button);
		assertFalse(ExclusiveToolTip.isShowing());
	}

	@Test
	void installClearsSwingTooltipTextSoTheManagerCannotStackWindows() {
		JButton button = new JButton("FM");
		button.setToolTipText("88–108 MHz. long citation");
		ExclusiveToolTip.install(button, "88–108 MHz");
		assertNull(button.getToolTipText());
		ExclusiveToolTip.show(button, "88–108 MHz");
		assertFalse(ExclusiveToolTip.isShowing(), "must not open a floating window");
	}
}
