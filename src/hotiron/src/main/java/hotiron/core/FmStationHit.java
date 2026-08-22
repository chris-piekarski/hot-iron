package hotiron.core;

/**
 * A US FM channel that is present in the current sweep (peak snapped to
 * the 200 kHz dial). {@link #confidence} is 0–1 after temporal smoothing.
 */
public final class FmStationHit
{
	public final FmChannel channel;
	public final float powerDbm;
	public final float confidence;

	public FmStationHit(FmChannel channel, float powerDbm)
	{
		this(channel, powerDbm, 1f);
	}

	public FmStationHit(FmChannel channel, float powerDbm, float confidence)
	{
		this.channel = channel;
		this.powerDbm = powerDbm;
		this.confidence = confidence;
	}

	public String label()
	{
		return channel.label();
	}

	@Override
	public String toString()
	{
		return channel.label() + "@" + powerDbm + "dBm/" + confidence;
	}
}
