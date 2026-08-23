package hotiron.core;

/**
 * Type A 100% ASK is carrier dropouts, not clip. nfc-lab's default LNA
 * 8 dB is for a loop sitting on a door reader; a desk loop needs more
 * so magnitude stays above the decoder's ~0.01 envelope floor. Do not
 * write Antenna LNA.
 */
public final class NfcSniffGainPolicy
{
	public static final int LNA_DB = 24;
	public static final int VGA_DB = 0;

	private NfcSniffGainPolicy()
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
}
