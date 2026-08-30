package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import hotiron.core.FmChannelPlan;
import hotiron.core.FmStationHit;

class TunerPanelTest
{
	@Test
	void liveAudioDrivesTheNeedleWhileListening()
	{
		TunerPanel p = new TunerPanel();
		p.setKHz(97300);
		p.setListening(true);
		p.setLiveLevel(0.62f);
		assertEquals(0.62f, p.gainGauge().getValue01(), 0.01f);
		p.setStations(List.of(new FmStationHit(FmChannelPlan.clamp(97.3), -40f)));
		assertEquals(0.62f, p.gainGauge().getValue01(), 0.01f);
		p.setListening(false);
		assertTrue(p.gainGauge().getValue01() > 0.4f, "leaving Listen falls back to Scan S-meter");
	}

	@Test
	void liveLevelIsIgnoredWhenNotListening()
	{
		TunerPanel p = new TunerPanel();
		p.setListening(false);
		p.setLiveLevel(0.9f);
		assertEquals(0f, p.gainGauge().getValue01(), 0.01f);
	}
}
