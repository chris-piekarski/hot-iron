package hotiron.core;

/**
 * Sweep classifier for RNSS L1. Occupancy is not a GPS fix and not a
 * unique GPS-vs-Galileo decode (they share 1575.42 MHz).
 */
public final class GnssActivity
{
	public final boolean gpsL1;
	public final boolean bdsB1;
	public final boolean gloL1;
	public final float gpsL1Dbm;
	public final float bdsB1Dbm;
	public final float gloL1Dbm;
	public final float noiseDbm;
	public final float confidence;
	public final boolean visible;

	public GnssActivity(boolean gpsL1, boolean bdsB1, boolean gloL1, float gpsL1Dbm, float bdsB1Dbm,
			float gloL1Dbm, float noiseDbm, float confidence, boolean visible)
	{
		this.gpsL1 = gpsL1;
		this.bdsB1 = bdsB1;
		this.gloL1 = gloL1;
		this.gpsL1Dbm = gpsL1Dbm;
		this.bdsB1Dbm = bdsB1Dbm;
		this.gloL1Dbm = gloL1Dbm;
		this.noiseDbm = noiseDbm;
		this.confidence = Math.max(0f, Math.min(1f, confidence));
		this.visible = visible;
	}

	public static GnssActivity hidden()
	{
		return new GnssActivity(false, false, false, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0f, false);
	}

	public static GnssActivity quiet()
	{
		return new GnssActivity(false, false, false, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0f, true);
	}

	public boolean any()
	{
		return gpsL1 || bdsB1 || gloL1;
	}

	public String summary()
	{
		if (!visible)
			return "";
		if (!any())
			return "GNSS L1 — quiet (no 8 dB brick at GPS / BDS / GLO)";
		StringBuilder sb = new StringBuilder("L1  ");
		boolean first = true;
		if (gpsL1)
		{
			sb.append("GPS/Galileo");
			first = false;
		}
		if (bdsB1)
		{
			if (!first)
				sb.append(" · ");
			sb.append("BDS");
			first = false;
		}
		if (gloL1)
		{
			if (!first)
				sb.append(" · ");
			sb.append("GLONASS");
		}
		return sb.toString();
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(192);
		sb.append('{');
		append(sb, "visible").append(visible).append(',');
		append(sb, "gpsL1").append(gpsL1).append(',');
		append(sb, "bdsB1").append(bdsB1).append(',');
		append(sb, "gloL1").append(gloL1).append(',');
		append(sb, "gpsL1Dbm").append(num(gpsL1Dbm)).append(',');
		append(sb, "bdsB1Dbm").append(num(bdsB1Dbm)).append(',');
		append(sb, "gloL1Dbm").append(num(gloL1Dbm)).append(',');
		append(sb, "noiseDbm").append(num(noiseDbm)).append(',');
		append(sb, "confidence").append(num(confidence)).append(',');
		append(sb, "summary").append(quote(summary()));
		sb.append('}');
		return sb.toString();
	}

	private static StringBuilder append(StringBuilder sb, String key)
	{
		return sb.append('"').append(key).append("\":");
	}

	private static String num(float v)
	{
		if (!Float.isFinite(v))
			return "null";
		return String.format(java.util.Locale.US, "%.2f", v);
	}

	private static String quote(String s)
	{
		if (s == null)
			return "null";
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
