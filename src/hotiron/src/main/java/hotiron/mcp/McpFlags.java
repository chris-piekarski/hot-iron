package hotiron.mcp;

/**
 * CLI for the in-process MCP server. TCP on 127.0.0.1:{@link SpectrumMcpServer#DEFAULT_PORT}
 * is on unless {@code --no-mcp}. {@code --mcp} is accepted and is the default.
 */
public final class McpFlags
{
	public final boolean tcp;
	public final boolean stdio;
	public final int port;

	public McpFlags(boolean tcp, boolean stdio, int port)
	{
		this.tcp = tcp;
		this.stdio = stdio;
		this.port = port > 0 ? port : SpectrumMcpServer.DEFAULT_PORT;
	}

	public static McpFlags parse(String[] args)
	{
		boolean tcp = true;
		boolean stdio = false;
		int port = SpectrumMcpServer.DEFAULT_PORT;
		if (args == null)
			return new McpFlags(tcp, stdio, port);
		for (int i = 0; i < args.length; i++)
		{
			String a = args[i];
			if (a == null)
				continue;
			if ("--no-mcp".equals(a) || "--mcp-off".equals(a))
				tcp = false;
			else if ("--mcp".equals(a) || "mcp".equals(a))
				tcp = true;
			else if ("--mcp-stdio".equals(a))
				stdio = true;
			else if (a.startsWith("--mcp-port="))
			{
				port = Integer.parseInt(a.substring("--mcp-port=".length()));
				tcp = true;
			}
		}
		return new McpFlags(tcp, stdio, port);
	}
}
