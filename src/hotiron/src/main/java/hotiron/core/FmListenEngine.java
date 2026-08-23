package hotiron.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demod thread: IQ chunks from the JNA callback → {@link WfmDemodulator}
 * → {@link AudioSink}. The libusb callback must only {@link #offerIq}.
 */
public final class FmListenEngine
{
	public static final int QUEUE_CAP = 8;
	/**
	 * First USB transfers after sweep→4 MS/s are unlocked PLL/DC. Demod
	 * of that sounds like loud static; skip it, then reset.
	 */
	public static final int SETTLE_MS = 200;

	private final WfmDemodulator demod = new WfmDemodulator();
	private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(QUEUE_CAP);
	private final AtomicInteger volume = new AtomicInteger(80);
	private final AtomicLong dropped = new AtomicLong();
	private final AtomicLong offered = new AtomicLong();
	private final short[] pcm = new short[WfmDemodulator.IQ_RATE_HZ / 50];
	private volatile AudioSink sink;
	private volatile AudioSpectrum.FrameListener spectrumListener;
	private volatile AudioSpectrum.FrameListener rfSpectrumListener;
	private final AudioSpectrum audioSpectrum = new AudioSpectrum();
	private final IqSpectrum rfSpectrum = new IqSpectrum(WfmDemodulator.IQ_RATE_HZ);
	private volatile boolean run;
	private volatile int settleMs = SETTLE_MS;
	private volatile long settleUntilMs;
	private volatile boolean armed;
	private Thread worker;

	public void setSettleMs(int ms)
	{
		settleMs = Math.max(0, ms);
	}

	public synchronized void start(AudioSink sink)
	{
		stop();
		this.sink = sink == null ? new RecordingAudioSink() : sink;
		demod.reset();
		audioSpectrum.reset();
		rfSpectrum.reset();
		queue.clear();
		dropped.set(0);
		offered.set(0);
		armed = false;
		settleUntilMs = System.currentTimeMillis() + settleMs;
		run = true;
		worker = new Thread(this::loop, "fm-wfm-demod");
		worker.setDaemon(true);
		worker.start();
	}

	public synchronized void stop()
	{
		run = false;
		if (worker != null)
		{
			worker.interrupt();
			try
			{
				worker.join(500);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			worker = null;
		}
		queue.clear();
		AudioSink s = sink;
		sink = null;
		if (s != null)
			s.close();
	}

	public void setSpectrumListener(AudioSpectrum.FrameListener listener)
	{
		this.spectrumListener = listener;
	}

	public AudioSpectrum audioSpectrum()
	{
		return audioSpectrum;
	}

	public void setRfSpectrumListener(AudioSpectrum.FrameListener listener)
	{
		this.rfSpectrumListener = listener;
	}

	public IqSpectrum rfSpectrum()
	{
		return rfSpectrum;
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

	public boolean offerIq(byte[] iq)
	{
		if (!run || iq == null || iq.length == 0)
			return false;
		offered.incrementAndGet();
		if (queue.offer(iq))
			return true;
		dropped.incrementAndGet();
		return false;
	}

	public long droppedChunks()
	{
		return dropped.get();
	}

	public long offeredChunks()
	{
		return offered.get();
	}

	public boolean running()
	{
		return run;
	}

	private void loop()
	{
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
			float[] rfRow = rfSpectrum.accept(chunk, chunk.length);
			AudioSpectrum.FrameListener rfSpec = rfSpectrumListener;
			if (rfRow != null && rfSpec != null)
				rfSpec.onFrame(rfRow);
			if (System.currentTimeMillis() < settleUntilMs)
				continue;
			if (!armed)
			{
				demod.reset();
				audioSpectrum.reset();
				armed = true;
			}
			int n = demod.processIq(chunk, chunk.length, 100, pcm);
			if (n <= 0)
				continue;
			float[] row = audioSpectrum.accept(pcm, n);
			AudioSpectrum.FrameListener spec = spectrumListener;
			if (row != null && spec != null)
				spec.onFrame(row);
			int vol = volume.get();
			if (vol < 100)
			{
				for (int i = 0; i < n; i++)
					pcm[i] = (short) (pcm[i] * vol / 100);
			}
			AudioSink s = sink;
			if (s != null)
				s.write(pcm, 0, n);
		}
	}
}
