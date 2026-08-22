package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import hotiron.core.McpStatus;

class SpectrumMcpServerTest {

	@Test
	void tcpInitializeShowsTheClientThenDropsOnClose() throws Exception {
		SpectrumMcpServer server = new SpectrumMcpServer(new SpectrumSnapshotStore());
		AtomicReference<McpStatus> last = new AtomicReference<McpStatus>();
		CountDownLatch listening = new CountDownLatch(1);
		CountDownLatch named = new CountDownLatch(1);
		CountDownLatch gone = new CountDownLatch(1);
		server.addStatusListener(s -> {
			last.set(s);
			if (s.listening && s.port > 0)
				listening.countDown();
			if (s.clientCount() == 1 && s.clients.get(0).name != null
					&& s.clients.get(0).name.equals("test-agent"))
				named.countDown();
			if (s.listening && s.clientCount() == 0 && named.getCount() == 0)
				gone.countDown();
		});
		server.startLocalhost(0);
		assertTrue(listening.await(2, TimeUnit.SECONDS), "server should publish listening");
		int port = server.boundPort();
		assertTrue(port > 0);
		assertEquals(0, server.status().clientCount());
		try (Socket sock = new Socket("127.0.0.1", port))
		{
			BufferedWriter w = new BufferedWriter(
					new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
			w.write("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
					+ "{\"clientInfo\":{\"name\":\"test-agent\",\"version\":\"9\"}}}\n");
			w.flush();
			BufferedReader r = new BufferedReader(
					new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
			assertNotNull(r.readLine());
			assertTrue(named.await(2, TimeUnit.SECONDS), "initialize should name the client");
			McpStatus s = last.get();
			assertEquals(1, s.clientCount());
			assertEquals("test-agent", s.clients.get(0).name);
			assertEquals("9", s.clients.get(0).version);
			assertTrue(s.statusHtml().contains("test-agent"));
			w.write("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":"
					+ "{\"name\":\"radio_identity\"}}\n");
			w.flush();
			assertNotNull(r.readLine());
		}
		assertTrue(gone.await(2, TimeUnit.SECONDS), "closing the socket should drop the client");
		assertEquals(0, server.status().clientCount());
		server.stop();
	}

	@Test
	void defaultStatusIsOffUntilListen() {
		SpectrumMcpServer server = new SpectrumMcpServer(new SpectrumSnapshotStore());
		assertEquals(McpStatus.OFF, server.status());
		assertFalse(server.status().enabled);
	}
}
