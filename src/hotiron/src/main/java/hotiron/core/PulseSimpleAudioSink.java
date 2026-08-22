package hotiron.core;

import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

/**
 * Playback via {@code libpulse-simple} (WSLg Pulse socket, native Linux
 * Pulse/PipeWire). Used when Java Sound has no mixer.
 */
public final class PulseSimpleAudioSink implements AudioSink
{
	private static final int PA_STREAM_PLAYBACK = 1;
	private static final int PA_SAMPLE_S16LE = 3;
	private static final String WSLG_PULSE = "unix:/mnt/wslg/PulseServer";

	private final PulseSimple lib;
	private final Pointer stream;
	private final byte[] bytes = new byte[8192];
	private final String server;

	public static PulseSimpleAudioSink open()
	{
		PulseSimple lib = loadLib();
		if (lib == null)
			return null;
		String server = pulseServer();
		SampleSpec spec = new SampleSpec();
		spec.format = PA_SAMPLE_S16LE;
		spec.rate = WfmDemodulator.AUDIO_RATE_HZ;
		spec.channels = 1;
		spec.write();
		IntByReference err = new IntByReference();
		Pointer stream = lib.pa_simple_new(server, "hotiron", PA_STREAM_PLAYBACK, null,
				"FM listen", spec, Pointer.NULL, Pointer.NULL, err);
		if (stream == null)
		{
			System.err.println("FM listen: PulseAudio pa_simple_new failed (err " + err.getValue()
					+ (server == null ? "" : ", server " + server) + ")");
			return null;
		}
		System.err.println("FM listen: PulseAudio " + (server == null ? "default" : server));
		return new PulseSimpleAudioSink(lib, stream, server);
	}

	static PulseSimple loadLib()
	{
		/*
		 * HackRFSweepNativeBridge sets jna.nosys=true so JNA will not search
		 * /usr/lib. Load Pulse by absolute path (WSL/Debian).
		 */
		String[] names = {
				"/usr/lib/x86_64-linux-gnu/libpulse-simple.so.0",
				"/usr/lib64/libpulse-simple.so.0",
				"pulse-simple",
				"libpulse-simple.so.0"
		};
		UnsatisfiedLinkError last = null;
		for (int i = 0; i < names.length; i++)
		{
			try
			{
				return Native.load(names[i], PulseSimple.class);
			}
			catch (UnsatisfiedLinkError e)
			{
				last = e;
			}
		}
		if (last != null)
			System.err.println("FM listen: cannot load libpulse-simple: " + last.getMessage());
		return null;
	}

	static String pulseServer()
	{
		String env = System.getenv("PULSE_SERVER");
		if (env != null && !env.isEmpty())
			return env;
		if (Files.exists(Path.of("/mnt/wslg/PulseServer")))
			return WSLG_PULSE;
		return null;
	}

	private PulseSimpleAudioSink(PulseSimple lib, Pointer stream, String server)
	{
		this.lib = lib;
		this.stream = stream;
		this.server = server;
	}

	public String server()
	{
		return server;
	}

	@Override
	public int sampleRateHz()
	{
		return WfmDemodulator.AUDIO_RATE_HZ;
	}

	@Override
	public void write(short[] pcm, int offset, int length)
	{
		if (pcm == null || length <= 0)
			return;
		int from = Math.max(0, offset);
		int n = Math.min(length, pcm.length - from);
		int i = 0;
		IntByReference err = new IntByReference();
		while (i < n)
		{
			int chunk = Math.min(n - i, bytes.length / 2);
			int b = 0;
			for (int s = 0; s < chunk; s++)
			{
				int v = pcm[from + i + s];
				bytes[b++] = (byte) (v & 0xff);
				bytes[b++] = (byte) ((v >> 8) & 0xff);
			}
			if (lib.pa_simple_write(stream, bytes, b, err) < 0)
			{
				System.err.println("FM listen: PulseAudio write failed (err " + err.getValue() + ")");
				return;
			}
			i += chunk;
		}
	}

	@Override
	public void close()
	{
		lib.pa_simple_free(stream);
	}

	public interface PulseSimple extends Library
	{
		Pointer pa_simple_new(String server, String name, int dir, String dev, String streamName, SampleSpec ss,
				Pointer map, Pointer attr, IntByReference error);

		int pa_simple_write(Pointer s, byte[] data, long bytes, IntByReference error);

		void pa_simple_free(Pointer s);
	}

	@Structure.FieldOrder({ "format", "rate", "channels" })
	public static class SampleSpec extends Structure
	{
		public int format;
		public int rate;
		public byte channels;
	}
}
