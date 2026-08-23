package hotiron.nativebridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import hotiron.core.NfcFrame;
import hotiron.core.NfcSniffEngine;
import hotiron.jna.HackrfSweepLibrary;

/**
 * JNA door to {@code nfc_dec_*} in libhackrf-sweep. Hand-maintained.
 */
public final class NfcDecNative implements NfcSniffEngine.Decoder
{
	static final int NAME_MAX = 32;
	static final int PAYLOAD_MAX = 256;
	static final int FRAME_BYTES = 4 * 6 + 8 * 2 + NAME_MAX + PAYLOAD_MAX;
	static final int MAX_FRAMES = 32;

	private final Pointer handle;
	private final Memory out = new Memory((long) FRAME_BYTES * MAX_FRAMES);

	private NfcDecNative(Pointer handle)
	{
		this.handle = handle;
	}

	public static NfcDecNative open()
	{
		HackRFSweepNativeBridge.class.getName();
		Pointer p = HackrfSweepLibrary.nfc_dec_create();
		if (p == null || Pointer.nativeValue(p) == 0)
			return null;
		HackrfSweepLibrary.nfc_dec_set_sample_rate(p, NfcSniffEngine.IQ_RATE_HZ);
		return new NfcDecNative(p);
	}

	@Override
	public List<NfcFrame> processIq(byte[] iq)
	{
		if (iq == null || iq.length < 2)
			return List.of();
		int n = HackrfSweepLibrary.nfc_dec_process_iq(handle, iq, iq.length, out, MAX_FRAMES);
		if (n <= 0)
			return List.of();
		long now = System.currentTimeMillis();
		List<NfcFrame> frames = new ArrayList<NfcFrame>(n);
		byte[] raw = out.getByteArray(0, n * FRAME_BYTES);
		ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < n; i++)
		{
			int tech = buf.getInt();
			int type = buf.getInt();
			int phase = buf.getInt();
			int flags = buf.getInt();
			int rate = buf.getInt();
			int nbytes = buf.getInt();
			double t0 = buf.getDouble();
			double t1 = buf.getDouble();
			byte[] nameBytes = new byte[NAME_MAX];
			buf.get(nameBytes);
			byte[] payload = new byte[PAYLOAD_MAX];
			buf.get(payload);
			int lim = Math.max(0, Math.min(PAYLOAD_MAX, nbytes));
			frames.add(new NfcFrame(now, tech, type, phase, flags, rate, t0, t1, cstr(nameBytes), hex(payload, lim)));
		}
		return frames;
	}

	@Override
	public void close()
	{
		HackrfSweepLibrary.nfc_dec_destroy(handle);
	}

	static String cstr(byte[] raw)
	{
		if (raw == null)
			return "";
		int n = 0;
		while (n < raw.length && raw[n] != 0)
			n++;
		return new String(raw, 0, n, java.nio.charset.StandardCharsets.US_ASCII);
	}

	static String hex(byte[] payload, int n)
	{
		if (payload == null || n <= 0)
			return "";
		StringBuilder sb = new StringBuilder(n * 3);
		for (int i = 0; i < n; i++)
		{
			if (i > 0)
				sb.append(' ');
			sb.append(String.format("%02X", payload[i] & 0xff));
		}
		return sb.toString();
	}
}
