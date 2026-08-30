package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class McpFlagsTest
{
	@Test
	void tcpIsOnByDefault()
	{
		McpFlags none = McpFlags.parse(null);
		assertTrue(none.tcp);
		assertFalse(none.stdio);
		assertEquals(SpectrumMcpServer.DEFAULT_PORT, none.port);
		McpFlags empty = McpFlags.parse(new String[0]);
		assertTrue(empty.tcp);
		assertFalse(empty.stdio);
	}

	@Test
	void noMcpDisablesTcp()
	{
		McpFlags off = McpFlags.parse(new String[] { "--no-mcp" });
		assertFalse(off.tcp);
		assertFalse(off.stdio);
		McpFlags alias = McpFlags.parse(new String[] { "--mcp-off" });
		assertFalse(alias.tcp);
	}

	@Test
	void mcpFlagIsRedundantWhenDefaultOn()
	{
		McpFlags on = McpFlags.parse(new String[] { "--mcp" });
		assertTrue(on.tcp);
		assertEquals(SpectrumMcpServer.DEFAULT_PORT, on.port);
	}

	@Test
	void lastFlagWins()
	{
		assertFalse(McpFlags.parse(new String[] { "--mcp", "--no-mcp" }).tcp);
		assertTrue(McpFlags.parse(new String[] { "--no-mcp", "--mcp" }).tcp);
		assertTrue(McpFlags.parse(new String[] { "--no-mcp", "--mcp-port=9000" }).tcp);
		assertEquals(9000, McpFlags.parse(new String[] { "--no-mcp", "--mcp-port=9000" }).port);
	}

	@Test
	void stdioIsIndependentOfTcp()
	{
		McpFlags both = McpFlags.parse(new String[] { "--mcp-stdio" });
		assertTrue(both.tcp);
		assertTrue(both.stdio);
		McpFlags stdioOnly = McpFlags.parse(new String[] { "--no-mcp", "--mcp-stdio" });
		assertFalse(stdioOnly.tcp);
		assertTrue(stdioOnly.stdio);
	}
}
