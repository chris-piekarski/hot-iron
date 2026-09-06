package hotiron.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Last agent note for one Spectrum tools face. Store write, no USB.
 * One latest brief per face — not a chat log.
 */
public final class OperatorBrief
{
	public static final int TITLE_MAX = 80;
	public static final int BODY_MAX = 2000;
	public static final int LABEL_MAX = 24;
	public static final int MARKS_MAX = 8;

	public static final String FACE_BLE = "ble";
	public static final String FACE_NFC = "nfc";
	public static final String FACE_GNSS = "gnss";
	public static final String FACE_CB = "cb";
	public static final String FACE_FM = "fm";
	public static final String FACE_TV = "tv";
	public static final String FACE_SWEEP = "sweep";

	public final String face;
	public final String title;
	public final String body;
	public final List<Mark> marks;
	public final long timestampMs;
	public final String source;

	public OperatorBrief(String face, String title, String body, List<Mark> marks, long timestampMs, String source)
	{
		this.face = normalizeFace(face);
		this.title = clip(title, TITLE_MAX);
		this.body = clip(body, BODY_MAX);
		this.marks = clampMarks(marks);
		this.timestampMs = timestampMs;
		this.source = clip(source == null || source.isBlank() ? "mcp" : source, 40);
	}

	public boolean isEmpty()
	{
		return title.isEmpty() && body.isEmpty() && marks.isEmpty();
	}

	public static OperatorBrief clear(String face)
	{
		return new OperatorBrief(face, "", "", List.of(), System.currentTimeMillis(), "mcp");
	}

	public static String faceOf(BandToolKind kind)
	{
		if (kind == null)
			return FACE_SWEEP;
		return kind.name().toLowerCase(Locale.ROOT);
	}

	public static String inferFace(String radioMode, boolean bleSniffing)
	{
		if (radioMode != null)
		{
			String m = radioMode.trim().toLowerCase(Locale.ROOT);
			if (FACE_NFC.equals(m) || FACE_GNSS.equals(m) || FACE_CB.equals(m))
				return m;
			if ("watch".equals(m))
				return FACE_TV;
			if ("listen".equals(m))
				return FACE_FM;
		}
		if (bleSniffing)
			return FACE_BLE;
		return FACE_SWEEP;
	}

	public static String normalizeFace(String raw)
	{
		if (raw == null || raw.isBlank())
			return FACE_SWEEP;
		String f = raw.trim().toLowerCase(Locale.ROOT);
		if (FACE_BLE.equals(f) || FACE_NFC.equals(f) || FACE_GNSS.equals(f) || FACE_CB.equals(f) || FACE_FM.equals(f)
				|| FACE_TV.equals(f) || FACE_SWEEP.equals(f))
			return f;
		if ("watch".equals(f))
			return FACE_TV;
		if ("listen".equals(f))
			return FACE_FM;
		return FACE_SWEEP;
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder(64 + title.length() + body.length() + marks.size() * 40);
		sb.append("{\"face\":").append(quote(face));
		sb.append(",\"title\":").append(quote(title));
		sb.append(",\"body\":").append(quote(body));
		sb.append(",\"timestampMs\":").append(timestampMs);
		sb.append(",\"source\":").append(quote(source));
		sb.append(",\"marks\":[");
		for (int i = 0; i < marks.size(); i++)
		{
			if (i > 0)
				sb.append(',');
			Mark m = marks.get(i);
			sb.append("{\"mhz\":").append(String.format(Locale.US, "%.4f", m.mhz));
			sb.append(",\"label\":").append(quote(m.label)).append('}');
		}
		sb.append("]}");
		return sb.toString();
	}

	static String clip(String s, int max)
	{
		if (s == null)
			return "";
		String t = s.strip();
		if (t.length() <= max)
			return t;
		return t.substring(0, max);
	}

	static List<Mark> clampMarks(List<Mark> in)
	{
		if (in == null || in.isEmpty())
			return List.of();
		List<Mark> out = new ArrayList<>();
		for (Mark m : in)
		{
			if (m == null || !Double.isFinite(m.mhz))
				continue;
			out.add(new Mark(m.mhz, clip(m.label, LABEL_MAX)));
			if (out.size() >= MARKS_MAX)
				break;
		}
		return List.copyOf(out);
	}

	static String quote(String s)
	{
		if (s == null)
			return "\"\"";
		StringBuilder b = new StringBuilder(s.length() + 8);
		b.append('"');
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '"' || c == '\\')
				b.append('\\').append(c);
			else if (c == '\n')
				b.append("\\n");
			else if (c == '\r')
				b.append("\\r");
			else if (c == '\t')
				b.append("\\t");
			else
				b.append(c);
		}
		b.append('"');
		return b.toString();
	}

	public static final class Mark
	{
		public final double mhz;
		public final String label;

		public Mark(double mhz, String label)
		{
			this.mhz = mhz;
			this.label = label == null ? "" : label;
		}
	}
}
