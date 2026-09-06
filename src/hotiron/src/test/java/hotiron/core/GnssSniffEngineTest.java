package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class GnssSniffEngineTest
{
	@Test
	void cropGpsKeepsTheL1View()
	{
		float[] row = new float[1024];
		java.util.Arrays.fill(row, -80f);
		row[512] = -40f;
		GnssSniffEngine.ViewRow view = GnssSniffEngine.cropGps(row, 3906.25f, GnssSniffEngine.LO_HZ);
		assertFalse(view.isEmpty());
		assertTrue(view.mhz[0] >= GnssBandPlan.GPS_VIEW_START_MHZ - 0.05);
		assertTrue(view.mhz[view.mhz.length - 1] <= GnssBandPlan.GPS_VIEW_END_MHZ + 0.05);
	}

	@Test
	void workerAcquiresSyntheticPrn() throws Exception
	{
		GnssSniffEngine engine = new GnssSniffEngine(new GpsL1Acquirer(1000, 500));
		AtomicReference<List<GpsSvHit>> got = new AtomicReference<>(List.of());
		engine.setHitsListener(got::set);
		engine.start();
		try
		{
			byte[] iq = GpsL1Acquirer.syntheticMs(3, 0, 0, 0.45f);
			byte[] fat = new byte[iq.length * 4];
			for (int i = 0; i < 4; i++)
				System.arraycopy(iq, 0, fat, i * iq.length, iq.length);
			long deadline = System.currentTimeMillis() + 5000;
			while (System.currentTimeMillis() < deadline && got.get().isEmpty())
			{
				engine.offerIq(fat);
				Thread.sleep(40);
			}
			assertTrue(got.get().stream().anyMatch(h -> h.prn == 3), "hits " + got.get());
			assertFalse(engine.lastHits().isEmpty());
			List<GpsSvHit> held = engine.lastHits();
			byte[] quiet = new byte[iq.length * 4];
			long quietUntil = System.currentTimeMillis() + 700;
			while (System.currentTimeMillis() < quietUntil)
			{
				engine.offerIq(quiet);
				Thread.sleep(40);
			}
			assertEquals(held, engine.lastHits());
			assertTrue(engine.lastHits().stream().anyMatch(h -> h.prn == 3));
		}
		finally
		{
			engine.stop();
		}
	}

	@Test
	void hitsJsonListsPrns()
	{
		String json = GnssSniffEngine.hitsJson(List.of(new GpsSvHit(5, 42.1f, 1500, 10, 12f)));
		assertTrue(json.contains("\"prn\":5"));
		assertTrue(json.contains("1575420000"));
		assertTrue(json.contains("4000000"));
	}
}
