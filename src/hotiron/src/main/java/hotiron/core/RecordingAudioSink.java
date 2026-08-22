package hotiron.core;

import java.util.ArrayList;

/**
 * Captures PCM for tests. Not a mixer.
 */
public final class RecordingAudioSink implements AudioSink
{
	private final ArrayList<Short> samples = new ArrayList<Short>();
	private boolean closed;

	@Override
	public int sampleRateHz()
	{
		return WfmDemodulator.AUDIO_RATE_HZ;
	}

	@Override
	public synchronized void write(short[] pcm, int offset, int length)
	{
		if (closed || pcm == null || length <= 0)
			return;
		int from = Math.max(0, offset);
		int to = Math.min(pcm.length, from + length);
		for (int i = from; i < to; i++)
			samples.add(pcm[i]);
	}

	@Override
	public synchronized void close()
	{
		closed = true;
	}

	public synchronized short[] snapshot()
	{
		short[] out = new short[samples.size()];
		for (int i = 0; i < out.length; i++)
			out[i] = samples.get(i);
		return out;
	}

	public synchronized int size()
	{
		return samples.size();
	}
}
