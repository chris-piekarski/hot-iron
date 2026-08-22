package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Operator-facing MCP endpoint state. No sockets, no JSON-RPC.
 */
public final class McpStatus
{
	public static final McpStatus OFF = new McpStatus(false, false, false, null, 0, null,
			List.of(), null, 0L);

	public final boolean enabled;
	public final boolean listening;
	public final boolean stdio;
	public final String bindHost;
	public final int port;
	public final String error;
	public final List<Client> clients;
	public final String lastTool;
	public final long lastRpcMs;

	public McpStatus(boolean enabled, boolean listening, boolean stdio, String bindHost, int port,
			String error, List<Client> clients, String lastTool, long lastRpcMs)
	{
		this.enabled = enabled;
		this.listening = listening;
		this.stdio = stdio;
		this.bindHost = emptyToNull(bindHost);
		this.port = port;
		this.error = emptyToNull(error);
		this.clients = clients == null || clients.isEmpty() ? List.of()
				: Collections.unmodifiableList(new ArrayList<Client>(clients));
		this.lastTool = emptyToNull(lastTool);
		this.lastRpcMs = lastRpcMs;
	}

	public static McpStatus listening(String host, int port)
	{
		return listening(host, port, false, List.of(), null, 0L);
	}

	public static McpStatus listening(String host, int port, boolean stdio, List<Client> clients,
			String lastTool, long lastRpcMs)
	{
		return new McpStatus(true, true, stdio, host, port, null, clients, lastTool, lastRpcMs);
	}

	public static McpStatus stdioOnly(List<Client> clients, String lastTool, long lastRpcMs)
	{
		return new McpStatus(true, false, true, null, 0, null, clients, lastTool, lastRpcMs);
	}

	public static McpStatus bindFailed(String host, int port, String message)
	{
		String err = message == null || message.isBlank() ? "could not bind" : message.trim();
		return new McpStatus(true, false, false, host, port, err, List.of(), null, 0L);
	}

	public int clientCount()
	{
		return clients.size();
	}

	public String endpoint()
	{
		if (bindHost != null && port > 0)
			return bindHost + ":" + port;
		if (stdio)
			return "stdio";
		return null;
	}

	public String statusHtml()
	{
		if (error != null)
			return "<html><b>MCP  failed</b><br><span style='color:#9a9a9a'>" + escape(errorLine())
					+ "</span></html>";
		if (!enabled)
			return "<html>MCP  off<br><span style='color:#9a9a9a'>Start with make mcp</span></html>";
		String title = endpoint() == null ? "MCP" : "MCP  " + endpoint();
		String secondary = secondaryLine();
		if (lastTool == null)
			return "<html><b>" + escape(title) + "</b><br><span style='color:#9a9a9a'>"
					+ escape(secondary) + "</span></html>";
		return "<html><b>" + escape(title) + "</b><br>" + escape(secondary)
				+ "<br><span style='color:#9a9a9a'>last " + escape(lastTool) + "</span></html>";
	}

	public String tooltip(long nowMs)
	{
		if (!enabled)
			return "MCP is off. Launch with make mcp or the launcher --mcp (127.0.0.1:8765).";
		StringBuilder tip = new StringBuilder();
		if (error != null)
		{
			tip.append("MCP failed to listen");
			if (endpoint() != null)
				tip.append(" on ").append(endpoint());
			tip.append("\n").append(error);
			return tip.toString();
		}
		tip.append("MCP  ");
		if (endpoint() != null)
			tip.append(endpoint());
		else
			tip.append("stdio");
		tip.append("\nProtocol  2024-11-05  ·  read-only v1");
		if (stdio && listening)
			tip.append("\nTransports  TCP + stdio");
		else if (stdio)
			tip.append("\nTransport  stdio");
		else
			tip.append("\nTransport  TCP localhost");
		if (clients.isEmpty())
			tip.append("\nNo clients connected");
		else
		{
			tip.append("\nClients  ").append(clients.size());
			for (Client c : clients)
			{
				tip.append("\n  ").append(c.displayName());
				if (c.version != null)
					tip.append(" ").append(c.version);
				if (c.remote != null && !c.remote.equals(c.displayName()))
					tip.append("  ").append(c.remote);
				if (c.lastCall != null)
					tip.append("\n    last  ").append(c.lastCall).append("  ")
							.append(age(c.lastSeenMs, nowMs));
				if (c.requests > 0)
					tip.append("  ·  ").append(c.requests).append(c.requests == 1 ? " call" : " calls");
			}
		}
		if (lastTool != null)
			tip.append("\nLast tool  ").append(lastTool).append("  ").append(age(lastRpcMs, nowMs));
		return tip.toString();
	}

	public String barText()
	{
		if (error != null)
			return "MCP  failed";
		if (!enabled)
			return "MCP  off";
		String ep = port > 0 ? ":" + port : "stdio";
		if (clients.isEmpty())
			return "MCP  " + ep;
		int n = clients.size();
		return "MCP  " + ep + "  ·  " + n + (n == 1 ? " client" : " clients");
	}

	public static String age(long thenMs, long nowMs)
	{
		if (thenMs <= 0)
			return "";
		long d = Math.max(0L, nowMs - thenMs);
		if (d < 1000)
			return "just now";
		if (d < 60_000)
			return (d / 1000) + "s ago";
		return (d / 60_000) + "m ago";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof McpStatus))
			return false;
		McpStatus o = (McpStatus) obj;
		return enabled == o.enabled && listening == o.listening && stdio == o.stdio && port == o.port
				&& lastRpcMs == o.lastRpcMs && Objects.equals(bindHost, o.bindHost)
				&& Objects.equals(error, o.error) && Objects.equals(lastTool, o.lastTool)
				&& clients.equals(o.clients);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(Boolean.valueOf(enabled), Boolean.valueOf(listening), Boolean.valueOf(stdio),
				bindHost, Integer.valueOf(port), error, clients, lastTool, Long.valueOf(lastRpcMs));
	}

	private String secondaryLine()
	{
		if (clients.isEmpty())
			return "idle · no clients · read-only";
		if (clients.size() <= 2)
		{
			StringBuilder b = new StringBuilder();
			for (Client c : clients)
			{
				if (b.length() > 0)
					b.append("  ·  ");
				b.append(c.displayName());
			}
			return b.toString();
		}
		return clients.size() + " clients";
	}

	private String errorLine()
	{
		if (port > 0)
			return (bindHost == null ? "port " : bindHost + ":") + port + "  " + error;
		return error;
	}

	private static String emptyToNull(String s)
	{
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static final class Client
	{
		public final String name;
		public final String version;
		public final String remote;
		public final String lastCall;
		public final long connectedAtMs;
		public final long lastSeenMs;
		public final long requests;

		public Client(String name, String version, String remote, String lastCall, long connectedAtMs,
				long lastSeenMs, long requests)
		{
			this.name = emptyToNull(name);
			this.version = emptyToNull(version);
			this.remote = emptyToNull(remote);
			this.lastCall = emptyToNull(lastCall);
			this.connectedAtMs = connectedAtMs;
			this.lastSeenMs = lastSeenMs;
			this.requests = requests;
		}

		public String displayName()
		{
			if (name != null)
				return name;
			if (remote != null)
				return remote;
			return "client";
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (!(obj instanceof Client))
				return false;
			Client o = (Client) obj;
			return requests == o.requests && lastSeenMs == o.lastSeenMs
					&& connectedAtMs == o.connectedAtMs && Objects.equals(name, o.name)
					&& Objects.equals(version, o.version) && Objects.equals(remote, o.remote)
					&& Objects.equals(lastCall, o.lastCall);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, version, remote, lastCall, Long.valueOf(connectedAtMs),
					Long.valueOf(lastSeenMs), Long.valueOf(requests));
		}
	}
}
