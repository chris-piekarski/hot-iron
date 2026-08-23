package hotiron.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Decode MPEG-TS (ATSC) to RGB frames + 48 kHz PCM via the host ffmpeg.
 */
public final class MpegTsPlayer
{
	public static final int WIDTH = 640;
	public static final int HEIGHT = 360;
	public static final int FRAME_BYTES = WIDTH * HEIGHT * 3;
	public static final int TS_QUEUE_CAP = 128;
	static final int STDERR_TAIL = 3;
	static final int STDERR_LINE_CAP = 240;
	static final String PROBE_SIZE = "1000000";
	static final String ANALYZE_DURATION = "2000000";

	private final ArrayBlockingQueue<byte[]> tsQ = new ArrayBlockingQueue<>(TS_QUEUE_CAP);
	private final MpegTsProbe probe = new MpegTsProbe();
	private final ArrayDeque<String> stderrTail = new ArrayDeque<>(STDERR_TAIL + 1);
	private Process video;
	private Process audio;
	private OutputStream videoIn;
	private OutputStream audioIn;
	private Thread videoThread;
	private Thread audioThread;
	private Thread writer;
	private volatile boolean run;
	private volatile boolean started;
	private volatile boolean launchFailed;
	private volatile long startedMs;
	private Consumer<BufferedImage> onFrame;
	private Consumer<short[]> onPcm;
	private final AtomicInteger frames = new AtomicInteger();
	private final AtomicLong tsBytesOffered = new AtomicLong();
	private final AtomicLong tsBytesWritten = new AtomicLong();
	private final AtomicLong tsDropped = new AtomicLong();
	private final AtomicLong writeErrors = new AtomicLong();
	private final AtomicLong stdoutBytes = new AtomicLong();
	private final AtomicInteger partialFrameBytes = new AtomicInteger();
	private final AtomicBoolean stdoutEof = new AtomicBoolean();

	public void start(Consumer<BufferedImage> onFrame, Consumer<short[]> onPcm)
	{
		start(onFrame, onPcm, -1, -1);
	}

	public void start(Consumer<BufferedImage> onFrame, Consumer<short[]> onPcm, int videoPid,
			int audioPid)
	{
		stop();
		this.onFrame = onFrame;
		this.onPcm = onPcm;
		resetStats();
		run = true;
		started = true;
		startedMs = System.currentTimeMillis();
		launch(videoPid, audioPid);
		writer = new Thread(this::writeLoop, "atsc-ffmpeg-write");
		writer.setDaemon(true);
		writer.start();
	}

	public void stop()
	{
		run = false;
		started = false;
		Thread w = writer;
		writer = null;
		if (w != null)
			w.interrupt();
		/*
		 * Break a writer blocked in a full OS pipe before closing its
		 * OutputStream. Closing first can wait forever for the write lock.
		 */
		if (video != null)
		{
			video.destroyForcibly();
			try
			{
				video.waitFor(2, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
		if (audio != null)
		{
			audio.destroyForcibly();
			try
			{
				audio.waitFor(2, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
		closeQuiet(videoIn);
		closeQuiet(audioIn);
		videoIn = null;
		audioIn = null;
		video = null;
		audio = null;
		tsQ.clear();
		if (w != null)
		{
			try
			{
				w.join(400);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	public boolean running()
	{
		return run && video != null && video.isAlive();
	}

	public int frames()
	{
		return frames.get();
	}

	public Stats stats()
	{
		Process v = video;
		Process a = audio;
		boolean videoAlive = v != null && v.isAlive();
		boolean audioAlive = a != null && a.isAlive();
		int videoExit = Stats.EXIT_NONE;
		if (v != null && !videoAlive)
		{
			try
			{
				videoExit = v.exitValue();
			}
			catch (IllegalThreadStateException ignored)
			{
				videoAlive = true;
			}
		}
		long start = startedMs;
		long wait = started && start > 0 ? Math.max(0, System.currentTimeMillis() - start) : 0;
		return new Stats(started, launchFailed, videoAlive, audioAlive, videoExit, start, wait,
				tsBytesOffered.get(), tsBytesWritten.get(), tsQ.size(), TS_QUEUE_CAP,
				tsDropped.get(), writeErrors.get(), stdoutBytes.get(), partialFrameBytes.get(),
				stdoutEof.get(), lastStderr(), probe.snapshot());
	}

	public void writeTs(byte[] ts, int nbytes)
	{
		if (!run || ts == null || nbytes < 188)
			return;
		int n = nbytes - (nbytes % 188);
		byte[] copy = Arrays.copyOf(ts, n);
		probe.accept(copy, n);
		tsBytesOffered.addAndGet(n);
		if (!tsQ.offer(copy))
			tsDropped.incrementAndGet();
	}

	private void resetStats()
	{
		frames.set(0);
		tsBytesOffered.set(0);
		tsBytesWritten.set(0);
		tsDropped.set(0);
		writeErrors.set(0);
		stdoutBytes.set(0);
		partialFrameBytes.set(0);
		stdoutEof.set(false);
		launchFailed = false;
		probe.reset();
		synchronized (stderrTail)
		{
			stderrTail.clear();
		}
		tsQ.clear();
	}

	private void writeLoop()
	{
		while (run)
		{
			byte[] pkt;
			try
			{
				pkt = tsQ.poll(50, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				break;
			}
			if (pkt == null)
				continue;
			write(videoIn, pkt, pkt.length);
			write(audioIn, pkt, pkt.length);
		}
	}

	private synchronized void launch(int videoPid, int audioPid)
	{
		closeQuiet(videoIn);
		closeQuiet(audioIn);
		if (video != null)
			video.destroyForcibly();
		if (audio != null)
			audio.destroyForcibly();
		video = null;
		audio = null;
		videoIn = null;
		audioIn = null;
		launchFailed = false;
		try
		{
			/* Probe after PAT is already in the pipe. discardcorrupt on a sparse
			 * ATSC stream made ffmpeg ingest megabytes and emit zero frames.
			 * info (not warning) so a stuck decode leaves stream-map / probe
			 * lines on stderr and in tv_debug. Map the PMT video/audio PIDs
			 * so a 0x0 subchannel is not the first stream ffmpeg tries. */
			video = new ProcessBuilder(videoCommand(videoPid)).start();
			audio = new ProcessBuilder(audioCommand(audioPid)).start();
			videoIn = video.getOutputStream();
			audioIn = audio.getOutputStream();
			drain(video.getErrorStream(), "atsc-ffmpeg-video-err", true);
			drain(audio.getErrorStream(), "atsc-ffmpeg-audio-err", false);
			videoThread = new Thread(this::readVideo, "atsc-ffmpeg-video");
			audioThread = new Thread(this::readAudio, "atsc-ffmpeg-audio");
			videoThread.setDaemon(true);
			audioThread.setDaemon(true);
			videoThread.start();
			audioThread.start();
			System.err.println("ATSC watch: ffmpeg video pid=" + video.pid() + " audio pid="
					+ audio.pid());
		}
		catch (IOException e)
		{
			launchFailed = true;
			System.err.println("ATSC watch: ffmpeg not started (" + e.getMessage() + ")");
			noteStderr("ffmpeg not started: " + e.getMessage());
		}
	}

	private void readVideo()
	{
		Process p = video;
		InputStream in = p == null ? null : p.getInputStream();
		if (in == null)
			return;
		byte[] buf = new byte[FRAME_BYTES];
		int n = 0;
		try
		{
			while (run && p == video)
			{
				int r = in.read(buf, n, buf.length - n);
				if (r < 0)
					break;
				n += r;
				stdoutBytes.addAndGet(r);
				partialFrameBytes.set(n);
				if (n < buf.length)
					continue;
				BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
				byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
				System.arraycopy(buf, 0, dst, 0, FRAME_BYTES);
				n = 0;
				partialFrameBytes.set(0);
				int nf = frames.incrementAndGet();
				if (nf == 1)
					System.err.println("ATSC watch: first video frame");
				Consumer<BufferedImage> cb = onFrame;
				if (cb != null)
					cb.accept(img);
			}
		}
		catch (IOException ignored)
		{
		}
		stdoutEof.set(true);
		if (run && p == video)
		{
			String exit = "";
			if (p != null && !p.isAlive())
			{
				try
				{
					exit = " code=" + p.exitValue();
				}
				catch (IllegalThreadStateException ignored)
				{
				}
			}
			System.err.println("ATSC watch: ffmpeg video stdout closed frames=" + frames.get()
					+ " stdoutBytes=" + stdoutBytes.get() + exit);
			noteStderr("video stdout closed frames=" + frames.get() + " stdoutBytes="
					+ stdoutBytes.get() + exit);
		}
	}

	private void readAudio()
	{
		Process p = audio;
		InputStream in = p == null ? null : p.getInputStream();
		if (in == null)
			return;
		byte[] buf = new byte[WfmDemodulator.AUDIO_RATE_HZ / 5 * 2];
		try
		{
			while (run && p == audio)
			{
				int r = in.read(buf);
				if (r < 0)
					break;
				int ns = r / 2;
				short[] pcm = new short[ns];
				for (int i = 0; i < ns; i++)
					pcm[i] = (short) ((buf[2 * i] & 0xff) | (buf[2 * i + 1] << 8));
				Consumer<short[]> cb = onPcm;
				if (cb != null)
					cb.accept(pcm);
			}
		}
		catch (IOException ignored)
		{
		}
	}

	private void drain(InputStream in, String name, boolean videoSide)
	{
		if (in == null)
			return;
		Thread t = new Thread(() -> {
			byte[] b = new byte[512];
			ByteArrayOutputStream line = new ByteArrayOutputStream();
			try
			{
				int n;
				while ((n = in.read(b)) >= 0)
				{
					for (int i = 0; i < n; i++)
					{
						int c = b[i] & 0xff;
						if (c == '\n' || c == '\r')
						{
							if (line.size() == 0)
								continue;
							String s = line.toString(StandardCharsets.UTF_8).trim();
							line.reset();
							if (s.isEmpty())
								continue;
							noteStderr(s);
							if (shouldPrintStderr(s, videoSide))
								System.err.println(name + ": " + s);
						}
						else if (line.size() < 512)
							line.write(c);
					}
				}
			}
			catch (IOException ignored)
			{
			}
		}, name);
		t.setDaemon(true);
		t.start();
	}

	static boolean isDecodeSpam(String s)
	{
		if (s == null || s.isEmpty())
			return true;
		String lower = s.toLowerCase();
		return lower.contains("conceal") || lower.contains("invalid mb")
				|| lower.contains("mvs not available") || lower.contains("cbp too large")
				|| lower.contains("corrupt decoded frame") || lower.contains("skipped mb")
				|| lower.contains("packet corrupt") || lower.contains("invalid frame dimensions")
				|| lower.contains("non-existing pps") || lower.contains("decode_slice_header")
				|| lower.contains("no frame!") || lower.contains("last message repeated")
				|| lower.contains("pes packet size mismatch") || lower.contains("header missing")
				|| (lower.contains("ac-3") && (lower.contains("exponent") || lower.contains("error")))
				|| lower.contains("error while decoding");
	}

	private boolean shouldPrintStderr(String s, boolean videoSide)
	{
		if (isDecodeSpam(s))
			return false;
		if (!videoSide)
			return true;
		if (frames.get() == 0)
			return true;
		String lower = s.toLowerCase();
		return lower.contains("error") || lower.contains("warning") || lower.contains("fail")
				|| lower.contains("invalid") || lower.contains("not found")
				|| lower.contains("could not") || lower.contains("dying");
	}

	static List<String> videoCommand(int videoPid)
	{
		List<String> c = new ArrayList<>();
		c.add("ffmpeg");
		c.add("-hide_banner");
		c.add("-loglevel");
		c.add("info");
		c.add("-fflags");
		c.add("+nobuffer+genpts");
		c.add("-flags");
		c.add("low_delay");
		c.add("-probesize");
		c.add(PROBE_SIZE);
		c.add("-analyzeduration");
		c.add(ANALYZE_DURATION);
		c.add("-f");
		c.add("mpegts");
		c.add("-i");
		c.add("pipe:0");
		if (videoPid >= 0)
		{
			c.add("-map");
			c.add(String.format(Locale.US, "0:i:0x%x", videoPid));
		}
		c.add("-an");
		c.add("-vf");
		c.add("scale=" + WIDTH + ":" + HEIGHT);
		c.add("-pix_fmt");
		c.add("bgr24");
		c.add("-vsync");
		c.add("0");
		c.add("-f");
		c.add("rawvideo");
		c.add("pipe:1");
		return c;
	}

	static List<String> audioCommand(int audioPid)
	{
		List<String> c = new ArrayList<>();
		c.add("ffmpeg");
		c.add("-hide_banner");
		c.add("-loglevel");
		c.add("warning");
		c.add("-fflags");
		c.add("+nobuffer+genpts");
		c.add("-probesize");
		c.add(PROBE_SIZE);
		c.add("-analyzeduration");
		c.add(ANALYZE_DURATION);
		c.add("-f");
		c.add("mpegts");
		c.add("-i");
		c.add("pipe:0");
		if (audioPid >= 0)
		{
			c.add("-map");
			c.add(String.format(Locale.US, "0:i:0x%x", audioPid));
		}
		c.add("-vn");
		c.add("-ac");
		c.add("1");
		c.add("-ar");
		c.add(Integer.toString(WfmDemodulator.AUDIO_RATE_HZ));
		c.add("-f");
		c.add("s16le");
		c.add("pipe:1");
		return c;
	}

	private void noteStderr(String s)
	{
		if (s == null)
			return;
		String clipped = s.trim();
		if (clipped.isEmpty())
			return;
		if (clipped.length() > STDERR_LINE_CAP)
			clipped = clipped.substring(0, STDERR_LINE_CAP);
		synchronized (stderrTail)
		{
			if (stderrTail.size() >= STDERR_TAIL)
				stderrTail.removeFirst();
			stderrTail.addLast(clipped);
		}
	}

	private String lastStderr()
	{
		synchronized (stderrTail)
		{
			if (stderrTail.isEmpty())
				return "";
			return String.join(" | ", stderrTail);
		}
	}

	private void write(OutputStream out, byte[] b, int n)
	{
		if (out == null)
			return;
		try
		{
			out.write(b, 0, n);
			out.flush();
			tsBytesWritten.addAndGet(n);
		}
		catch (IOException e)
		{
			writeErrors.incrementAndGet();
			noteStderr("stdin write failed: " + e.getMessage());
		}
	}

	private static void closeQuiet(OutputStream o)
	{
		if (o == null)
			return;
		try
		{
			o.close();
		}
		catch (IOException ignored)
		{
		}
	}

	public static final class Stats
	{
		public static final int EXIT_NONE = Integer.MIN_VALUE;

		public final boolean started;
		public final boolean launchFailed;
		public final boolean videoAlive;
		public final boolean audioAlive;
		public final int videoExitCode;
		public final long startedMs;
		public final long waitMs;
		public final long tsBytesOffered;
		public final long tsBytesWritten;
		public final int tsQueueDepth;
		public final int tsQueueCap;
		public final long tsDropped;
		public final long writeErrors;
		public final long stdoutBytes;
		public final int partialFrameBytes;
		public final boolean stdoutEof;
		public final String lastStderr;
		public final MpegTsProbe.Snapshot ts;

		public Stats(boolean started, boolean launchFailed, boolean videoAlive, boolean audioAlive,
				int videoExitCode, long startedMs, long waitMs, long tsBytesOffered,
				long tsBytesWritten, int tsQueueDepth, int tsQueueCap, long tsDropped,
				long writeErrors, long stdoutBytes, int partialFrameBytes, boolean stdoutEof,
				String lastStderr, MpegTsProbe.Snapshot ts)
		{
			this.started = started;
			this.launchFailed = launchFailed;
			this.videoAlive = videoAlive;
			this.audioAlive = audioAlive;
			this.videoExitCode = videoExitCode;
			this.startedMs = startedMs;
			this.waitMs = waitMs;
			this.tsBytesOffered = tsBytesOffered;
			this.tsBytesWritten = tsBytesWritten;
			this.tsQueueDepth = tsQueueDepth;
			this.tsQueueCap = tsQueueCap;
			this.tsDropped = tsDropped;
			this.writeErrors = writeErrors;
			this.stdoutBytes = stdoutBytes;
			this.partialFrameBytes = partialFrameBytes;
			this.stdoutEof = stdoutEof;
			this.lastStderr = lastStderr == null ? "" : lastStderr;
			this.ts = ts == null ? MpegTsProbe.Snapshot.empty() : ts;
		}

		public static Stats empty()
		{
			return new Stats(false, false, false, false, EXIT_NONE, 0, 0, 0, 0, 0, TS_QUEUE_CAP, 0,
					0, 0, 0, false, "", MpegTsProbe.Snapshot.empty());
		}

		public String consoleSummary()
		{
			String alive = launchFailed ? "missing" : videoAlive ? "alive" : "dead";
			String exit = videoExitCode == EXIT_NONE ? "-" : Integer.toString(videoExitCode);
			String err = lastStderr;
			if (err.length() > 80)
				err = err.substring(0, 80);
			return "ffmpeg video=" + alive + " exit=" + exit + " offered=" + tsBytesOffered
					+ " written=" + tsBytesWritten + " q=" + tsQueueDepth + "/" + tsQueueCap
					+ " drop=" + tsDropped + " wrErr=" + writeErrors + " stdout=" + stdoutBytes
					+ "/" + FRAME_BYTES + " pmt=" + ts.pmtPid + " vpid=" + ts.videoPid + " vpkts="
					+ ts.videoPackets + " pes=" + ts.videoPesStarts + " wait=" + waitMs + "ms"
					+ (err.isEmpty() ? "" : " err=" + err);
		}
	}
}
