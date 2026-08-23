package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Parked NFC IQ: USB callback only {@link #offerIq}. Worker FFT + nfc-lab
 * magnitude decode. Same exclusive radio as Listen/Watch.
 */
public final class NfcSniffEngine
{
	public static final int IQ_RATE_HZ = 10_000_000;
	public static final long LO_HZ = 11_560_000L;
	public static final int QUEUE_CAP = 8;
	public static final int SETTLE_MS = 150;

	public interface Decoder
	{
		List<NfcFrame> processIq(byte[] iq);

		default void close()
		{
		}

		Decoder NONE = new Decoder()
		{
			@Override
			public List<NfcFrame> processIq(byte[] iq)
			{
				return List.of();
			}
		};
	}

	private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(QUEUE_CAP);
	private final AtomicLong dropped = new AtomicLong();
	private final IqSpectrum rfSpectrum = new IqSpectrum(IQ_RATE_HZ);
	private final NfcEnvelopeTrace envelopeTrace = new NfcEnvelopeTrace();
	private volatile Decoder decoder = Decoder.NONE;
	private volatile Consumer<float[]> rfListener;
	private volatile Consumer<float[]> envelopeListener;
	private volatile Consumer<NfcFrame> frameListener;
	private volatile boolean run;
	private volatile long settleUntilMs;
	private Thread worker;

	public synchronized void start(Decoder decoder)
	{
		stop();
		this.decoder = decoder == null ? Decoder.NONE : decoder;
		rfSpectrum.reset();
		envelopeTrace.reset();
		queue.clear();
		dropped.set(0);
		settleUntilMs = System.currentTimeMillis() + SETTLE_MS;
		run = true;
		worker = new Thread(this::loop, "nfc-sniff");
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
		Decoder d = decoder;
		decoder = Decoder.NONE;
		if (d != null)
			d.close();
	}

	public void setRfSpectrumListener(Consumer<float[]> listener)
	{
		this.rfListener = listener;
	}

	public void setEnvelopeListener(Consumer<float[]> listener)
	{
		this.envelopeListener = listener;
	}

	public void setFrameListener(Consumer<NfcFrame> listener)
	{
		this.frameListener = listener;
	}

	public IqSpectrum rfSpectrum()
	{
		return rfSpectrum;
	}

	public boolean offerIq(byte[] iq)
	{
		if (!run || iq == null || iq.length == 0)
			return false;
		if (queue.offer(iq))
			return true;
		dropped.incrementAndGet();
		return false;
	}

	public long droppedChunks()
	{
		return dropped.get();
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
			Consumer<float[]> rf = rfListener;
			if (rfRow != null && rf != null)
				rf.accept(rfRow);
			Consumer<float[]> env = envelopeListener;
			if (env != null)
			{
				envelopeTrace.acceptIq(chunk);
				env.accept(envelopeTrace.snapshot());
			}
			if (System.currentTimeMillis() < settleUntilMs)
				continue;
			List<NfcFrame> frames = decoder.processIq(chunk);
			Consumer<NfcFrame> framesOut = frameListener;
			if (framesOut != null && frames != null)
			{
				for (NfcFrame frame : frames)
				{
					if (frame != null)
						framesOut.accept(frame);
				}
			}
		}
	}

	/**
	 * Crop a parked FFT row to the 12–15 MHz PHY window. Bin 0 is −fs/2.
	 */
	public static ViewRow cropPhy(float[] row, float binHz, long loHz)
	{
		if (row == null || row.length == 0 || !(binHz > 0))
			return ViewRow.EMPTY;
		double loMHz = loHz / 1_000_000.0;
		int first = -1;
		int last = -1;
		for (int i = 0; i < row.length; i++)
		{
			double mhz = loMHz + (i - row.length / 2.0) * binHz / 1_000_000.0;
			if (mhz >= NfcBandPlan.VIEW_START_MHZ && mhz < NfcBandPlan.VIEW_END_MHZ)
			{
				if (first < 0)
					first = i;
				last = i;
			}
		}
		if (first < 0)
			return ViewRow.EMPTY;
		int n = last - first + 1;
		float[] mhz = new float[n];
		float[] dbfs = new float[n];
		for (int i = 0; i < n; i++)
		{
			mhz[i] = (float) (loMHz + (first + i - row.length / 2.0) * binHz / 1_000_000.0);
			dbfs[i] = row[first + i];
		}
		return new ViewRow(mhz, dbfs, binHz);
	}

	public static final class ViewRow
	{
		static final ViewRow EMPTY = new ViewRow(new float[0], new float[0], 0);

		public final float[] mhz;
		public final float[] dbfs;
		public final float binHz;

		public ViewRow(float[] mhz, float[] dbfs, float binHz)
		{
			this.mhz = mhz == null ? new float[0] : mhz;
			this.dbfs = dbfs == null ? new float[0] : dbfs;
			this.binHz = binHz;
		}

		public boolean isEmpty()
		{
			return mhz.length == 0 || mhz.length != dbfs.length;
		}
	}

	static float[] envelope(byte[] iq)
	{
		return NfcEnvelopeTrace.mixCarrierDb(iq, IQ_RATE_HZ, NfcEnvelopeTrace.IF_HZ, NfcEnvelopeTrace.SAMPLE_HZ);
	}

	public static List<NfcFrame> copyRing(List<NfcFrame> ring)
	{
		if (ring == null || ring.isEmpty())
			return List.of();
		return Collections.unmodifiableList(new ArrayList<NfcFrame>(ring));
	}
}
