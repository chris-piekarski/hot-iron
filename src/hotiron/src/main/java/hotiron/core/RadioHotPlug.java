package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * USB appear/disappear policy. Idle probes may use
 * {@code hackrf_device_list}; a live session must use sysfs only.
 * Start a session when a stick shows up after boot; do not steal USB
 * after the operator pressed Stop.
 */
public final class RadioHotPlug
{
	public enum Action
	{
		IDLE, START, MARK_ABSENT
	}

	public static final long RETRY_MS = 3000;

	private List<String> previous;
	private long lastStartMs;
	private final LongSupplier clock;

	public RadioHotPlug()
	{
		this(System::currentTimeMillis);
	}

	public RadioHotPlug(LongSupplier clock)
	{
		this.clock = clock == null ? System::currentTimeMillis : clock;
	}

	public Action observe(List<String> serials, boolean released, boolean identityPresent)
	{
		List<String> current = normalize(serials);
		if (previous == null)
		{
			previous = current;
			return Action.IDLE;
		}
		boolean had = !previous.isEmpty();
		boolean has = !current.isEmpty();
		previous = current;
		if (!has)
		{
			lastStartMs = 0;
			return had || identityPresent ? Action.MARK_ABSENT : Action.IDLE;
		}
		if (released)
			return Action.IDLE;
		if (identityPresent)
			return Action.IDLE;
		long now = clock.getAsLong();
		if (lastStartMs != 0 && now - lastStartMs < RETRY_MS)
			return Action.IDLE;
		lastStartMs = now;
		return Action.START;
	}

	static List<String> normalize(List<String> serials)
	{
		if (serials == null || serials.isEmpty())
			return List.of();
		List<String> out = new ArrayList<String>();
		for (String s : serials)
		{
			if (s != null && !s.isBlank())
				out.add(s);
		}
		Collections.sort(out);
		return List.copyOf(out);
	}
}
