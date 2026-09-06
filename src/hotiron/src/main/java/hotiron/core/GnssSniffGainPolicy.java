package hotiron.core;

/**
 * Parked GPS L1 C/A is buried in the 8-bit ADC. Use IF gain; do not
 * write the Antenna LNA checkbox (that restarts USB). SMA antenna
 * port bias (3.3 V) is on for this park so an active GPS patch can
 * power its LNA — that is not the RF amp.
 */
public final class GnssSniffGainPolicy
{
	public static final int LNA_DB = 40;
	public static final int VGA_DB = 16;

	private GnssSniffGainPolicy()
	{
	}

	public static int seedLna()
	{
		return LNA_DB;
	}

	public static int seedVga()
	{
		return VGA_DB;
	}

	/** 3.3 V on the HackRF SMA for an active GPS antenna. Not the RF amp. */
	public static boolean seedAntennaPower()
	{
		return true;
	}
}
