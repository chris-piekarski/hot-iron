package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class StationKnobTest {

	@Test
	void nudgeRightIsPositiveAndLeftIsNegative() {
		StationKnob knob = new StationKnob();
		AtomicInteger last = new AtomicInteger();
		knob.setOnStep(last::set);
		knob.nudge(+1);
		assertEquals(1, last.get());
		knob.nudge(-3);
		assertEquals(-1, last.get());
	}

	@Test
	void setKHzSnapsToTheUsDial() {
		StationKnob knob = new StationKnob();
		knob.setKHz(97300);
		assertEquals(97300, knob.getKHz());
		knob.setKHz(80000);
		assertEquals(88100, knob.getKHz());
	}

	@Test
	void detentsTrackDetectedStations() {
		StationKnob knob = new StationKnob();
		knob.setDetents(java.util.List.of(88100, 97300, 101100));
		knob.setKHz(97300);
		assertEquals(97300, knob.getKHz());
	}

	@Test
	void pointerStaysOnTheFmScaleWhenDetectionsFlicker() {
		StationKnob knob = new StationKnob();
		knob.setKHz(97300);
		knob.setDetents(java.util.List.of(88100, 97300));
		double a = knob.pointerAngle();
		knob.setDetents(java.util.List.of(88100, 97300, 101100, 107900));
		assertEquals(a, knob.pointerAngle(), 1e-9);
		knob.setDetents(java.util.List.of());
		assertEquals(a, knob.pointerAngle(), 1e-9);
		assertTrue(StationKnob.angleForKHz(88100) < a);
		assertTrue(a < StationKnob.angleForKHz(107900));
	}
}
