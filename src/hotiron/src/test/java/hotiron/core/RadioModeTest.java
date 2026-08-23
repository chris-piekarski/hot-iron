package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class RadioModeTest
{
	@Test
	void ofReadsSettingsAndDoesNotStoreASecondMode()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		assertEquals(RadioMode.SWEEP, RadioMode.of(s));
		assertEquals(RadioMode.SWEEP, s.radioMode());
		s.startListen();
		assertEquals(RadioMode.LISTEN, s.radioMode());
		assertTrue(s.radioMode().parked());
		s.startWatch();
		assertEquals(RadioMode.WATCH, s.radioMode());
		s.releaseRadio();
		assertEquals(RadioMode.STOPPED, s.radioMode());
		assertFalse(s.radioMode().parked());
		assertEquals(RadioMode.STOPPED, RadioMode.of(null));
	}

	@Test
	void jsonNamesAreStableForMcp()
	{
		assertEquals("sweep", RadioMode.SWEEP.jsonName());
		assertEquals("listen", RadioMode.LISTEN.jsonName());
		assertEquals("watch", RadioMode.WATCH.jsonName());
		assertEquals("stopped", RadioMode.STOPPED.jsonName());
	}
}
