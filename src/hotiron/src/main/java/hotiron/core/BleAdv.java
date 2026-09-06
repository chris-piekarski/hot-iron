package hotiron.core;

/**
 * GAP advertising data from a BLE PDU. Ads-only — not GATT.
 */
public final class BleAdv
{
	public static final BleAdv NONE = new BleAdv("", "", "", -1, -1);

	public final String localName;
	public final String uuid;
	public final String service;
	public final int flags;
	public final int appearance;

	public BleAdv(String localName, String uuid, String service, int flags, int appearance)
	{
		this.localName = localName == null ? "" : localName;
		this.uuid = uuid == null ? "" : uuid;
		this.service = service == null ? "" : service;
		this.flags = flags;
		this.appearance = appearance;
	}

	public boolean isEmpty()
	{
		return localName.isEmpty() && uuid.isEmpty() && service.isEmpty() && flags < 0 && appearance < 0;
	}

	public static BleAdv parseHex(String hex)
	{
		if (hex == null || hex.isEmpty())
			return NONE;
		String h = hex.replace("…", "").replace("...", "").replace(" ", "");
		int n = h.length() - (h.length() % 2);
		if (n < 2)
			return NONE;
		byte[] pdu = new byte[n / 2];
		for (int i = 0; i < pdu.length; i++)
		{
			int hi = nibble(h.charAt(i * 2));
			int lo = nibble(h.charAt(i * 2 + 1));
			if (hi < 0 || lo < 0)
				return NONE;
			pdu[i] = (byte) ((hi << 4) | lo);
		}
		return parsePdu(pdu);
	}

	public static BleAdv parsePdu(byte[] pdu)
	{
		if (pdu == null || pdu.length < 8)
			return NONE;
		int t = pdu[0] & 0x0F;
		if (t == 1 || t == 3 || t == 5)
			return NONE;
		int end = Math.min(pdu.length, 2 + (pdu[1] & 0xFF));
		String localName = "";
		String uuid = "";
		String service = "";
		int flags = -1;
		int appearance = -1;
		int i = 8;
		while (i < end)
		{
			int len = pdu[i] & 0xFF;
			if (len == 0)
				break;
			int typeOff = i + 1;
			if (typeOff >= end)
				break;
			int type = pdu[typeOff] & 0xFF;
			int dataOff = typeOff + 1;
			int dataLen = Math.min(len - 1, end - dataOff);
			if (dataLen < 0)
				break;
			switch (type)
			{
			case 0x01:
				if (dataLen >= 1)
					flags = pdu[dataOff] & 0xFF;
				break;
			case 0x02:
			case 0x03:
				if (dataLen >= 2 && uuid.isEmpty())
					uuid = uuid16(pdu, dataOff);
				break;
			case 0x06:
			case 0x07:
				if (dataLen > 0)
				{
					String le = toHex(pdu, dataOff, dataLen);
					if (service.isEmpty())
						service = knownFromLe(le);
					if (dataLen >= 16 && uuid.isEmpty())
						uuid = uuid128(pdu, dataOff);
				}
				break;
			case 0x08:
			case 0x09:
				if (dataLen > 0 && (localName.isEmpty() || type == 0x09))
					localName = utf8(pdu, dataOff, dataLen);
				break;
			case 0x19:
				if (dataLen >= 2)
					appearance = (pdu[dataOff] & 0xFF) | ((pdu[dataOff + 1] & 0xFF) << 8);
				break;
			default:
				break;
			}
			i += 1 + len;
		}
		if (service.isEmpty() && !uuid.isEmpty())
			service = knownFromUuid(uuid);
		return new BleAdv(localName, uuid, service, flags, appearance);
	}

	static String knownFromLe(String leHex)
	{
		String h = leHex.toLowerCase();
		if (h.startsWith("23d1bcea5f7823"))
			return "Nordic LBS";
		if (h.startsWith("9ecadc240ee5a9e0"))
			return "Nordic UART";
		return "";
	}

	static String knownFromUuid(String uuid)
	{
		String u = uuid.toLowerCase();
		if (u.equals("00001523-1212-efde-1523-785feabcd123"))
			return "Nordic LBS";
		if (u.equals("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
			return "Nordic UART";
		return "";
	}

	private static String uuid16(byte[] p, int off)
	{
		int v = (p[off] & 0xFF) | ((p[off + 1] & 0xFF) << 8);
		return String.format("0000%04x-0000-1000-8000-00805f9b34fb", v);
	}

	private static String uuid128(byte[] p, int off)
	{
		return String.format("%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x", p[off + 15] & 0xFF,
				p[off + 14] & 0xFF, p[off + 13] & 0xFF, p[off + 12] & 0xFF, p[off + 11] & 0xFF, p[off + 10] & 0xFF,
				p[off + 9] & 0xFF, p[off + 8] & 0xFF, p[off + 7] & 0xFF, p[off + 6] & 0xFF, p[off + 5] & 0xFF,
				p[off + 4] & 0xFF, p[off + 3] & 0xFF, p[off + 2] & 0xFF, p[off + 1] & 0xFF, p[off] & 0xFF);
	}

	private static String utf8(byte[] p, int off, int n)
	{
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++)
		{
			int c = p[off + i] & 0xFF;
			if (c < 0x20 || c > 0x7E)
				return sb.toString().trim();
			sb.append((char) c);
		}
		return sb.toString().trim();
	}

	private static String toHex(byte[] p, int off, int n)
	{
		StringBuilder sb = new StringBuilder(n * 2);
		for (int i = 0; i < n; i++)
			sb.append(String.format("%02x", p[off + i] & 0xFF));
		return sb.toString();
	}

	private static int nibble(char c)
	{
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		return -1;
	}
}
