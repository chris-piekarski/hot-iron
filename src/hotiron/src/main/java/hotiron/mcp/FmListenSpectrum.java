package hotiron.mcp;

import java.util.Arrays;

/**
 * Local RF spectrum derived from the same parked IQ stream used by FM Listen.
 */
public final class FmListenSpectrum
{
	public final long timestampMs;
	public final float dialMHz;
	public final float captureCenterMHz;
	public final float sampleRateHz;
	public final float binHz;
	public final float[] mhz;
	public final float[] dbfs;
	public final float noiseDbfs;
	public final float peakDbfs;
	public final float peakMHz;

	public FmListenSpectrum(long timestampMs, float dialMHz, float captureCenterMHz,
			float sampleRateHz, float binHz, float[] mhz, float[] dbfs,
			float noiseDbfs, float peakDbfs, float peakMHz)
	{
		this.timestampMs = timestampMs;
		this.dialMHz = dialMHz;
		this.captureCenterMHz = captureCenterMHz;
		this.sampleRateHz = sampleRateHz;
		this.binHz = binHz;
		this.mhz = mhz == null ? new float[0] : mhz.clone();
		this.dbfs = dbfs == null ? new float[0] : dbfs.clone();
		this.noiseDbfs = noiseDbfs;
		this.peakDbfs = peakDbfs;
		this.peakMHz = peakMHz;
	}

	public static FmListenSpectrum empty()
	{
		return new FmListenSpectrum(0, 0, 0, 0, 0,
				new float[0], new float[0], Float.NaN, Float.NaN, Float.NaN);
	}

	public static FmListenSpectrum fromRow(long timestampMs, double dialMHz,
			double captureCenterHz, float sampleRateHz, float binHz, float[] row)
	{
		if (row == null || row.length == 0)
			return empty();
		float[] x = new float[row.length];
		float[] y = row.clone();
		int peak = 0;
		for (int i = 0; i < row.length; i++)
		{
			x[i] = (float) ((captureCenterHz + (i - row.length / 2.0) * binHz) / 1_000_000.0);
			if (row[i] > row[peak])
				peak = i;
		}
		float[] sorted = row.clone();
		Arrays.sort(sorted);
		float noise = sorted[Math.min(sorted.length - 1, Math.max(0, sorted.length / 5))];
		return new FmListenSpectrum(timestampMs, (float) dialMHz,
				(float) (captureCenterHz / 1_000_000.0), sampleRateHz, binHz,
				x, y, noise, row[peak], x[peak]);
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
		SpectrumSnapshot.Json.appendKey(sb, "dialMHz").append(SpectrumSnapshot.Json.num(dialMHz)).append(',');
		SpectrumSnapshot.Json.appendKey(sb, "captureCenterMHz")
				.append(SpectrumSnapshot.Json.num(captureCenterMHz)).append(',');
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
