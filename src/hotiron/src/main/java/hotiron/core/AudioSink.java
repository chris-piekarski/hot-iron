package hotiron.core;

/**
 * 48 kHz mono PCM destination. Unit tests use a recording fake so they
 * never open a mixer.
 */
public interface AudioSink
{
	int sampleRateHz();

	void write(short[] pcm, int offset, int length);

	void close();
}
