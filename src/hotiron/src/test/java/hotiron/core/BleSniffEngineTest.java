package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class BleSniffEngineTest
{
	@Test
	void fakePortDeliversOneAdvFrame() throws Exception
	{
		byte[] payload = new byte[18];
		payload[0] = 10;
		payload[2] = 39;
		payload[3] = 40;
		payload[10] = 0x00;
		payload[11] = 6;
		byte[] v2 = new byte[NordicSnifferProto.HEADER_LEN + payload.length];
		v2[0] = (byte) payload.length;
		v2[1] = 0;
		v2[2] = 2;
		v2[3] = 3;
		v2[5] = (byte) NordicSnifferProto.EVENT_PACKET_ADV;
		System.arraycopy(payload, 0, v2, NordicSnifferProto.HEADER_LEN, payload.length);
		byte[] slip = NordicSlip.encode(v2);
		QueuePort port = new QueuePort();
		port.inbound.offer(slip);
		List<BleFrame> got = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);
		try (BleSniffEngine engine = new BleSniffEngine(port, f -> {
			got.add(f);
			latch.countDown();
		}, s -> {
		}))
		{
			engine.start();
			assertTrue(latch.await(2, TimeUnit.SECONDS), "engine should parse the queued SLIP frame");
			assertEquals(1, got.size());
			assertEquals(39, got.get(0).channel);
			assertEquals("ADV_IND", got.get(0).name);
			assertFalse(port.writes.isEmpty(), "ping + scan_cont");
		}
	}

	private static final class QueuePort implements BleSniffEngine.Port
	{
		final BlockingQueue<byte[]> inbound = new ArrayBlockingQueue<>(8);
		final List<byte[]> writes = new ArrayList<>();
		volatile boolean closed;

		@Override
		public String path()
		{
			return "fake";
		}

		@Override
		public void configure(int baud)
		{
		}

		@Override
		public int read(byte[] buf) throws IOException
		{
			if (closed)
				return -1;
			try
			{
				byte[] next = inbound.poll(50, TimeUnit.MILLISECONDS);
				if (next == null)
					return 0;
				System.arraycopy(next, 0, buf, 0, Math.min(next.length, buf.length));
				return Math.min(next.length, buf.length);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return -1;
			}
		}

		@Override
		public void write(byte[] buf)
		{
			writes.add(buf.clone());
		}

		@Override
		public void close()
		{
			closed = true;
		}
	}
}
