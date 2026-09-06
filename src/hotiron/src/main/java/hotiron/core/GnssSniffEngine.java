package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Parked GPS L1 IQ: USB callback only {@link #offerIq}. Worker FFT +
 * C/A acquisition. Hits are last-known per PRN — a miss does not
 * blank the list. Same exclusive radio as Listen/Watch/NFC.
 */
public final class GnssSniffEngine
{
	public static final int IQ_RATE_HZ = GpsL1Acquirer.SAMPLE_RATE;
	public static final long LO_HZ = GnssBandPlan.GPS_L1_HZ;
	public static final int QUEUE_CAP = 8;
	public static final int SETTLE_MS = 200;
	public static final int ACQUIRE_PERIOD_MS = 250;

	private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(QUEUE_CAP);
	private final AtomicLong dropped = new AtomicLong();
	private final IqSpectrum rfSpectrum = new IqSpectrum(IQ_RATE_HZ);
	private final GpsL1Acquirer acquirer;
	private final GpsSvTracker held = new GpsSvTracker();
	private volatile Consumer<float[]> rfListener;
	private volatile Consumer<List<GpsSvHit>> hitsListener;
	private volatile List<GpsSvHit> lastHits = List.of();
	private volatile boolean run;
	private volatile long settleUntilMs;
	private Thread worker;

	public GnssSniffEngine()
	{
		this(new GpsL1Acquirer());
	}

	public GnssSniffEngine(GpsL1Acquirer acquirer)
	{
		this.acquirer = acquirer == null ? new GpsL1Acquirer() : acquirer;
	}

	public synchronized void start()
	{
		stop();
		rfSpectrum.reset();
		queue.clear();
		dropped.set(0);
		held.reset();
		lastHits = List.of();
		settleUntilMs = System.currentTimeMillis() + SETTLE_MS;
		run = true;
		worker = new Thread(this::loop, "gnss-sniff");
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
	}

	public void setRfSpectrumListener(Consumer<float[]> listener)
	{
		this.rfListener = listener;
	}

	public void setHitsListener(Consumer<List<GpsSvHit>> listener)
	{
		this.hitsListener = listener;
	}

	public IqSpectrum rfSpectrum()
	{
		return rfSpectrum;
	}

	public List<GpsSvHit> lastHits()
	{
		return lastHits;
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
		long lastAcq = 0;
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
			long now = System.currentTimeMillis();
			if (now < settleUntilMs || now - lastAcq < ACQUIRE_PERIOD_MS)
				continue;
			if (chunk.length < GpsL1Acquirer.SAMPLES_MS * 2)
				continue;
			lastAcq = now;
			int tail = chunk.length - GpsL1Acquirer.SAMPLES_MS * 2;
			byte[] ms = new byte[GpsL1Acquirer.SAMPLES_MS * 2];
			System.arraycopy(chunk, Math.max(0, tail) & ~1, ms, 0, ms.length);
			List<GpsSvHit> hits = acquirer.search(ms);
			if (!held.update(hits))
				continue;
			lastHits = held.last();
			Consumer<List<GpsSvHit>> out = hitsListener;
			if (out != null)
				out.accept(lastHits);
		}
	}

	/**
	 * Crop a parked FFT row to the GPS L1 view. Bin 0 is −fs/2.
	 */
	public static ViewRow cropGps(float[] row, float binHz, long loHz)
	{
		if (row == null || row.length == 0 || !(binHz > 0))
			return ViewRow.EMPTY;
		double loMHz = loHz / 1_000_000.0;
		int first = -1;
		int last = -1;
		for (int i = 0; i < row.length; i++)
		{
			double mhz = loMHz + (i - row.length / 2.0) * binHz / 1_000_000.0;
			if (mhz >= GnssBandPlan.GPS_VIEW_START_MHZ && mhz < GnssBandPlan.GPS_VIEW_END_MHZ)
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

	public static String hitsJson(List<GpsSvHit> hits)
	{
		List<GpsSvHit> list = hits == null ? List.of() : hits;
		StringBuilder sb = new StringBuilder(64 + list.size() * 80);
		sb.append("{\"loHz\":").append(LO_HZ);
		sb.append(",\"sampleRate\":").append(IQ_RATE_HZ);
		sb.append(",\"count\":").append(list.size());
		sb.append(",\"hits\":[");
		for (int i = 0; i < list.size(); i++)
		{
			if (i > 0)
				sb.append(',');
			sb.append(list.get(i).toJson());
		}
		sb.append("]}");
		return sb.toString();
	}

	public static List<GpsSvHit> copyHits(List<GpsSvHit> hits)
	{
		if (hits == null || hits.isEmpty())
			return List.of();
		return Collections.unmodifiableList(new ArrayList<GpsSvHit>(hits));
	}
}
