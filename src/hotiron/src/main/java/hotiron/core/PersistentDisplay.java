package hotiron.core;

import java.awt.Color;
import java.awt.image.BufferedImage;

import hotiron.ui.HotIronBluePalette;
import hotiron.mvc.ModelValue;

public class PersistentDisplay {
	/**
	 * Image represented by single float array
	 */
	private static class FloatImage {
		private final float[]	data;
		private final int		width, height;

		public FloatImage(int width, int height) {
			data = new float[width * height];
			this.width = width;
			this.height = height;
		}

		public float add(int x, int y, float power) {
			return data[y * width + x] += power;
		}

		public float get(int x, int y) {
			return data[y * width + x];
		}

		public int getIndex(int x, int y) {
			return y * width + x;
		}

		public void multiplyAllValues(float value) {
			for (int i = 0; i < data.length; i++) {
				data[i] *= value;
			}
		}

		public void set(int x, int y, float value) {
			data[y * width + x] = value;
		}

		public void subtractAllValues(float value) {
			for (int i = 0; i < data.length; i++) {
				data[i] -= value;
			}
		}
	}

	public static float map(float in, float in_min, float in_max, float out_min, float out_max) {
		return (in - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}

	public static int map(int x, int in_min, int in_max, int out_min, int out_max) {
		return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}

	private boolean						calibrated			= false;
	private boolean						calibrating			= false;
	private long						calibrationStarted	= 0;
	private final long					calibrationTime		= 1000;
	private final Object				imageLock			= new Object();
	private BufferedImage				drawImage;
	private ModelValue<BufferedImage>	displayImage		= new ModelValue<BufferedImage>("", null);
	private FloatImage					imagePowerAccumulated;
	private int							incomingDataCounter	= 0;
	private HotIronBluePalette			palette				= new HotIronBluePalette();
	private int							persistenceTimeSecs	= 5;
	private float						updatesPerSecond	= 1;
	private long						lastDecayMillis		= 0;
	private long						flushUntilMillis	= 0;
	private static final long			PAUSE_DECAY_SKIP_MS	= 2000;
	static final long					FLUSH_HALF_LIFE_MS	= 55;
	static final long					FLUSH_MAX_MS		= 350;
	private static final float			ZERO_THRESHOLD		= 0.01f;

	public PersistentDisplay() {
		setImageSize(320, 240);
	}

	public void beginFlush() {
		beginFlush(System.currentTimeMillis());
	}

	void beginFlush(long nowMs) {
		flushUntilMillis = nowMs + FLUSH_MAX_MS;
		lastDecayMillis = nowMs;
	}

	public boolean isFlushing() {
		return isFlushing(System.currentTimeMillis());
	}

	boolean isFlushing(long nowMs) {
		return flushUntilMillis > 0 && nowMs < flushUntilMillis;
	}

	/**
	 * Fast-fade the overlay toward black. Returns true while the flush
	 * still has frames to show. Does not accumulate new spectrum.
	 */
	public boolean tickFlush(long nowMs) {
		BufferedImage image;
		FloatImage accum;
		synchronized (imageLock) {
			image = this.drawImage;
			accum = this.imagePowerAccumulated;
			if (image == null || accum == null) {
				flushUntilMillis = 0;
				return false;
			}
			if (lastDecayMillis <= 0)
				lastDecayMillis = nowMs;
			long dt = nowMs - lastDecayMillis;
			lastDecayMillis = nowMs;
			if (dt > 0)
				accum.multiplyAllValues((float) EMA.decayFactor(dt, FLUSH_HALF_LIFE_MS));
			boolean done = nowMs >= flushUntilMillis || max(accum.data) < ZERO_THRESHOLD;
			if (done) {
				java.util.Arrays.fill(accum.data, 0);
				flushUntilMillis = 0;
			}
			renderLocked(image, accum);
		}
		publishImage(image);
		return flushUntilMillis > 0;
	}

	public void drawSpectrumFloat(DatasetSpectrum datasetSpectrum, float yMin, float yMax, boolean renderImage) {
		if (flushUntilMillis > 0)
			return;
		if (!calibrated) {
			if (!calibrating) {
				calibrating = true;
				calibrationStarted = System.currentTimeMillis();
				incomingDataCounter = 0;
			} else {
				incomingDataCounter++;
				long t = System.currentTimeMillis() - calibrationStarted;
				if (t >= calibrationTime) {
					updatesPerSecond = (float) incomingDataCounter / (t / 1000f);
					int bins = (int) ((datasetSpectrum.getFreqStopMHz() - datasetSpectrum.getFreqStartMHz()) * 1000000l
							/ datasetSpectrum.getFFTBinSizeHz());
					BufferedImage image = displayImage.getValue();
					if (image != null && bins < image.getWidth()) {
						setImageSize(bins, image.getHeight());
					}
					calibrated = true;
					calibrating = false;

					if (updatesPerSecond < 1)
						updatesPerSecond = 1;
				}
			}
			return;
		}

		BufferedImage image;
		FloatImage imagePowerAccumulated;
		synchronized (imageLock) {
			image = this.drawImage;
			imagePowerAccumulated = this.imagePowerAccumulated;
		}

		if (image == null || imagePowerAccumulated == null)
			return;

		long now = System.currentTimeMillis();
		if (lastDecayMillis <= 0)
			lastDecayMillis = now;
		long dt = now - lastDecayMillis;
		if (dt > 0 && dt <= PAUSE_DECAY_SKIP_MS)
		{
			imagePowerAccumulated.multiplyAllValues(
					(float) EMA.decayFactor(dt, persistenceTimeSecs * 1000L));
			updatesPerSecond = 0.8f * updatesPerSecond + 0.2f * (1000f / dt);
		}
		lastDecayMillis = now;

		float[] spectrum = datasetSpectrum.getSpectrumArray();
		int width = image.getWidth();
		int height = image.getHeight();
		float hDivYRange = (-height) / (yMax - yMin);

		/**
		 * pipeline: float image accumulates power for each pixel, then the
		 * power value gets converted to color based on the hot iron palette
		 */
		float maxAccumulatedValue = updatesPerSecond * persistenceTimeSecs;
		for (int i = 0; i < spectrum.length; i++) {
			float power = spectrum[i];
			if (DatasetSpectrum.isChartHole(power))
				continue;
			float powerLin = 1; /*
								 * each occurence of power value at given
								 * frequency is simply +1
								 */

			int x = i * width / spectrum.length;
			int y = //(power - yMin) * (0 - height) / (yMax - yMin) + height; 
					(int) ((power - yMin) * hDivYRange
							+ height); /* optimized map() */

			if (x >= 0 && y >= 0 && x < width && y < height) {
				int index = imagePowerAccumulated.getIndex(x, y);
				if (imagePowerAccumulated.data[index] < maxAccumulatedValue)
					imagePowerAccumulated.data[index] += powerLin;
			}
		}

		/**
		 * render image only when requested
		 */
		if (renderImage) {
			renderLocked(image, imagePowerAccumulated);
			publishImage(image);
		}
	}

	private void renderLocked(BufferedImage image, FloatImage accum) {
		float[] raw = accum.data;
		float maxValue = Float.MIN_NORMAL;
		for (int i = 0; i < raw.length; i++) {
			if (raw[i] > maxValue)
				maxValue = raw[i];
		}
		int width = image.getWidth();
		int height = image.getHeight();
		float minOutToLog = 1.0f;
		float maxOutToLog = 100;
		float logMin = (float) Math.log10(minOutToLog);
		float logMax = (float) Math.log10(maxOutToLog);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				float val = accum.get(x, y);
				if (val < ZERO_THRESHOLD) {
					accum.set(x, y, 0);
					val = 0;
				}
				if (val == 0)
					image.setRGB(x, y, Color.black.getRGB());
				else {
					float outPower = (float) Math.log10(map(val, 0, maxValue, minOutToLog, maxOutToLog));
					float normalized = map(outPower, logMin, logMax, 0.15f, 0.95f);
					image.setRGB(x, y, palette.getColorNormalized(normalized).getRGB());
				}
			}
		}
	}

	private static float max(float[] data) {
		float m = 0;
		for (float v : data) {
			if (v > m)
				m = v;
		}
		return m;
	}

	/** Chart/EDT only ever sees this copy; {@code image} may be written again. */
	private void publishImage(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage published = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.copyData(published.getRaster());
		displayImage.setValue(published);
	}

	public ModelValue<BufferedImage> getDisplayImage() {
		return displayImage;
	}

	public int getPersistenceTime() {
		return persistenceTimeSecs;
	}

	public void reset() {
		BufferedImage image = displayImage.getValue();
		if (image != null) {
			setImageSize(image.getWidth(), image.getHeight());
		}
	}

	public void setImageSize(int width, int height) {
		if (width < 1 || height < 1)
			return;

		calibrated = false;
		calibrating = false;
		lastDecayMillis = 0;
		flushUntilMillis = 0;

		System.out.println("Persistent image set to " + width + "x" + height);
		// Heap INT_RGB: compatible/accelerated images crash in libawt when
		// setRGB races ChartPanel paint (BufImg_GetRasInfo).
		BufferedImage next = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		synchronized (imageLock) {
			drawImage = next;
			imagePowerAccumulated = new FloatImage(width, height);
		}
		displayImage.setValue(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
	}

	public void setPersistenceTime(int persistenceTimeSecs) {
		this.persistenceTimeSecs = persistenceTimeSecs;
	}

	float maxAccumulated() {
		synchronized (imageLock) {
			return imagePowerAccumulated == null ? 0 : max(imagePowerAccumulated.data);
		}
	}
}
