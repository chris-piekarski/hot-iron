package hotiron.core;

import java.util.Optional;

public class FrequencyRange{
	private final int startMHz, endMHz;

	public FrequencyRange(int startMHz, int endMHz) {
		this.startMHz = startMHz;
		this.endMHz = endMHz;
	}
	public int getEndMHz() {
		return endMHz;
	}
	public int getStartMHz() {
		return startMHz;
	}

	public int spanMHz() {
		return endMHz - startMHz;
	}

	/**
	 * Operator typing: {@code 88-108}, {@code 88 108}, {@code 2402–2472 MHz}.
	 * A single number is a center with {@code defaultSpanMHz}.
	 */
	public static Optional<FrequencyRange> parse(String raw, int defaultSpanMHz) {
		if (raw == null)
			return Optional.empty();
		String s = raw.trim();
		if (s.isEmpty())
			return Optional.empty();
		s = s.replace("MHz", "").replace("mhz", "").replace("MHZ", "");
		s = s.replace(',', ' ').replace('–', '-').replace('—', '-').replace('−', '-');
		s = s.replaceAll("\\s+", " ").trim();
		String[] parts = s.split("[- ]+");
		if (parts.length >= 2)
		{
			Integer a = parseMHz(parts[0]);
			Integer b = parseMHz(parts[1]);
			if (a == null || b == null)
				return Optional.empty();
			int lo = Math.min(a.intValue(), b.intValue());
			int hi = Math.max(a.intValue(), b.intValue());
			return Optional.of(SpectrumZoom.clamp(lo, hi));
		}
		if (parts.length == 1)
		{
			Integer c = parseMHz(parts[0]);
			if (c == null)
				return Optional.empty();
			int span = Math.max(SpectrumZoom.MIN_SPAN_MHZ, defaultSpanMHz);
			int half = Math.max(1, span / 2);
			return Optional.of(SpectrumZoom.clamp(c.intValue() - half, c.intValue() + half));
		}
		return Optional.empty();
	}

	private static Integer parseMHz(String token) {
		if (token == null || token.isEmpty())
			return null;
		try
		{
			int v = (int) Math.round(Double.parseDouble(token.trim()));
			return Integer.valueOf(v);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * Native INTERLEAVED hops (20 MHz) export [f,f+5] and [f+10,f+15].
	 * Pad ±10 MHz so the requested window is actually filled (otherwise
	 * FM 88–108 misses 93–98 and 103–108, and 97.3 sits in a hole).
	 */
	public static final int INTERLEAVED_PAD_MHZ = 10;
	public static final int MIN_MHZ = 1;
	public static final int MAX_MHZ = 7250;

	public FrequencyRange forInterleavedNativeSweep() {
		int start = Math.max(MIN_MHZ, startMHz - INTERLEAVED_PAD_MHZ);
		int end = Math.min(MAX_MHZ, endMHz + INTERLEAVED_PAD_MHZ);
		if (end <= start)
			end = Math.min(MAX_MHZ, start + 20);
		return new FrequencyRange(start, end);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FrequencyRange) {
			FrequencyRange fr	= (FrequencyRange)obj;
			if (fr.endMHz == endMHz && fr.startMHz == startMHz)
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * startMHz + endMHz;
	}

	@Override
	public String toString() {
		return startMHz + "–" + endMHz + " MHz";
	}
}