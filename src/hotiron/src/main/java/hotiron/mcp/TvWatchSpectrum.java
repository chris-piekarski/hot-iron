package hotiron.mcp;

import java.util.Arrays;

/**
 * Local RF spectrum derived from the same parked IQ stream used by TV Watch.
 */
public final class TvWatchSpectrum
{
	public final long timestampMs;
	public final int tvChannel;
	public final float centerMHz;
	public final float sampleRateHz;
	public final float binHz;
	public final float[] mhz;
	public final float[] dbfs;
	public final float noiseDbfs;
	public final float peakDbfs;
	public final float peakMHz;

	public TvWatchSpectrum(long timestampMs, int tvChannel, float centerMHz,
			float sampleRateHz, float binHz, float[] mhz, float[] dbfs,
			float noiseDbfs, float peakDbfs, float peakMHz)
	{
		this.timestampMs = timestampMs;
		this.tvChannel = tvChannel;
		this.centerMHz = centerMHz;
		this.sampleRateHz = sampleRateHz;
		this.binHz = binHz;
		this.mhz = mhz == null ? new float[0] : mhz.clone();
		this.dbfs = dbfs == null ? new float[0] : dbfs.clone();
		this.noiseDbfs = noiseDbfs;
		this.peakDbfs = peakDbfs;
		this.peakMHz = peakMHz;
	}

	public static TvWatchSpectrum empty()
	{
		return new TvWatchSpectrum(0, 0, 0, 0, 0,
				new float[0], new float[0], Float.NaN, Float.NaN, Float.NaN);
	}

	public static TvWatchSpectrum fromRow(long timestampMs, int channel,
			double centerHz, float sampleRateHz, float binHz, float[] row)
	{
		if (row == null || row.length == 0)
			return empty();
		float[] x = new float[row.length];
		float[] y = row.clone();
		int peak = 0;
		for (int i = 0; i < row.length; i++)
		{
			x[i] = (float) ((centerHz + (i - row.length / 2.0) * binHz) / 1_000_000.0);
			if (row[i] > row[peak])
				peak = i;
		}
		float[] sorted = row.clone();
		Arrays.sort(sorted);
		float noise = sorted[Math.min(sorted.length - 1, Math.max(0, sorted.length / 5))];
		return new TvWatchSpectrum(timestampMs, channel, (float) (centerHz / 1_000_000.0),
				sampleRateHz, binHz, x, y, noise, row[peak], x[peak]);
	}

	public boolean isEmpty()
	{
		return mhz.length == 0;
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(256 + mhz.length * 20);
		sb.append('{');
		SpectrumSnapshot.Json.appendKey(sb, "timestampMs").append(timestampMs).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "tvChannel").append(tvChannel).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "centerMHz").append(SpectrumSnapshot.Json.num(centerMHz)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "sampleRateHz").append(SpectrumSnapshot.Json.num(sampleRateHz)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "binHz").append(SpectrumSnapshot.Json.num(binHz)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "powerUnit").append(SpectrumSnapshot.Json.quote("dBFS")).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "noiseDbfs").append(SpectrumSnapshot.Json.num(noiseDbfs)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "peakDbfs").append(SpectrumSnapshot.Json.num(peakDbfs)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "peakMHz").append(SpectrumSnapshot.Json.num(peakMHz)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "mhz").append('[');
		for (int i = 0; i < mhz.length; i++)
		{
			if (i > 0)
				sb.append(',');
			sb.append(SpectrumSnapshot.Json.num(mhz[i]));
		}
		sb.append("],");
		SpectrumSnapshot.Json.appendKey(sb, "dbfs").append('[');
		for (int i = 0; i < dbfs.length; i++)
		{
			if (i > 0)
				sb.append(',');
			sb.append(SpectrumSnapshot.Json.num(dbfs[i]));
		}
		sb.append("]}");
		return sb.toString();
	}
}
