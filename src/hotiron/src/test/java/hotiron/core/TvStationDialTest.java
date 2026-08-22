package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TvStationDialTest {

	@Test
	void seekJumpsDetectedChannelsAndWraps() {
		List<TvStationHit> hits = List.of(hit(7), hit(14), hit(36));
		assertEquals(36, TvStationDial.seek(hits, 14, +1).fccChannel);
		assertEquals(7, TvStationDial.seek(hits, 36, +1).fccChannel);
		assertEquals(14, TvStationDial.seek(hits, 36, -1).fccChannel);
	}

	@Test
	void emptySeekFallsBackToTune() {
		assertEquals(15, TvStationDial.seek(List.of(), 14, +1).fccChannel);
		assertEquals(13, TvStationDial.seek(List.of(), 14, -1).fccChannel);
	}

	private static TvStationHit hit(int ch) {
		return new TvStationHit(TvChannelPlan.findByFccChannel(ch), -40f, 1f);
	}
}
