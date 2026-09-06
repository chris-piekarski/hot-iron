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
				new BleFrame(1, 37, -40, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true),
				new BleFrame(2, 39, -41, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true),
				new BleFrame(3, 38, -50, "ADV_SCAN", "11:22:33:44:55:66", "00", true))));
		assertNull(panel.selected());
		assertDoesNotThrow(() -> panel.ingest(new BleFrame(4, 37, -42, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true)));
		assertDoesNotThrow(() -> panel.setSniffing(true));
		assertEquals("Stop", panel.sniffButton().getText());
		assertDoesNotThrow(() -> panel.setSniffing(false));
		assertEquals("Sniff", panel.sniffButton().getText());
	}
}
