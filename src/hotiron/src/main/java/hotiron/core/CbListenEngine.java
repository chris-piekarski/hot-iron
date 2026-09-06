package hotiron.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleConsumer;

/**
 * Parked CB AM: USB callback only {@link #offerIq}. Mix/demod on a worker.
 * Channel changes are Java mix — no USB retune.
 */
public final class CbListenEngine
{
	public static final int QUEUE_CAP = 8;
	public static final int SETTLE_MS = 200;

	private final AmDemodulator demod = new AmDemodulator();
	private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(QUEUE_CAP);
	private final AtomicInteger volume = new AtomicInteger(80);
	private final AtomicLong dropped = new AtomicLong();
	private final AtomicLong offered = new AtomicLong();
	private final short[] pcm = new short[AmDemodulator.IQ_RATE_HZ / 50];
	private final AtomicReference<float[]> pendingRf = new AtomicReference<>();
	private final AtomicReference<float[]> pendingAudio = new AtomicReference<>();
	private final Object displayWake = new Object();
	private volatile AudioSink sink;
	private volatile AudioSpectrum.FrameListener spectrumListener;
	private volatile AudioSpectrum.FrameListener rfSpectrumListener;
	private final AudioSpectrum audioSpectrum = new AudioSpectrum();
	private final IqSpectrum rfSpectrum = new IqSpectrum(AmDemodulator.IQ_RATE_HZ);
	private final VuMeter vu = new VuMeter();
	private final AtomicReference<Float> pendingLevel = new AtomicReference<>();
	private volatile DoubleConsumer levelListener;
	private volatile ChatterListener chatterListener;
	private volatile boolean run;
	private volatile int settleMs = SETTLE_MS;
	private volatile long settleUntilMs;
	private volatile boolean armed;
	private Thread worker;
	private Thread display;

	public void setSettleMs(int ms)
	{
		settleMs = Math.max(0, ms);
	}

	public void setChannel(int number)
	{
		CbChannel ch = CbChannelPlan.clamp(number);
		demod.setChannelHz(ch.centerHz());
		demod.retune();
	}

	@FunctionalInterface
	public interface ChatterListener
	{
		void onObservation(float carrier, float modulation);
	}

	public void setChatterListener(ChatterListener listener)
	{
		this.chatterListener = listener;
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
		pendingRf.set(null);
		pendingAudio.set(null);
		pendingLevel.set(null);
		vu.reset();
		armed = false;
		settleUntilMs = System.currentTimeMillis() + settleMs;
		run = true;
		display = new Thread(this::displayLoop, "cb-am-display");
		display.setDaemon(true);
		display.start();
		worker = new Thread(this::loop, "cb-am-demod");
		worker.setDaemon(true);
		worker.start();
	}

	public synchronized void stop()
	{
		run = false;
		synchronized (displayWake)
		{
			displayWake.notifyAll();
		}
		joinQuiet(worker);
		joinQuiet(display);
		worker = null;
		display = null;
		queue.clear();
		pendingRf.set(null);
		pendingAudio.set(null);
		pendingLevel.set(null);
		AudioSink s = sink;
		sink = null;
		if (s != null)
			s.close();
	}

	private static void joinQuiet(Thread t)
	{
		if (t == null)
			return;
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

	public void setSpectrumListener(AudioSpectrum.FrameListener listener)
	{
		this.spectrumListener = listener;
	}

	public void setRfSpectrumListener(AudioSpectrum.FrameListener listener)
	{
		this.rfSpectrumListener = listener;
	}

	public void setLevelListener(DoubleConsumer listener)
	{
		this.levelListener = listener;
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
			if (System.currentTimeMillis() >= settleUntilMs)
			{
				if (!armed)
				{
					demod.reset();
					audioSpectrum.reset();
					armed = true;
				}
				int n = demod.processIq(chunk, chunk.length, 100, pcm);
				ChatterListener chatter = chatterListener;
				if (chatter != null)
					chatter.onObservation(demod.carrier(), demod.modulation());
				if (n > 0)
				{
					publishLevel(vu.accept(pcm, n, System.currentTimeMillis()));
					float[] audioRow = audioSpectrum.accept(pcm, n);
					int vol = volume.get();
					if (vol < 100)
					{
						for (int i = 0; i < n; i++)
							pcm[i] = (short) (pcm[i] * vol / 100);
					}
					AudioSink s = sink;
					if (s != null)
						s.write(pcm, 0, n);
					publishDisplay(pendingAudio, audioRow);
				}
			}
			publishDisplay(pendingRf, rfSpectrum.accept(chunk, chunk.length));
		}
	}

	private void publishDisplay(AtomicReference<float[]> slot, float[] row)
	{
		if (row == null)
			return;
		slot.set(row);
		synchronized (displayWake)
		{
			displayWake.notify();
		}
	}

	private void displayLoop()
	{
		while (run)
		{
			float[] rf;
			float[] audio;
			Float level;
			synchronized (displayWake)
			{
				while (run && pendingRf.get() == null && pendingAudio.get() == null && pendingLevel.get() == null)
				{
					try
					{
						displayWake.wait(50);
					}
					catch (InterruptedException e)
					{
						return;
					}
				}
				rf = pendingRf.getAndSet(null);
				audio = pendingAudio.getAndSet(null);
				level = pendingLevel.getAndSet(null);
			}
			dispatch(rfSpectrumListener, rf);
			dispatch(spectrumListener, audio);
			DoubleConsumer levels = levelListener;
			if (levels != null && level != null)
			{
				try
				{
					levels.accept(level.floatValue());
				}
				catch (RuntimeException ignored)
				{
				}
			}
		}
	}

	private void publishLevel(float level01)
	{
		pendingLevel.set(Float.valueOf(level01));
		synchronized (displayWake)
		{
			displayWake.notify();
		}
	}

	private static void dispatch(AudioSpectrum.FrameListener listener, float[] row)
	{
		if (listener == null || row == null)
			return;
		try
		{
			listener.onFrame(row);
		}
		catch (RuntimeException ignored)
		{
		}
	}
}
