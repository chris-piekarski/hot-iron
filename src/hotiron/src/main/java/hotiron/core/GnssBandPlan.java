package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RNSS L1 survey envelope (ITU / 47 CFR 2.106: 1559–1610 MHz). GPS is
 * one constellation: L1 C/A at 1575.42 MHz.
 */
public final class GnssBandPlan
{
	public static final int VIEW_START_MHZ = 1559;
	public static final int VIEW_END_MHZ = 1610;
	public static final int GPS_VIEW_START_MHZ = 1573;
	public static final int GPS_VIEW_END_MHZ = 1578;
	public static final double GPS_L1_MHZ = 1575.42;
	public static final long GPS_L1_HZ = 1_575_420_000L;
	/** C/A main lobe, null-to-null. */
	public static final double GPS_L1_WIDTH_MHZ = 2.046;
	public static final double BDS_B1I_MHZ = 1561.098;
	public static final double BDS_B1I_WIDTH_MHZ = 2.046;
	public static final double GLO_L1_MHZ = 1602.0;
	/** FDMA envelope, roughly ch −7…+6. */
	public static final double GLO_L1_WIDTH_MHZ = 8.0;
	public static final float DETECT_MARGIN_DB = 8f;
	public static final float NOISE_PERCENTILE = 0.20f;
	public static final double MAX_CLASSIFY_SPAN_MHZ = 80;

	public static final GnssSignal GPS_L1 = new GnssSignal("gps-l1", "GPS", GPS_L1_MHZ, GPS_L1_WIDTH_MHZ);
	public static final GnssSignal BDS_B1I = new GnssSignal("bds-b1", "BDS", BDS_B1I_MHZ, BDS_B1I_WIDTH_MHZ);
	public static final GnssSignal GLO_L1 = new GnssSignal("glo-l1", "GLO", GLO_L1_MHZ, GLO_L1_WIDTH_MHZ);

	public static final List<GnssSignal> SIGNALS = List.of(BDS_B1I, GPS_L1, GLO_L1);

	private GnssBandPlan()
	{
	}

	public static FrequencyRange viewWindow()
	{
		return new FrequencyRange(VIEW_START_MHZ, VIEW_END_MHZ);
	}

	public static FrequencyRange gpsL1Window()
	{
		return new FrequencyRange(GPS_VIEW_START_MHZ, GPS_VIEW_END_MHZ);
	}

	public static boolean overlapsL1(double startMHz, double endMHz)
	{
		return endMHz > VIEW_START_MHZ && startMHz < VIEW_END_MHZ;
	}

	public static boolean viewIsGnss(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz || endMHz - startMHz > MAX_CLASSIFY_SPAN_MHZ)
			return false;
		return overlapsL1(startMHz, endMHz);
	}

	public static List<GnssSignal> visibleSignals(double startMHz, double endMHz)
	{
		if (!viewIsGnss(startMHz, endMHz))
			return List.of();
		List<GnssSignal> out = new ArrayList<>();
		for (GnssSignal s : SIGNALS)
		{
			if (s.overlaps(startMHz, endMHz))
				out.add(s);
		}
		return Collections.unmodifiableList(out);
	}

	public static String labelForPeak(double peakMhz, double viewStartMHz, double viewEndMHz)
	{
		if (!viewIsGnss(viewStartMHz, viewEndMHz))
			return null;
		GnssSignal best = null;
		double bestDist = Double.POSITIVE_INFINITY;
		for (GnssSignal s : SIGNALS)
		{
			if (!s.contains(peakMhz))
				continue;
			double dist = Math.abs(s.centerMhz - peakMhz);
			if (dist < bestDist)
			{
				bestDist = dist;
				best = s;
			}
		}
		return best == null ? null : best.label;
	}

	public static GnssObservation observe(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		if (ds == null || !viewIsGnss(startMHz, endMHz))
			return GnssObservation.quiet();
		long loHz = Math.round(startMHz * 1_000_000d);
		long hiHz = Math.round(endMHz * 1_000_000d);
		float noise = FmChannelPlan.percentileInRange(ds, loHz, hiHz, NOISE_PERCENTILE);
		if (!Float.isFinite(noise))
			return GnssObservation.quiet();
		float thresh = noise + DETECT_MARGIN_DB;
		boolean gps = occupied(ds, GPS_L1, thresh);
		boolean bds = occupied(ds, BDS_B1I, thresh);
		boolean glo = occupied(ds, GLO_L1, thresh);
		return new GnssObservation(noise, meanPower(ds, GPS_L1), meanPower(ds, BDS_B1I), meanPower(ds, GLO_L1),
				gps, bds, glo);
	}

	static boolean occupied(DatasetSpectrum ds, GnssSignal s, float thresh)
	{
		float m = meanPower(ds, s);
		return Float.isFinite(m) && m >= thresh;
	}

	static float meanPower(DatasetSpectrum ds, GnssSignal s)
	{
		if (ds == null || s == null)
			return Float.NaN;
		long loHz = Math.round(s.lowMHz() * 1_000_000d);
		long hiHz = Math.round(s.highMHz() * 1_000_000d);
		double sum = 0;
		int n = 0;
		int len = ds.spectrumLength();
		for (int i = 0; i < len; i++)
		{
			double fHz = ds.getFrequency(i);
			if (fHz < loHz || fHz >= hiHz)
				continue;
			float p = ds.getPower(i);
			if (!Float.isFinite(p) || p <= SpectrumPowerScale.EMPTY_CEILING)
				continue;
			sum += p;
			n++;
		}
		if (n == 0)
			return Float.NaN;
		return (float) (sum / n);
	}

	public static final class GnssSignal
	{
		public final String id;
		public final String label;
		public final double centerMhz;
		public final double widthMhz;

		public GnssSignal(String id, String label, double centerMhz, double widthMhz)
		{
			this.id = id;
			this.label = label;
			this.centerMhz = centerMhz;
			this.widthMhz = widthMhz;
		}

		public double lowMHz()
		{
			return centerMhz - widthMhz / 2;
		}

		public double highMHz()
		{
			return centerMhz + widthMhz / 2;
		}

		public boolean overlaps(double startMHz, double endMHz)
		{
			return highMHz() > startMHz && lowMHz() < endMHz;
		}

		public boolean contains(double mhz)
		{
			return mhz >= lowMHz() && mhz < highMHz();
		}
	}

	public static final class GnssObservation
	{
		public final float noiseDbm;
		public final float gpsL1Dbm;
		public final float bdsB1Dbm;
		public final float gloL1Dbm;
		public final boolean gpsL1;
		public final boolean bdsB1;
		public final boolean gloL1;

		public GnssObservation(float noiseDbm, float gpsL1Dbm, float bdsB1Dbm, float gloL1Dbm, boolean gpsL1,
				boolean bdsB1, boolean gloL1)
		{
			this.noiseDbm = noiseDbm;
			this.gpsL1Dbm = gpsL1Dbm;
			this.bdsB1Dbm = bdsB1Dbm;
			this.gloL1Dbm = gloL1Dbm;
			this.gpsL1 = gpsL1;
			this.bdsB1 = bdsB1;
			this.gloL1 = gloL1;
		}

		public static GnssObservation quiet()
		{
			return new GnssObservation(Float.NaN, Float.NaN, Float.NaN, Float.NaN, false, false, false);
		}

		public boolean any()
		{
			return gpsL1 || bdsB1 || gloL1;
		}
	}
}
