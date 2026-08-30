package hotiron.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Host speakers via Java Sound. If no mixer is available (common on
 * headless / WSL without Pulse), writes are dropped and {@link #available()}
 * is false.
 */
public final class JavaSoundAudioSink implements AudioSink
{
	private final SourceDataLine line;
	private final byte[] bytes = new byte[8192];

	public JavaSoundAudioSink()
	{
		this.line = openLine();
	}

	private static SourceDataLine openLine()
	{
		AudioFormat fmt = new AudioFormat(WfmDemodulator.AUDIO_RATE_HZ, 16, 1, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
		try
		{
			if (!AudioSystem.isLineSupported(info))
			{
				System.err.println("FM listen: no Java Sound output line (check Pulse/PipeWire / WSL audio).");
				return null;
			}
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			/* 200 ms. 100 ms underran when the UI briefly stalled the mixer. */
			line.open(fmt, WfmDemodulator.AUDIO_RATE_HZ / 5 * 2);
			line.start();
			return line;
		}
		catch (LineUnavailableException | IllegalArgumentException | SecurityException e)
		{
			System.err.println("FM listen: audio output unavailable: " + e.getMessage());
			return null;
		}
	}

	public boolean available()
	{
		return line != null;
	}

	@Override
	public int sampleRateHz()
	{
		return WfmDemodulator.AUDIO_RATE_HZ;
	}

	@Override
	public void write(short[] pcm, int offset, int length)
	{
		if (line == null || pcm == null || length <= 0)
			return;
		int from = Math.max(0, offset);
		int n = Math.min(length, pcm.length - from);
		int i = 0;
		while (i < n)
		{
			int chunk = Math.min((n - i), bytes.length / 2);
			int b = 0;
			for (int s = 0; s < chunk; s++)
			{
				int v = pcm[from + i + s];
				bytes[b++] = (byte) (v & 0xff);
				bytes[b++] = (byte) ((v >> 8) & 0xff);
			}
			line.write(bytes, 0, b);
			i += chunk;
		}
	}

	@Override
	public void close()
	{
		if (line == null)
			return;
		try
		{
			line.stop();
			line.flush();
		}
		catch (RuntimeException ignored)
		{
		}
		line.close();
	}
}
