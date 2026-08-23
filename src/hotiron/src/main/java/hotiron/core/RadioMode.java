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

	private static RadioMode from(boolean released, boolean parked, ListenService service)
	{
		if (released)
			return STOPPED;
		if (!parked)
			return SWEEP;
		if (service == ListenService.TV)
			return WATCH;
		return LISTEN;
	}

	/** Settings are the source of truth; do not store a second mode. */
	public static RadioMode of(HackRFSettings settings)
	{
		if (settings == null)
			return STOPPED;
		boolean released = settings.isRadioReleased() != null
				&& Boolean.TRUE.equals(settings.isRadioReleased().getValue());
		boolean parked = settings.isListening() != null && Boolean.TRUE.equals(settings.isListening().getValue());
		ListenService service = settings.getListenService() != null ? settings.getListenService().getValue()
				: ListenService.FM;
		return from(released, parked, service);
	}

	public boolean parked()
	{
		return this == LISTEN || this == WATCH;
	}
}
