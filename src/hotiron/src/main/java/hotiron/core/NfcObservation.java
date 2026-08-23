package hotiron.core;

/**
 * One-sweep NFC / 13.56 MHz measurements. No temporal state.
 */
public final class NfcObservation
{
	public final float noiseDbm;
	public final float thresholdDbm;
	public final float carrierDbm;
	public final float carrierMhz;
	public final boolean carrierOn;
	public final boolean narrowCarrier;
	public final boolean sidebandAb;
	public final boolean sidebandF;
	public final boolean sidebandV;
	public final boolean harmonic2;
	public final boolean harmonic3;

	public NfcObservation(float noiseDbm, float thresholdDbm, float carrierDbm, float carrierMhz,
			boolean carrierOn, boolean narrowCarrier, boolean sidebandAb, boolean sidebandF, boolean sidebandV,
			boolean harmonic2, boolean harmonic3)
	{
		this.noiseDbm = noiseDbm;
		this.thresholdDbm = thresholdDbm;
		this.carrierDbm = carrierDbm;
		this.carrierMhz = carrierMhz;
		this.carrierOn = carrierOn;
		this.narrowCarrier = narrowCarrier;
		this.sidebandAb = sidebandAb;
		this.sidebandF = sidebandF;
		this.sidebandV = sidebandV;
		this.harmonic2 = harmonic2;
		this.harmonic3 = harmonic3;
	}

	public static NfcObservation empty()
	{
		return new NfcObservation(Float.NaN, Float.NaN, Float.NEGATIVE_INFINITY, Float.NaN, false, false, false,
				false, false, false, false);
	}

	public boolean anyEnergy()
	{
		return carrierOn || sidebandAb || sidebandF || sidebandV || harmonic2 || harmonic3;
	}
}
