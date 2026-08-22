package hotiron.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import hotiron.nativebridge.HackRFSweepDataCallback;
import hotiron.nativebridge.HackRFSweepNativeBridge;

/**
 * Headless sweep + processing path used by
 * {@code HotIron}: native start → queue →
 * {@link DatasetSpectrumPeak}. UI (chart / waterfall) is optional via
 * {@link Hooks}.
 */
public class SpectrumSweepEngine implements HackRFSweepDataCallback {
	public interface Hooks {
		void onPacketAccepted();

		void onFirstDataset(DatasetSpectrumPeak dataset, float fftBinHz);

		void onFullSweepProcessed(DatasetSpectrumPeak dataset);
	}

	public static final Hooks NOOP_HOOKS = new Hooks() {
		@Override
		public void onPacketAccepted() {
		}

		@Override
		public void onFirstDataset(DatasetSpectrumPeak dataset, float fftBinHz) {
		}

		@Override
		public void onFullSweepProcessed(DatasetSpectrumPeak dataset) {
		}
	};

	private static final int QUEUE_CAPACITY = 1000;
	private static final float DEFAULT_INIT_POWER = -150f;

	private final HackRFSettings settings;
	private final float spectrumInitValue;
	private final Hooks hooks;
	private final ArrayBlockingQueue<FFTBins> queue = new ArrayBlockingQueue<FFTBins>(QUEUE_CAPACITY);
	private final AtomicInteger acceptedPackets = new AtomicInteger();
	private final AtomicInteger droppedPackets = new AtomicInteger();
	private final AtomicInteger processedPackets = new AtomicInteger();
	private final ReentrantLock sweepLock = new ReentrantLock();

	private volatile boolean forceStop;
	private volatile boolean nativeSweepActive;
	private volatile DatasetSpectrumPeak dataset;
	private volatile SpurFilter spurFilter;
	private Thread processingThread;
	private Thread sweepThread;

	public SpectrumSweepEngine(HackRFSettings settings) {
		this(settings, DEFAULT_INIT_POWER, NOOP_HOOKS);
	}

	public SpectrumSweepEngine(HackRFSettings settings, float spectrumInitValue, Hooks hooks) {
		if (settings == null)
			throw new IllegalArgumentException("settings");
		this.settings = settings;
		this.spectrumInitValue = spectrumInitValue;
		this.hooks = hooks == null ? NOOP_HOOKS : hooks;
	}

	@Override
	public void newSpectrumData(boolean fullSweepDone, double[] frequencyStart, float fftBinWidthHz,
			float[] signalPowerdBm) {
		accept(fullSweepDone, frequencyStart, fftBinWidthHz, signalPowerdBm);
	}

	public void accept(boolean fullSweepDone, double[] frequencyStart, float fftBinWidthHz, float[] signalPowerdBm) {
		if (!queue.offer(new FFTBins(fullSweepDone, frequencyStart, fftBinWidthHz, signalPowerdBm))) {
			droppedPackets.incrementAndGet();
			return;
		}
		acceptedPackets.incrementAndGet();
		hooks.onPacketAccepted();
	}

	public int queuedPackets() {
		return queue.size();
	}

	public int acceptedPackets() {
		return acceptedPackets.get();
	}

	public int droppedPackets() {
		return droppedPackets.get();
	}

	public int processedPackets() {
		return processedPackets.get();
	}

	public DatasetSpectrumPeak getDataset() {
		return dataset;
	}

	public SpurFilter getSpurFilter() {
		return spurFilter;
	}

	public boolean isStopRequested() {
		return forceStop;
	}

	public void requestStop() {
		forceStop = true;
		if (nativeSweepActive)
			HackRFSweepNativeBridge.stop();
		if (processingThread != null)
			processingThread.interrupt();
	}

	public void clearStop() {
		forceStop = false;
	}

	/**
	 * Blocking processing loop. Returns on interrupt or {@link #requestStop()}.
	 * First queued packet sizes the dataset; later packets update it.
	 */
	public void runProcessingLoop() {
		FFTBins first;
		try {
			first = queue.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		float binHz = first.fftBinWidthHz;
		FrequencyRange range = settings.getFrequency().getValue();
		dataset = new DatasetSpectrumPeak(binHz, range.getStartMHz(), range.getEndMHz(), spectrumInitValue, 15,
				settings.getPeakFallRate().getValue() * 1000L);
		spurFilter = new SpurFilter(6f, 4f, 4, 25, dataset);
		hooks.onFirstDataset(dataset, binHz);

		while (!forceStop) {
			try {
				FFTBins bins = queue.take();
				if (settings.isCapturingPaused().getValue())
					continue;
				if (bins.freqStart != null && bins.sigPowdBm != null) {
					dataset.addNewData(bins);
					processedPackets.incrementAndGet();
				}
				if (bins.fullSweepDone) {
					if (settings.isSpurRemoval().getValue())
						spurFilter.filterDataset();
					if (settings.isChartsPeaksVisible().getValue())
						dataset.refreshPeakSpectrum();
					hooks.onFullSweepProcessed(dataset);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/** Blocking native sweep until {@link #requestStop()} or the native start returns. */
	public void runSweepLoop() {
		sweepLock.lock();
		nativeSweepActive = true;
		try {
			if (forceStop)
				return;
			FrequencyRange range = settings.getFrequency().getValue().forInterleavedNativeSweep();
			HackRFSweepNativeBridge.configure(settings.getSelectedSerial().getValue(),
					settings.getClkoutEnable().getValue());
			HackRFSweepNativeBridge.start(this, range.getStartMHz(), range.getEndMHz(),
					settings.getFFTBinHz().getValue(), settings.getSamples().getValue(),
					settings.getGainLNA().getValue(), settings.getGainVGA().getValue(),
					settings.getAntennaPowerEnable().getValue(), settings.getAntennaLNA().getValue());
		} finally {
			nativeSweepActive = false;
			sweepLock.unlock();
		}
	}

	public void startBackgroundThreads() {
		clearStop();
		processingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				runProcessingLoop();
			}
		}, "hackrf_sweep data processing thread");
		sweepThread = new Thread(new Runnable() {
			@Override
			public void run() {
				runSweepLoop();
			}
		}, "hackrf_sweep");
		processingThread.start();
		sweepThread.start();
	}

	public void stopAndJoin(long timeoutMs) {
		requestStop();
		joinQuietly(sweepThread, timeoutMs);
		joinQuietly(processingThread, timeoutMs);
		sweepThread = null;
		processingThread = null;
	}

	private static void joinQuietly(Thread t, long timeoutMs) {
		if (t == null)
			return;
		try {
			t.join(timeoutMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
