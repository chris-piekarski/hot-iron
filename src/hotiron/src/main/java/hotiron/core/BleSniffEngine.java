package hotiron.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Second USB: Nordic nRF Sniffer on ACM. Does not touch the HackRF.
 */
public final class BleSniffEngine implements AutoCloseable
{
	public static final int BAUD = 1_000_000;
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
			port.configure(BAUD);
			status.accept("ping " + port.path());
			port.write(NordicSlip.encode(NordicSnifferProto.pingReq(txCounter++)));
			port.write(NordicSlip.encode(NordicSnifferProto.scanCont(txCounter++)));
			status.accept("scan " + port.path());
			NordicSlip.Decoder slip = new NordicSlip.Decoder();
			byte[] buf = new byte[1024];
			int idle = 0;
			while (!stop)
			{
				int n = port.read(buf);
				if (n < 0)
					break;
				if (n == 0)
				{
					idle++;
					if (idle == 20)
						status.accept("waiting for Nordic sniffer firmware on " + port.path());
					continue;
				}
				idle = 0;
				List<byte[]> got = slip.push(buf, 0, n);
				for (byte[] raw : got)
				{
					NordicSnifferProto.HostHeader h = NordicSnifferProto.parseHost(raw);
					if (h != null && h.type == NordicSnifferProto.PING_RESP)
						status.accept("firmware " + h.version);
					BleFrame frame = NordicSnifferProto.toFrame(raw, System.currentTimeMillis());
					if (frame != null)
						frames.accept(frame);
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
		private final String path;
		private FileInputStream in;
		private FileOutputStream out;

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
			try
			{
				Process stty = new ProcessBuilder("stty", "-F", path, String.valueOf(baud), "cs8", "-cstopb",
						"-parenb", "raw", "-echo", "min", "0", "time", "1").start();
				int rc = stty.waitFor();
				if (rc != 0)
					throw new IOException("stty " + path + " exited " + rc);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IOException("stty interrupted", e);
			}
			in = new FileInputStream(path);
			out = new FileOutputStream(path);
		}

		@Override
		public int read(byte[] buf) throws IOException
		{
			if (in == null)
				return -1;
			return in.read(buf);
		}

		@Override
		public void write(byte[] buf) throws IOException
		{
			if (out == null)
				return;
			out.write(buf);
			out.flush();
		}

		@Override
		public void close() throws IOException
		{
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}
}
