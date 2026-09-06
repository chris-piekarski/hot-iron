package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CbChatterTest
{
	@Test
	void noiseIsNotChatter()
	{
		assertFalse(CbChatter.isChatter(0.005f, 0.004f));
		assertFalse(CbChatter.isChatter(Float.NaN, 0.2f));
	}

	@Test
	void deadCarrierIsNotChatter()
	{
		assertFalse(CbChatter.isChatter(0.4f, 0.01f));
	}

	@Test
	void modulatedAmIsChatter()
	{
		assertTrue(CbChatter.isChatter(0.4f, 0.12f));
	}

	@Test
	void demodOfAmToneLooksLikeChatterAndCarrierDoesNot()
	{
		AmDemodulator voice = new AmDemodulator();
		voice.setChannelHz(CbChannelPlan.LO_HZ);
		short[] pcm = new short[AmDemodulator.AUDIO_RATE_HZ];
		byte[] am = AmDemodulatorTest.modulate(CbChannelPlan.LO_HZ, 1000, 0.5, 0.15);
		voice.processIq(am, am.length, 100, pcm);
		assertTrue(CbChatter.isChatter(voice.carrier(), voice.modulation()),
				"carrier=" + voice.carrier() + " mod=" + voice.modulation());

		AmDemodulator dead = new AmDemodulator();
		dead.setChannelHz(CbChannelPlan.LO_HZ);
		byte[] cw = AmDemodulatorTest.modulate(CbChannelPlan.LO_HZ, 1000, 0.0, 0.15);
		dead.processIq(cw, cw.length, 100, pcm);
		assertTrue(dead.carrier() >= CbChatter.CARRIER_MIN, "carrier " + dead.carrier());
		assertFalse(CbChatter.isChatter(dead.carrier(), dead.modulation()),
				"dead carrier mod=" + dead.modulation());
	}
}
