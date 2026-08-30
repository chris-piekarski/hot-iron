package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import hotiron.core.McpStatus;
import net.miginfocom.swing.MigLayout;

/**
 * Rolling MCP activity under the spectrum tools. Status snapshots become
 * one line each so the operator can see bind, clients, and tool calls.
 */
public final class McpLogPane extends JPanel
{
	private static final long serialVersionUID = 1L;
	public static final int CAP = 80;
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final JTextArea area = new JTextArea();
	private final ArrayDeque<String> lines = new ArrayDeque<String>();
	private boolean wasListening;
	private String lastError;
	private int lastClients;
	private String lastTool;
	private long lastRpcMs;

	public McpLogPane()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 0, fill", "[grow,fill]", "[grow,fill]"));
		setBorder(BorderFactory.createTitledBorder("MCP log"));
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		area.setBackground(new Color(16, 16, 18));
		area.setForeground(new Color(210, 214, 200));
		area.setCaretColor(area.getForeground());
		JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		Dimension size = new Dimension(OperatorLayout.TOOLS_WIDTH - 16, OperatorLayout.MCP_LOG_HEIGHT);
		setPreferredSize(size);
		setMinimumSize(new Dimension(200, 80));
		add(scroll, "grow");
		append("waiting for MCP…");
	}

	public synchronized void apply(McpStatus status)
	{
		McpStatus s = status == null ? McpStatus.OFF : status;
		if (s.error != null && !s.error.equals(lastError))
		{
			lastError = s.error;
			append("failed  " + (s.endpoint() == null ? "" : s.endpoint() + "  ") + s.error);
		}
		if (s.error == null)
			lastError = null;
		if (s.listening && !wasListening)
			append("listening  " + s.endpoint());
		if (!s.enabled && wasListening)
			append("off  (--no-mcp)");
		wasListening = s.listening && s.enabled;
		int n = s.clientCount();
		if (n != lastClients)
		{
			if (n > lastClients)
			{
				String name = s.clients.isEmpty() ? "client" : s.clients.get(s.clients.size() - 1).displayName();
				append("client  " + name);
			}
			else if (n == 0)
				append("clients  none");
			lastClients = n;
		}
		if (s.lastTool != null && (s.lastRpcMs != lastRpcMs || !s.lastTool.equals(lastTool)))
		{
			lastTool = s.lastTool;
			lastRpcMs = s.lastRpcMs;
			append("tools/call  " + s.lastTool);
		}
	}

	public synchronized void append(String line)
	{
		if (line == null || line.isBlank())
			return;
		String stamped = CLOCK.format(LocalTime.now()) + "  " + line.trim();
		lines.addLast(stamped);
		while (lines.size() > CAP)
			lines.removeFirst();
		StringBuilder b = new StringBuilder();
		for (String l : lines)
		{
			if (b.length() > 0)
				b.append('\n');
			b.append(l);
		}
		area.setText(b.toString());
		area.setCaretPosition(area.getDocument().getLength());
	}

	public synchronized String text()
	{
		return area.getText();
	}
}
