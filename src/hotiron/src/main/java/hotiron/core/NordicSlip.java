package hotiron.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Nordic nRF Sniffer UART SLIP (not RFC 1055). Consume their bytes;
 * do not invent a second link layer.
 */
public final class NordicSlip
{
	public static final int START = 0xAB;
	public static final int END = 0xBC;
	public static final int ESC = 0xCD;
	public static final int ESC_START = 0xAC;
	public static final int ESC_END = 0xBD;
	public static final int ESC_ESC = 0xCE;

	private NordicSlip()
	{
	}

	public static byte[] encode(byte[] payload)
	{
		if (payload == null)
			payload = new byte[0];
		ByteArrayOutputStream out = new ByteArrayOutputStream(payload.length + 4);
		out.write(START);
		for (byte b : payload)
		{
			int v = b & 0xFF;
			if (v == START)
			{
				out.write(ESC);
				out.write(ESC_START);
			}
			else if (v == END)
			{
				out.write(ESC);
				out.write(ESC_END);
			}
			else if (v == ESC)
			{
				out.write(ESC);
				out.write(ESC_ESC);
			}
			else
				out.write(v);
		}
		out.write(END);
		return out.toByteArray();
	}

	public static final class Decoder
	{
		private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
		private boolean esc;
		private boolean inFrame;

		public List<byte[]> push(byte[] data, int off, int len)
		{
			List<byte[]> frames = new ArrayList<>();
			if (data == null)
				return frames;
			int end = Math.min(data.length, off + len);
			for (int i = Math.max(0, off); i < end; i++)
			{
				int v = data[i] & 0xFF;
				if (v == START)
				{
					buf.reset();
					esc = false;
					inFrame = true;
					continue;
				}
				if (!inFrame)
					continue;
				if (v == END)
				{
					if (buf.size() > 0)
						frames.add(buf.toByteArray());
					buf.reset();
					esc = false;
					inFrame = false;
					continue;
				}
				if (esc)
				{
					if (v == ESC_START)
						buf.write(START);
					else if (v == ESC_END)
						buf.write(END);
					else if (v == ESC_ESC)
						buf.write(ESC);
					else
						buf.write(v);
					esc = false;
				}
				else if (v == ESC)
					esc = true;
				else
					buf.write(v);
			}
			return frames;
		}
	}
}
