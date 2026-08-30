package hotiron.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which band face belongs on the operator column. Policy per tool is
 * {@link BandToolKind}. The live {@link #of(HackRFSettings)} path keeps
 * <strong>one</strong> kind: a parked/scan pin, else the best in-view
 * fit (so V-TV is TV, FM is FM). Parked Listen/Watch/Sniff and an
 * in-progress Scan pin their tool even when the sweep window would not.
 */
public final class BandContext
{
	private final EnumSet<BandToolKind> kinds;

	public BandContext(Set<BandToolKind> kinds)
	{
		this.kinds = kinds == null || kinds.isEmpty() ? EnumSet.noneOf(BandToolKind.class)
				: EnumSet.copyOf(kinds);
	}

	public static BandContext none()
	{
		return new BandContext(EnumSet.noneOf(BandToolKind.class));
	}

	public static BandContext of(BandToolKind... kinds)
	{
		if (kinds == null || kinds.length == 0)
			return none();
		EnumSet<BandToolKind> set = EnumSet.noneOf(BandToolKind.class);
		for (BandToolKind k : kinds)
		{
			if (k != null)
				set.add(k);
		}
		return new BandContext(set);
	}

	public static BandContext of(HackRFSettings settings)
	{
		if (settings == null)
			return none();
		boolean parked = Boolean.TRUE.equals(settings.isListening().getValue());
		return of(settings.getFrequency().getValue(), parked, settings.getListenService().getValue(),
				Boolean.TRUE.equals(settings.isBleSniffing().getValue()), settings.getBandScan().getValue());
	}

	public static BandContext of(FrequencyRange view, boolean parked, ListenService service,
			boolean bleSniffing, BandScan scan)
	{
		for (BandToolKind kind : BandToolKind.values())
		{
			if (kind.pinned(parked, service, bleSniffing, scan))
				return of(kind);
		}
		BandToolKind best = null;
		double bestFit = 0;
		for (BandToolKind kind : BandToolKind.values())
		{
			if (!kind.inView(view))
				continue;
			double fit = kind.viewFit(view);
			if (fit > bestFit)
			{
				bestFit = fit;
				best = kind;
			}
		}
		return best == null ? none() : of(best);
	}

	public boolean shows(BandToolKind kind)
	{
		return kind != null && kinds.contains(kind);
	}

	/** The single face to put in the slot, or {@code null} when idle. */
	public BandToolKind face()
	{
		if (kinds.isEmpty())
			return null;
		if (kinds.contains(BandToolKind.TV) && kinds.contains(BandToolKind.FM))
			return BandToolKind.TV;
		for (BandToolKind kind : BandToolKind.values())
		{
			if (kinds.contains(kind))
				return kind;
		}
		return null;
	}

	public boolean any()
	{
		return !kinds.isEmpty();
	}

	public Set<BandToolKind> kinds()
	{
		return Collections.unmodifiableSet(kinds);
	}

	/**
	 * Keep the previous face across a small pan/zoom that would otherwise
	 * flicker at the span threshold. A Quick Select that fits a different
	 * kind still switches. Hidden as soon as {@link BandToolKind#hold}
	 * is false.
	 */
	public BandContext stabilize(BandContext previous, FrequencyRange view)
	{
		if (previous == null || view == null)
			return this;
		BandToolKind was = previous.face();
		if (was != null && !shows(was) && was.hold(view) && !was.inView(view))
			return of(was);
		return this;
	}

	public static double overlapMHz(double a0, double a1, double b0, double b1)
	{
		return Math.max(0, Math.min(a1, b1) - Math.max(a0, b0));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BandContext other))
			return false;
		return kinds.equals(other.kinds);
	}

	@Override
	public int hashCode()
	{
		return kinds.hashCode();
	}

	@Override
	public String toString()
	{
		return "BandContext" + kinds;
	}
}
