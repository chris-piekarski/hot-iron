package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.core.McpStatus;

class McpLogPaneTest
{
	@Test
	void logsBindAndToolCalls()
	{
		McpLogPane pane = new McpLogPane();
		pane.apply(McpStatus.listening("127.0.0.1", 8765));
		assertTrue(pane.text().contains("listening"));
		assertTrue(pane.text().contains("8765"));
		McpStatus.Client c = new McpStatus.Client("claude-code", "1", "127.0.0.1:9", "spectrum_summary", 1, 2, 1);
		pane.apply(McpStatus.listening("127.0.0.1", 8765, false, java.util.List.of(c), "spectrum_summary", 2L));
		assertTrue(pane.text().contains("claude-code"));
		assertTrue(pane.text().contains("spectrum_summary"));
	}
}
