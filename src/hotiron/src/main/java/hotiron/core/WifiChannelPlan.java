package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * US / FCC 802.11 20 MHz channel centers for the 2.4 GHz ISM and 5 GHz U-NII
 * bands. 2.4 GHz is channels 1–11 (15.247). 5 GHz is U-NII-1 through U-NII-4
 * (15.407), including DFS channels; 12–14 and 6 GHz are not listed.
 */
public final class WifiChannelPlan
{
	public static final String BAND_24 = "2.4";
	public static final String BAND_5 = "5";

	/** US 2.4 GHz: ch 1–11, 5 MHz spacing, 20 MHz OFDM. 1/6/11 are primary. */
	public static final List<WifiChannel> WIFI_24;
	/** US 5 GHz 20 MHz: UNII-1/2A/2C/3/4. */
	public static final List<WifiChannel> WIFI_5;
	public static final List<WifiChannel> ALL;

	static
	{
		List<WifiChannel> two = new ArrayList<>();
		for (int ch = 1; ch <= 11; ch++)
		{
			boolean primary = ch == 1 || ch == 6 || ch == 11;
			two.add(new WifiChannel(BAND_24, ch, 2407 + 5 * ch, 20, primary));
		}
		WIFI_24 = Collections.unmodifiableList(two);

		// 20 MHz centers. UNII-1/2A/2C use 5180 + 20*n; UNII-3/4 start at 5745.
		int[] five = {
				36, 40, 44, 48,
				52, 56, 60, 64,
				100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144,
				149, 153, 157, 161, 165, 169, 173, 177
		};
		List<WifiChannel> fiveList = new ArrayList<>();
		for (int ch : five)
		{
			// IEEE 802.11: 20 MHz center MHz = 5000 + 5 * channel number.
			boolean primary = ch == 36 || ch == 48 || ch == 149 || ch == 165;
			fiveList.add(new WifiChannel(BAND_5, ch, 5000 + 5 * ch, 20, primary));
		}
		WIFI_5 = Collections.unmodifiableList(fiveList);

		List<WifiChannel> all = new ArrayList<>(two.size() + fiveList.size());
		all.addAll(two);
		all.addAll(fiveList);
		ALL = Collections.unmodifiableList(all);
	}

	private WifiChannelPlan()
	{
	}

	/** Occupied OFDM envelope of US ch 1–11: 2402–2472 MHz. */
	public static final double WIFI_24_OCCUPIED_START_MHZ = 2402;
	public static final double WIFI_24_OCCUPIED_END_MHZ = 2472;
	/**
	 * US ch 1 occupied start through ch 11 occupied end (2402–2472).
	 * 2407 is the start of channel 2 (2417−10), not channel 1.
	 */
	public static final int WIFI_24_VIEW_START_MHZ = 2402;
	public static final int WIFI_24_VIEW_END_MHZ = 2472;
	/** Occupied 20 MHz envelope of US ch 36–177: 5170–5895 MHz. */
	public static final double WIFI_5_OCCUPIED_START_MHZ = 5170;
	public static final double WIFI_5_OCCUPIED_END_MHZ = 5895;
	public static final int WIFI_5_VIEW_START_MHZ = 5170;
	public static final int WIFI_5_VIEW_END_MHZ = 5895;

	/** Channels whose 20 MHz occupancy overlaps {@code [startMHz, endMHz]}. */
	public static List<WifiChannel> visibleOccupancy(double startMHz, double endMHz)
	{
		if (endMHz < startMHz)
			return Collections.emptyList();
		List<WifiChannel> out = new ArrayList<>();
		for (WifiChannel ch : ALL)
		{
			if (ch.occupancyOverlaps(startMHz, endMHz))
				out.add(ch);
		}
		return out;
	}

	/** Channels whose center sits inside {@code [startMHz, endMHz]}. */
	public static List<WifiChannel> visibleCenters(double startMHz, double endMHz)
	{
		if (endMHz < startMHz)
			return Collections.emptyList();
		List<WifiChannel> out = new ArrayList<>();
		for (WifiChannel ch : ALL)
		{
			if (ch.centerIn(startMHz, endMHz))
				out.add(ch);
		}
		return out;
	}

	/**
	 * Label order: primary channels first (1/6/11, 36/48/149/165), then the
	 * rest by frequency, so crowding drops the overlapping 2.4 GHz fillers.
	 */
	public static List<WifiChannel> labelPriority(List<WifiChannel> channels)
	{
		List<WifiChannel> copy = new ArrayList<>(channels);
		copy.sort((a, b) -> {
			if (a.primary != b.primary)
				return a.primary ? -1 : 1;
			return Double.compare(a.centerMHz, b.centerMHz);
		});
		return copy;
	}

	public static WifiChannel find(String band, int number)
	{
		for (WifiChannel ch : ALL)
		{
			if (ch.band.equals(band) && ch.number == number)
				return ch;
		}
		return null;
	}

	/** True if {@code [start,end]} overlaps the US 2.4 or 5 GHz occupied envelope. */
	public static boolean viewIsWifi(double startMHz, double endMHz)
	{
		return rangesOverlap(startMHz, endMHz, WIFI_24_OCCUPIED_START_MHZ, WIFI_24_OCCUPIED_END_MHZ)
				|| rangesOverlap(startMHz, endMHz, WIFI_5_OCCUPIED_START_MHZ, WIFI_5_OCCUPIED_END_MHZ);
	}

	/**
	 * Label like {@code ch 6} when the view is Wi-Fi and {@code peakMhz} sits
	 * in a 20 MHz occupancy. Overlapping 2.4 GHz channels pick the closest
	 * center. Null outside Wi-Fi views (do not invent LTE/FM from power).
	 */
	public static String labelForPeak(double peakMhz, double viewStartMHz, double viewEndMHz)
	{
		if (!viewIsWifi(viewStartMHz, viewEndMHz))
			return null;
		WifiChannel best = null;
		double bestDist = Double.POSITIVE_INFINITY;
		for (WifiChannel ch : ALL)
		{
			if (peakMhz < ch.lowMHz() || peakMhz > ch.highMHz())
				continue;
			double dist = Math.abs(ch.centerMHz - peakMhz);
			if (dist < bestDist)
			{
				bestDist = dist;
				best = ch;
			}
		}
		return best == null ? null : "ch " + best.number;
	}

	private static boolean rangesOverlap(double a0, double a1, double b0, double b1)
	{
		return a1 > b0 && a0 < b1;
	}

}
