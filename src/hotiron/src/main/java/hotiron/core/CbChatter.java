package hotiron.core;

/**
 * Squelch + modulation test for parked CB AM. A dead carrier is not
 * chatter; noise with AGC pumped is not chatter.
 */
public final class CbChatter
{
	/** Mix magnitude after /128. Weak skip-wave still clears this. */
	public static final float CARRIER_MIN = 0.02f;
	/** Envelope AC / carrier. ~50% AM sits well above this. */
	public static final float MOD_RATIO = 0.06f;

	private CbChatter()
	{
	}

	public static boolean isChatter(float carrier, float modulation)
	{
		if (!(carrier >= CARRIER_MIN) || !(modulation >= 0f))
			return false;
		return modulation >= MOD_RATIO * carrier;
	}
}
