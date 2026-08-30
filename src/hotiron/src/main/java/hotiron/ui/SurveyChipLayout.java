package hotiron.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import hotiron.core.SpectrumSurveyAxis;

/**
 * Quick Select on a full-span survey: services above the wave, ITU
 * envelopes + All below. Button width follows the band’s pixel span
 * (padded to a readable minimum); extra top rows are added when chips
 * would overlap. {@link Chip#spanX0}/{@link Chip#spanX1} are the true
 * dividers on the wave.
 */
public final class SurveyChipLayout
{
	public enum Side
	{
		TOP, BOTTOM
	}

	public static final int BUTTON_H = 34;
	public static final float FONT_PT = 16f;
	public static final int MIN_BUTTON_W = 72;
	public static final int ROW_GAP = 4;
	public static final int WAVE_H = 64;
	public static final int GAP = 4;

	public static final class Chip
	{
		public final QuickSelectPreset preset;
		public final Side side;
		public final int x;
		public final int y;
		public final int w;
		public final int h;
		public final int row;
		public final int spanX0;
		public final int spanX1;

		Chip(QuickSelectPreset preset, Side side, int x, int y, int w, int h, int row, int spanX0, int spanX1)
		{
			this.preset = preset;
			this.side = side;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.row = row;
			this.spanX0 = spanX0;
			this.spanX1 = spanX1;
		}

		public int anchorX()
		{
			return (spanX0 + spanX1) / 2;
		}
	}

	private SurveyChipLayout()
	{
	}

	public static boolean bottomSide(QuickSelectPreset preset)
	{
		return preset == QuickSelectPreset.ALL || preset.surveyEnvelope();
	}

	public static int rowsHeight(int rows)
	{
		int n = Math.max(1, rows);
		return n * BUTTON_H + (n - 1) * ROW_GAP;
	}

	public static int topRows(List<Chip> chips)
	{
		return countRows(chips, Side.TOP);
	}

	public static int bottomRows(List<Chip> chips)
	{
		return countRows(chips, Side.BOTTOM);
	}

	public static int heightFor(List<Chip> chips)
	{
		return rowsHeight(topRows(chips)) + WAVE_H + rowsHeight(bottomRows(chips));
	}

	public static List<Chip> place(int width, ToIntFunction<QuickSelectPreset> minWidths)
	{
		int w = Math.max(MIN_BUTTON_W * 4, width);
		List<Chip> top = placeTop(w, minWidths);
		List<Chip> bottom = placeBottom(w, minWidths);
		int topN = Math.max(1, maxRow(top) + 1);
		List<Chip> out = new ArrayList<>();
		for (Chip c : top)
		{
			int y = (topN - 1 - c.row) * (BUTTON_H + ROW_GAP);
			out.add(new Chip(c.preset, c.side, c.x, y, c.w, c.h, c.row, c.spanX0, c.spanX1));
		}
		out.addAll(bottom);
		return out;
	}

	private static List<Chip> placeTop(int w, ToIntFunction<QuickSelectPreset> minWidths)
	{
		List<Proto> protos = new ArrayList<>();
		for (QuickSelectPreset preset : QuickSelectPreset.values())
		{
			if (bottomSide(preset))
				continue;
			protos.add(proto(preset, w, minWidths));
		}
		/* Widest first so envelopes (V-TV) take the row on the wave and
		 * nested chips (FM, Air, 2 m) stack above the same MHz, not slide
		 * into 2.4 GHz. Never move x off the band. */
		protos.sort(Comparator.comparingInt((Proto p) -> p.spanX1 - p.spanX0).reversed());
		List<List<int[]>> occupied = new ArrayList<>();
		occupied.add(new ArrayList<>());
		List<Chip> out = new ArrayList<>();
		for (Proto p : protos)
		{
			int row = 0;
			while (row < occupied.size() && overlaps(occupied.get(row), p.x, p.x + p.w))
				row++;
			if (row >= occupied.size())
				occupied.add(new ArrayList<>());
			occupied.get(row).add(new int[] { p.x, p.x + p.w });
			out.add(new Chip(p.preset, Side.TOP, p.x, 0, p.w, BUTTON_H, row, p.spanX0, p.spanX1));
		}
		return out;
	}

	private static List<Chip> placeBottom(int w, ToIntFunction<QuickSelectPreset> minWidths)
	{
		List<Chip> out = new ArrayList<>();
		for (QuickSelectPreset preset : new QuickSelectPreset[] { QuickSelectPreset.HF, QuickSelectPreset.VHF,
				QuickSelectPreset.UHF })
		{
			Proto p = proto(preset, w, minWidths);
			out.add(new Chip(p.preset, Side.BOTTOM, p.x, 0, p.w, BUTTON_H, 0, p.spanX0, p.spanX1));
		}
		Proto all = proto(QuickSelectPreset.ALL, w, minWidths);
		int y = BUTTON_H + ROW_GAP;
		int x0 = SpectrumSurveyAxis.mhzToX(SpectrumSurveyAxis.MIN_MHZ, w);
		int x1 = SpectrumSurveyAxis.mhzToX(SpectrumSurveyAxis.MAX_MHZ, w);
		out.add(new Chip(all.preset, Side.BOTTOM, 0, y, w, BUTTON_H, 1, x0, x1));
		return out;
	}

	private static Proto proto(QuickSelectPreset preset, int w, ToIntFunction<QuickSelectPreset> minWidths)
	{
		int span0 = SpectrumSurveyAxis.mhzToX(preset.startMHz, w);
		int span1 = SpectrumSurveyAxis.mhzToX(preset.endMHz, w);
		if (span1 < span0)
		{
			int t = span0;
			span0 = span1;
			span1 = t;
		}
		if (span1 - span0 < 3)
			span1 = span0 + 3;
		int minW = MIN_BUTTON_W;
		if (minWidths != null)
			minW = Math.max(MIN_BUTTON_W, minWidths.applyAsInt(preset));
		int span = span1 - span0;
		int bw = Math.max(span, minW);
		int x = span0 - (bw - span) / 2;
		if (x < 0)
			x = 0;
		if (x + bw > w)
			x = Math.max(0, w - bw);
		return new Proto(preset, x, bw, span0, span1);
	}

	private static boolean overlaps(List<int[]> intervals, int a, int b)
	{
		for (int[] iv : intervals)
		{
			if (a < iv[1] + GAP && iv[0] < b + GAP)
				return true;
		}
		return false;
	}

	private static int countRows(List<Chip> chips, Side side)
	{
		int n = 0;
		if (chips != null)
		{
			for (Chip c : chips)
			{
				if (c.side == side && c.row + 1 > n)
					n = c.row + 1;
			}
		}
		return Math.max(1, n);
	}

	private static int maxRow(List<Chip> chips)
	{
		int m = 0;
		for (Chip c : chips)
		{
			if (c.row > m)
				m = c.row;
		}
		return m;
	}

	private static final class Proto
	{
		final QuickSelectPreset preset;
		final int x;
		final int w;
		final int spanX0;
		final int spanX1;

		Proto(QuickSelectPreset preset, int x, int w, int spanX0, int spanX1)
		{
			this.preset = preset;
			this.x = x;
			this.w = w;
			this.spanX0 = spanX0;
			this.spanX1 = spanX1;
		}
	}
}
