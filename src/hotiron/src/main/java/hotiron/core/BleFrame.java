package hotiron.core;

/**
 * One Nordic nRF Sniffer UART frame (display only — not a BLE stack).
 */
public final class BleFrame
{
	public final long timestampMs;
	public final int channel;
	public final int rssiDbm;
	public final String name;
	public final String address;
	public final String hex;
	public final boolean advertising;

	public BleFrame(long timestampMs, int channel, int rssiDbm, String name, String address, String hex,
			boolean advertising)
	{
		this.timestampMs = timestampMs;
		this.channel = channel;
		this.rssiDbm = rssiDbm;
		this.name = name == null || name.isEmpty() ? "pdu" : name;
		this.address = address == null ? "" : address;
		this.hex = hex == null ? "" : hex;
		this.advertising = advertising;
	}

	public String line()
	{
		StringBuilder sb = new StringBuilder(48 + hex.length());
		sb.append(name);
		if (!address.isEmpty())
			sb.append(' ').append(address);
		sb.append("  ch").append(channel).append(' ').append(rssiDbm).append(" dBm");
		if (!hex.isEmpty() && hex.length() <= 24)
			sb.append("  ").append(hex);
		return sb.toString();
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(128 + hex.length());
		sb.append("{\"timestampMs\":").append(timestampMs);
		sb.append(",\"channel\":").append(channel);
		sb.append(",\"rssiDbm\":").append(rssiDbm);
		sb.append(",\"name\":").append(jsonQuote(name));
		sb.append(",\"address\":").append(jsonQuote(address));
		sb.append(",\"advertising\":").append(advertising);
		sb.append(",\"hex\":").append(jsonQuote(hex));
		sb.append('}');
		return sb.toString();
	}

	private static String jsonQuote(String s)
	{
		if (s == null)
			return "null";
		StringBuilder b = new StringBuilder(s.length() + 8);
		b.append('"');
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '"' || c == '\\')
				b.append('\\');
			if (c == '\n')
				b.append("\\n");
			else if (c == '\r')
				b.append("\\r");
			else
				b.append(c);
		}
		b.append('"');
		return b.toString();
	}
}
