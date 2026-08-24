package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.core.BleFrame;

class BleSniffPanelTest
{
	@Test
	void setFramesAndToggleDoNotThrow()
	{
		BleSniffPanel panel = new BleSniffPanel();
		assertDoesNotThrow(() -> panel.setFrames(java.util.List.of(
				new BleFrame(1, 37, -40, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true))));
		assertDoesNotThrow(() -> panel.setSniffing(true));
		assertEquals("Stop", panel.sniffButton().getText());
		assertDoesNotThrow(() -> panel.setSniffing(false));
		assertEquals("Sniff", panel.sniffButton().getText());
	}
}
