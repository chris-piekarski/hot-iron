package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;

class StationDetectSinkTest
{
	@Test
	void zoomedOutDoesNotOverwriteSeekList()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		FmStationHit keep = new FmStationHit(FmChannelPlan.nearest(97.3), -40f, 0.9f);
		s.getDetectedFmStations().setValue(List.of(keep));
		StationDetectSink sink = new StationDetectSink();
		DatasetSpectrum wifi = new DatasetSpectrum(20_000f, 2402, 2472, -90f);
		sink.update(wifi, new FrequencyRange(2402, 2472), s);
		assertEquals(1, s.getDetectedFmStations().getValue().size());
		assertEquals("97.3", s.getDetectedFmStations().getValue().get(0).label());
		assertTrue(sink.lastFm().isEmpty(), "last FM hits stay empty until an FM-scale view");
	}

	@Test
	void fmViewWritesSeekListOnceStationsLock()
	{
		AtomicLong now = new AtomicLong(0);
		StationDetectSink sink = new StationDetectSink(now::get);
		FakeHackRFSettings s = new FakeHackRFSettings();
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		FrequencyRange fm = new FrequencyRange(88, 108);
		sink.update(ds, fm, s);
		assertTrue(s.getDetectedFmStations().getValue().isEmpty(), "one sweep is a flash");
		for (int t = 0; t <= 700; t += 20)
		{
			now.set(t);
			sink.update(ds, fm, s);
		}
		assertEquals(1, s.getDetectedFmStations().getValue().size());
		assertEquals("97.3", s.getDetectedFmStations().getValue().get(0).label());
		assertTrue(FmStationDial.sameChannels(s.getDetectedFmStations().getValue(), sink.lastFm()));
	}

	@Test
	void axisChangeOnFmDropsPartialConfidence()
	{
		AtomicLong now = new AtomicLong(0);
		StationDetectSink sink = new StationDetectSink(now::get);
		FakeHackRFSettings s = new FakeHackRFSettings();
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		now.set(200);
		sink.update(ds, new FrequencyRange(88, 108), s);
		assertTrue(sink.lastFm().isEmpty());
		sink.onAxisChanged(88, 108);
		now.set(210);
		sink.update(ds, new FrequencyRange(88, 108), s);
		assertTrue(sink.lastFm().isEmpty(), "reset must drop the in-progress lock");
	}

	@Test
	void tvMergeKeepsStationsOutsideTheLiveWindow()
	{
		FakeHackRFSettings s = new FakeHackRFSettings();
		TvChannel ch2 = TvChannelPlan.CHANNELS.get(0);
		s.getDetectedTvStations().setValue(List.of(new TvStationHit(ch2, -40f, 1f)));
		StationDetectSink sink = new StationDetectSink();
		DatasetSpectrum uhf = new DatasetSpectrum(100_000f, 470, 608, -90f);
		sink.update(uhf, new FrequencyRange(470, 608), s);
		assertTrue(s.getDetectedTvStations().getValue().stream().anyMatch(h -> h.channel.fccChannel == ch2.fccChannel),
				"VHF Seek hit must survive a UHF sweep");
	}

	@Test
	void sameFmListDoesNotFireModelListeners()
	{
		AtomicLong now = new AtomicLong(0);
		StationDetectSink sink = new StationDetectSink(now::get);
		FakeHackRFSettings s = new FakeHackRFSettings();
		DatasetSpectrum ds = fmSpectrum();
		spike(ds, 97.3, -40f);
		FrequencyRange fm = new FrequencyRange(88, 108);
		for (int t = 0; t <= 700; t += 20)
		{
			now.set(t);
			sink.update(ds, fm, s);
		}
		AtomicInteger fires = new AtomicInteger();
		s.getDetectedFmStations().addListener(hits -> fires.incrementAndGet());
		now.set(900);
		sink.update(ds, fm, s);
		assertEquals(0, fires.get());
	}

	@Test
	void nfcViewUpdatesActivityAndWifiHidesIt()
	{
		AtomicLong now = new AtomicLong(0);
		StationDetectSink sink = new StationDetectSink(now::get);
		FakeHackRFSettings s = new FakeHackRFSettings();
		DatasetSpectrum ds = new DatasetSpectrum(10_000f, 12, 15, -90f);
		NfcBandPlanTest.spike(ds, 13.56, -40f);
		for (int t = 0; t <= 800; t += 20)
		{
			now.set(t);
			sink.update(ds, new FrequencyRange(12, 15), s);
		}
		assertEquals(NfcActivity.Kind.FIELD_ON, sink.lastNfc().kind);
		sink.update(new DatasetSpectrum(20_000f, 2402, 2472, -90f), new FrequencyRange(2402, 2472), s);
		assertEquals(NfcActivity.Kind.HIDDEN, sink.lastNfc().kind);
		assertFalse(sink.lastNfc().visible);
	}

	@Test
	void axisChangeOnNfcDropsPartialConfidence()
	{
		AtomicLong now = new AtomicLong(0);
		StationDetectSink sink = new StationDetectSink(now::get);
		FakeHackRFSettings s = new FakeHackRFSettings();
		DatasetSpectrum ds = new DatasetSpectrum(10_000f, 12, 15, -90f);
		NfcBandPlanTest.spike(ds, 13.56, -40f);
		now.set(200);
		sink.update(ds, new FrequencyRange(12, 15), s);
		assertEquals(NfcActivity.Kind.QUIET, sink.lastNfc().kind);
		sink.onAxisChanged(12, 15);
		now.set(210);
		sink.update(ds, new FrequencyRange(12, 15), s);
		assertEquals(NfcActivity.Kind.QUIET, sink.lastNfc().kind, "reset must drop the in-progress lock");
	}

	private static DatasetSpectrum fmSpectrum()
	{
		return new DatasetSpectrum(100_000f, 88, 108, -90f);
	}

	private static void spike(DatasetSpectrum ds, double mhz, float dbm)
	{
		double targetHz = mhz * 1_000_000d;
		int best = 0;
		double bestErr = Double.POSITIVE_INFINITY;
		for (int i = 0; i < ds.spectrumLength(); i++)
		{
			double err = Math.abs(ds.getFrequency(i) - targetHz);
			if (err < bestErr)
			{
				bestErr = err;
				best = i;
			}
		}
		ds.getSpectrumArray()[best] = dbm;
	}
}
