package hotiron.core;

/**
 * Host playback: Java Sound first, PulseAudio on WSL/Linux when there is
 * no mixer. Unit tests pass a fake {@link AudioSink}; they do not call
 * {@link #openPlayback()}.
 */
public final class AudioSinks
{
	private AudioSinks()
	{
	}

	public static AudioSink openPlayback()
	{
		JavaSoundAudioSink javaSound = new JavaSoundAudioSink();
		if (javaSound.available())
		{
			System.err.println("FM listen: Java Sound output");
			return javaSound;
		}
		javaSound.close();
		PulseSimpleAudioSink pulse = PulseSimpleAudioSink.open();
		if (pulse != null)
			return pulse;
		System.err.println("FM listen: no speakers (Java Sound mixer empty, PulseAudio unavailable)");
		return new RecordingAudioSink();
	}
}
