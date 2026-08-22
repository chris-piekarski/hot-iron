package hotiron.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

import hotiron.jna.HackrfSweepLibrary;

/**
 * ATSC 1.0 watch: int8 IQ → native 8VSB → MPEG-TS → ffmpeg frames/PCM.
 * The libusb callback must only {@link #offerIq}.
 */
public final class TvWatchEngine
{
	public static final int QUEUE_CAP = 128;
	static final int TS_PACKET_BYTES = 188;
	static final int TS_OUTPUT_PACKETS = 128;
	static final long POLARITY_RETRY_MS = 12_000;
	static final int DEBUG_COUNTERS = 10;
	static final int DEBUG_GAUGES = 6;
	static final long DEBUG_INTERVAL_MS = 200;

	private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAP);
	private final AtomicLong bytes = new AtomicLong();
	private final AtomicLong dropped = new AtomicLong();
	private final AtomicInteger volume = new AtomicInteger(80);
	private final MpegTsPlayer player = new MpegTsPlayer();
	private final IqSpectrum iqSpectrum = new IqSpectrum();
	private final WatchPreview preview = new WatchPreview();
	private volatile AudioSpectrum.FrameListener spectrumListener;
	private volatile boolean run;
	private volatile Pointer rx;
	private volatile boolean locked;
	private volatile float snrDb;
	private volatile int packets;
	private volatile TvWatchDebug debug = TvWatchDebug.empty();
	private Consumer<BufferedImage> onFrame;
	private AudioSink sink;
	private Thread worker;
	private Thread previewWorker;
	private final AtomicReference<byte[]> previewIq = new AtomicReference<>();
	private long lastLogMs;
	private long lastPreviewMs;
	private long startMs;
	private volatile boolean sawPat;
	private FileOutputStream tsDump;
	private int dumpLeft;
	private boolean inverted;

	public synchronized void start(Consumer<BufferedImage> onFrame, AudioSink sink)
	{
		stop();
		this.onFrame = onFrame;
		this.sink = sink == null ? new RecordingAudioSink() : sink;
		bytes.set(0);
		dropped.set(0);
		locked = false;
		snrDb = 0;
		packets = 0;
		sawPat = false;
		lastLogMs = 0;
		iqSpectrum.reset();
		preview.reset();
		lastPreviewMs = 0;
		startMs = System.currentTimeMillis();
		previewIq.set(null);
		queue.clear();
		openDump();
		run = true;
		inverted = false;
		rx = createReceiver(false);
		debug = new TvWatchDebug(startMs, true, rx != null, false, false, false,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0);
		worker = new Thread(this::loop, "atsc-8vsb");
		worker.setDaemon(true);
		worker.setPriority(Thread.MAX_PRIORITY);
		worker.start();
		previewWorker = new Thread(this::previewLoop, "atsc-iq-preview");
		previewWorker.setDaemon(true);
		previewWorker.start();
	}

	public synchronized void stop()
	{
		run = false;
		if (worker != null)
		{
			worker.interrupt();
			try
			{
				worker.join(800);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			worker = null;
		}
		if (previewWorker != null)
		{
			previewWorker.interrupt();
			try
			{
				previewWorker.join(400);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			previewWorker = null;
		}
		previewIq.set(null);
		queue.clear();
		player.stop();
		closeDump();
		Pointer p = rx;
		rx = null;
		if (p != null)
		{
			try
			{
				HackrfSweepLibrary.atsc_rx_destroy(p);
			}
			catch (UnsatisfiedLinkError ignored)
			{
			}
		}
		AudioSink s = sink;
		sink = null;
		if (s != null)
			s.close();
		locked = false;
		debug = TvWatchDebug.empty();
	}

	public void setVolume(int volume0to100)
	{
		int v = volume0to100;
		if (v < 0)
			v = 0;
		if (v > 100)
			v = 100;
		volume.set(v);
	}

	public boolean isRunning()
	{
		return run;
	}

	public boolean locked()
	{
		return locked;
	}

	public float snrDb()
	{
		return snrDb;
	}

	public int packets()
	{
		return packets;
	}

	public int frames()
	{
		return player.frames();
	}

	public int previewFrames()
	{
		return preview.frames();
	}

	public TvWatchDebug debug()
	{
		return debug;
	}

	public boolean hasPat()
	{
		return sawPat;
	}

	public void setSpectrumListener(AudioSpectrum.FrameListener listener)
	{
		this.spectrumListener = listener;
	}

	public IqSpectrum iqSpectrum()
	{
		return iqSpectrum;
	}

	static boolean shouldRetryPolarity(boolean hasPat, int packetCount, long elapsedMs)
	{
		return !hasPat && packetCount == 0 && elapsedMs >= POLARITY_RETRY_MS;
	}

	public boolean offerIq(byte[] iq)
	{
		if (!run || iq == null || iq.length == 0)
			return false;
		bytes.addAndGet(iq.length);
		previewIq.set(iq);
		if (queue.offer(iq))
			return true;
		dropped.incrementAndGet();
		/*
		 * Staying behind is worse than losing this backlog: trellis and
		 * deinterleaver state cannot cross the sample gap. Resume from the
		 * newest chunk and let the worker recreate the complete receiver.
		 */
		queue.clear();
		return queue.offer(iq);
	}

	public long bytesOffered()
	{
		return bytes.get();
	}

	public long droppedChunks()
	{
		return dropped.get();
	}

	private void pushPreview(float[] row)
	{
		if (player.frames() > 0)
			return;
		long now = System.currentTimeMillis();
		if (now - lastPreviewMs < 33)
			return;
		lastPreviewMs = now;
		BufferedImage img = preview.pushDb(row);
		Consumer<BufferedImage> cb = onFrame;
		if (preview.frames() == 1)
			System.err.println("ATSC watch: IQ video frame 1 " + img.getWidth() + "x" + img.getHeight());
		if (cb != null)
			cb.accept(img);
	}

	private void startPlayer()
	{
		player.start(img -> {
			Consumer<BufferedImage> cb = this.onFrame;
			if (cb != null)
				cb.accept(img);
		}, pcm -> {
			AudioSink s = this.sink;
			if (s == null || pcm == null || pcm.length == 0)
				return;
			int vol = volume.get();
			if (vol < 100)
			{
				for (int i = 0; i < pcm.length; i++)
					pcm[i] = (short) (pcm[i] * vol / 100);
			}
			s.write(pcm, 0, pcm.length);
		});
	}

	private void loop()
	{
		/*
		 * One 262144-byte HackRF transfer contains about 106 ATSC data
		 * segments at 16 MS/s. A 64-packet buffer made the native pipeline
		 * stop mid-transfer, permanently accumulating unprocessed segments.
		 */
		byte[] ts = new byte[TS_PACKET_BYTES * TS_OUTPUT_PACKETS];
		FloatByReference snr = new FloatByReference();
		long[] debugCounters = new long[DEBUG_COUNTERS];
		float[] debugGauges = new float[DEBUG_GAUGES];
		PatDetector patDetector = new PatDetector();
		long lastDebugMs = 0;
		long polaritySince = System.currentTimeMillis();
		long receiverDrops = dropped.get();
		while (run)
		{
			byte[] chunk;
			try
			{
				chunk = queue.poll(50, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				break;
			}
			if (chunk == null)
				continue;
			Pointer p = rx;
			long currentDrops = dropped.get();
			if (p != null && currentDrops != receiverDrops)
			{
				Pointer replacement = createReceiver(inverted);
				if (replacement != null)
				{
					rx = replacement;
					HackrfSweepLibrary.atsc_rx_destroy(p);
					p = replacement;
					receiverDrops = currentDrops;
					player.stop();
					sawPat = false;
					patDetector.reset();
					locked = false;
					snrDb = 0;
					packets = 0;
					polaritySince = System.currentTimeMillis();
					System.err.println("ATSC watch: reset receiver after IQ drop " + currentDrops);
					continue;
				}
			}
			int n = 0;
			if (p != null)
				n = HackrfSweepLibrary.atsc_rx_process(p, chunk, chunk.length, ts, ts.length, snr);
			if (p == null)
				continue;
			snrDb = snr.getValue();
			try
			{
				locked = HackrfSweepLibrary.atsc_rx_locked(p) != 0;
				packets = HackrfSweepLibrary.atsc_rx_packets(p);
			}
			catch (UnsatisfiedLinkError ignored)
			{
			}
			if (n >= 188)
			{
				int nbytes = n - (n % 188);
				dumpTs(ts, nbytes);
				if (!sawPat)
					sawPat = patDetector.accept(ts, nbytes);
				if (sawPat)
				{
					if (!player.running())
						startPlayer();
					player.writeTs(ts, nbytes);
				}
			}
			long now = System.currentTimeMillis();
			if (now - lastDebugMs >= DEBUG_INTERVAL_MS)
			{
				lastDebugMs = now;
				try
				{
					if (HackrfSweepLibrary.atsc_rx_debug(p, debugCounters, debugCounters.length,
							debugGauges, debugGauges.length) > 0)
					{
						debug = new TvWatchDebug(now, run, true, sawPat, locked,
								debugCounters[8] != 0, debugCounters[0], debugCounters[1],
								debugCounters[2], debugCounters[3], debugCounters[4],
								debugCounters[5], debugCounters[6], debugCounters[7],
								debugCounters[9], dropped.get(), queue.size(), player.frames(),
								preview.frames(), debugGauges[0], debugGauges[1],
								debugGauges[2], debugGauges[3], debugGauges[4],
								debugGauges[5]);
					}
				}
				catch (UnsatisfiedLinkError ignored)
				{
				}
			}
			if (p != null && shouldRetryPolarity(sawPat, packets, now - polaritySince))
			{
				boolean nextInverted = !inverted;
				Pointer replacement = createReceiver(nextInverted);
				if (replacement != null)
				{
					rx = replacement;
					HackrfSweepLibrary.atsc_rx_destroy(p);
					inverted = nextInverted;
					locked = false;
					snrDb = 0;
					packets = 0;
					patDetector.reset();
					polaritySince = now;
					System.err.println("ATSC watch: retrying IQ polarity "
							+ (inverted ? "inverted" : "normal"));
				}
			}
			if (now - lastLogMs > 2000)
			{
				lastLogMs = now;
				TvWatchDebug d = debug;
				System.err.println("ATSC watch: stage=" + d.stage() + " locked=" + locked
						+ " packets=" + packets + " bad=" + d.badPackets + " good=" + d.goodPackets + " snr="
						+ String.format(java.util.Locale.US, "%.1f", snrDb) + " dB dropped="
						+ dropped.get() + " pat=" + sawPat + " frames=" + player.frames()
						+ " preview=" + preview.frames() + " agc="
						+ String.format(java.util.Locale.US, "%.1f", d.agcGain)
						+ " rmsIq=" + String.format(java.util.Locale.US, "%.4f", d.rmsIq)
						+ " rmsBb=" + String.format(java.util.Locale.US, "%.2f", d.rmsBaseband));
			}
		}
	}

	private Pointer createReceiver(boolean invert)
	{
		try
		{
			HackrfSweepLibrary.class.getName();
			Pointer created = HackrfSweepLibrary.atsc_rx_create(TvChannelPlan.IQ_RATE_HZ);
			if (created != null)
				HackrfSweepLibrary.atsc_rx_set_invert(created, invert ? 1 : 0);
			return created;
		}
		catch (UnsatisfiedLinkError e)
		{
			System.err.println("ATSC watch: native 8VSB missing (" + e.getMessage() + ")");
			return null;
		}
	}

	private void previewLoop()
	{
		while (run)
		{
			byte[] chunk = previewIq.getAndSet(null);
			if (chunk == null)
			{
				try
				{
					Thread.sleep(15);
				}
				catch (InterruptedException e)
				{
					break;
				}
				continue;
			}
			float[] row = iqSpectrum.accept(chunk, chunk.length);
			if (row == null)
				continue;
			AudioSpectrum.FrameListener spec = spectrumListener;
			if (spec != null)
				spec.onFrame(row);
			pushPreview(row);
		}
	}

	private void openDump()
	{
		closeDump();
		if (!Boolean.getBoolean("hackrf.atsc.dump"))
			return;
		dumpLeft = 2 * 1024 * 1024;
		try
		{
			File f = new File(System.getProperty("java.io.tmpdir"), "hackrf-atsc.ts");
			tsDump = new FileOutputStream(f);
			System.err.println("ATSC watch: dumping TS to " + f.getAbsolutePath());
		}
		catch (IOException e)
		{
			tsDump = null;
		}
	}

	private void dumpTs(byte[] ts, int n)
	{
		FileOutputStream out = tsDump;
		if (out == null || dumpLeft <= 0)
			return;
		int w = Math.min(n, dumpLeft);
		try
		{
			out.write(ts, 0, w);
			dumpLeft -= w;
			if (dumpLeft <= 0)
				closeDump();
		}
		catch (IOException e)
		{
			closeDump();
		}
	}

	private void closeDump()
	{
		FileOutputStream out = tsDump;
		tsDump = null;
		if (out != null)
		{
			try
			{
				out.close();
			}
			catch (IOException ignored)
			{
			}
		}
	}

	static boolean containsPat(byte[] ts, int n)
	{
		return new PatDetector().accept(ts, n);
	}

	static final class PatDetector
	{
		private final byte[] section = new byte[1024];
		private int filled;
		private int expected = -1;
		private int lastContinuity = -1;
		private boolean collecting;

		void reset()
		{
			resetSection();
			lastContinuity = -1;
		}

		boolean accept(byte[] ts, int n)
		{
			if (ts == null)
				return false;
			int lim = Math.min(n, ts.length);
			lim -= lim % 188;
			for (int packet = 0; packet + 188 <= lim; packet += 188)
			{
				if (ts[packet] != 0x47)
					continue;
				int pid = ((ts[packet + 1] & 0x1f) << 8) | (ts[packet + 2] & 0xff);
				int adaptation = (ts[packet + 3] >>> 4) & 0x03;
				if (pid != 0 || adaptation == 0 || adaptation == 2)
					continue;
				int continuity = ts[packet + 3] & 0x0f;
				if (lastContinuity >= 0 && continuity != ((lastContinuity + 1) & 0x0f))
					resetSection();
				lastContinuity = continuity;
				int payload = packet + 4;
				if (adaptation == 3)
				{
					if (payload >= packet + 188)
						continue;
					payload += 1 + (ts[payload] & 0xff);
				}
				if (payload >= packet + 188)
					continue;
				boolean unitStart = (ts[packet + 1] & 0x40) != 0;
				if (unitStart)
				{
					int pointer = ts[payload] & 0xff;
					int sectionStart = payload + 1 + pointer;
					if (collecting && pointer > 0
							&& append(ts, payload + 1, Math.min(sectionStart, packet + 188)))
						return true;
					resetSection();
					if (sectionStart < packet + 188)
					{
						collecting = true;
						if (append(ts, sectionStart, packet + 188))
							return true;
					}
				}
				else if (collecting && append(ts, payload, packet + 188))
					return true;
			}
			return false;
		}

		private boolean append(byte[] data, int start, int end)
		{
			while (start < end && collecting)
			{
				if (filled >= section.length)
				{
					resetSection();
					return false;
				}
				section[filled++] = data[start++];
				if (filled == 3)
				{
					int sectionLength = ((section[1] & 0x0f) << 8) | (section[2] & 0xff);
					if (section[0] != 0x00 || (section[1] & 0x80) == 0
							|| sectionLength < 9 || sectionLength > 1021)
					{
						resetSection();
						return false;
					}
					expected = 3 + sectionLength;
				}
				if (expected > 0 && filled == expected)
				{
					boolean valid = mpegCrc32(section, 0, expected) == 0;
					resetSection();
					return valid;
				}
			}
			return false;
		}

		private void resetSection()
		{
			filled = 0;
			expected = -1;
			collecting = false;
		}
	}

	static int mpegCrc32(byte[] data, int offset, int length)
	{
		int crc = 0xffffffff;
		for (int i = 0; i < length; i++)
		{
			crc ^= (data[offset + i] & 0xff) << 24;
			for (int bit = 0; bit < 8; bit++)
				crc = (crc & 0x80000000) != 0 ? (crc << 1) ^ 0x04c11db7 : crc << 1;
		}
		return crc;
	}
}
