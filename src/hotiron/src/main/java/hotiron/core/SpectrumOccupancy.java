package hotiron.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic occupancy / emitter list from filled-bin power. Hop holes
 * must already be omitted. Used by MCP; no Swing, no USB.
 */
public final class SpectrumOccupancy
{
	public static final float MARGIN_DB = 8f;
	public static final int MAX_EMITTERS = 16;
	/** Merge runs whose edges are at most this many FFT bins apart. */
	public static final int MERGE_BINS = 2;

	private SpectrumOccupancy()
	{
	}

	public static final class Emitter
	{
		public final float peakMhz;
		public final float peakDbm;
		public final float startMHz;
		public final float endMHz;
		public final float occupiedMhz;
		public final String label;

		public Emitter(float peakMhz, float peakDbm, float startMHz, float endMHz, float occupiedMhz, String label)
		{
			this.peakMhz = peakMhz;
			this.peakDbm = peakDbm;
			this.startMHz = startMHz;
			this.endMHz = endMHz;
			this.occupiedMhz = occupiedMhz;
			this.label = label;
		}
	}

	public static final class Result
	{
		public final float noiseDbm;
		public final float thresholdDbm;
		public final int filledBins;
		public final int occupiedBins;
		public final float occupiedFraction;
		public final List<Emitter> emitters;

		public Result(float noiseDbm, float thresholdDbm, int filledBins, int occupiedBins, float occupiedFraction,
				List<Emitter> emitters)
		{
			this.noiseDbm = noiseDbm;
			this.thresholdDbm = thresholdDbm;
			this.filledBins = filledBins;
			this.occupiedBins = occupiedBins;
			this.occupiedFraction = occupiedFraction;
			this.emitters = emitters == null ? List.of() : List.copyOf(emitters);
		}

		public static Result empty()
		{
			return new Result(Float.NaN, Float.NaN, 0, 0, 0f, List.of());
		}

		public String toJson()
		{
			StringBuilder sb = new StringBuilder(128 + emitters.size() * 96);
			sb.append('{');
			key(sb, "noiseDbm").append(num(noiseDbm)).append(',');
			key(sb, "thresholdDbm").append(num(thresholdDbm)).append(',');
			key(sb, "filledBins").append(filledBins).append(',');
			key(sb, "occupiedBins").append(occupiedBins).append(',');
			key(sb, "occupiedFraction").append(num(occupiedFraction)).append(',');
			key(sb, "emitterCount").append(emitters.size()).append(',');
			key(sb, "emitters").append('[');
			for (int i = 0; i < emitters.size(); i++)
			{
				if (i > 0)
					sb.append(',');
				Emitter e = emitters.get(i);
				sb.append('{');
				key(sb, "peakMhz").append(num(e.peakMhz)).append(',');
				key(sb, "peakDbm").append(num(e.peakDbm)).append(',');
				key(sb, "startMHz").append(num(e.startMHz)).append(',');
				key(sb, "endMHz").append(num(e.endMHz)).append(',');
				key(sb, "occupiedMhz").append(num(e.occupiedMhz));
				if (e.label != null)
				{
					sb.append(',');
					key(sb, "label").append('"').append(e.label).append('"');
				}
				sb.append('}');
			}
			sb.append("]}");
			return sb.toString();
		}
	}

	public static Result from(float[] mhz, float[] dbm, float noiseDbm, float fftBinHz, int viewStartMHz,
			int viewEndMHz)
	{
		if (mhz == null || dbm == null || mhz.length == 0 || mhz.length != dbm.length || !Float.isFinite(noiseDbm))
			return Result.empty();
		float binMHz = fftBinHz > 0 ? fftBinHz / 1_000_000f : 0.1f;
		float threshold = noiseDbm + MARGIN_DB;
		int occupied = 0;
		List<int[]> runs = new ArrayList<>();
		int run0 = -1;
		for (int i = 0; i < dbm.length; i++)
		{
			if (!Float.isFinite(dbm[i]) || dbm[i] < threshold)
			{
				if (run0 >= 0)
				{
					runs.add(new int[] { run0, i - 1 });
					run0 = -1;
				}
				continue;
			}
			occupied++;
			if (run0 < 0)
				run0 = i;
		}
		if (run0 >= 0)
			runs.add(new int[] { run0, dbm.length - 1 });

		List<int[]> merged = mergeRuns(runs, mhz, binMHz);
		List<Emitter> emitters = new ArrayList<>();
		for (int[] run : merged)
		{
			int i0 = run[0];
			int i1 = run[1];
			float peak = Float.NEGATIVE_INFINITY;
			float peakAt = mhz[i0];
			for (int i = i0; i <= i1; i++)
			{
				if (dbm[i] > peak)
				{
					peak = dbm[i];
					peakAt = mhz[i];
				}
			}
			float start = mhz[i0];
			float end = mhz[i1];
			float width = end - start;
			if (width < binMHz)
				width = binMHz;
			String label = WifiChannelPlan.labelForPeak(peakAt, viewStartMHz, viewEndMHz);
			if (label == null)
				label = NfcBandPlan.labelForPeak(peakAt, viewStartMHz, viewEndMHz);
			emitters.add(new Emitter(peakAt, peak, start, end, width, label));
		}
		emitters.sort(Comparator.comparingDouble((Emitter e) -> e.peakDbm).reversed());
		if (emitters.size() > MAX_EMITTERS)
			emitters = new ArrayList<>(emitters.subList(0, MAX_EMITTERS));
		float frac = dbm.length == 0 ? 0f : occupied / (float) dbm.length;
		return new Result(noiseDbm, threshold, dbm.length, occupied, frac, emitters);
	}

	static List<int[]> mergeRuns(List<int[]> runs, float[] mhz, float binMHz)
	{
		if (runs.size() < 2)
			return runs;
		float gap = MERGE_BINS * binMHz;
		List<int[]> out = new ArrayList<>();
		int[] cur = new int[] { runs.get(0)[0], runs.get(0)[1] };
		for (int r = 1; r < runs.size(); r++)
		{
			int[] n = runs.get(r);
			float dist = mhz[n[0]] - mhz[cur[1]];
			if (dist <= gap)
				cur[1] = n[1];
			else
			{
				out.add(cur);
				cur = new int[] { n[0], n[1] };
			}
		}
		out.add(cur);
		return out;
	}

	private static StringBuilder key(StringBuilder sb, String k)
	{
		return sb.append('"').append(k).append("\":");
	}

	private static String num(float v)
	{
		if (!Float.isFinite(v))
			return "null";
		return String.format(Locale.US, "%.4f", v);
	}
}
