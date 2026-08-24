package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bluetooth LE / 2.4 GHz ISM catalog. Survey envelope is 2400–2484 so
 * advertising 39 (2480) is on-screen. Not the Wi-Fi 2 occupied window.
 */
public final class BleBandPlan
{
	public static final int VIEW_START_MHZ = 2400;
	public static final int VIEW_END_MHZ = 2484;
	public static final double ISM_START_MHZ = 2400;
	public static final double ISM_END_MHZ = 2483.5;
	public static final double ANT_PLUS_MHZ = 2457;
	public static final double MAX_OVERLAY_SPAN_MHZ = 150;
	public static final double MAX_LABEL_SPAN_MHZ = 100;

	public static final BleFeature ADV_37 = new BleFeature("adv37", "37", 37, 2402, true);
	public static final BleFeature ADV_38 = new BleFeature("adv38", "38", 38, 2426, true);
	public static final BleFeature ADV_39 = new BleFeature("adv39", "39", 39, 2480, true);
	public static final BleFeature ANT = new BleFeature("ant", "ANT+", -1, ANT_PLUS_MHZ, false);

	public static final List<BleFeature> OVERLAY;

	static
	{
		List<BleFeature> list = new ArrayList<>();
		list.add(ADV_37);
		list.add(ADV_38);
		list.add(ADV_39);
		list.add(ANT);
		OVERLAY = Collections.unmodifiableList(list);
	}

	private BleBandPlan()
	{
	}

	public static FrequencyRange viewWindow()
	{
		return new FrequencyRange(VIEW_START_MHZ, VIEW_END_MHZ);
	}

	public static boolean overlapsIsm(double startMHz, double endMHz)
	{
		return endMHz > ISM_START_MHZ && startMHz < ISM_END_MHZ;
	}

	/** Wi-Fi 2 (2402–2472) qualifies; All / UHF do not. */
	public static boolean viewShowsOverlay(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz || endMHz - startMHz > MAX_OVERLAY_SPAN_MHZ)
			return false;
		return overlapsIsm(startMHz, endMHz);
	}

	/** Full BLE Quick Select (includes ch 39). Not Wi-Fi 2. */
	public static boolean viewIsBle(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz || endMHz - startMHz > MAX_LABEL_SPAN_MHZ)
			return false;
		return startMHz <= 2401 && endMHz >= 2480;
	}

	public static double mhzForChannel(int ch)
	{
		if (ch == 37)
			return 2402;
		if (ch == 38)
			return 2426;
		if (ch == 39)
			return 2480;
		if (ch >= 0 && ch <= 10)
			return 2404 + 2 * ch;
		if (ch >= 11 && ch <= 36)
			return 2406 + 2 * ch;
		return Double.NaN;
	}

	public static int channelForMhz(double mhz)
	{
		if (!Double.isFinite(mhz))
			return -1;
		if (Math.abs(mhz - 2402) <= 1)
			return 37;
		if (Math.abs(mhz - 2426) <= 1)
			return 38;
		if (Math.abs(mhz - 2480) <= 1)
			return 39;
		if (mhz < 2403 || mhz > 2479)
			return -1;
		int raw = (int) Math.round((mhz - 2404) / 2.0);
		if (mhz < 2426)
			return raw >= 0 && raw <= 10 ? raw : -1;
		int ch = raw - 1;
		return ch >= 11 && ch <= 36 ? ch : -1;
	}

	public static List<BleFeature> visibleOverlay(double startMHz, double endMHz)
	{
		if (!viewShowsOverlay(startMHz, endMHz))
			return List.of();
		List<BleFeature> out = new ArrayList<>();
		for (BleFeature f : OVERLAY)
		{
			if (f.centerMhz >= startMHz && f.centerMhz <= endMHz)
				out.add(f);
		}
		return out;
	}

	public static String labelForPeak(double peakMhz, double viewStartMHz, double viewEndMHz)
	{
		if (!viewIsBle(viewStartMHz, viewEndMHz))
			return null;
		if (Math.abs(peakMhz - ANT_PLUS_MHZ) <= 0.6)
			return "ANT+";
		int ch = channelForMhz(peakMhz);
		if (ch < 0)
			return null;
		return "BLE " + ch;
	}

	public static final class BleFeature
	{
		public final String id;
		public final String label;
		public final int channel;
		public final double centerMhz;
		public final boolean advertising;

		public BleFeature(String id, String label, int channel, double centerMhz, boolean advertising)
		{
			this.id = id;
			this.label = label;
			this.channel = channel;
			this.centerMhz = centerMhz;
			this.advertising = advertising;
		}

		public double lowMHz()
		{
			return centerMhz - 1;
		}

		public double highMHz()
		{
			return centerMhz + 1;
		}
	}
}
