package hotiron.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which band tools belong on the operator column for the current view.
 * Policy per tool is {@link BandToolKind}; this type is only the set of
 * kinds that currently qualify. Parked Listen/Watch/Sniff and an
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
		EnumSet<BandToolKind> set = EnumSet.noneOf(BandToolKind.class);
		for (BandToolKind kind : BandToolKind.values())
		{
			if (kind.qualifies(view, parked, service, bleSniffing, scan))
				set.add(kind);
		}
		return new BandContext(set);
	}

	public boolean shows(BandToolKind kind)
	{
		return kind != null && kinds.contains(kind);
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
	 * Keep a tool on screen across a small pan/zoom that would otherwise
	 * flicker at the span threshold. Hidden as soon as {@link BandToolKind#hold}
	 * is false.
	 */
	public BandContext stabilize(BandContext previous, FrequencyRange view)
	{
		if (previous == null || view == null)
			return this;
		EnumSet<BandToolKind> set = EnumSet.copyOf(kinds);
		for (BandToolKind kind : BandToolKind.values())
		{
			if (previous.shows(kind) && !shows(kind) && kind.hold(view))
				set.add(kind);
		}
		return new BandContext(set);
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
