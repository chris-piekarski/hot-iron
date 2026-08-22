package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;

import hotiron.core.DatasetSpectrum;
import hotiron.core.EMA;

public class WaterfallPlot extends JPanel {
	/**
	 * 
	 */
	private static final long	serialVersionUID		= 3249110968962287324L;
	/** Audio waterfall dBFS window. Must not overwrite the RF palette — that
	 * palette also places the color bar on the spectrum Y-axis. */
	public static final double	AUDIO_PALETTE_START_DB	= -80;
	public static final double	AUDIO_PALETTE_SIZE_DB	= 80;
	private BufferedImage		bufferedImages[]		= new BufferedImage[2];
	private int					chartXOffset			= 0, chartWidth = 100;
	private boolean				displayMarker			= false;
	private double				displayMarkerFrequency	= 0;
	private int					displayMarkerX			= 0;
	private int					displayMarkerY			= 0;
	private int					drawIndex				= 0;
	/**
	 * stores max value in pixel
	 */
	private float				drawMaxBuffer[];
	private EMA					fps						= new EMA(3);
	private int					fpsRenderedFrames		= 0;
	private long				lastFPSRecalculated		= 0;
	private DatasetSpectrum		lastSpectrum			= null;
	private ColorPalette		palette					= new HotIronBluePalette();
	private Rectangle2D.Float	rect					= new Rectangle2D.Float(0f, 0f, 1f, 1f);
	private int					lastBinCount			= 0;
	private int					screenWidth;
	private double				spectrumPaletteSize		= 65;
	private double				spectrumPaletteStart	= -90;
	private long[]				rowEpochMs;
	private static final Color	TIME_AXIS_COLOR			= new Color(0xBB, 0xBB, 0xBB);
	private static final Color	BANNER_RF				= new Color(40, 40, 44, 210);
	private static final Color	BANNER_RF_TEXT			= new Color(200, 200, 204);
	private static final Color	BANNER_AUDIO			= new Color(255, 186, 64, 230);
	private static final Color	BANNER_AUDIO_TEXT		= new Color(20, 16, 8);
	private static final int	TIME_AXIS_MIN_GUTTER	= 28;
	private boolean				audioMode				= false;
	private boolean				videoMode				= false;
	private float				audioHzMax				= 16_000f;
	private double				videoCenterHz			= 0;
	private float				videoSpanHz				= 12_000_000f;
	private volatile BufferedImage videoStill;

	public WaterfallPlot(ChartPanel chartPanel, int maxHeight) {
		setPreferredSize(new Dimension(100, 200));
		setMinimumSize(new Dimension(100, 200));

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				setHistorySize(getHeight());
			}
		});

		screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		drawMaxBuffer = new float[screenWidth];

		bufferedImages[0] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, maxHeight);
		bufferedImages[1] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, maxHeight);
		rowEpochMs = new long[Math.max(1, maxHeight)];

		/**
		 * setup frequency marker
		 */
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				displayMarker = false;
				int x = e.getX();
				if (x < chartXOffset || x > chartXOffset + chartWidth) {
					return;
				}
				double freq = translateChartXToFrequency(x - chartXOffset);
				if (freq != -1) {
					displayMarker = true;
					displayMarkerFrequency = freq;
					displayMarkerX = x;
					displayMarkerY = e.getY();
				}
				WaterfallPlot.this.repaint();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				displayMarker = false;
			}
		});
	}

	private EMA newDataTimeEMA =	 new EMA(100);
	/**
	 * Adds new data to the waterfall plot and renders it
	 * 
	 * @param spectrum
	 */
	public synchronized void addNewData(DatasetSpectrum spectrum) {
		audioMode = false;
		videoMode = false;
		long start	= System.nanoTime();

		int size = spectrum.spectrumLength();
		double startFreq = spectrum.getFreqStartMHz() * 1000000d;
		double freqRange = (spectrum.getFreqStopMHz() - spectrum.getFreqStartMHz()) * 1000000d;
		double width = bufferedImages[0].getWidth();
		this.lastSpectrum = spectrum;

		/**
		 * shift image by one pixel down
		 */
		BufferedImage previousImage = bufferedImages[drawIndex];
		drawIndex = (drawIndex + 1) % 2;
		Graphics2D g = bufferedImages[drawIndex].createGraphics();
		g.drawImage(previousImage, 0, 1, null);
		g.setColor(Color.black);
		g.fillRect(0, 0, (int) width, 1);
		shiftRowTimes(System.currentTimeMillis());

		float binWidth = (float) (spectrum.getFFTBinSizeHz() / freqRange * width);
		rect.x = 0;
		rect.y = 0;
		rect.height = 0;
		rect.width = binWidth;

		float minimumValueDrawBuffer = -150;
		Arrays.fill(drawMaxBuffer, minimumValueDrawBuffer);

		/**
		 * draw in two passes - first determines maximum power for the pixel,
		 * second draws it
		 */
		if (true) {
			//optimized drawing
			double widthDivSize = (double)width / size;
			for (int i = 0; i < size; i++) {
				double power = spectrum.getPower(i);
				double percentagePower = normalizePower(power, spectrumPaletteStart, spectrumPaletteSize);
				int pixelX = clampPixelX((int) Math.round(widthDivSize * i), drawMaxBuffer.length);
				if (percentagePower > drawMaxBuffer[pixelX])
					drawMaxBuffer[pixelX] = (float) percentagePower;
			}
		} else {
			//unoptimized drawing
			for (int i = 0; i < size; i++) {
				double freq = spectrum.getFrequency(i);
				double power = spectrum.getPower(i);
				double percentageFreq = (freq - startFreq) / freqRange;
				double percentagePower = normalizePower(power, spectrumPaletteStart, spectrumPaletteSize);
				int pixelX = clampPixelX((int) Math.round(width * percentageFreq), drawMaxBuffer.length);
				if (percentagePower > drawMaxBuffer[pixelX])
					drawMaxBuffer[pixelX] = (float) percentagePower;
			}
		}

		/**
		 * fill in pixels that do not have power with last bin's color in order
		 * to smooth the spectrum
		 */
		Color lastValidColor = palette.getColor(0);
		for (int x = 0; x < drawMaxBuffer.length; x++) {
			Color color;
			if (drawMaxBuffer[x] == minimumValueDrawBuffer)
				color = lastValidColor;
			else {
				color = palette.getColorNormalized(drawMaxBuffer[x]);
				lastValidColor = color;
			}
			rect.x = x;
			g.setColor(color);
			g.draw(rect);
		}

		lastBinCount = size;
		fpsRenderedFrames++;
		if (System.currentTimeMillis() - lastFPSRecalculated > 1000) {
			double rawfps = fpsRenderedFrames / ((System.currentTimeMillis() - (double) lastFPSRecalculated) / 1000d);
			fps.addNewValue(rawfps);
			lastFPSRecalculated = System.currentTimeMillis();
			fpsRenderedFrames = 0;
		}
		g.dispose();

//		double time	= newDataTimeEMA.addNewValue(((System.nanoTime()-start)/1000));
//		System.out.println("draw "+(int)time+"us");

//		repaint();
	}

	/**
	 * Draws color palette into given area from bottom (0%) to top (100%)
	 * 
	 * @param g
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public void drawScale(Graphics2D g, int x, int y, int w, int h) {
		g = (Graphics2D) g.create(x, y, w, h);
		int step = 3;
		for (int i = 0; i < h; i += step) {
			Color c = palette.getColorNormalized(1 - (double) i / h);
			g.setColor(c);
			g.fillRect(0, i, w, step);
		}

		/**
		 * draw border around the scale
		 */
		int thickness = 2;
		g.setColor(Color.darkGray);
		g.fillRect(0, 0, w, thickness);
		g.fillRect(w - thickness, 0, thickness, h);
		g.fillRect(0, h - thickness, w, thickness);
		g.dispose();
	}

	public int getHistorySize() {
		return bufferedImages[0].getHeight();
	}

	public double getSpectrumPaletteSize() {
		return spectrumPaletteSize;
	}

	public double getSpectrumPaletteStart() {
		return spectrumPaletteStart;
	}

	public void setDrawingOffsets(int xOffsetLeft, int width) {
		this.chartXOffset = xOffsetLeft;
		this.chartWidth = width;
	}

	/**
	 * One row of audio dBFS vs Hz (0…{@code hzMax}), newest at the top.
	 * Call only while listen mode owns this panel.
	 */
	public synchronized void addAudioFrame(float[] db, float hzMax) {
		if (db == null || db.length == 0)
			return;
		audioMode = true;
		videoMode = false;
		audioHzMax = hzMax > 0 ? hzMax : 16_000f;
		lastSpectrum = null;
		addParkedDbRow(db);
	}

	/**
	 * One row of parked-IQ dBFS vs baseband (fftshifted −fs/2…+fs/2).
	 */
	public synchronized void addVideoFrame(float[] db, float spanHz, double centerHz) {
		if (db == null || db.length == 0)
			return;
		videoMode = true;
		audioMode = false;
		videoSpanHz = spanHz > 0 ? spanHz : 12_000_000f;
		videoCenterHz = centerHz;
		lastSpectrum = null;
		addParkedDbRow(db);
	}

	private void addParkedDbRow(float[] db) {
		int size = db.length;
		double width = bufferedImages[0].getWidth();

		BufferedImage previousImage = bufferedImages[drawIndex];
		drawIndex = (drawIndex + 1) % 2;
		Graphics2D g = bufferedImages[drawIndex].createGraphics();
		g.drawImage(previousImage, 0, 1, null);
		g.setColor(Color.black);
		g.fillRect(0, 0, (int) width, 1);
		shiftRowTimes(System.currentTimeMillis());

		rect.y = 0;
		rect.height = 0;
		rect.width = 1;
		float minimumValueDrawBuffer = -150;
		Arrays.fill(drawMaxBuffer, minimumValueDrawBuffer);
		double widthDivSize = (double) width / size;
		for (int i = 0; i < size; i++) {
			double percentagePower = normalizePower(db[i], AUDIO_PALETTE_START_DB, AUDIO_PALETTE_SIZE_DB);
			int pixelX = clampPixelX((int) Math.round(widthDivSize * i), drawMaxBuffer.length);
			if (percentagePower > drawMaxBuffer[pixelX])
				drawMaxBuffer[pixelX] = (float) percentagePower;
		}
		Color lastValidColor = palette.getColor(0);
		for (int x = 0; x < drawMaxBuffer.length; x++) {
			Color color;
			if (drawMaxBuffer[x] == minimumValueDrawBuffer)
				color = lastValidColor;
			else {
				color = palette.getColorNormalized(drawMaxBuffer[x]);
				lastValidColor = color;
			}
			rect.x = x;
			g.setColor(color);
			g.draw(rect);
		}
		lastBinCount = size;
		fpsRenderedFrames++;
		if (System.currentTimeMillis() - lastFPSRecalculated > 1000) {
			double rawfps = fpsRenderedFrames / ((System.currentTimeMillis() - (double) lastFPSRecalculated) / 1000d);
			fps.addNewValue(rawfps);
			lastFPSRecalculated = System.currentTimeMillis();
			fpsRenderedFrames = 0;
		}
		g.dispose();
	}

	public boolean isAudioMode() {
		return audioMode;
	}

	public boolean isVideoMode() {
		return videoMode;
	}

	public float getAudioHzMax() {
		return audioHzMax;
	}

	public synchronized void setAudioMode(boolean on) {
		if (audioMode == on && !videoMode)
			return;
		audioMode = on;
		if (on)
			videoMode = false;
		clearHistory();
	}

	public synchronized void setVideoMode(boolean on, double centerHz) {
		boolean was = videoMode;
		videoMode = on;
		if (on)
		{
			audioMode = false;
			videoCenterHz = centerHz;
			videoSpanHz = hotiron.core.IqSpectrum.DISPLAY_HZ;
		}
		else
			videoStill = null;
		if (was != on)
			clearHistory();
	}

	public void setVideoStill(BufferedImage img) {
		videoStill = img;
		repaint();
	}

	public double getLastRbwHz() {
		if (videoMode)
			return videoSpanHz / Math.max(1, lastBinCount);
		if (audioMode)
			return audioHzMax / Math.max(1, lastBinCount);
		return lastSpectrum == null ? 0 : lastSpectrum.getFFTBinSizeHz();
	}

	public int getLastBinCount() {
		return lastBinCount;
	}

	public double getFps() {
		return fps.getEma();
	}

	/** Drop scrolled history (used on retune so old MHz mapping is not reused). */
	public synchronized void clearHistory() {
		for (BufferedImage image : bufferedImages) {
			if (image == null)
				continue;
			Graphics2D g = image.createGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.dispose();
		}
		lastSpectrum = null;
		lastBinCount = 0;
		if (rowEpochMs != null)
			Arrays.fill(rowEpochMs, 0L);
	}

	public synchronized void setHistorySize(int historyInPixels) {
		BufferedImage bufferedImages[] = new BufferedImage[2];
		bufferedImages[0] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, historyInPixels);
		bufferedImages[1] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, historyInPixels);
		copyImage(this.bufferedImages[0], bufferedImages[0]);
		copyImage(this.bufferedImages[1], bufferedImages[1]);
		this.bufferedImages = bufferedImages;
		ensureRowTimes(Math.max(1, historyInPixels));
	}

	public void setSpectrumPaletteSize(int dB) {
		this.spectrumPaletteSize = dB;
	}

	/**
	 * Sets start and end of the color scale
	 * 
	 * @param minFreqency
	 * @param maxFrequency
	 */
	public void setSpectrumPaletteStart(int dB) {
		this.spectrumPaletteStart = dB;
	}

	/**
	 * Map the waterfall palette onto a live dB window (same bounds as the
	 * spectrum Y-axis) so a 15 dB FM band is not crushed into the blue
	 * third of a fixed −90…−25 scale.
	 */
	public void applyPowerWindow(double lowDb, double highDb) {
		setSpectrumPaletteStart(paletteStartDb(lowDb));
		setSpectrumPaletteSize(paletteSizeDb(lowDb, highDb));
	}

	public static int paletteStartDb(double lowDb) {
		return (int) Math.round(lowDb);
	}

	public static int paletteSizeDb(double lowDb, double highDb) {
		int span = (int) Math.round(highDb - lowDb);
		return Math.max(1, span);
	}

	private void ensureRowTimes(int historyInPixels) {
		int n = Math.max(1, historyInPixels);
		if (rowEpochMs != null && rowEpochMs.length == n)
			return;
		long[] next = new long[n];
		if (rowEpochMs != null)
			System.arraycopy(rowEpochMs, 0, next, 0, Math.min(rowEpochMs.length, n));
		rowEpochMs = next;
	}

	private void shiftRowTimes(long nowMs) {
		int hist = bufferedImages[drawIndex].getHeight();
		ensureRowTimes(hist);
		if (rowEpochMs.length > 1)
			System.arraycopy(rowEpochMs, 0, rowEpochMs, 1, rowEpochMs.length - 1);
		rowEpochMs[0] = nowMs;
	}

	private void copyImage(BufferedImage src, BufferedImage dst) {
		Graphics2D g = dst.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
	}

	/**
	 * Maps a power sample onto the waterfall palette, 0 (at or below start) to 1 (at or above start+size).
	 */
	public static double normalizePower(double power, double paletteStart, double paletteSize) {
		if (paletteSize <= 0)
			return 0;
		if (power <= paletteStart)
			return 0;
		if (power >= paletteStart + paletteSize)
			return 1;
		return (power - paletteStart) / paletteSize;
	}

	public static int clampPixelX(int pixelX, int bufferLength) {
		if (bufferLength <= 0)
			return 0;
		if (pixelX >= bufferLength)
			return bufferLength - 1;
		if (pixelX < 0)
			return 0;
		return pixelX;
	}

	public static double translateXToFrequency(int x, int chartWidth, double startFreqHz, double stopFreqHz) {
		if (chartWidth <= 0)
			return -1;
		hotiron.core.FrequencyAxis axis = hotiron.core.FrequencyAxis.of(startFreqHz / 1_000_000d,
				stopFreqHz / 1_000_000d, chartWidth);
		return axis.xToMhz(x) * 1_000_000d;
	}

	private double translateChartXToFrequency(int x) {
		if (videoMode) {
			if (chartWidth <= 0)
				return -1;
			double u = x / (double) chartWidth;
			if (u < 0)
				u = 0;
			if (u > 1)
				u = 1;
			return videoCenterHz + (u - 0.5) * videoSpanHz;
		}
		if (audioMode) {
			if (chartWidth <= 0)
				return -1;
			double u = x / (double) chartWidth;
			if (u < 0)
				u = 0;
			if (u > 1)
				u = 1;
			return u * audioHzMax;
		}
		if (lastSpectrum != null) {
			double startFreq = lastSpectrum.getFreqStartMHz() * 1000000d;
			double stopFreq = lastSpectrum.getFreqStopMHz() * 1000000d;
			return translateXToFrequency(x, chartWidth, startFreq, stopFreq);
		}
		return -1;
	}

	public static String modeBanner(boolean audio) {
		return modeBanner(audio, false);
	}

	public static String modeBanner(boolean audio, boolean video) {
		if (video)
			return "VIDEO  ·  ±8 MHz";
		if (audio)
			return "AUDIO  ·  0–16 kHz";
		return "RF waterfall";
	}

	static String formatAudioHz(double hz) {
		if (!(hz >= 0) || Double.isNaN(hz) || Double.isInfinite(hz))
			return "—";
		if (hz >= 1000)
			return String.format("%.1f kHz", hz / 1000.0);
		return String.format("%.0f Hz", hz);
	}

	private void drawModeBanner(Graphics2D g, int x0) {
		String title = modeBanner(audioMode, videoMode);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = getFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, 12) : getFont().deriveFont(Font.BOLD, 12f);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int padX = 8;
		int padY = 3;
		int tw = fm.stringWidth(title);
		int th = fm.getAscent() + fm.getDescent();
		int x = x0 + 8;
		int y = 8;
		boolean gold = audioMode || videoMode;
		g.setColor(gold ? BANNER_AUDIO : BANNER_RF);
		g.fillRoundRect(x, y, tw + padX * 2, th + padY * 2, 8, 8);
		g.setColor(gold ? BANNER_AUDIO_TEXT : BANNER_RF_TEXT);
		g.drawString(title, x + padX, y + padY + fm.getAscent());
	}

	private void drawAudioHzAxis(Graphics2D g, int x0, int w, int h) {
		if (w < 40 || h < 16)
			return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 11) : getFont().deriveFont(Font.PLAIN, 11f);
		g.setFont(font);
		g.setColor(TIME_AXIS_COLOR);
		int y = h - 2;
		g.drawLine(x0, y - 10, x0 + w, y - 10);
		float[] ticks = { 0, 4000, 8000, 12000, 16000 };
		FontMetrics fm = g.getFontMetrics();
		for (float hz : ticks) {
			if (hz > audioHzMax + 1)
				continue;
			int x = x0 + (int) Math.round(w * (hz / audioHzMax));
			g.drawLine(x, y - 14, x, y - 10);
			String lab = hz == 0 ? "0" : String.format("%.0fk", hz / 1000f);
			int tw = fm.stringWidth(lab);
			int tx = x - tw / 2;
			if (tx < x0)
				tx = x0;
			if (tx + tw > x0 + w)
				tx = x0 + w - tw;
			g.drawString(lab, tx, y);
		}
	}

	private void drawVideoMhzAxis(Graphics2D g, int x0, int w, int h) {
		if (w < 40 || h < 16)
			return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 11) : getFont().deriveFont(Font.PLAIN, 11f);
		g.setFont(font);
		g.setColor(TIME_AXIS_COLOR);
		int y = h - 2;
		g.drawLine(x0, y - 10, x0 + w, y - 10);
		float half = videoSpanHz / 2f;
		float[] ticks = { -half, -half / 2f, 0, half / 2f, half };
		FontMetrics fm = g.getFontMetrics();
		for (float off : ticks) {
			int x = x0 + (int) Math.round(w * ((off + half) / videoSpanHz));
			g.drawLine(x, y - 14, x, y - 10);
			String lab;
			if (off == 0)
				lab = "0";
			else
				lab = String.format("%+.0f", off / 1e6f);
			int tw = fm.stringWidth(lab);
			int tx = x - tw / 2;
			if (tx < x0)
				tx = x0;
			if (tx + tw > x0 + w)
				tx = x0 + w - tw;
			g.drawString(lab, tx, y);
		}
	}

	private void drawVideoStill(Graphics2D g, int x0, int w, int h) {
		BufferedImage still = videoStill;
		if (still == null || still.getWidth() < 2 || w < 80 || h < 60)
			return;
		int pw = Math.min(w / 3, 280);
		int ph = (int) Math.round(pw * (still.getHeight() / (double) still.getWidth()));
		if (ph > h / 2)
		{
			ph = h / 2;
			pw = (int) Math.round(ph * (still.getWidth() / (double) still.getHeight()));
		}
		int px = x0 + w - pw - 8;
		int py = 28;
		g.drawImage(still, px, py, pw, ph, null);
		g.setColor(BANNER_AUDIO);
		g.drawRect(px - 1, py - 1, pw + 1, ph + 1);
	}

	void drawTimeAxis(Graphics2D g, long[] times, int height) {
		int gutter = chartXOffset;
		if (gutter < TIME_AXIS_MIN_GUTTER || height < 8)
			return;
		List<WaterfallTimeScale.Tick> ticks = WaterfallTimeScale.ticks(times, height,
				WaterfallTimeScale.DEFAULT_MAX_TICKS);
		if (ticks.isEmpty())
			return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 11) : getFont().deriveFont(Font.PLAIN, 11f);
		g.setFont(font);
		g.setColor(TIME_AXIS_COLOR);
		FontMetrics fm = g.getFontMetrics();
		int axisX = gutter - 1;
		g.drawLine(axisX, 0, axisX, height);
		int ascent = fm.getAscent();
		for (WaterfallTimeScale.Tick tick : ticks) {
			int y = tick.y;
			if (y < 0)
				y = 0;
			if (y > height - 1)
				y = height - 1;
			g.drawLine(axisX - 4, y, axisX, y);
			int tw = fm.stringWidth(tick.label);
			int tx = axisX - 6 - tw;
			if (tx < 2)
				tx = 2;
			int ty = y + ascent / 2;
			if (ty < ascent)
				ty = ascent;
			if (ty > height - 2)
				ty = height - 2;
			g.drawString(tick.label, tx, ty);
		}
	}

	@Override
	protected void paintComponent(Graphics arg0) {
		long drawStart	= System.nanoTime();
		Graphics2D g = (Graphics2D) arg0;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int w = chartWidth;
		int h = getHeight();
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.drawImage(bufferedImages[drawIndex], chartXOffset, 0, w, h, null);

		long[] times;
		synchronized (this) {
			times = rowEpochMs == null ? null : rowEpochMs.clone();
		}
		drawTimeAxis(g, times, h);

		drawModeBanner(g, chartXOffset);
		if (audioMode)
			drawAudioHzAxis(g, chartXOffset, w, h);
		else if (videoMode)
		{
			drawVideoMhzAxis(g, chartXOffset, w, h);
			drawVideoStill(g, chartXOffset, w, h);
		}

		if (displayMarker) {
			g.setColor(Color.gray);
			g.drawLine(displayMarkerX, 0, displayMarkerX, h);
			double age = WaterfallTimeScale.ageAtY(times, h, displayMarkerY);
			String hz = audioMode ? formatAudioHz(displayMarkerFrequency)
					: String.format("%.1f MHz", displayMarkerFrequency / 1000000.0);
			g.drawString(hz + "  " + WaterfallTimeScale.formatAge(age), displayMarkerX + 5,
					Math.max(14, displayMarkerY - 6));
		} 

		long drawingTime	= System.nanoTime()-drawStart;
		drawingTimeSum	+= drawingTime;
		drawingCounter++;
	}
	private volatile long drawingTimeSum	= 0;
	private volatile int drawingCounter	= 0;
	public int getDrawingCounterAndReset() {
		int val	= drawingCounter;
		drawingCounter	= 0;
		return val;
	}
	/**
	 * Retrieves time in nanos the component spent in drawing itself and resets
	 * the counter to zero.
	 * @return
	 */
	public long getDrawTimeSumAndReset() {
		long val	= drawingTimeSum;
		drawingTimeSum	= 0;
		return val;
	}
}
