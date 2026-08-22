package hotiron.core;

import java.util.ArrayList;
import java.util.Arrays;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import hotiron.core.jfc.XYSeriesImmutable;

public class DatasetSpectrum implements Cloneable
{
	/**
	 * caching decreases GC usage
	 */
	private final boolean useCached	= false;
	protected ArrayList<ArrayList<XYDataItem>> cachedDataItems	= new ArrayList<>();
	protected int cachedDataItemsIndex	= 0;
	protected  final float	fftBinSizeHz;

	protected  final long	freqStartHz;
	protected  final int	freqStartMHz;
	
	protected  final int	freqStopMHz;
	protected  float[]		spectrum;
	protected  float		spectrumInitPower;
	private float[]			cachedFreqMHz;
	
	/**
	 * Inits
	 * @param fftBinSizeHz
	 * @param freqStartMHz
	 * @param freqStopMHz
	 * @param spectrumInitPower 
	 * @param peaks enable calculation of peaks
	 * @param peakFallThreshold
	 * @param peakFalloutMillis
	 */
	public DatasetSpectrum(float fftBinSizeHz, int freqStartMHz, int freqStopMHz, float spectrumInitPower)
	{
		this.fftBinSizeHz = fftBinSizeHz;
		this.freqStartMHz = freqStartMHz;
		this.freqStartHz = freqStartMHz * 1000000l;
		this.freqStopMHz = freqStopMHz;
		this.spectrumInitPower = spectrumInitPower;
		int datapoints = (int) (Math.ceil(freqStopMHz - freqStartMHz) * 1000000d / fftBinSizeHz);
		spectrum = new float[datapoints];
		Arrays.fill(spectrum, spectrumInitPower);

		if (useCached) {
			for (int j = 0; j < 5; j++) {
				ArrayList<XYDataItem> list	= new ArrayList<>();
				for (int i = 0; i < datapoints; i++) {
					double freq = (freqStartHz + fftBinSizeHz * i) / 1000000;
					list.add(new XYDataItem(freq, 0));
				}
				cachedDataItems.add(list);
			}
		}
	}
	
	/**
	 * Adds new data to spectrum's dataset
	 * @param fftBins
	 * @return true if the whole spectrum was refreshed once
	 */
	public boolean addNewData(FFTBins fftBins)
	{
		boolean triggerRefresh = false;
		triggerRefresh	= fftBins.fullSweepDone;

		for (int binsIndex = 0; binsIndex < fftBins.freqStart.length; binsIndex++)
		{
			double freqStart = fftBins.freqStart[binsIndex];
			int spectrIndex = (int) ((freqStart - freqStartHz) / fftBinSizeHz);
			if (spectrIndex < 0 || spectrIndex >= spectrum.length)
				continue;
			spectrum[spectrIndex] = fftBins.sigPowdBm[binsIndex];
		}
		

		return triggerRefresh;
	}
	
	public DatasetSpectrum cloneMe()
	{
		DatasetSpectrum copy;
		try
		{
			copy = (DatasetSpectrum) clone();
		}
		catch (CloneNotSupportedException e)
		{
			e.printStackTrace();
			return null;
		}
		return copy;
	}

	/**
	 * Copies spectrum to destination dataset
	 * @param filtered
	 */
	public void copyTo(DatasetSpectrum filtered)
	{
		System.arraycopy(spectrum, 0, filtered.spectrum, 0, spectrum.length);
	}

	/**
	 * Creates {@link XYSeriesImmutable} from spectrum data 
	 * @param name
	 * @return
	 */
	public XYSeriesImmutable createSpectrumDataset(String name) {
		return createSpectrumDataset(name, Integer.MAX_VALUE);
	}

	/**
	 * Chart series. Full-resolution traces keep raw bin values (including
	 * unfilled hop init) so FM/Wi-Fi peaks stay connected the way they
	 * used to. Wide spans downsample to about one vertex per pixel and
	 * drop empty buckets.
	 */
	public XYSeriesImmutable createSpectrumDataset(String name, int maxPoints) {
		return toChartSeries(name, spectrum, maxPoints);
	}

	protected XYSeriesImmutable toChartSeries(String name, float[] ySource, int maxPoints) {
		int n = ySource.length;
		if (n == 0)
			return new XYSeriesImmutable(name, new float[0], new float[0]);
		float[] freq = frequencyAxisMHz();
		int out = maxPoints < 1 ? n : Math.min(n, maxPoints);
		float[] xValues = new float[out];
		float[] yValues = new float[out];
		if (out == n)
		{
			for (int i = 0; i < n; i++)
			{
				xValues[i] = freq[i];
				// Break the line on unfilled hop holes so −150 dB does not
				// drag the trace to the floor. Auto-scale ignores holes.
				yValues[i] = isChartHole(ySource[i]) ? Float.NaN : ySource[i];
			}
		}
		else
		{
			for (int p = 0; p < out; p++)
			{
				int i0 = (int) ((long) p * n / out);
				int i1 = Math.max(i0 + 1, (int) ((long) (p + 1) * n / out));
				float peak = Float.NEGATIVE_INFINITY;
				float xAt = freq[i0];
				boolean any = false;
				for (int i = i0; i < i1 && i < n; i++)
				{
					float y = ySource[i];
					if (isChartHole(y))
						continue;
					any = true;
					if (y > peak)
					{
						peak = y;
						xAt = freq[i];
					}
				}
				xValues[p] = xAt;
				yValues[p] = any ? peak : Float.NaN;
			}
		}
		return new XYSeriesImmutable(name, xValues, yValues);
	}

	public float[] frequencyAxisMHz()
	{
		if (cachedFreqMHz == null || cachedFreqMHz.length != spectrum.length)
		{
			float[] axis = new float[spectrum.length];
			float binMHz = fftBinSizeHz / 1_000_000f;
			float startMHz = freqStartHz / 1_000_000f;
			for (int i = 0; i < axis.length; i++)
				axis[i] = startMHz + binMHz * i;
			cachedFreqMHz = axis;
		}
		return cachedFreqMHz;
	}

	public static boolean isChartHole(float y)
	{
		return !Float.isFinite(y) || y <= SpectrumPowerScale.EMPTY_CEILING;
	}
	
	/**
	 * Fills data to {@link XYSeries}, uses x units in MHz
	 * @param series
	 */
	public void fillToXYSeries(XYSeries series)
	{
		fillToXYSeriesPriv(series, spectrum);
	}


	public float getFFTBinSizeHz()
	{
		return fftBinSizeHz;
	}
	
	public int getFreqStartMHz()
	{
		return freqStartMHz;
	}

	public int getFreqStopMHz()
	{
		return freqStopMHz;
	}

	/** Same MHz span and bin size — a gain-only restart must not wipe the waterfall. */
	public boolean sameAxisAs(DatasetSpectrum other)
	{
		if (other == null)
			return false;
		return freqStartMHz == other.freqStartMHz && freqStopMHz == other.freqStopMHz
				&& fftBinSizeHz == other.fftBinSizeHz;
	}

	/**
	 * Translates index of spectrum to frequency in Hz
	 * @param index
	 * @return
	 */
	public double getFrequency(int index)
	{
		double freq = (freqStartHz + fftBinSizeHz * index);
		return freq;
	}

	public float getPower(int index)
	{
		return spectrum[index];
	}

	public float[] getSpectrumArray()
	{
		return spectrum;
	}

	public void resetSpectrum()
	{
		Arrays.fill(spectrum, spectrumInitPower);
	}
	public void setSpectrumInitPower(float spectrumInitPower)
	{
		this.spectrumInitPower = spectrumInitPower;
	}
	
	public int spectrumLength()
	{
		return spectrum.length;
	}

	@Override protected Object clone() throws CloneNotSupportedException
	{
		DatasetSpectrum copy	= (DatasetSpectrum) super.clone();
		copy.spectrum			= spectrum.clone();
		return copy;
	}
	
	protected void fillToXYSeriesPriv(XYSeries series, float[] spectrum){
		series.clear();
		if (!useCached){
			for (int i = 0; i < spectrum.length; i++)
			{
				double freq = (freqStartHz + fftBinSizeHz * i) / 1000000;
				series.add(freq, spectrum[i]);
			}
		}
		else{
			ArrayList<XYDataItem> items	= cachedDataItems.get(cachedDataItemsIndex);
			for (int i = 0; i < spectrum.length; i++)
			{
				XYDataItem item	= items.get(i);
				item.setY(spectrum[i]);
				series.add(item);
			}
			cachedDataItemsIndex	= (cachedDataItemsIndex+1)%cachedDataItems.size();
		}
	}
}
