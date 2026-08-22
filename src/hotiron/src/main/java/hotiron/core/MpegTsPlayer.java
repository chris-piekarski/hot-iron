package hotiron.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Decode MPEG-TS (ATSC) to RGB frames + 48 kHz PCM via the host ffmpeg.
 */
public final class MpegTsPlayer
{
	public static final int WIDTH = 640;
	public static final int HEIGHT = 360;
	public static final int FRAME_BYTES = WIDTH * HEIGHT * 3;

	private final ArrayBlockingQueue<byte[]> tsQ = new ArrayBlockingQueue<>(128);
	private Process video;
	private Process audio;
	private OutputStream videoIn;
	private OutputStream audioIn;
	private Thread videoThread;
	private Thread audioThread;
	private Thread writer;
	private volatile boolean run;
	private Consumer<BufferedImage> onFrame;
	private Consumer<short[]> onPcm;
	private final AtomicInteger frames = new AtomicInteger();

	public void start(Consumer<BufferedImage> onFrame, Consumer<short[]> onPcm)
	{
		stop();
		this.onFrame = onFrame;
		this.onPcm = onPcm;
		frames.set(0);
		run = true;
		tsQ.clear();
		launch();
		writer = new Thread(this::writeLoop, "atsc-ffmpeg-write");
		writer.setDaemon(true);
		writer.start();
	}

	public void stop()
	{
		run = false;
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

	public void writeTs(byte[] ts, int nbytes)
	{
		if (!run || ts == null || nbytes < 188)
			return;
		int n = nbytes - (nbytes % 188);
		byte[] copy = Arrays.copyOf(ts, n);
		tsQ.offer(copy);
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

	private synchronized void launch()
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
		try
		{
			/* Probe after PAT is already in the pipe. discardcorrupt on a sparse
			 * ATSC stream made ffmpeg ingest megabytes and emit zero frames. */
			video = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "warning", "-fflags",
					"+nobuffer+genpts", "-flags", "low_delay", "-probesize", "100000",
					"-analyzeduration", "500000", "-f", "mpegts", "-i", "pipe:0", "-an", "-vf",
					"scale=" + WIDTH + ":" + HEIGHT, "-pix_fmt", "bgr24", "-vsync", "0", "-f", "rawvideo",
					"pipe:1").start();
			audio = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "warning", "-fflags",
					"+nobuffer+genpts", "-probesize", "100000", "-analyzeduration", "500000", "-f",
					"mpegts", "-i", "pipe:0", "-vn", "-ac", "1", "-ar",
					Integer.toString(WfmDemodulator.AUDIO_RATE_HZ), "-f", "s16le", "pipe:1").start();
			videoIn = video.getOutputStream();
			audioIn = audio.getOutputStream();
			drain(video.getErrorStream(), "atsc-ffmpeg-video-err");
			drain(audio.getErrorStream(), "atsc-ffmpeg-audio-err");
			videoThread = new Thread(this::readVideo, "atsc-ffmpeg-video");
			audioThread = new Thread(this::readAudio, "atsc-ffmpeg-audio");
			videoThread.setDaemon(true);
			audioThread.setDaemon(true);
			videoThread.start();
			audioThread.start();
		}
		catch (IOException e)
		{
			System.err.println("ATSC watch: ffmpeg not started (" + e.getMessage() + ")");
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
				if (n < buf.length)
					continue;
				BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
				byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
				System.arraycopy(buf, 0, dst, 0, FRAME_BYTES);
				n = 0;
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

	private static void drain(InputStream in, String name)
	{
		if (in == null)
			return;
		Thread t = new Thread(() -> {
			byte[] b = new byte[512];
			try
			{
				int n;
				while ((n = in.read(b)) >= 0)
				{
					if (n == 0)
						continue;
					String s = new String(b, 0, n, StandardCharsets.UTF_8).trim();
					if (!s.isEmpty())
						System.err.println(name + ": " + s);
				}
			}
			catch (IOException ignored)
			{
			}
		}, name);
		t.setDaemon(true);
		t.start();
	}

	private static void write(OutputStream out, byte[] b, int n)
	{
		if (out == null)
			return;
		try
		{
			out.write(b, 0, n);
			out.flush();
		}
		catch (IOException ignored)
		{
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
}
