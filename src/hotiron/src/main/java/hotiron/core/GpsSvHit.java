package hotiron.core;

/**
 * One GPS L1 C/A acquisition peak. Not a PVT fix.
 */
public final class GpsSvHit
{
	public final int prn;
	public final float cn0DbHz;
	public final int dopplerHz;
	public final int codePhase;
	public final float peakRatio;
	public final int dwell;

	public GpsSvHit(int prn, float cn0DbHz, int dopplerHz, int codePhase, float peakRatio)
	{
		this(prn, cn0DbHz, dopplerHz, codePhase, peakRatio, 0);
	}

	public GpsSvHit(int prn, float cn0DbHz, int dopplerHz, int codePhase, float peakRatio, int dwell)
	{
		this.prn = prn;
		this.cn0DbHz = cn0DbHz;
		this.dopplerHz = dopplerHz;
		this.codePhase = codePhase;
		this.peakRatio = peakRatio;
		this.dwell = Math.max(0, dwell);
	}

	public String line()
	{
		return String.format(java.util.Locale.US, "PRN %2d  %4.0f dB-Hz  %+5d Hz", prn, cn0DbHz, dopplerHz);
	}

	public String toJson()
	{
		return String.format(java.util.Locale.US,
				"{\"prn\":%d,\"cn0DbHz\":%.1f,\"dopplerHz\":%d,\"codePhase\":%d,\"peakRatio\":%.2f,\"dwell\":%d}",
				prn, cn0DbHz, dopplerHz, codePhase, peakRatio, dwell);
	}
}
