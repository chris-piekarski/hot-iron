package hotiron.core;

/**
 * A CB channel present in the current sweep. Confidence is 0–1 after
 * temporal smoothing.
 */
public final class CbChannelHit
{
	public final CbChannel channel;
	public final float powerDbm;
	public final float confidence;

	public CbChannelHit(CbChannel channel, float powerDbm)
	{
		this(channel, powerDbm, 1f);
	}

	public CbChannelHit(CbChannel channel, float powerDbm, float confidence)
	{
		this.channel = channel;
		this.powerDbm = powerDbm;
		this.confidence = confidence;
	}

	public String label()
	{
		return channel.label();
	}

	public String toJson()
	{
		return String.format(java.util.Locale.US,
				"{\"channel\":%d,\"mhz\":%.3f,\"powerDbm\":%.1f,\"confidence\":%.2f}", channel.number,
				channel.centerMHz(), powerDbm, confidence);
	}
}
