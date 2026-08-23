package hotiron.core;

/**
 * One nfc-laboratory frame. Display only — not a second ISO 14443 stack.
 */
public final class NfcFrame
{
	public final long timestampMs;
	public final int tech;
	public final int type;
	public final int phase;
	public final int flags;
	public final int rate;
	public final double t0;
	public final double t1;
	public final String name;
	public final String hex;

	public NfcFrame(long timestampMs, int tech, int type, int phase, int flags, int rate, double t0, double t1,
			String name, String hex)
	{
		this.timestampMs = timestampMs;
		this.tech = tech;
		this.type = type;
		this.phase = phase;
		this.flags = flags;
		this.rate = rate;
		this.t0 = t0;
		this.t1 = t1;
		this.name = name == null || name.isEmpty() ? "frame" : name;
		this.hex = hex == null ? "" : hex;
	}

	public boolean carrier()
	{
		return fieldOff() || fieldOn();
	}

	public boolean fieldOff()
	{
		return type == 0x0100;
	}

	public boolean fieldOn()
	{
		return type == 0x0101;
	}

	public String techLabel()
	{
		switch (tech)
		{
		case 0x0101:
			return "A";
		case 0x0102:
			return "B";
		case 0x0103:
			return "F";
		case 0x0104:
			return "V";
		default:
			return "";
		}
	}

	public String line()
	{
		if (carrier())
			return name;
		String t = techLabel();
		if (hex.isEmpty())
			return t.isEmpty() ? name : t + " " + name;
		return (t.isEmpty() ? name : t + " " + name) + "  " + hex;
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(128 + hex.length());
		sb.append("{\"timestampMs\":").append(timestampMs);
		sb.append(",\"tech\":").append(jsonQuote(techLabel()));
		sb.append(",\"name\":").append(jsonQuote(name));
		sb.append(",\"rate\":").append(rate);
		sb.append(",\"t0\":").append(Double.isFinite(t0) ? String.format(java.util.Locale.US, "%.4f", t0) : "null");
		sb.append(",\"t1\":").append(Double.isFinite(t1) ? String.format(java.util.Locale.US, "%.4f", t1) : "null");
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
