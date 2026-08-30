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

	@Test
	void seekPrefersPictureAndSkipsNoLock() {
		List<TvStationHit> hits = List.of(
				graded(18, TvChannelGrade.OCCUPIED),
				graded(28, TvChannelGrade.NO_LOCK),
				graded(33, TvChannelGrade.PICTURE),
				graded(36, TvChannelGrade.ATSC_LIKE));
		assertEquals(36, TvStationDial.seek(hits, 33, +1).fccChannel);
		assertEquals(18, TvStationDial.seek(hits, 36, +1).fccChannel);
		assertEquals(33, TvStationDial.seek(hits, 18, +1).fccChannel);
		assertEquals(36, TvStationDial.seek(hits, 18, -1).fccChannel);
	}

	@Test
	void mergeLiveDoesNotDemotePicture() {
		TvStationHit pic = new TvStationHit(TvChannelPlan.findByFccChannel(33), -40f, 1f,
				TvChannelGrade.PICTURE, "picture", 12, 15f, 6f);
		TvStationHit live = new TvStationHit(TvChannelPlan.findByFccChannel(33), -38f, 0.8f,
				TvChannelGrade.OCCUPIED, "", 0, Float.NaN, Float.NaN);
		List<TvStationHit> merged = TvStationDial.mergeLive(List.of(pic), List.of(live), 580, 594);
		assertEquals(1, merged.size());
		assertEquals(TvChannelGrade.PICTURE, merged.get(0).grade);
		assertEquals(12, merged.get(0).frames);
		assertEquals(-38f, merged.get(0).powerDbm, 0.01f);
	}

	@Test
	void keepWatchMemoryDropsOccupancyOnly() {
		List<TvStationHit> kept = TvStationDial.keepWatchMemory(List.of(
				graded(14, TvChannelGrade.OCCUPIED),
				graded(33, TvChannelGrade.PICTURE),
				graded(28, TvChannelGrade.NO_LOCK)));
		assertEquals(2, kept.size());
		assertTrue(kept.stream().anyMatch(h -> h.channel.fccChannel == 33 && h.grade == TvChannelGrade.PICTURE));
		assertTrue(kept.stream().anyMatch(h -> h.channel.fccChannel == 28 && h.grade == TvChannelGrade.NO_LOCK));
	}

	@Test
	void stampPromotesOccupancyToPicture() {
		List<TvStationHit> next = TvStationDial.stamp(List.of(graded(33, TvChannelGrade.ATSC_LIKE)), 33,
				TvChannelGrade.PICTURE, "picture", 4, 12f);
		assertEquals(TvChannelGrade.PICTURE, next.get(0).grade);
		assertEquals(4, next.get(0).frames);
		assertFalse(TvStationDial.sameChannels(List.of(graded(33, TvChannelGrade.ATSC_LIKE)), next));
	}

	private static TvStationHit hit(int ch) {
		return new TvStationHit(TvChannelPlan.findByFccChannel(ch), -40f, 1f);
	}

	private static TvStationHit graded(int ch, TvChannelGrade grade) {
		return new TvStationHit(TvChannelPlan.findByFccChannel(ch), -40f, 1f, grade, "", 0, Float.NaN,
				Float.NaN);
	}
}
