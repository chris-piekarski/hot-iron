package hotiron.core;

/**
 * What an operator needs to know about the attached radio. Formatting is
 * HTML for the settings column; keep this free of JNA / Swing.
 */
public final class RadioIdentity
{
	public static final RadioIdentity ABSENT = new RadioIdentity(false, null, null, null, null);

	public final boolean present;
	public final String boardName;
	public final String serialHex;
	public final String firmware;
	public final String usbApi;

	public RadioIdentity(boolean present, String boardName, String serialHex, String firmware, String usbApi)
	{
		this.present = present;
		this.boardName = emptyToNull(boardName);
		this.serialHex = normalizeSerial(serialHex);
		this.firmware = emptyToNull(firmware);
		this.usbApi = emptyToNull(usbApi);
	}

	public static RadioIdentity of(String boardName, String serialHex, String firmware, String usbApi)
	{
		return new RadioIdentity(true, boardName, serialHex, firmware, usbApi);
	}

	public String displayBoard()
	{
		if (!present)
			return "No radio detected";
		if (boardName == null)
			return "HackRF";
		return boardName;
	}

	public String displayFirmware()
	{
		if (firmware == null)
			return null;
		String s = firmware.trim();
		if (s.startsWith("v") || s.startsWith("V"))
			s = s.substring(1);
		int space = s.indexOf(' ');
		if (space > 0)
			s = s.substring(0, space);
		int paren = s.indexOf('(');
		if (paren > 0)
			s = s.substring(0, paren);
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	public String shortSerial()
	{
		if (serialHex == null)
			return null;
		return serialHex.length() <= 8 ? serialHex : serialHex.substring(serialHex.length() - 8);
	}

	public String statusHtml()
	{
		if (!present)
			return "<html>No radio detected<br><span style='color:#9a9a9a'>Check the USB cable and permissions</span></html>";
		StringBuilder line2 = new StringBuilder();
		String sn = shortSerial();
		if (sn != null)
			line2.append("SN ").append(sn);
		String fw = displayFirmware();
		if (fw != null)
		{
			if (line2.length() > 0)
				line2.append("  ·  ");
			line2.append("FW ").append(fw);
		}
		if (line2.length() == 0)
			line2.append("Radio open");
		return "<html><b>" + escape(displayBoard()) + "</b><br>" + escape(line2.toString()) + "</html>";
	}

	public String tooltip(boolean sweeping)
	{
		if (!present)
			return "No radio on USB. On Linux run make udev once; on WSL re-attach usbipd.";
		StringBuilder tip = new StringBuilder();
		tip.append(displayBoard());
		if (serialHex != null)
			tip.append("\nSerial  ").append(serialHex);
		String fw = displayFirmware();
		if (fw != null)
			tip.append("\nFirmware  ").append(fw);
		if (usbApi != null)
			tip.append("\nUSB API  ").append(usbApi);
		tip.append(sweeping ? "\nSweep running" : "\nSweep idle");
		return tip.toString();
	}

	public static String formatMcuSerial(int[] words)
	{
		if (words == null || words.length < 4)
			return null;
		return String.format("%08x%08x%08x%08x",
				Integer.valueOf(words[0]), Integer.valueOf(words[1]),
				Integer.valueOf(words[2]), Integer.valueOf(words[3]));
	}

	private static String normalizeSerial(String raw)
	{
		if (raw == null)
			return null;
		String hex = raw.trim().toLowerCase().replaceAll("[^0-9a-f]", "");
		return hex.isEmpty() ? null : hex;
	}

	private static String emptyToNull(String s)
	{
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
