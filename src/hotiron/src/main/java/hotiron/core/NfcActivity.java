package hotiron.core;

import java.util.Locale;
import java.util.Objects;

/**
 * Temporally smoothed NFC / 13.56 MHz classification for the operator and
 * MCP. Does not decode frames, UIDs, or Morse text.
 */
public final class NfcActivity
{
	public enum Kind
	{
		QUIET("quiet"),
		FIELD_ON("field_on"),
		POLLING("polling"),
		CW("cw"),
		HIFER("hifer"),
		NFC_AB("nfc-ab"),
		NFC_F("nfc-f"),
		NFC_V("nfc-v"),
		UNKNOWN("unknown"),
		HIDDEN("hidden");

		public final String json;

		Kind(String json)
		{
			this.json = json;
		}
	}

	/**
	 * AirTags / Find My / Tile are not in this band. Do not imply they are.
	 */
	public static final String TRACKING_HINT = "AirTags, Tiles, and Find My trackers beacon on Bluetooth 2.4 GHz "
			+ "(AirTag precision is UWB near 6-8 GHz), not 13.56 MHz. This tool classifies the field and "
			+ "sidebands only; it does not decode NFC payloads, card UIDs, or Morse text.";

	public final Kind kind;
	public final float carrierDbm;
	public final float carrierMhz;
	public final float duty;
	public final float onMs;
	public final float offMs;
	public final float pollHz;
	public final float confidence;
	public final boolean sidebandAb;
	public final boolean sidebandF;
	public final boolean sidebandV;
	public final boolean harmonic2;
	public final boolean harmonic3;
	public final boolean visible;

	public NfcActivity(Kind kind, float carrierDbm, float carrierMhz, float duty, float onMs, float offMs,
			float pollHz, float confidence, boolean sidebandAb, boolean sidebandF, boolean sidebandV,
			boolean harmonic2, boolean harmonic3, boolean visible)
	{
		this.kind = kind == null ? Kind.QUIET : kind;
		this.carrierDbm = carrierDbm;
		this.carrierMhz = carrierMhz;
		this.duty = duty;
		this.onMs = onMs;
		this.offMs = offMs;
		this.pollHz = pollHz;
		this.confidence = Math.max(0f, Math.min(1f, confidence));
		this.sidebandAb = sidebandAb;
		this.sidebandF = sidebandF;
		this.sidebandV = sidebandV;
		this.harmonic2 = harmonic2;
		this.harmonic3 = harmonic3;
		this.visible = visible;
	}

	public static NfcActivity quiet()
	{
		return hidden(false);
	}

	public static NfcActivity hidden()
	{
		return hidden(false);
	}

	private static NfcActivity hidden(boolean visible)
	{
		return new NfcActivity(visible ? Kind.QUIET : Kind.HIDDEN, Float.NEGATIVE_INFINITY, Float.NaN, 0f,
				Float.NaN, Float.NaN, 0f, 0f, false, false, false, false, false, visible);
	}

	public static NfcActivity quietVisible()
	{
		return new NfcActivity(Kind.QUIET, Float.NEGATIVE_INFINITY, Float.NaN, 0f, Float.NaN, Float.NaN, 0f, 0f,
				false, false, false, false, false, true);
	}

	public String label()
	{
		switch (kind)
		{
		case FIELD_ON:
			return "13.56";
		case POLLING:
			return "poll";
		case CW:
			return "CW";
		case HIFER:
			return "HiFER";
		case NFC_AB:
			return "NFC-A/B";
		case NFC_F:
			return "NFC-F";
		case NFC_V:
			return "NFC-V";
		case UNKNOWN:
			return "13.56";
		default:
			return "";
		}
	}

	public String summary()
	{
		switch (kind)
		{
		case QUIET:
			return "NFC quiet — no 13.56 field (or the antenna is too small). Not an AirTag band.";
		case FIELD_ON:
			return String.format(Locale.US, "Reader field on at %.3f MHz (%.0f dBm). Continuous CW.",
					carrierMhz, carrierDbm);
		case POLLING:
			return String.format(Locale.US,
					"13.56 MHz polling ~%.1f Hz (on %.0f ms / off %.0f ms). Phone or reader search, not Morse data.",
					pollHz, onMs, offMs);
		case CW:
			return String.format(Locale.US,
					"On/off keying at %.3f MHz (on %.0f ms / off %.0f ms). Morse-like — often HiFER or a slow poll, not NFC payload.",
					carrierMhz, onMs, offMs);
		case HIFER:
			return String.format(Locale.US,
					"Narrow 13.56 ISM CW (HiFER / Part 15 beacon). Keying on %.0f ms / off %.0f ms. Not NFC-A and not an AirTag.",
					onMs, offMs);
		case NFC_AB:
			return "Type A/B load-mod sidebands at 12.71 / 14.41 MHz — a card is talking on an NFC reader field.";
		case NFC_F:
			return "FeliCa-like sidebands near ±212 kHz (NFC-F).";
		case NFC_V:
			return "Vicinity / 15693-like sidebands near ±424 kHz (NFC-V).";
		case UNKNOWN:
			return "Energy near 13.56 MHz — unclassified. Loop antenna helps; this is not AirTag / Find My.";
		default:
			return "NFC overlay hidden (zoom to 12–15 MHz, or 26–28 / 40–42 for harmonics).";
		}
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(320);
		sb.append('{');
		key(sb, "kind").append(quote(kind.json)).append(',');
		key(sb, "label").append(quote(label())).append(',');
		key(sb, "summary").append(quote(summary())).append(',');
		key(sb, "visible").append(visible).append(',');
		key(sb, "carrierDbm").append(num(carrierDbm)).append(',');
		key(sb, "carrierMhz").append(num(carrierMhz)).append(',');
		key(sb, "duty").append(num(duty)).append(',');
		key(sb, "onMs").append(num(onMs)).append(',');
		key(sb, "offMs").append(num(offMs)).append(',');
		key(sb, "pollHz").append(num(pollHz)).append(',');
		key(sb, "confidence").append(num(confidence)).append(',');
		key(sb, "sidebandAb").append(sidebandAb).append(',');
		key(sb, "sidebandF").append(sidebandF).append(',');
		key(sb, "sidebandV").append(sidebandV).append(',');
		key(sb, "harmonic2").append(harmonic2).append(',');
		key(sb, "harmonic3").append(harmonic3).append(',');
		key(sb, "trackingHint").append(quote(TRACKING_HINT));
		sb.append('}');
		return sb.toString();
	}

	public boolean sameAs(NfcActivity other)
	{
		if (other == null)
			return false;
		return kind == other.kind && visible == other.visible && sidebandAb == other.sidebandAb
				&& sidebandF == other.sidebandF && sidebandV == other.sidebandV && harmonic2 == other.harmonic2
				&& harmonic3 == other.harmonic3 && almost(carrierDbm, other.carrierDbm)
				&& almost(carrierMhz, other.carrierMhz) && almost(duty, other.duty) && almost(onMs, other.onMs)
				&& almost(offMs, other.offMs) && almost(pollHz, other.pollHz)
				&& Math.abs(confidence - other.confidence) < 0.05f;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof NfcActivity && sameAs((NfcActivity) obj);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(kind, visible);
	}

	private static boolean almost(float a, float b)
	{
		if (!Float.isFinite(a) && !Float.isFinite(b))
			return true;
		return Float.isFinite(a) && Float.isFinite(b) && Math.abs(a - b) < 0.15f;
	}

	private static StringBuilder key(StringBuilder sb, String k)
	{
		return sb.append(quote(k)).append(':');
	}

	private static String quote(String s)
	{
		if (s == null)
			return "\"\"";
		StringBuilder sb = new StringBuilder(s.length() + 2);
		sb.append('"');
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '"' || c == '\\')
				sb.append('\\');
			if (c == '\n')
				sb.append("\\n");
			else
				sb.append(c);
		}
		sb.append('"');
		return sb.toString();
	}

	private static String num(float v)
	{
		if (!Float.isFinite(v))
			return "null";
		return String.format(Locale.US, "%.4f", v);
	}
}
