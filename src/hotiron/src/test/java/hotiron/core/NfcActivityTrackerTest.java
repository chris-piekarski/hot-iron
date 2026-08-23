package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class NfcActivityTrackerTest
{
	@Test
	void aSingleSweepFlashIsNotLabeled()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		DatasetSpectrum ds = NfcBandPlanTest.nfcSpectrum();
		NfcBandPlanTest.spike(ds, 13.56, -40f);
		NfcActivity first = tracker.update(ds, 12, 15, 0);
		assertEquals(NfcActivity.Kind.QUIET, first.kind);
		assertTrue(first.visible);
		assertTrue(first.confidence < NfcActivityTracker.SHOW_AT);
	}

	@Test
	void continuousCarrierBecomesFieldOn()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		DatasetSpectrum ds = NfcBandPlanTest.nfcSpectrum();
		NfcBandPlanTest.spike(ds, 13.56, -40f);
		NfcActivity last = drive(tracker, ds, now, 800, 20);
		assertEquals(NfcActivity.Kind.FIELD_ON, last.kind, last.summary());
		assertTrue(last.confidence >= NfcActivityTracker.SHOW_AT);
		assertTrue(last.duty >= 0.85f);
	}

	@Test
	void regularBlinksArePollingNotMorseData()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		NfcActivity last = NfcActivity.quiet();
		for (int t = 0; t <= 2500; t += 20)
		{
			now.set(t);
			DatasetSpectrum ds = NfcBandPlanTest.nfcSpectrum();
			if (t % 400 < 150)
				NfcBandPlanTest.spike(ds, 13.56, -40f);
			last = tracker.update(ds, 12, 15, t);
		}
		assertEquals(NfcActivity.Kind.POLLING, last.kind, last.summary());
		assertTrue(last.pollHz > 2f && last.pollHz < 4f, "150/250 ms is 2.5 Hz");
		assertTrue(last.summary().contains("not Morse"));
	}

	@Test
	void narrowSlowKeyingIsHiferNotNfcPayload()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		NfcActivity last = NfcActivity.quiet();
		for (int t = 0; t <= 2500; t += 20)
		{
			now.set(t);
			DatasetSpectrum ds = NfcBandPlanTest.nfcSpectrum();
			if (t % 600 < 400)
				NfcBandPlanTest.spike(ds, 13.56, -40f);
			last = tracker.update(ds, 12, 15, t);
		}
		assertEquals(NfcActivity.Kind.HIFER, last.kind, last.summary());
		assertTrue(last.summary().contains("HiFER"));
		assertFalse(last.summary().toLowerCase().contains("airtag") && last.kind == NfcActivity.Kind.NFC_AB);
	}

	@Test
	void typeAbSidebandsWinOverABareCarrier()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		DatasetSpectrum ds = NfcBandPlanTest.nfcSpectrum();
		NfcBandPlanTest.spike(ds, 13.56, -40f);
		NfcBandPlanTest.spike(ds, 12.7125, -55f);
		NfcBandPlanTest.spike(ds, 14.4075, -54f);
		NfcActivity last = drive(tracker, ds, now, 800, 20);
		assertEquals(NfcActivity.Kind.NFC_AB, last.kind, last.summary());
		assertTrue(last.sidebandAb);
		assertTrue(last.summary().contains("12.71"));
	}

	@Test
	void zoomedOutHidesTheOverlay()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 3, 30, -90f);
		NfcBandPlanTest.spike(ds, 13.56, -20f);
		NfcActivity hidden = tracker.update(ds, 3, 30, 0);
		assertEquals(NfcActivity.Kind.HIDDEN, hidden.kind);
		assertFalse(hidden.visible);
	}

	@Test
	void resetDropsPartialConfidence()
	{
		AtomicLong now = new AtomicLong(0);
		NfcActivityTracker tracker = new NfcActivityTracker(now::get);
		DatasetSpectrum ds = NfcBandPlanTest.nfcSpectrum();
		NfcBandPlanTest.spike(ds, 13.56, -40f);
		drive(tracker, ds, now, 200, 20);
		assertTrue(tracker.last().confidence > 0f);
		tracker.reset();
		assertEquals(NfcActivity.Kind.HIDDEN, tracker.last().kind);
		assertEquals(NfcActivity.Kind.QUIET, tracker.update(ds, 12, 15, 220).kind);
	}

	@Test
	void jsonNamesTheKindAndRefusesAirTagDecode()
	{
		NfcActivity quiet = NfcActivity.quietVisible();
		String json = quiet.toJson();
		assertTrue(json.contains("\"kind\":\"quiet\""));
		assertTrue(json.contains("trackingHint"));
		assertTrue(json.contains("Bluetooth"));
		assertTrue(json.contains("does not decode"));
		assertEquals("quiet", NfcActivity.Kind.QUIET.json);
		assertEquals("nfc-ab", NfcActivity.Kind.NFC_AB.json);
	}

	private static NfcActivity drive(NfcActivityTracker tracker, DatasetSpectrum ds, AtomicLong now, int endMs,
			int stepMs)
	{
		NfcActivity last = NfcActivity.quiet();
		for (int t = 0; t <= endMs; t += stepMs)
		{
			now.set(t);
			last = tracker.update(ds, 12, 15, t);
		}
		return last;
	}
}
