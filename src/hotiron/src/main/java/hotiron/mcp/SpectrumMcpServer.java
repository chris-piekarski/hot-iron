package hotiron.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import hotiron.core.McpStatus;
import hotiron.core.McpStatus.Client;

/**
 * MCP over stdio (Content-Length or one JSON object per line) or a
 * localhost TCP accept loop. I/O never runs on the Swing EDT.
 */
public final class SpectrumMcpServer
{
	public static final int DEFAULT_PORT = 8765;
	public static final String BIND_HOST = "127.0.0.1";

	private final SpectrumMcpTools tools;
	private final Object lock = new Object();
	private final List<Session> sessions = new ArrayList<Session>();
	private final List<Consumer<McpStatus>> listeners = new ArrayList<Consumer<McpStatus>>();
	private volatile boolean stop;
	private volatile ServerSocket server;
	private volatile boolean tcpEnabled;
	private volatile boolean stdioEnabled;
	private volatile String bindHost;
	private volatile int bindPort;

	public SpectrumMcpServer(SpectrumSnapshotStore store)
	{
		this(store, null, null, null);
	}

	public SpectrumMcpServer(SpectrumSnapshotStore store, SpectrumMcpTools.TvWatchHook tvWatch,
			SpectrumMcpTools.FmListenHook fmListen)
	{
		this(store, tvWatch, fmListen, null);
	}

	public SpectrumMcpServer(SpectrumSnapshotStore store, SpectrumMcpTools.TvWatchHook tvWatch,
			SpectrumMcpTools.FmListenHook fmListen, SpectrumMcpTools.NfcSniffHook nfcSniff)
	{
		this(store, tvWatch, fmListen, nfcSniff, null, null);
	}

	public SpectrumMcpServer(SpectrumSnapshotStore store, SpectrumMcpTools.TvWatchHook tvWatch,
			SpectrumMcpTools.FmListenHook fmListen, SpectrumMcpTools.NfcSniffHook nfcSniff,
			SpectrumMcpTools.AutoGainHook autoGain, SpectrumMcpTools.SweepHook sweep)
	{
		this.tools = new SpectrumMcpTools(store, tvWatch, fmListen, nfcSniff, autoGain, sweep);
	}

	public SpectrumMcpTools tools()
	{
		return tools;
	}

	public String handle(String requestJson)
	{
		return tools.handleRpc(requestJson);
	}

	public void addStatusListener(Consumer<McpStatus> listener)
	{
		if (listener == null)
			return;
		synchronized (lock)
		{
			listeners.add(listener);
		}
	}

	public int boundPort()
	{
		return bindPort;
	}

	public McpStatus status()
	{
		synchronized (lock)
		{
			return snapshotLocked();
		}
	}

	public void stop()
	{
		stop = true;
		ServerSocket s = server;
		if (s != null)
		{
			try
			{
				s.close();
			}
			catch (IOException ignored)
			{
			}
		}
		tcpEnabled = false;
		stdioEnabled = false;
		fireStatus();
	}

	public void runStdio() throws IOException
	{
		stdioEnabled = true;
		fireStatus();
		runStreams(System.in, System.out, openSession("stdio"));
	}

	public void runStreams(InputStream in, OutputStream out) throws IOException
	{
		stdioEnabled = true;
		fireStatus();
		runStreams(in, out, openSession("stdio"));
	}

	public Thread startLocalhost(int port) throws IOException
	{
		ServerSocket ss = new ServerSocket(port, 8, InetAddress.getByName(BIND_HOST));
		server = ss;
		bindHost = BIND_HOST;
		bindPort = ss.getLocalPort();
		tcpEnabled = true;
		fireStatus();
		Thread t = new Thread(() -> {
			while (!stop)
			{
				try
				{
					Socket sock = ss.accept();
					String remote = remoteOf(sock);
					Session session = openSession(remote);
					Thread client = new Thread(() -> {
						try
						{
							runStreams(sock.getInputStream(), sock.getOutputStream(), session);
						}
						catch (IOException ignored)
						{
						}
						finally
						{
							try
							{
								sock.close();
							}
							catch (IOException ignored)
							{
							}
						}
					}, "spectrum-mcp-client");
					client.setDaemon(true);
					client.start();
				}
				catch (IOException e)
				{
					if (!stop)
						e.printStackTrace();
				}
			}
		}, "spectrum-mcp-listen");
		t.setDaemon(true);
		t.start();
		System.err.println("HotIron MCP QRV on " + BIND_HOST + ":" + bindPort);
		return t;
	}

	static String readMessage(BufferedReader reader) throws IOException
	{
		String first = reader.readLine();
		if (first == null)
			return null;
		if (first.isEmpty())
			return readMessage(reader);
		if (first.startsWith("{"))
			return first;
		int contentLength = -1;
		String line = first;
		while (line != null && !line.isEmpty())
		{
			int colon = line.indexOf(':');
			if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length"))
			{
				try
				{
					contentLength = Integer.parseInt(line.substring(colon + 1).trim());
				}
				catch (NumberFormatException e)
				{
					contentLength = -1;
				}
			}
			line = reader.readLine();
		}
		if (contentLength < 0)
			return first.startsWith("{") ? first : "";
		char[] buf = new char[contentLength];
		int n = 0;
		while (n < contentLength)
		{
			int r = reader.read(buf, n, contentLength - n);
			if (r < 0)
				break;
			n += r;
		}
		return new String(buf, 0, n);
	}

	static void writeMessage(BufferedWriter writer, String json) throws IOException
	{
		byte[] raw = json.getBytes(StandardCharsets.UTF_8);
		writer.write("Content-Length: ");
		writer.write(Integer.toString(raw.length));
		writer.write("\r\n\r\n");
		writer.write(json);
		writer.flush();
	}

	private void runStreams(InputStream in, OutputStream out, Session session) throws IOException
	{
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
			while (!stop)
			{
				String msg = readMessage(reader);
				if (msg == null)
					return;
				if (msg.isEmpty())
					continue;
				session.note(msg);
				fireStatus();
				String reply = handle(msg);
				if (reply != null)
					writeMessage(writer, reply);
			}
		}
		finally
		{
			closeSession(session);
		}
	}

	private Session openSession(String remote)
	{
		Session session = new Session(remote);
		synchronized (lock)
		{
			sessions.add(session);
		}
		fireStatus();
		return session;
	}

	private void closeSession(Session session)
	{
		synchronized (lock)
		{
			sessions.remove(session);
		}
		fireStatus();
	}

	private void fireStatus()
	{
		McpStatus snapshot;
		List<Consumer<McpStatus>> ls;
		synchronized (lock)
		{
			snapshot = snapshotLocked();
			ls = new ArrayList<Consumer<McpStatus>>(listeners);
		}
		for (Consumer<McpStatus> listener : ls)
			listener.accept(snapshot);
	}

	private McpStatus snapshotLocked()
	{
		List<Client> clients = new ArrayList<Client>(sessions.size());
		String lastTool = null;
		long lastRpc = 0L;
		for (Session s : sessions)
		{
			clients.add(s.toClient());
			if (s.lastTool != null && s.lastSeenMs >= lastRpc)
			{
				lastRpc = s.lastSeenMs;
				lastTool = s.lastTool;
			}
		}
		if (!tcpEnabled && !stdioEnabled && sessions.isEmpty())
			return McpStatus.OFF;
		if (tcpEnabled)
			return McpStatus.listening(bindHost, bindPort, stdioEnabled, clients, lastTool, lastRpc);
		return McpStatus.stdioOnly(clients, lastTool, lastRpc);
	}

	private static String remoteOf(Socket sock)
	{
		if (sock == null)
			return "tcp";
		InetAddress addr = sock.getInetAddress();
		String host = addr == null ? BIND_HOST : addr.getHostAddress();
		return host + ":" + sock.getPort();
	}

	private static final class Session
	{
		final String remote;
		final long connectedAtMs = System.currentTimeMillis();
		volatile String clientName;
		volatile String clientVersion;
		volatile String lastMethod;
		volatile String lastTool;
		volatile long lastSeenMs;
		volatile long requestCount;

		Session(String remote)
		{
			this.remote = remote;
			this.lastSeenMs = connectedAtMs;
		}

		void note(String json)
		{
			lastSeenMs = System.currentTimeMillis();
			requestCount++;
			try
			{
				Map<String, Object> req = McpJson.parseObject(json);
				String method = McpJson.getString(req, "method");
				lastMethod = method;
				if ("initialize".equals(method))
				{
					Map<String, Object> params = McpJson.getObject(req, "params");
					Map<String, Object> info = McpJson.getObject(params, "clientInfo");
					String n = McpJson.getString(info, "name");
					if (n != null && !n.isBlank())
						clientName = n.trim();
					String v = McpJson.getString(info, "version");
					if (v != null && !v.isBlank())
						clientVersion = v.trim();
				}
				if ("tools/call".equals(method))
				{
					Map<String, Object> params = McpJson.getObject(req, "params");
					String tool = McpJson.getString(params, "name");
					if (tool != null && !tool.isBlank())
						lastTool = tool.trim();
				}
			}
			catch (RuntimeException ignored)
			{
			}
		}

		Client toClient()
		{
			String call = lastTool != null ? lastTool : lastMethod;
			return new Client(clientName, clientVersion, remote, call, connectedAtMs, lastSeenMs,
					requestCount);
		}
	}
}
