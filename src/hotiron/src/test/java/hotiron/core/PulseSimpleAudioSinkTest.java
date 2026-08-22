package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PulseSimpleAudioSinkTest {

	@Test
	void pulseServerUsesWslgSocketWhenPresent() {
		String server = PulseSimpleAudioSink.pulseServer();
		if (java.nio.file.Files.exists(java.nio.file.Path.of("/mnt/wslg/PulseServer")))
		{
			String env = System.getenv("PULSE_SERVER");
			if (env == null || env.isEmpty())
				assertEquals("unix:/mnt/wslg/PulseServer", server);
			else
				assertEquals(env, server);
		}
	}

	@Test
	void openDoesNotThrowWhenPulseIsMissing() {
		assertDoesNotThrow(() -> {
			PulseSimpleAudioSink sink = PulseSimpleAudioSink.open();
			if (sink != null)
				sink.close();
		});
	}

	@Test
	void loadLibFindsDebianPulseSimpleWhenPresent() {
		if (!java.nio.file.Files.exists(java.nio.file.Path.of("/usr/lib/x86_64-linux-gnu/libpulse-simple.so.0")))
			return;
		System.setProperty("jna.nosys", "true");
		assertNotNull(PulseSimpleAudioSink.loadLib());
	}
}
