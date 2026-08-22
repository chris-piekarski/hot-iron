package hotiron.core;

import java.util.Locale;

/**
 * Exclusive USB use: sweep, parked FM listen, parked ATSC watch, or released.
 */
public enum RadioMode
{
	SWEEP, LISTEN, WATCH, STOPPED;

	public String jsonName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	public static RadioMode from(boolean released, boolean listening)
	{
		return from(released, listening, ListenService.FM);
	}

	public static RadioMode from(boolean released, boolean parked, ListenService service)
	{
		if (released)
			return STOPPED;
		if (!parked)
			return SWEEP;
		if (service == ListenService.TV)
			return WATCH;
		return LISTEN;
	}
}
