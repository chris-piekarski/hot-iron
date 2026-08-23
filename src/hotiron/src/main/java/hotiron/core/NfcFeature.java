package hotiron.core;

/**
 * One labeled NFC / HF-RFID spectral feature (carrier, sideband, harmonic).
 */
public final class NfcFeature
{
	public final String id;
	public final String label;
	public final double centerMhz;
	public final double halfKHz;
	public final boolean sideband;
	public final boolean harmonic;

	public NfcFeature(String id, String label, double centerMhz, double halfKHz, boolean sideband,
			boolean harmonic)
	{
		this.id = id == null ? "" : id;
		this.label = label == null ? "" : label;
		this.centerMhz = centerMhz;
		this.halfKHz = halfKHz;
		this.sideband = sideband;
		this.harmonic = harmonic;
	}

	public double lowMHz()
	{
		return centerMhz - halfKHz / 1000.0;
	}

	public double highMHz()
	{
		return centerMhz + halfKHz / 1000.0;
	}

	public boolean overlaps(double startMHz, double endMHz)
	{
		return highMHz() > startMHz && lowMHz() < endMHz;
	}

	public boolean contains(double mhz)
	{
		return mhz >= lowMHz() && mhz <= highMHz();
	}
}
