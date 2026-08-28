package hotiron.core;

/**
 * Nordic nRF Sniffer host UART (protocol v1 host / v2–v3 device). Parse
 * their EVENT packets and emit named BLE PDUs. Not a controller / host stack.
 */
public final class NordicSnifferProto
{
	public static final int HEADER_LEN = 6;
	/** Host commands match nrfutil / SnifferAPI ({@code PROTOVER_V1}). */
	public static final int HOST_PROTO = 1;
	public static final int VERSION = 3;
	public static final int REQ_FOLLOW = 0x00;
	public static final int EVENT_FOLLOW = 0x01;
	public static final int EVENT_PACKET_ADV = 0x02;
	public static final int EVENT_CONNECT = 0x05;
	public static final int EVENT_PACKET = 0x06;
	public static final int REQ_SCAN_CONT = 0x07;
	public static final int PING_REQ = 0x0D;
	public static final int PING_RESP = 0x0E;
	public static final int SET_ADV_HOP = 0x17;
	public static final byte[] ADV_ACCESS_ADDRESS = { (byte) 0xD6, (byte) 0xBE, (byte) 0x89, (byte) 0x8E };

	private NordicSnifferProto()
	{
	}

	public static byte[] hostPacket(int type, byte[] payload, int counter)
	{
		int pay = payload == null ? 0 : payload.length;
		byte[] out = new byte[HEADER_LEN + pay];
		out[0] = (byte) HEADER_LEN;
		out[1] = (byte) pay;
		out[2] = (byte) HOST_PROTO;
		out[3] = (byte) (counter & 0xFF);
		out[4] = (byte) ((counter >> 8) & 0xFF);
		out[5] = (byte) type;
		if (pay > 0)
			System.arraycopy(payload, 0, out, HEADER_LEN, pay);
		return out;
	}

	public static byte[] pingReq(int counter)
	{
		return hostPacket(PING_REQ, null, counter);
	}

	public static byte[] scanCont(int counter)
	{
		return hostPacket(REQ_SCAN_CONT, new byte[] { 0 }, counter);
	}

	public static byte[] advHop(int counter)
	{
		return hostPacket(SET_ADV_HOP, new byte[] { 3, 37, 38, 39 }, counter);
	}

	public static HostHeader parseHost(byte[] slipPayload)
	{
		if (slipPayload == null || slipPayload.length < HEADER_LEN)
			return null;
		int version = slipPayload[2] & 0xFF;
		int payloadLen;
		if (version <= HOST_PROTO)
			payloadLen = slipPayload[1] & 0xFF;
		else
			payloadLen = (slipPayload[0] & 0xFF) | ((slipPayload[1] & 0xFF) << 8);
		if (payloadLen < 0 || slipPayload.length < HEADER_LEN + payloadLen)
			return null;
		int counter = (slipPayload[3] & 0xFF) | ((slipPayload[4] & 0xFF) << 8);
		int type = slipPayload[5] & 0xFF;
		byte[] payload = new byte[payloadLen];
		if (payloadLen > 0)
			System.arraycopy(slipPayload, HEADER_LEN, payload, 0, payloadLen);
		return new HostHeader(version, counter, type, payload);
	}

	public static BleFrame toFrame(byte[] slipPayload, long nowMs)
	{
		HostHeader h = parseHost(slipPayload);
		if (h == null)
			return null;
		if (h.type != EVENT_PACKET && h.type != EVENT_PACKET_ADV)
			return null;
		if (h.payload.length < 10)
			return null;
		int headerLen = h.payload[0] & 0xFF;
		if (headerLen < 10 || headerLen > h.payload.length)
			headerLen = 10;
		int channel = h.payload[2] & 0xFF;
		int rssiRaw = h.payload[3] & 0xFF;
		int rssi = rssiRaw == 0 ? 0 : -rssiRaw;
		byte[] radio = new byte[h.payload.length - headerLen];
		if (radio.length > 0)
			System.arraycopy(h.payload, headerLen, radio, 0, radio.length);
		boolean advAa = startsWithAdvAa(radio);
		byte[] pdu = stripRadio(radio);
		boolean adv = h.type == EVENT_PACKET_ADV || advAa || channel == 37 || channel == 38 || channel == 39;
		String name = pduName(pdu, adv);
		String addr = advAddress(pdu);
		return new BleFrame(nowMs, channel, rssi, name, addr, toHex(pdu, 24), adv);
	}

	static boolean startsWithAdvAa(byte[] radio)
	{
		if (radio == null || radio.length < 4)
			return false;
		for (int i = 0; i < 4; i++)
		{
			if (radio[i] != ADV_ACCESS_ADDRESS[i])
				return false;
		}
		return true;
	}

	/**
	 * Drop advertising AA and the nRF radio padding byte after the 2-byte PDU
	 * header (SnifferAPI {@code BLEPACKET_POS+6}).
	 */
	static byte[] stripRadio(byte[] radio)
	{
		if (radio == null || radio.length == 0)
			return new byte[0];
		int off = startsWithAdvAa(radio) ? 4 : 0;
		if (radio.length - off < 3)
		{
			byte[] pdu = new byte[radio.length - off];
			if (pdu.length > 0)
				System.arraycopy(radio, off, pdu, 0, pdu.length);
			return pdu;
		}
		byte[] pdu = new byte[radio.length - off - 1];
		pdu[0] = radio[off];
		pdu[1] = radio[off + 1];
		System.arraycopy(radio, off + 3, pdu, 2, pdu.length - 2);
		return pdu;
	}

	public static String pduName(byte[] pdu, boolean advertising)
	{
		if (pdu == null || pdu.length == 0)
			return advertising ? "ADV" : "DATA";
		int t = pdu[0] & 0x0F;
		if (!advertising && t > 7)
			return "DATA";
		switch (t)
		{
		case 0:
			return "ADV_IND";
		case 1:
			return "ADV_DIRECT";
		case 2:
			return "ADV_NONCONN";
		case 3:
			return "SCAN_REQ";
		case 4:
			return "SCAN_RSP";
		case 5:
			return "CONNECT_IND";
		case 6:
			return "ADV_SCAN";
		case 7:
			return "ADV_EXT";
		default:
			return advertising ? "ADV" : "DATA";
		}
	}

	public static String advAddress(byte[] pdu)
	{
		if (pdu == null || pdu.length < 8)
			return "";
		int t = pdu[0] & 0x0F;
		if (t > 6)
			return "";
		StringBuilder sb = new StringBuilder(17);
		for (int i = 7; i >= 2; i--)
		{
			if (sb.length() > 0)
				sb.append(':');
			sb.append(String.format("%02X", pdu[i] & 0xFF));
		}
		return sb.toString();
	}

	private static String toHex(byte[] pdu, int maxBytes)
	{
		int n = Math.min(pdu.length, maxBytes);
		StringBuilder sb = new StringBuilder(n * 2);
		for (int i = 0; i < n; i++)
			sb.append(String.format("%02X", pdu[i] & 0xFF));
		if (pdu.length > n)
			sb.append("…");
		return sb.toString();
	}

	public static final class HostHeader
	{
		public final int version;
		public final int counter;
		public final int type;
		public final byte[] payload;

		public HostHeader(int version, int counter, int type, byte[] payload)
		{
			this.version = version;
			this.counter = counter;
			this.type = type;
			this.payload = payload == null ? new byte[0] : payload;
		}
	}
}
