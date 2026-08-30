package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class FmTunerScaleTest {

	@Test
	void xMapsLeftToLowerMhzAndRightToHigher() {
		int w = 400;
		int lo = FmTunerScale.xForKHz(88100, w);
		int mid = FmTunerScale.xForKHz(97300, w);
		int hi = FmTunerScale.xForKHz(107900, w);
		assertTrue(lo < mid);
		assertTrue(mid < hi);
		assertEquals(88100, FmTunerScale.kHzForX(lo, w));
		assertEquals(107900, FmTunerScale.kHzForX(hi, w));
		assertTrue(FmTunerScale.kHzForX(lo, w) < FmTunerScale.kHzForX(hi, w));
	}

	@Test
	void setKHzSnapsToTheUsDial() {
		FmTunerScale scale = new FmTunerScale();
		scale.setKHz(97300);
		assertEquals(97300, scale.getKHz());
		scale.setKHz(80000);
		assertEquals(88100, scale.getKHz());
	}

	@Test
	void nudgeFineTunesNotSeek() {
		FmTunerScale scale = new FmTunerScale();
		AtomicInteger last = new AtomicInteger();
		scale.setOnTune(last::set);
		scale.nudge(+1);
		assertEquals(1, last.get());
		scale.nudge(-3);
		assertEquals(-1, last.get());
	}

	@Test
	void kHzMapsOntoTheFreqGauge() {
		assertEquals(0f, FmTunerScale.kHzTo01(88100), 0.001f);
		assertEquals(1f, FmTunerScale.kHzTo01(107900), 0.001f);
		assertTrue(FmTunerScale.kHzTo01(97300) > 0.4f);
		assertEquals(97300, FmTunerScale.kHzFrom01(FmTunerScale.kHzTo01(97300)));
	}
}
