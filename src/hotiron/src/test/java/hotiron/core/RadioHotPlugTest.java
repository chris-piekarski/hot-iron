package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RadioHotPlugTest
{
	@Test
	void firstSampleOnlySeeds()
	{
		RadioHotPlug plug = new RadioHotPlug();
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of("aabb"), false, false));
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of("aabb"), false, true));
	}

	@Test
	void usbAppearStartsUnlessStopped()
	{
		RadioHotPlug plug = new RadioHotPlug();
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of(), false, false));
		assertEquals(RadioHotPlug.Action.START, plug.observe(List.of("aabb"), false, false));
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of("aabb"), true, false),
				"Stop owns USB — do not auto-start");
	}

	@Test
	void unplugClearsIdentity()
	{
		RadioHotPlug plug = new RadioHotPlug();
		plug.observe(List.of("aabb"), false, true);
		assertEquals(RadioHotPlug.Action.MARK_ABSENT, plug.observe(List.of(), false, true));
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of(), false, false));
	}

	@Test
	void failedOpenRetriesAfterBackoffNotEveryTick()
	{
		AtomicLong now = new AtomicLong(1_000);
		RadioHotPlug plug = new RadioHotPlug(now::get);
		plug.observe(List.of(), false, false);
		assertEquals(RadioHotPlug.Action.START, plug.observe(List.of("aabb"), false, false));
		now.addAndGet(500);
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of("aabb"), false, false));
		now.addAndGet(RadioHotPlug.RETRY_MS);
		assertEquals(RadioHotPlug.Action.START, plug.observe(List.of("aabb"), false, false));
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of("aabb"), false, true));
	}

	@Test
	void blankSerialsAreAbsent()
	{
		RadioHotPlug plug = new RadioHotPlug();
		plug.observe(List.of(), false, false);
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of("  "), false, false));
	}
}
