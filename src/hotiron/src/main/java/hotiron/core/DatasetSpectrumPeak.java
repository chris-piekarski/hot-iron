package hotiron.core;

import java.util.Arrays;

import hotiron.core.jfc.XYSeriesImmutable;

public class DatasetSpectrumPeak extends DatasetSpectrum
{
	protected long		lastAdded			= System.currentTimeMillis();
	protected long		peakFalloutMillis	= 1000;
	protected float		peakFallThreshold;
	/**
	 * Decaying peak (same samples as {@link #spectrumPeakHold}).
	 */
	protected float[]	spectrumPeak;

	/**
	 * Plotted peak hold: snaps up to a new high, then falls toward live
	 * with half-life {@link #peakFalloutMillis}.
	 */
	protected float[]	spectrumPeakHold;
	
	public DatasetSpectrumPeak(float fftBinSizeHz, int freqStartMHz, int freqStopMHz, float spectrumInitPower, float peakFallThreshold, long peakFalloutMillis)
	{
		super(fftBinSizeHz, freqStartMHz, freqStopMHz, spectrumInitPower);
		initPeaks(spectrumInitPower, peakFallThreshold, peakFalloutMillis);
	}

	public DatasetSpectrumPeak(float fftBinSizeHz, long freqStartHz, int binCount, float spectrumInitPower,
			float peakFallThreshold, long peakFalloutMillis)
	{
		super(fftBinSizeHz, freqStartHz, binCount, spectrumInitPower);
		initPeaks(spectrumInitPower, peakFallThreshold, peakFalloutMillis);
	}

	private void initPeaks(float spectrumInitPower, float peakFallThreshold, long peakFalloutMillis)
	{
		this.peakFalloutMillis = peakFalloutMillis;
		this.spectrumInitPower = spectrumInitPower;
		this.peakFallThreshold = peakFallThreshold;
		spectrumPeak = new float[spectrum.length];
		Arrays.fill(spectrumPeak, spectrumInitPower);
		spectrumPeakHold = new float[spectrum.length];
		Arrays.fill(spectrumPeakHold, spectrumInitPower);
	}

	/**
	 * Copy a parked-IQ FFT into the live bins and age the peak hold.
	 * Reuses {@code current} when the Hz axis matches.
	 */
	public static DatasetSpectrumPeak ingestParkedFrame(DatasetSpectrumPeak current, float[] mhz, float[] dbfs,
			float binHz, long peakFalloutMillis)
	{
		if (mhz == null || dbfs == null || mhz.length == 0 || mhz.length != dbfs.length || !(binHz > 0f))
			return current;
		long startHz = Math.round((double) mhz[0] * 1_000_000d);
		DatasetSpectrumPeak ds = current;
		if (ds == null || ds.getFreqStartHz() != startHz || ds.spectrumLength() != dbfs.length
				|| ds.getFFTBinSizeHz() != binHz)
			ds = new DatasetSpectrumPeak(binHz, startHz, dbfs.length, -150f, 15f, peakFalloutMillis);
		else
			ds.setPeakFalloutMillis(peakFalloutMillis);
		System.arraycopy(dbfs, 0, ds.spectrum, 0, dbfs.length);
		ds.refreshPeakSpectrum();
		return ds;
	}

	public void setPeakFalloutMillis(long peakFalloutMillis) {
		this.peakFalloutMillis = peakFalloutMillis;
	}
	
	public void copyTo(DatasetSpectrumPeak filtered)
	{
		super.copyTo(filtered);
		System.arraycopy(spectrumPeak, 0, filtered.spectrumPeak, 0, spectrumPeak.length);
		System.arraycopy(spectrumPeakHold, 0, filtered.spectrumPeakHold, 0, spectrumPeakHold.length);
	}

	public XYSeriesImmutable createPeaksDataset(String name) {
		return createPeaksDataset(name, Integer.MAX_VALUE);
	}

	public XYSeriesImmutable createPeaksDataset(String name, int maxPoints) {
		return toChartSeries(name, spectrumPeakHold, maxPoints);
	}

	public double calculateSpectrumPeakPower(){
		double powerSum	= 0;
		for (int i = 0; i < spectrumPeakHold.length; i++) {
			powerSum	+= Math.pow(10, spectrumPeakHold[i]/10); /*convert dB to mW to sum power in linear form*/
		}
		powerSum	= 10*Math.log10(powerSum); /*convert back to dB*/ 
		return powerSum;
	}
	
	public void refreshPeakSpectrum()
	{
		long now = System.currentTimeMillis();
		long dt = now - lastAdded;
		if (dt < 1)
			dt = 1;
		lastAdded = now;

		for (int spectrIndex = 0; spectrIndex < spectrum.length; spectrIndex++)
		{
			float live = spectrum[spectrIndex];
			if (isChartHole(live))
				continue;
			float next = EMA.decayToward(live, spectrumPeakHold[spectrIndex], dt, peakFalloutMillis);
			spectrumPeakHold[spectrIndex] = next;
			spectrumPeak[spectrIndex] = next;
		}
	}

	public void resetPeaks()
	{
		Arrays.fill(spectrumPeak, spectrumInitPower);
		Arrays.fill(spectrumPeakHold, spectrumInitPower);
	}

	@Override protected Object clone() throws CloneNotSupportedException
	{
		DatasetSpectrumPeak copy = (DatasetSpectrumPeak) super.clone();
		copy.spectrumPeakHold = spectrumPeakHold.clone();
		copy.spectrumPeak = spectrumPeak.clone();
		return super.clone();
	}

}
