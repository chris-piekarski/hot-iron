package hotiron.hw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;

import hotiron.core.RadioHotPlug;
import hotiron.nativebridge.HackRFDeviceQuery;

/**
 * USB hot-load against a real HackRF. Does not unplug the cable.
 * {@code hackrf_device_list} is only used after RX stops; live presence
 * is sysfs (same split as the operator watch).
 */
@Tag("hardware")
@EnabledIf("hotiron.hw.HardwareConditions#hackrfUsbPresent")
class HackRFHotPlugHardwareIT {

	@BeforeAll
	static void configureNativeLibraryPath() {
		File lib = HardwareConditions.findNativeLibrary();
		if (lib != null)
			System.setProperty("hackrf.sweep.lib.dir", lib.getParentFile().getAbsolutePath() + File.separator);
	}

	@HardwareTest
	@Timeout(value = 40, unit = TimeUnit.SECONDS)
	void hotLoadAppearStartsThenStopOwnsUsbAndSweepRecovers() throws Exception {
		HardwareSweepSession.assumeSweepReady();

		AtomicBoolean sysfsDuringSweep = new AtomicBoolean();
		HardwareSweepSession.Result first = HardwareSweepSession.runUntilFullSweep(2400, 2500, 1_000_000,
				HardwareSweepSession.DEFAULT_SAMPLES, HardwareSweepSession.DEFAULT_LNA_GAIN,
				HardwareSweepSession.DEFAULT_VGA_GAIN, false, false,
				(fullSweepDone, frequencyStart, fftBinWidthHz, signalPowerdBm) -> {
					if (HackRFDeviceQuery.usbEnumerated())
						sysfsDuringSweep.set(true);
				});
		first.assertHealthy("sweep before hotplug probe");
		assertTrue(first.sawFullSweep);
		assertTrue(sysfsDuringSweep.get(), "sysfs must still see 1d50:6089 while the sweep holds USB");

		List<String> serials = HackRFDeviceQuery.listSerials();
		assertFalse(serials.isEmpty(), "hackrf_device_list empty after a healthy sweep stop");

		RadioHotPlug plug = new RadioHotPlug();
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(List.of(), false, false));
		assertEquals(RadioHotPlug.Action.START, plug.observe(serials, false, false),
				"USB appear after boot-empty must start the sweep");
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(serials, true, false),
				"Stop owns USB — hot-load must not steal");
		assertEquals(RadioHotPlug.Action.IDLE, plug.observe(serials, false, true),
				"identity already present — do not restart");
		assertEquals(RadioHotPlug.Action.MARK_ABSENT, plug.observe(List.of(), false, true));

		HardwareSweepSession.Result after = HardwareSweepSession.run(2400, 2500, 1_000_000, false, false);
		after.assertHealthy("sweep after hotplug START (recovery)");
	}
}
