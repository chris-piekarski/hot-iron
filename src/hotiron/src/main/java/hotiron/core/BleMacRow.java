package hotiron.core;

/**
 * One BLE advertiser/scanner in the sniff table. Last-known PDU on
 * the right; the MAC row stays put.
 */
public final class BleMacRow
{
	public final String address;
	public final String name;
	public final String localName;
	public final String service;
	public final int channel;
	public final int rssiDbm;
	public final int hits;
	public final long lastMs;
	public final boolean advertising;

	public BleMacRow(String address, String name, int channel, int rssiDbm, int hits, long lastMs,
			boolean advertising)
	{
		this(address, name, channel, rssiDbm, hits, lastMs, advertising, "", "");
	}

	public BleMacRow(String address, String name, int channel, int rssiDbm, int hits, long lastMs,
			boolean advertising, String localName, String service)
	{
		this.address = address == null ? "" : address;
		this.name = name == null || name.isEmpty() ? "pdu" : name;
		this.localName = localName == null ? "" : localName;
		this.service = service == null ? "" : service;
		this.channel = channel;
		this.rssiDbm = rssiDbm;
		this.hits = Math.max(1, hits);
		this.lastMs = lastMs;
		this.advertising = advertising;
	}

	public double mhz()
	{
		return BleBandPlan.mhzForChannel(channel);
	}

	public String values()
	{
		String who = !localName.isEmpty() ? localName : (!service.isEmpty() ? service : name);
		return String.format(java.util.Locale.US, "%s  ch%d  %+d dBm  n=%d", who, channel, rssiDbm, hits);
	}
}
