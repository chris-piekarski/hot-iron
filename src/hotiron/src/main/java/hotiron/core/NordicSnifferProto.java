package hotiron.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Nordic nRF Sniffer host UART (protocol v2/v3). Parse their EVENT
 * packets and emit named BLE PDUs. Not a controller / host stack.
 */
public final class NordicSnifferProto
{
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

	private NordicSnifferProto()
	{
	}

	public static byte[] hostPacket(int type, byte[] payload, int counter)
	{
		int pay = payload == null ? 0 : payload.length;
		int rest = 1 + 2 + 1 + pay;
		ByteBuffer bb = ByteBuffer.allocate(2 + rest).order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) rest);
		bb.put((byte) VERSION);
		bb.putShort((short) (counter & 0xFFFF));
		bb.put((byte) type);
		if (pay > 0)
			bb.put(payload);
		return bb.array();
	}

	public static byte[] pingReq(int counter)
	{
		return hostPacket(PING_REQ, null, counter);
	}

	public static byte[] scanCont(int counter)
	{
		return hostPacket(REQ_SCAN_CONT, null, counter);
	}

	public static HostHeader parseHost(byte[] slipPayload)
	{
		if (slipPayload == null || slipPayload.length < 6)
			return null;
		int rest = (slipPayload[0] & 0xFF) | ((slipPayload[1] & 0xFF) << 8);
		if (rest < 4 || slipPayload.length < 2 + rest)
			return null;
		int version = slipPayload[2] & 0xFF;
		int counter = (slipPayload[3] & 0xFF) | ((slipPayload[4] & 0xFF) << 8);
		int type = slipPayload[5] & 0xFF;
		byte[] payload = new byte[Math.max(0, rest - 4)];
		if (payload.length > 0)
			System.arraycopy(slipPayload, 6, payload, 0, payload.length);
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
		int rssiRaw = h.payload[3];
		int rssi = rssiRaw > 0 ? -rssiRaw : rssiRaw;
		int pduOff = headerLen;
		if (pduOff >= h.payload.length)
			return null;
		byte[] pdu = new byte[h.payload.length - pduOff];
		System.arraycopy(h.payload, pduOff, pdu, 0, pdu.length);
		boolean adv = h.type == EVENT_PACKET_ADV || channel == 37 || channel == 38 || channel == 39;
		String name = pduName(pdu, adv);
		String addr = advAddress(pdu);
		return new BleFrame(nowMs, channel, rssi, name, addr, toHex(pdu, 24), adv);
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
