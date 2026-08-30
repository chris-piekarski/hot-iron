package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class McpStatusTest {

	@Test
	void offTellsTheOperatorHowToStart() {
		assertTrue(McpStatus.OFF.statusHtml().contains("MCP  off"));
		assertTrue(McpStatus.OFF.statusHtml().contains("--no-mcp"));
		assertEquals("MCP  off", McpStatus.OFF.barText());
		assertTrue(McpStatus.OFF.tooltip(0L).contains("--no-mcp"));
	}

	@Test
	void listeningIdleShowsBindAndNoClients() {
		McpStatus s = McpStatus.listening("127.0.0.1", 8765);
		assertTrue(s.statusHtml().contains("127.0.0.1:8765"));
		assertTrue(s.statusHtml().contains("no clients"));
		assertEquals("MCP  :8765", s.barText());
		assertEquals(0, s.clientCount());
	}

	@Test
	void oneClientShowsTheInitializeNameAndLastTool() {
		McpStatus.Client c = new McpStatus.Client("claude-code", "1.2", "127.0.0.1:45122",
				"spectrum_summary", 1L, 2L, 4L);
		McpStatus s = McpStatus.listening("127.0.0.1", 8765, false, List.of(c), "spectrum_summary", 2L);
		assertTrue(s.statusHtml().contains("claude-code"));
		assertTrue(s.statusHtml().contains("last spectrum_summary"));
		assertEquals("MCP  :8765  ·  1 client", s.barText());
		String tip = s.tooltip(2002L);
		assertTrue(tip.contains("claude-code"));
		assertTrue(tip.contains("spectrum_summary"));
		assertTrue(tip.contains("2s ago"));
		assertTrue(tip.contains("read-only"));
	}

	@Test
	void threeClientsCollapseToACount() {
		McpStatus.Client a = new McpStatus.Client("a", null, "r1", null, 0, 0, 1);
		McpStatus.Client b = new McpStatus.Client("b", null, "r2", null, 0, 0, 1);
		McpStatus.Client c = new McpStatus.Client("c", null, "r3", null, 0, 0, 1);
		McpStatus s = McpStatus.listening("127.0.0.1", 8765, false, List.of(a, b, c), null, 0L);
		assertTrue(s.statusHtml().contains("3 clients"));
		assertFalse(s.statusHtml().contains("a  ·  b"));
		assertEquals("MCP  :8765  ·  3 clients", s.barText());
	}

	@Test
	void bindFailedShowsThePort() {
		McpStatus s = McpStatus.bindFailed("127.0.0.1", 8765, "Address already in use");
		assertTrue(s.statusHtml().contains("MCP  failed"));
		assertTrue(s.statusHtml().contains("8765"));
		assertTrue(s.statusHtml().contains("Address already in use"));
		assertEquals("MCP  failed", s.barText());
	}

	@Test
	void htmlEscapesClientNames() {
		McpStatus.Client c = new McpStatus.Client("x<y>", null, "r", null, 0, 0, 1);
		McpStatus s = McpStatus.listening("127.0.0.1", 8765, false, List.of(c), null, 0L);
		assertFalse(s.statusHtml().contains("<y>"));
		assertTrue(s.statusHtml().contains("x&lt;y&gt;"));
	}

	@Test
	void ageBuckets() {
		assertEquals("just now", McpStatus.age(1000, 1500));
		assertEquals("3s ago", McpStatus.age(1000, 4000));
		assertEquals("2m ago", McpStatus.age(1000, 121_000));
		assertEquals("", McpStatus.age(0, 121_000));
	}
}
