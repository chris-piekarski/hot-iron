package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TvChannelPlanTest {

	@Test
	void watchSampleRateLeavesRoomForTheSixMegahertzBrick() {
		assertEquals(16_000_000, TvChannelPlan.IQ_RATE_HZ);
		assertTrue(TvChannelPlan.IQ_RATE_HZ / 2 > TvChannelPlan.WIDTH_MHZ * 1_000_000);
	}

	@Test
	void postRepackRasterHasThirtyFiveChannels() {
		assertEquals(35, TvChannelPlan.CHANNELS.size());
		assertEquals(54, TvChannelPlan.findByFccChannel(2).lowMHz);
		assertEquals(60, TvChannelPlan.findByFccChannel(2).highMHz());
		assertEquals(66, TvChannelPlan.findByFccChannel(4).lowMHz);
		assertEquals(76, TvChannelPlan.findByFccChannel(5).lowMHz);
		assertEquals(82, TvChannelPlan.findByFccChannel(6).lowMHz);
		assertEquals(88, TvChannelPlan.findByFccChannel(6).highMHz());
		assertEquals(174, TvChannelPlan.findByFccChannel(7).lowMHz);
		assertEquals(210, TvChannelPlan.findByFccChannel(13).lowMHz);
		assertEquals(216, TvChannelPlan.findByFccChannel(13).highMHz());
		assertEquals(470, TvChannelPlan.findByFccChannel(14).lowMHz);
		assertEquals(476, TvChannelPlan.findByFccChannel(14).highMHz());
		assertEquals(473.0, TvChannelPlan.findByFccChannel(14).centerMHz(), 0.0001);
		assertEquals(470.31, TvChannelPlan.findByFccChannel(14).pilotMHz(), 0.0001);
		assertEquals(602, TvChannelPlan.findByFccChannel(36).lowMHz);
		assertEquals(608, TvChannelPlan.findByFccChannel(36).highMHz());
		assertNull(TvChannelPlan.findByFccChannel(1));
		assertNull(TvChannelPlan.findByFccChannel(37));
	}

	@Test
	void containingMHzSkipsTheFmAviationGap() {
		assertEquals(6, TvChannelPlan.containingMHz(85).fccChannel);
		assertNull(TvChannelPlan.containingMHz(97.3));
		assertEquals(7, TvChannelPlan.containingMHz(174.1).fccChannel);
		assertEquals(14, TvChannelPlan.containingMHz(473).fccChannel);
	}

	@Test
	void tuneSkipsPlanGapsAndWraps() {
		assertEquals(5, TvChannelPlan.tune(4, +1).fccChannel);
		assertEquals(7, TvChannelPlan.tune(6, +1).fccChannel);
		assertEquals(14, TvChannelPlan.tune(13, +1).fccChannel);
		assertEquals(2, TvChannelPlan.tune(36, +1).fccChannel);
		assertEquals(36, TvChannelPlan.tune(2, -1).fccChannel);
		assertEquals(6, TvChannelPlan.tune(7, -1).fccChannel);
	}

	@Test
	void clampPinsToPlan() {
		assertEquals(2, TvChannelPlan.clamp(0).fccChannel);
		assertEquals(36, TvChannelPlan.clamp(99).fccChannel);
		assertEquals(14, TvChannelPlan.clamp(14).fccChannel);
	}

	@Test
	void detectIgnoresFlatNoiseAndFlagsASixMegahertzBrick() {
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 470, 608, -90f);
		assertTrue(TvChannelPlan.detectStations(ds, 470, 608).isEmpty());
		int n = ds.spectrumLength();
		for (int i = 0; i < n; i++)
		{
			double mhz = ds.getFrequency(i) / 1_000_000d;
			if (mhz >= 470 && mhz < 476)
				ds.getSpectrumArray()[i] = -50f;
		}
		List<TvStationHit> hits = TvChannelPlan.detectStations(ds, 470, 608);
		assertEquals(1, hits.size());
		assertEquals(14, hits.get(0).channel.fccChannel);
		assertEquals(TvChannelGrade.OCCUPIED, hits.get(0).grade);
	}

	@Test
	void pilotBumpMarksAtscLikeNotPicture() {
		DatasetSpectrum ds = new DatasetSpectrum(50_000f, 470, 608, -90f);
		int n = ds.spectrumLength();
		TvChannel ch14 = TvChannelPlan.findByFccChannel(14);
		for (int i = 0; i < n; i++)
		{
			double mhz = ds.getFrequency(i) / 1_000_000d;
			if (mhz >= 470 && mhz < 476)
				ds.getSpectrumArray()[i] = -50f;
			if (Math.abs(mhz - ch14.pilotMHz()) < 0.08)
				ds.getSpectrumArray()[i] = -40f;
		}
		List<TvStationHit> hits = TvChannelPlan.detectStations(ds, 470, 608);
		assertEquals(1, hits.size());
		assertEquals(TvChannelGrade.ATSC_LIKE, hits.get(0).grade);
		assertTrue(hits.get(0).pilotExcessDb >= TvChannelPlan.PILOT_EXCESS_DB);
	}

	@Test
	void overlapsBroadcastIsUsTvNotWifi() {
		assertTrue(TvChannelPlan.overlapsBroadcast(470, 608));
		assertTrue(TvChannelPlan.overlapsBroadcast(54, 88));
		assertTrue(TvChannelPlan.overlapsBroadcast(174, 216));
		assertFalse(TvChannelPlan.overlapsBroadcast(88, 108));
		assertFalse(TvChannelPlan.overlapsBroadcast(2402, 2472));
	}

	@Test
	void parkedIqDetectsASixMegahertzBrick() {
		float[] mhz = new float[160];
		float[] dbfs = new float[160];
		for (int i = 0; i < mhz.length; i++)
		{
			mhz[i] = 468f + i * 0.1f;
			dbfs[i] = mhz[i] >= 470f && mhz[i] < 476f ? -48f : -70f;
		}
		List<TvStationHit> hits = TvChannelPlan.detectStations(mhz, dbfs);
		assertEquals(1, hits.size());
		assertEquals(14, hits.get(0).channel.fccChannel);
	}
}
