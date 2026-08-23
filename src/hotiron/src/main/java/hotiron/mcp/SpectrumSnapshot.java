package hotiron.mcp;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import hotiron.core.DatasetSpectrum;

/**
 * Immutable filled-bin view of one sweep. Hop holes (dataset init /
 * ≤ −140 dB) are omitted, not reported as noise.
 */
public final class SpectrumSnapshot
{
	public static final int DEFAULT_MAX_POINTS = 2048;

	public final long timestampMs;
	public final int startMHz;
	public final int endMHz;
	public final long freqStartHz;
	public final float fftBinHz;
	public final float[] mhz;
	public final float[] dbm;
	public final int filledBins;
	public final int omittedHoles;
	public final float noiseDbm;
	public final float peakDbm;
	public final float peakMhz;

	public SpectrumSnapshot(long timestampMs, int startMHz, int endMHz, float fftBinHz, float[] mhz, float[] dbm,
			int filledBins, int omittedHoles, float noiseDbm, float peakDbm, float peakMhz)
	{
		this(timestampMs, startMHz, endMHz, fftBinHz, mhz, dbm, filledBins, omittedHoles, noiseDbm, peakDbm, peakMhz,
				startMHz * 1_000_000L);
	}

	public SpectrumSnapshot(long timestampMs, int startMHz, int endMHz, float fftBinHz, float[] mhz, float[] dbm,
			int filledBins, int omittedHoles, float noiseDbm, float peakDbm, float peakMhz, long freqStartHz)
	{
		this.timestampMs = timestampMs;
		this.startMHz = startMHz;
		this.endMHz = endMHz;
		this.freqStartHz = freqStartHz;
		this.fftBinHz = fftBinHz;
		this.mhz = mhz == null ? new float[0] : mhz.clone();
		this.dbm = dbm == null ? new float[0] : dbm.clone();
		this.filledBins = filledBins;
		this.omittedHoles = omittedHoles;
		this.noiseDbm = noiseDbm;
		this.peakDbm = peakDbm;
		this.peakMhz = peakMhz;
	}

	public static SpectrumSnapshot empty(long timestampMs)
	{
		return new SpectrumSnapshot(timestampMs, 0, 0, 0f, new float[0], new float[0], 0, 0, Float.NaN, Float.NaN,
				Float.NaN);
	}

	public boolean isEmpty()
	{
		return mhz.length == 0;
	}

	public static SpectrumSnapshot fromDataset(DatasetSpectrum ds, long timestampMs, int maxPoints, Float minDbm)
	{
		if (ds == null || ds.spectrumLength() == 0)
			return empty(timestampMs);
		float[] freq = ds.frequencyAxisMHz();
		float[] spec = ds.getSpectrumArray();
		int n = spec.length;
		int filled = 0;
		int holes = 0;
		float peak = Float.NEGATIVE_INFINITY;
		float peakAt = Float.NaN;
		float[] filledPowers = new float[n];
		for (int i = 0; i < n; i++)
		{
			float y = spec[i];
			if (DatasetSpectrum.isChartHole(y))
			{
				holes++;
				continue;
			}
			if (minDbm != null && y < minDbm.floatValue())
			{
				holes++;
				continue;
			}
			filledPowers[filled++] = y;
			if (y > peak)
			{
				peak = y;
				peakAt = freq[i];
			}
		}
		float noise = Float.NaN;
		if (filled > 0)
		{
			float[] sorted = Arrays.copyOf(filledPowers, filled);
			Arrays.sort(sorted);
			noise = sorted[(int) Math.floor(0.10 * (filled - 1))];
		}
		int cap = maxPoints < 1 ? n : maxPoints;
		float[] outM = new float[Math.min(cap, Math.max(filled, 0))];
		float[] outD = new float[outM.length];
		int outN = 0;
		if (filled == 0)
		{
			return new SpectrumSnapshot(timestampMs, ds.getFreqStartMHz(), ds.getFreqStopMHz(), ds.getFFTBinSizeHz(),
					new float[0], new float[0], 0, holes, Float.NaN, Float.NaN, Float.NaN, ds.getFreqStartHz());
		}
		if (filled <= cap && n == filled)
		{
			for (int i = 0; i < n; i++)
			{
				if (DatasetSpectrum.isChartHole(spec[i]))
					continue;
				if (minDbm != null && spec[i] < minDbm.floatValue())
					continue;
				outM[outN] = freq[i];
				outD[outN] = spec[i];
				outN++;
			}
		}
		else if (n <= cap)
		{
			for (int i = 0; i < n; i++)
			{
				if (DatasetSpectrum.isChartHole(spec[i]))
					continue;
				if (minDbm != null && spec[i] < minDbm.floatValue())
					continue;
				outM[outN] = freq[i];
				outD[outN] = spec[i];
				outN++;
			}
		}
		else
		{
			for (int p = 0; p < cap; p++)
			{
				int i0 = (int) ((long) p * n / cap);
				int i1 = Math.max(i0 + 1, (int) ((long) (p + 1) * n / cap));
				float bucketPeak = Float.NEGATIVE_INFINITY;
				float xAt = freq[i0];
				boolean any = false;
				for (int i = i0; i < i1 && i < n; i++)
				{
					float y = spec[i];
					if (DatasetSpectrum.isChartHole(y))
						continue;
					if (minDbm != null && y < minDbm.floatValue())
						continue;
					any = true;
					if (y > bucketPeak)
					{
						bucketPeak = y;
						xAt = freq[i];
					}
				}
				if (!any)
					continue;
				if (outN >= outM.length)
				{
					outM = Arrays.copyOf(outM, outM.length + 8);
					outD = Arrays.copyOf(outD, outM.length);
				}
				outM[outN] = xAt;
				outD[outN] = bucketPeak;
				outN++;
			}
		}
		if (outN != outM.length)
		{
			outM = Arrays.copyOf(outM, outN);
			outD = Arrays.copyOf(outD, outN);
		}
		return new SpectrumSnapshot(timestampMs, ds.getFreqStartMHz(), ds.getFreqStopMHz(), ds.getFFTBinSizeHz(), outM,
				outD, filled, holes, noise, filled > 0 ? peak : Float.NaN, peakAt, ds.getFreqStartHz());
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(64 + mhz.length * 24);
		sb.append('{');
		Json.appendKey(sb, "timestampMs").append(timestampMs).append(',');
		Json.appendKey(sb, "startMHz").append(startMHz).append(',');
		Json.appendKey(sb, "endMHz").append(endMHz).append(',');
		Json.appendKey(sb, "fftBinHz").append(Json.num(fftBinHz)).append(',');
		Json.appendKey(sb, "filledBins").append(filledBins).append(',');
		Json.appendKey(sb, "omittedHoles").append(omittedHoles).append(',');
		Json.appendKey(sb, "noiseDbm").append(Json.num(noiseDbm)).append(',');
		Json.appendKey(sb, "peakDbm").append(Json.num(peakDbm)).append(',');
		Json.appendKey(sb, "peakMhz").append(Json.num(peakMhz)).append(',');
		appendPoints(sb);
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Peak-pick into at most {@code maxPoints} bins. Optional {@code minDbm}
	 * drops weaker points. Same helper the snapshot tool uses so history
	 * frames match the live downsample.
	 */
	public SpectrumSnapshot downsampled(int maxPoints, Float minDbm)
	{
		if (isEmpty())
			return this;
		int cap = Math.max(1, maxPoints);
		int n = mhz.length;
		float[] m = new float[Math.min(n, cap)];
		float[] d = new float[m.length];
		int out = 0;
		if (n <= cap)
		{
			for (int i = 0; i < n; i++)
			{
				if (minDbm != null && dbm[i] < minDbm.floatValue())
					continue;
				if (out < m.length)
				{
					m[out] = mhz[i];
					d[out] = dbm[i];
					out++;
				}
			}
		}
		else
		{
			for (int p = 0; p < cap; p++)
			{
				int i0 = (int) ((long) p * n / cap);
				int i1 = Math.max(i0 + 1, (int) ((long) (p + 1) * n / cap));
				float peak = Float.NEGATIVE_INFINITY;
				float xAt = mhz[i0];
				boolean any = false;
				for (int i = i0; i < i1 && i < n; i++)
				{
					if (minDbm != null && dbm[i] < minDbm.floatValue())
						continue;
					any = true;
					if (dbm[i] > peak)
					{
						peak = dbm[i];
						xAt = mhz[i];
					}
				}
				if (!any)
					continue;
				m[out] = xAt;
				d[out] = peak;
				out++;
			}
		}
		float[] mo = new float[out];
		float[] do_ = new float[out];
		System.arraycopy(m, 0, mo, 0, out);
		System.arraycopy(d, 0, do_, 0, out);
		return new SpectrumSnapshot(timestampMs, startMHz, endMHz, fftBinHz, mo, do_, filledBins, omittedHoles,
				noiseDbm, peakDbm, peakMhz, freqStartHz);
	}

	void appendPoints(StringBuilder sb)
	{
		Json.appendKey(sb, "points").append('[');
		for (int i = 0; i < mhz.length; i++)
		{
			if (i > 0)
				sb.append(',');
			sb.append("{\"mhz\":").append(Json.num(mhz[i])).append(",\"dbm\":").append(Json.num(dbm[i])).append('}');
		}
		sb.append(']');
	}

	public String toSummaryJson(RadioContext ctx)
	{
		StringBuilder sb = new StringBuilder(256);
		sb.append('{');
		if (isEmpty())
			Json.appendKey(sb, "error").append(Json.quote("no sweep yet")).append(',');
		Json.appendKey(sb, "timestampMs").append(timestampMs).append(',');
		Json.appendKey(sb, "startMHz").append(startMHz).append(',');
		Json.appendKey(sb, "endMHz").append(endMHz).append(',');
		Json.appendKey(sb, "spanMHz").append(endMHz - startMHz).append(',');
		Json.appendKey(sb, "fftBinHz").append(Json.num(fftBinHz)).append(',');
		Json.appendKey(sb, "filledBins").append(filledBins).append(',');
		Json.appendKey(sb, "omittedHoles").append(omittedHoles).append(',');
		Json.appendKey(sb, "noiseDbm").append(Json.num(noiseDbm)).append(',');
		Json.appendKey(sb, "peakDbm").append(Json.num(peakDbm)).append(',');
		Json.appendKey(sb, "peakMhz").append(Json.num(peakMhz));
		if (!isEmpty())
		{
			hotiron.core.SpectrumOccupancy.Result occ = hotiron.core.SpectrumOccupancy.from(mhz, dbm,
					noiseDbm, fftBinHz, startMHz, endMHz);
			sb.append(',');
			Json.appendKey(sb, "occupiedFraction").append(Json.num(occ.occupiedFraction)).append(',');
			Json.appendKey(sb, "emitterCount").append(occ.emitters.size());
		}
		if (ctx != null)
		{
			sb.append(',');
			Json.appendKey(sb, "paused").append(ctx.paused).append(',');
			Json.appendKey(sb, "released").append(ctx.released).append(',');
			Json.appendKey(sb, "sweepsPerSec").append(Json.num((float) ctx.sweepsPerSec));
		}
		sb.append('}');
		return sb.toString();
	}

	public static final class RadioContext
	{
		public final boolean paused;
		public final boolean released;
		public final double sweepsPerSec;
		public final String board;
		public final String serial;
		public final String firmware;
		public final String usbApi;
		public final boolean present;
		public final int radioStartMHz;
		public final int radioEndMHz;
		public final int radioFftBinHz;
		public final int samples;
		public final int lnaGain;
		public final int vgaGain;
		public final boolean antennaPower;
		public final boolean antennaLna;
		public final boolean clkout;
		public final String selectedSerial;
		public final boolean peaks;
		public final boolean autoScale;
		public final boolean autoGain;
		public final boolean autoSweep;
		public final List<FmHit> fmStations;
		public final String radioMode;
		public final int listenKHz;
		public final int tvChannel;

		public RadioContext(boolean paused, boolean released, double sweepsPerSec, String board, String serial,
				String firmware, String usbApi, boolean present, int radioStartMHz, int radioEndMHz, int radioFftBinHz,
				int samples, int lnaGain, int vgaGain, boolean antennaPower, boolean antennaLna, boolean clkout,
				String selectedSerial, boolean peaks, boolean autoScale, boolean autoGain, boolean autoSweep,
				List<FmHit> fmStations, String radioMode, int listenKHz, int tvChannel)
		{
			this.paused = paused;
			this.released = released;
			this.sweepsPerSec = sweepsPerSec;
			this.board = board;
			this.serial = serial;
			this.firmware = firmware;
			this.usbApi = usbApi;
			this.present = present;
			this.radioStartMHz = radioStartMHz;
			this.radioEndMHz = radioEndMHz;
			this.radioFftBinHz = radioFftBinHz;
			this.samples = samples;
			this.lnaGain = lnaGain;
			this.vgaGain = vgaGain;
			this.antennaPower = antennaPower;
			this.antennaLna = antennaLna;
			this.clkout = clkout;
			this.selectedSerial = selectedSerial == null ? "" : selectedSerial;
			this.peaks = peaks;
			this.autoScale = autoScale;
			this.autoGain = autoGain;
			this.autoSweep = autoSweep;
			this.fmStations = fmStations == null ? List.of() : List.copyOf(fmStations);
			this.radioMode = radioMode == null || radioMode.isEmpty() ? "sweep" : radioMode;
			this.listenKHz = listenKHz;
			this.tvChannel = tvChannel;
		}

		public String identityJson()
		{
			StringBuilder sb = new StringBuilder(128);
			sb.append('{');
			Json.appendKey(sb, "present").append(present).append(',');
			Json.appendKey(sb, "board").append(Json.quote(board)).append(',');
			Json.appendKey(sb, "serial").append(Json.quote(serial)).append(',');
			Json.appendKey(sb, "firmware").append(Json.quote(firmware)).append(',');
			Json.appendKey(sb, "usbApi").append(Json.quote(usbApi));
			sb.append('}');
			return sb.toString();
		}

		public String sweepConfigJson()
		{
			StringBuilder sb = new StringBuilder(256);
			sb.append('{');
			Json.appendKey(sb, "radioMode").append(Json.quote(radioMode)).append(',');
			Json.appendKey(sb, "listenMHz").append(Json.num(listenKHz / 1000f)).append(',');
			Json.appendKey(sb, "tvChannel").append(tvChannel).append(',');
			sb.append("\"radio\":{");
			Json.appendKey(sb, "startMHz").append(radioStartMHz).append(',');
			Json.appendKey(sb, "endMHz").append(radioEndMHz).append(',');
			Json.appendKey(sb, "fftBinHz").append(radioFftBinHz).append(',');
			Json.appendKey(sb, "samples").append(samples).append(',');
			Json.appendKey(sb, "lnaGain").append(lnaGain).append(',');
			Json.appendKey(sb, "vgaGain").append(vgaGain).append(',');
			Json.appendKey(sb, "antennaPower").append(antennaPower).append(',');
			Json.appendKey(sb, "antennaLna").append(antennaLna).append(',');
			Json.appendKey(sb, "clkout").append(clkout).append(',');
			Json.appendKey(sb, "serial").append(Json.quote(selectedSerial));
			sb.append("},\"display\":{");
			Json.appendKey(sb, "peaks").append(peaks).append(',');
			Json.appendKey(sb, "autoScale").append(autoScale).append(',');
			Json.appendKey(sb, "autoGain").append(autoGain).append(',');
			Json.appendKey(sb, "autoSweep").append(autoSweep);
			sb.append("}}");
			return sb.toString();
		}

		public String fmStationsJson()
		{
			StringBuilder sb = new StringBuilder(64 + fmStations.size() * 48);
			sb.append('[');
			for (int i = 0; i < fmStations.size(); i++)
			{
				if (i > 0)
					sb.append(',');
				FmHit h = fmStations.get(i);
				sb.append('{');
				Json.appendKey(sb, "label").append(Json.quote(h.label)).append(',');
				Json.appendKey(sb, "mhz").append(Json.num(h.mhz)).append(',');
				Json.appendKey(sb, "dbm").append(Json.num(h.dbm)).append(',');
				Json.appendKey(sb, "confidence").append(Json.num(h.confidence));
				sb.append('}');
			}
			sb.append(']');
			return sb.toString();
		}
	}

	public static final class FmHit
	{
		public final String label;
		public final float mhz;
		public final float dbm;
		public final float confidence;

		public FmHit(String label, float mhz, float dbm, float confidence)
		{
			this.label = label;
			this.mhz = mhz;
			this.dbm = dbm;
			this.confidence = confidence;
		}
	}

	static final class Json
	{
		static StringBuilder appendKey(StringBuilder sb, String key)
		{
			return sb.append(quote(key)).append(':');
		}

		static String quote(String s)
		{
			if (s == null)
				return "null";
			StringBuilder b = new StringBuilder(s.length() + 8);
			b.append('"');
			for (int i = 0; i < s.length(); i++)
			{
				char c = s.charAt(i);
				if (c == '"' || c == '\\')
					b.append('\\').append(c);
				else if (c == '\n')
					b.append("\\n");
				else if (c == '\r')
					b.append("\\r");
				else if (c == '\t')
					b.append("\\t");
				else
					b.append(c);
			}
			return b.append('"').toString();
		}

		static String num(float v)
		{
			if (!Float.isFinite(v))
				return "null";
			return String.format(Locale.US, "%.4f", v);
		}
	}
}
