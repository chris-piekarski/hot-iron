package hotiron.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Second USB: Nordic nRF Sniffer on ACM. Does not touch the HackRF.
 */
public final class BleSniffEngine implements AutoCloseable
{
	public static final int BAUD = 460_800;
	/** nRF51 HEX on this bench is 460800; nRF52 4.x is 1 Mbps. Wrong baud on J-Link VCOM garbles the bridge until the port is closed. */
	public static final int[] BAUDS = { 460_800, 1_000_000 };
	public static final String DEFAULT_PORT = "/dev/ttyACM0";

	public interface Port extends AutoCloseable
	{
		String path();

		void configure(int baud) throws IOException;

		int read(byte[] buf) throws IOException;

		void write(byte[] buf) throws IOException;

		@Override
		void close() throws IOException;
	}

	private final Port port;
	private final Consumer<BleFrame> frames;
	private final Consumer<String> status;
	private final Object lock = new Object();
	private Thread worker;
	private volatile boolean stop;
	private int txCounter;

	public BleSniffEngine(Port port, Consumer<BleFrame> frames, Consumer<String> status)
	{
		if (port == null)
			throw new IllegalArgumentException("port");
		this.port = port;
		this.frames = frames == null ? f -> {
		} : frames;
		this.status = status == null ? s -> {
		} : status;
	}

	public static String discoverPort()
	{
		String env = System.getenv("HOTIRON_BLE_PORT");
		if (env != null && !env.isBlank() && new File(env.trim()).exists())
			return env.trim();
		try
		{
			Path byId = Path.of("/dev/serial/by-id");
			if (Files.isDirectory(byId))
			{
				try (var stream = Files.list(byId))
				{
					var hit = stream.filter(p -> p.getFileName().toString().contains("SEGGER_J-Link")).findFirst();
					if (hit.isPresent())
						return hit.get().toString();
				}
			}
		}
		catch (IOException ignored)
		{
		}
		if (new File(DEFAULT_PORT).exists())
			return DEFAULT_PORT;
		return null;
	}

	public static Port openLinux(String path) throws IOException
	{
		return new LinuxAcmPort(path);
	}

	public void start()
	{
		synchronized (lock)
		{
			if (worker != null && worker.isAlive())
				return;
			stop = false;
			worker = new Thread(this::run, "ble-sniff");
			worker.setDaemon(true);
			worker.start();
		}
	}

	private void run()
	{
		try
		{
			NordicSlip.Decoder slip = new NordicSlip.Decoder();
			byte[] buf = new byte[1024];
			boolean talking = false;
			int baudUsed = BAUD;
			for (int baud : BAUDS)
			{
				if (stop)
					return;
				port.configure(baud);
				status.accept("ping " + baud + " " + port.path());
				writeHost();
				long until = System.currentTimeMillis() + 800;
				while (!stop && System.currentTimeMillis() < until)
				{
					int n = port.read(buf);
					if (n < 0)
						return;
					if (n == 0)
					{
						try
						{
							Thread.sleep(20);
						}
						catch (InterruptedException e)
						{
							Thread.currentThread().interrupt();
							return;
						}
						continue;
					}
					if (consume(slip, buf, n))
						talking = true;
				}
				if (talking)
				{
					baudUsed = baud;
					status.accept("scan " + baud + " " + port.path());
					break;
				}
			}
			if (!talking)
				status.accept("waiting for Nordic sniffer firmware on " + port.path() + " (tried 1M and 460800)");
			int idle = 0;
			while (!stop)
			{
				int n = port.read(buf);
				if (n < 0)
					break;
				if (n == 0)
				{
					idle++;
					if (!talking && idle == 20)
						status.accept("waiting for Nordic sniffer firmware on " + port.path());
					try
					{
						Thread.sleep(20);
					}
					catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
						break;
					}
					continue;
				}
				idle = 0;
				if (consume(slip, buf, n) && !talking)
				{
					talking = true;
					status.accept("scan " + baudUsed + " " + port.path());
				}
			}
		}
		catch (IOException e)
		{
			status.accept(e.getMessage() == null ? "port error" : e.getMessage());
		}
		finally
		{
			try
			{
				port.close();
			}
			catch (IOException ignored)
			{
			}
			status.accept("idle");
		}
	}

	private void writeHost() throws IOException
	{
		port.write(NordicSlip.encode(NordicSnifferProto.pingReq(txCounter++)));
		port.write(NordicSlip.encode(NordicSnifferProto.advHop(txCounter++)));
		port.write(NordicSlip.encode(NordicSnifferProto.scanCont(txCounter++)));
	}

	private boolean consume(NordicSlip.Decoder slip, byte[] buf, int n)
	{
		boolean any = false;
		List<byte[]> got = slip.push(buf, 0, n);
		for (byte[] raw : got)
		{
			NordicSnifferProto.HostHeader h = NordicSnifferProto.parseHost(raw);
			if (h != null)
			{
				any = true;
				if (h.type == NordicSnifferProto.PING_RESP)
					status.accept("firmware proto " + h.version);
			}
			BleFrame frame = NordicSnifferProto.toFrame(raw, System.currentTimeMillis());
			if (frame != null)
				frames.accept(frame);
		}
		return any;
	}

	@Override
	public void close()
	{
		stop = true;
		try
		{
			port.close();
		}
		catch (IOException ignored)
		{
		}
		Thread t;
		synchronized (lock)
		{
			t = worker;
			worker = null;
		}
		if (t != null)
		{
			t.interrupt();
			try
			{
				t.join(500);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	static final class LinuxAcmPort implements Port
	{
		private static final int O_RDWR = 2;
		private static final int O_NOCTTY = 256;
		private static final int O_NONBLOCK = 2048;
		private static final int EAGAIN = 11;
		private static final int EWOULDBLOCK = 11;

		interface LibC extends Library
		{
			LibC INSTANCE = Native.load("c", LibC.class);

			int open(String path, int flags);

			long read(int fd, byte[] buf, long len);

			long write(int fd, byte[] buf, long len);

			int close(int fd);

			int ioctl(int fd, long request, int[] argp);
		}

		private final String path;
		private int fd = -1;

		LinuxAcmPort(String path)
		{
			this.path = path;
		}

		@Override
		public String path()
		{
			return path;
		}

		@Override
		public void configure(int baud) throws IOException
		{
			closeFd();
			stty(baud);
			int nfd = LibC.INSTANCE.open(path, O_RDWR | O_NOCTTY | O_NONBLOCK);
			if (nfd < 0)
				throw new IOException("open " + path + " errno " + Native.getLastError());
			fd = nfd;
			// pyserial rtscts=True raises RTS/DTR; J-Link VCOM needs that
			int[] bits = { 0x002 | 0x004 };
			LibC.INSTANCE.ioctl(fd, 0x541CL, bits);
		}

		private void stty(int baud) throws IOException
		{
			try
			{
				Process stty = new ProcessBuilder("timeout", "2", "stty", "-F", path, String.valueOf(baud), "cs8",
						"-cstopb", "-parenb", "crtscts", "raw", "-echo", "clocal", "min", "0", "time", "1").start();
				int rc = stty.waitFor();
				if (rc != 0)
				{
					stty = new ProcessBuilder("timeout", "2", "stty", "-F", path, String.valueOf(baud), "cs8",
							"-cstopb", "-parenb", "raw", "-echo", "clocal", "min", "0", "time", "1").start();
					rc = stty.waitFor();
					if (rc != 0)
						throw new IOException("stty " + path + " " + baud + " exited " + rc);
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IOException("stty interrupted", e);
			}
		}

		@Override
		public int read(byte[] buf) throws IOException
		{
			if (fd < 0)
				return -1;
			long n = LibC.INSTANCE.read(fd, buf, buf.length);
			if (n < 0)
			{
				int err = Native.getLastError();
				if (err == EAGAIN || err == EWOULDBLOCK)
					return 0;
				throw new IOException("read " + path + " errno " + err);
			}
			return (int) n;
		}

		@Override
		public void write(byte[] buf) throws IOException
		{
			if (fd < 0 || buf == null || buf.length == 0)
				return;
			int off = 0;
			while (off < buf.length)
			{
				long n = LibC.INSTANCE.write(fd, slice(buf, off), (long) (buf.length - off));
				if (n < 0)
				{
					int err = Native.getLastError();
					if (err == EAGAIN || err == EWOULDBLOCK)
					{
						try
						{
							Thread.sleep(5);
						}
						catch (InterruptedException e)
						{
							Thread.currentThread().interrupt();
							throw new IOException("write interrupted", e);
						}
						continue;
					}
					throw new IOException("write " + path + " errno " + err);
				}
				if (n == 0)
					break;
				off += n;
			}
		}

		private static byte[] slice(byte[] buf, int off)
		{
			if (off == 0)
				return buf;
			byte[] rest = new byte[buf.length - off];
			System.arraycopy(buf, off, rest, 0, rest.length);
			return rest;
		}

		@Override
		public void close() throws IOException
		{
			closeFd();
		}

		private void closeFd() throws IOException
		{
			if (fd < 0)
				return;
			int n = LibC.INSTANCE.close(fd);
			fd = -1;
			if (n < 0)
				throw new IOException("close " + path + " errno " + Native.getLastError());
		}
	}
}
