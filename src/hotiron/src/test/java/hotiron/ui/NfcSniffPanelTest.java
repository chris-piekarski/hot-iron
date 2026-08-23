package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NfcSniffPanelTest
{
	@Test
	void setEnvelopeAndIdleClearDoNotThrow()
	{
		NfcSniffPanel panel = new NfcSniffPanel();
		assertDoesNotThrow(() -> panel.setEnvelope(new float[] { -36f, -12f }));
		assertDoesNotThrow(() -> panel.setSniffing(true));
		assertDoesNotThrow(() -> panel.setSniffing(false));
	}
}
