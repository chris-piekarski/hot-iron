package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BandScanSinkTest
{
	private static final class Rec implements BandScanSink.Effects
	{
		int clears;
		int finishes;
		final List<FrequencyRange> retunes = new ArrayList<>();

		@Override
		public void clearWaterfall()
		{
			clears++;
		}

		@Override
		public void retune(FrequencyRange next)
		{
			retunes.add(next);
		}

		@Override
		public void finishScan()
		{
			finishes++;
		}
	}

	@Test
	void inactiveIsNoop()
	{
		Rec rec = new Rec();
		BandScanSink.advance(new BandScanSession(), fmDs(), 10_000, rec);
		assertEquals(0, rec.finishes);
		assertEquals(0, rec.clears);
		assertTrue(rec.retunes.isEmpty());
	}

	@Test
	void matchingDatasetStartsDwellClock()
	{
		BandScanSession session = new BandScanSession();
		session.start(BandScan.FM, 0);
		Rec rec = new Rec();
		BandScanSink.advance(session, fmDs(), 100, rec);
		assertTrue(session.windowLive());
		assertEquals(0, rec.finishes);
		assertTrue(rec.retunes.isEmpty());
	}

	@Test
	void mismatchedDatasetDoesNotCountAsDwell()
	{
		BandScanSession session = new BandScanSession();
		session.start(BandScan.FM, 0);
		DatasetSpectrum wifi = new DatasetSpectrum(20_000f, 2402, 2472, -90f);
		Rec rec = new Rec();
		BandScanSink.advance(session, wifi, BandScanSession.DWELL_MS + 50, rec);
		assertFalse(session.windowLive());
		assertEquals(0, rec.finishes);
	}

	@Test
	void fmFinishesAfterLiveDwell()
	{
		BandScanSession session = new BandScanSession();
		session.start(BandScan.FM, 0);
		Rec rec = new Rec();
		BandScanSink.advance(session, fmDs(), 100, rec);
		BandScanSink.advance(session, fmDs(), 100 + BandScanSession.DWELL_MS, rec);
		assertEquals(1, rec.finishes);
		assertTrue(rec.retunes.isEmpty());
	}

	@Test
	void tvHopClearsWaterfallAndRetunes()
	{
		BandScanSession session = new BandScanSession();
		session.start(BandScan.TV, 0);
		Rec rec = new Rec();
		DatasetSpectrum vhf = new DatasetSpectrum(100_000f, TvChannelPlan.VHF_VIEW_START_MHZ,
				TvChannelPlan.VHF_VIEW_END_MHZ, -90f);
		BandScanSink.advance(session, vhf, 50, rec);
		BandScanSink.advance(session, vhf, 50 + BandScanSession.DWELL_MS, rec);
		assertEquals(1, rec.clears);
		assertEquals(1, rec.retunes.size());
		assertEquals(TvChannelPlan.UHF_VIEW_START_MHZ, rec.retunes.get(0).getStartMHz());
		assertEquals(0, rec.finishes);
	}

	@Test
	void nfcHopClearsWaterfallAndAdvancesToHarmonic()
	{
		BandScanSession session = new BandScanSession();
		session.start(BandScan.NFC, 0);
		Rec rec = new Rec();
		DatasetSpectrum phy = new DatasetSpectrum(10_000f, NfcBandPlan.VIEW_START_MHZ, NfcBandPlan.VIEW_END_MHZ, -90f);
		BandScanSink.advance(session, phy, 50, rec);
		BandScanSink.advance(session, phy, 50 + BandScanSession.DWELL_MS, rec);
		assertEquals(1, rec.clears);
		assertEquals(1, rec.retunes.size());
		assertEquals(NfcBandPlan.H2_VIEW_START_MHZ, rec.retunes.get(0).getStartMHz());
		assertEquals(0, rec.finishes);
	}

	private static DatasetSpectrum fmDs()
	{
		return new DatasetSpectrum(100_000f, FmChannelPlan.VIEW_START_MHZ, FmChannelPlan.VIEW_END_MHZ, -90f);
	}
}
