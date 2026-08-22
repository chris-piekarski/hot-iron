package hotiron.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SweepStatusBarTest {

	@Test
	void formatHzPicksUnit() {
		assertEquals("—", SweepStatusBar.formatHz(0));
		assertEquals("500 Hz", SweepStatusBar.formatHz(500));
		assertEquals("98.0 kHz", SweepStatusBar.formatHz(98_000));
		assertEquals("1.00 MHz", SweepStatusBar.formatHz(1_000_000));
	}

	@Test
	void formatBinsGroupsThousands() {
		assertEquals("—", SweepStatusBar.formatBins(0));
		assertEquals("1,019", SweepStatusBar.formatBins(1019));
	}

	@Test
	void formatFpsAndPeak() {
		assertEquals("—", SweepStatusBar.formatFps(0));
		assertEquals("81 fps", SweepStatusBar.formatFps(81.4));
		assertEquals("—", SweepStatusBar.formatPeakDbm(null));
		assertEquals("-6.6 dBm", SweepStatusBar.formatPeakDbm(Double.valueOf(-6.6)));
		assertEquals("-12.0 dBFS", SweepStatusBar.formatPeakDbfs(Double.valueOf(-12)));
	}

	@Test
	void setSweepInfoShowsRfOrAudioPanelMode() {
		SweepStatusBar bar = new SweepStatusBar();
		bar.setSweepInfo(100_000, 200, 80, Double.valueOf(-40));
		assertTrue(bar.getModeText().contains("RF waterfall"));
		assertTrue(bar.getPeakText().contains("dBm"));
		bar.setSweepInfo(46.875, 342, 30, Double.valueOf(-12), true);
		assertTrue(bar.getModeText().contains("AUDIO"));
		assertTrue(bar.getPeakText().contains("dBFS"));
		bar.setSweepInfo(11718.75, 1024, 30, Double.valueOf(-8), false, true);
		assertTrue(bar.getModeText().contains("VIDEO"));
		assertTrue(bar.getPeakText().contains("dBFS"));
	}

	@Test
	void mcpFieldTracksEndpointAndClients() {
		SweepStatusBar bar = new SweepStatusBar();
		assertEquals("MCP  off", bar.getMcpText());
		bar.setMcp(hotiron.core.McpStatus.listening("127.0.0.1", 8765));
		assertEquals("MCP  :8765", bar.getMcpText());
		hotiron.core.McpStatus.Client c = new hotiron.core.McpStatus.Client(
				"grok", null, "127.0.0.1:1", "spectrum_summary", 0, 0, 1);
		bar.setMcp(hotiron.core.McpStatus.listening("127.0.0.1", 8765, false,
				java.util.List.of(c), "spectrum_summary", 0L));
		assertTrue(bar.getMcpText().contains("1 client"));
	}
}
