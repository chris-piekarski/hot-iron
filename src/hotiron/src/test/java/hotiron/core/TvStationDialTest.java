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

	@Test
	void mergeLiveKeepsStationsOutsideTheParkedWindow() {
		List<TvStationHit> remembered = List.of(hit(14), hit(28), hit(33));
		List<TvStationHit> live = List.of(hit(28));
		List<TvStationHit> merged = TvStationDial.mergeLive(remembered, live, 549, 565);
		assertEquals(3, merged.size());
		assertEquals(14, merged.get(0).channel.fccChannel);
		assertEquals(28, merged.get(1).channel.fccChannel);
		assertEquals(33, merged.get(2).channel.fccChannel);
		assertEquals(33, TvStationDial.seek(merged, 28, +1).fccChannel);
		assertEquals(14, TvStationDial.seek(merged, 28, -1).fccChannel);
	}

	private static TvStationHit hit(int ch) {
		return new TvStationHit(TvChannelPlan.findByFccChannel(ch), -40f, 1f);
	}
}
