package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class NfcSniffEngineTest
{
	@Test
	void envelopeIsCarrierMixedMagnitude()
	{
		byte[] iq = new byte[1000];
		for (int n = 0; n < 500; n++)
		{
			double ph = 2.0 * Math.PI * 0.2 * n;
			iq[2 * n] = (byte) Math.round(64 * Math.cos(ph));
			iq[2 * n + 1] = (byte) Math.round(64 * Math.sin(ph));
		}
		float[] env = NfcSniffEngine.envelope(iq);
		assertEquals(1, env.length);
		assertTrue(env[0] > -10f);
		assertTrue(NfcSniffEngine.envelope(new byte[1]).length == 0);
	}

	@Test
	void cropPhyKeepsThe12To15Window()
	{
		float binHz = 10_000f;
		int n = 1024;
		float[] row = new float[n];
		for (int i = 0; i < n; i++)
			row[i] = i;
		NfcSniffEngine.ViewRow view = NfcSniffEngine.cropPhy(row, binHz, NfcSniffEngine.LO_HZ);
		assertFalse(view.isEmpty());
		assertTrue(view.mhz[0] >= NfcBandPlan.VIEW_START_MHZ - 0.02);
		assertTrue(view.mhz[view.mhz.length - 1] < NfcBandPlan.VIEW_END_MHZ);
		assertEquals(view.mhz.length, view.dbfs.length);
	}

	@Test
	void injectedDecoderEmitsFramesAfterSettle() throws Exception
	{
		NfcSniffEngine engine = new NfcSniffEngine();
		CountDownLatch saw = new CountDownLatch(1);
		List<NfcFrame> got = new ArrayList<NfcFrame>();
		NfcFrame reqa = new NfcFrame(1L, 0x0101, 0x0102, 0, 0, 106000, 0, 0.001, "REQA", "26");
		engine.setFrameListener(f -> {
			got.add(f);
			saw.countDown();
		});
		engine.start(iq -> List.of(reqa));
		assertTrue(engine.running());
		byte[] iq = new byte[NfcSniffEngine.IQ_RATE_HZ > 0 ? 64 : 64];
		for (int i = 0; i < 20; i++)
			engine.offerIq(iq);
		Thread.sleep(NfcSniffEngine.SETTLE_MS + 80);
		engine.offerIq(iq);
		assertTrue(saw.await(1, TimeUnit.SECONDS), "decoder frames after settle");
		assertEquals("REQA", got.get(0).name);
		assertEquals("A REQA  26", got.get(0).line());
		engine.stop();
		assertFalse(engine.running());
		assertTrue(engine.offerIq(iq) == false);
	}

	@Test
	void frameJsonAndCarrierFlags()
	{
		NfcFrame on = new NfcFrame(1, 0x0100, 0x0101, 0, 0, 0, 0, 0, "field on", "");
		assertTrue(on.fieldOn());
		assertTrue(on.carrier());
		assertEquals("field on", on.line());
		NfcFrame uid = new NfcFrame(2, 0x0101, 0x0103, 0, 0, 106000, 0, 0, "UID", "04 AA BB");
		assertTrue(uid.toJson().contains("\"name\":\"UID\""));
		assertTrue(uid.toJson().contains("04 AA BB"));
		assertEquals("A", uid.techLabel());
	}
}
