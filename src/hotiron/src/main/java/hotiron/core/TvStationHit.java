package hotiron.core;

/**
 * A US TV channel occupied in the current sweep (6 MHz brick, not a
 * narrow FM peak). {@link #confidence} is 0–1 after temporal smoothing.
 */
public final class TvStationHit
{
	public final TvChannel channel;
	public final float powerDbm;
	public final float confidence;

	public TvStationHit(TvChannel channel, float powerDbm)
	{
		this(channel, powerDbm, 1f);
	}

	public TvStationHit(TvChannel channel, float powerDbm, float confidence)
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
		return "ch" + label() + "@" + powerDbm + "dBm/" + confidence;
	}
}
