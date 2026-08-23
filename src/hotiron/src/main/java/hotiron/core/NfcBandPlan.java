package hotiron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HF RFID / NFC spectral features around 13.56 MHz (ISO 14443/15693,
 * 47 CFR 15.225). Not a channel raster — one carrier plus sidebands.
 */
public final class NfcBandPlan
{
	public static final double CARRIER_MHZ = 13.56;
	public static final double ISM_START_MHZ = 13.553;
	public static final double ISM_END_MHZ = 13.567;
	public static final double FCC_START_MHZ = 13.110;
	public static final double FCC_END_MHZ = 14.010;
	/** PHY view so Type A/B ±847.5 kHz sidebands are on-screen. */
	public static final int VIEW_START_MHZ = 12;
	public static final int VIEW_END_MHZ = 15;
	public static final int H2_VIEW_START_MHZ = 26;
	public static final int H2_VIEW_END_MHZ = 28;
	public static final int H3_VIEW_START_MHZ = 40;
	public static final int H3_VIEW_END_MHZ = 42;
	public static final double H2_MHZ = 27.12;
	public static final double H3_MHZ = 40.68;
	public static final double AB_LSB_MHZ = 12.7125;
	public static final double AB_USB_MHZ = 14.4075;
	public static final double F_LSB_MHZ = 13.348;
	public static final double F_USB_MHZ = 13.772;
	public static final double V_LSB_MHZ = 13.13625;
	public static final double V_USB_MHZ = 13.98375;
	public static final float DETECT_MARGIN_DB = 8f;
	public static final float SIDEBAND_MARGIN_DB = 6f;
	public static final float NOISE_PERCENTILE = 0.20f;
	public static final double MAX_CLASSIFY_SPAN_MHZ = 16;

	public static final NfcFeature CARRIER = new NfcFeature("carrier", "13.56", CARRIER_MHZ, 7, false, false);
	public static final NfcFeature ISM = new NfcFeature("ism", "ISM", 13.56, 7, false, false);
	public static final NfcFeature AB_LSB = new NfcFeature("ab-lsb", "A/B", AB_LSB_MHZ, 50, true, false);
	public static final NfcFeature AB_USB = new NfcFeature("ab-usb", "A/B", AB_USB_MHZ, 50, true, false);
	public static final NfcFeature F_LSB = new NfcFeature("f-lsb", "F", F_LSB_MHZ, 30, true, false);
	public static final NfcFeature F_USB = new NfcFeature("f-usb", "F", F_USB_MHZ, 30, true, false);
	public static final NfcFeature V_LSB = new NfcFeature("v-lsb", "V", V_LSB_MHZ, 40, true, false);
	public static final NfcFeature V_USB = new NfcFeature("v-usb", "V", V_USB_MHZ, 40, true, false);
	public static final NfcFeature H2 = new NfcFeature("h2", "×2", H2_MHZ, 80, false, true);
	public static final NfcFeature H3 = new NfcFeature("h3", "×3", H3_MHZ, 80, false, true);

	public static final List<NfcFeature> FEATURES;

	static
	{
		List<NfcFeature> list = new ArrayList<>();
		list.add(CARRIER);
		list.add(AB_LSB);
		list.add(AB_USB);
		list.add(F_LSB);
		list.add(F_USB);
		list.add(V_LSB);
		list.add(V_USB);
		list.add(H2);
		list.add(H3);
		FEATURES = Collections.unmodifiableList(list);
	}

	private NfcBandPlan()
	{
	}

	public static FrequencyRange phyWindow()
	{
		return new FrequencyRange(VIEW_START_MHZ, VIEW_END_MHZ);
	}

	public static FrequencyRange harmonic2Window()
	{
		return new FrequencyRange(H2_VIEW_START_MHZ, H2_VIEW_END_MHZ);
	}

	public static FrequencyRange harmonic3Window()
	{
		return new FrequencyRange(H3_VIEW_START_MHZ, H3_VIEW_END_MHZ);
	}

	public static boolean overlapsPhy(double startMHz, double endMHz)
	{
		return endMHz > VIEW_START_MHZ && startMHz < VIEW_END_MHZ;
	}

	public static boolean overlapsHarmonic(double startMHz, double endMHz)
	{
		return rangesOverlap(startMHz, endMHz, H2_VIEW_START_MHZ, H2_VIEW_END_MHZ)
				|| rangesOverlap(startMHz, endMHz, H3_VIEW_START_MHZ, H3_VIEW_END_MHZ);
	}

	public static boolean overlapsInterest(double startMHz, double endMHz)
	{
		return overlapsPhy(startMHz, endMHz) || overlapsHarmonic(startMHz, endMHz);
	}

	/** Overlay / classify only on a tight NFC or harmonic window, not HF 3–30. */
	public static boolean viewIsNfc(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz || endMHz - startMHz > MAX_CLASSIFY_SPAN_MHZ)
			return false;
		return overlapsInterest(startMHz, endMHz);
	}

	public static List<NfcFeature> visibleFeatures(double startMHz, double endMHz)
	{
		if (!viewIsNfc(startMHz, endMHz))
			return List.of();
		List<NfcFeature> out = new ArrayList<>();
		for (NfcFeature f : FEATURES)
		{
			if (f.overlaps(startMHz, endMHz))
				out.add(f);
		}
		return out;
	}

	public static String labelForPeak(double peakMhz, double viewStartMHz, double viewEndMHz)
	{
		if (!viewIsNfc(viewStartMHz, viewEndMHz))
			return null;
		NfcFeature best = null;
		double bestDist = Double.POSITIVE_INFINITY;
		for (NfcFeature f : FEATURES)
		{
			if (!f.contains(peakMhz))
				continue;
			double dist = Math.abs(f.centerMhz - peakMhz);
			if (dist < bestDist)
			{
				bestDist = dist;
				best = f;
			}
		}
		if (best == null)
			return null;
		if (best == CARRIER || best == ISM)
			return "13.56";
		if (best == AB_LSB || best == AB_USB)
			return "NFC-A/B";
		if (best == F_LSB || best == F_USB)
			return "NFC-F";
		if (best == V_LSB || best == V_USB)
			return "NFC-V";
		if (best == H2)
			return "NFC×2";
		if (best == H3)
			return "NFC×3";
		return best.label;
	}

	public static NfcObservation observe(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		if (ds == null || endMHz < startMHz)
			return NfcObservation.empty();
		int n = ds.spectrumLength();
		if (n == 0)
			return NfcObservation.empty();
		float noise = percentile(ds, startMHz, endMHz, NOISE_PERCENTILE);
		if (!Float.isFinite(noise))
			return NfcObservation.empty();
		float thresh = noise + DETECT_MARGIN_DB;
		Peak carrier = peakIn(ds, 13.50, 13.62);
		boolean carrierOn = carrier.dbm >= thresh;
		boolean narrow = carrierOn && occupiedKHz(ds, carrier.index, thresh) <= 40;
		boolean ab = sidebandOn(ds, AB_LSB_MHZ, noise) || sidebandOn(ds, AB_USB_MHZ, noise);
		boolean f = sidebandOn(ds, F_LSB_MHZ, noise) || sidebandOn(ds, F_USB_MHZ, noise);
		boolean v = sidebandOn(ds, V_LSB_MHZ, noise) || sidebandOn(ds, V_USB_MHZ, noise);
		boolean h2 = peakIn(ds, H2_MHZ - 0.08, H2_MHZ + 0.08).dbm >= thresh;
		boolean h3 = peakIn(ds, H3_MHZ - 0.08, H3_MHZ + 0.08).dbm >= thresh;
		float mhz = carrierOn ? carrier.mhz : (h2 ? (float) H2_MHZ : h3 ? (float) H3_MHZ : Float.NaN);
		float dbm = carrierOn ? carrier.dbm : (h2 ? peakIn(ds, H2_MHZ - 0.08, H2_MHZ + 0.08).dbm
				: h3 ? peakIn(ds, H3_MHZ - 0.08, H3_MHZ + 0.08).dbm : Float.NEGATIVE_INFINITY);
		return new NfcObservation(noise, thresh, dbm, mhz, carrierOn, narrow, ab, f, v, h2, h3);
	}

	private static boolean sidebandOn(DatasetSpectrum ds, double centerMhz, float noise)
	{
		Peak p = peakIn(ds, centerMhz - 0.04, centerMhz + 0.04);
		return p.dbm >= noise + SIDEBAND_MARGIN_DB;
	}

	private static Peak peakIn(DatasetSpectrum ds, double loMhz, double hiMhz)
	{
		int best = -1;
		float bestDbm = Float.NEGATIVE_INFINITY;
		long lo = Math.round(loMhz * 1_000_000d);
		long hi = Math.round(hiMhz * 1_000_000d);
		for (int i = 0; i < ds.spectrumLength(); i++)
		{
			double hz = ds.getFrequency(i);
			if (hz < lo || hz > hi)
				continue;
			float p = ds.getPower(i);
			if (!Float.isFinite(p) || DatasetSpectrum.isChartHole(p))
				continue;
			if (p > bestDbm)
			{
				bestDbm = p;
				best = i;
			}
		}
		if (best < 0)
			return new Peak(-1, Float.NEGATIVE_INFINITY, Float.NaN);
		return new Peak(best, bestDbm, (float) (ds.getFrequency(best) / 1_000_000d));
	}

	private static float occupiedKHz(DatasetSpectrum ds, int center, float thresh)
	{
		if (center < 0)
			return Float.NaN;
		int lo = center;
		int hi = center;
		while (lo > 0 && ds.getPower(lo - 1) >= thresh)
			lo--;
		while (hi + 1 < ds.spectrumLength() && ds.getPower(hi + 1) >= thresh)
			hi++;
		double hz = ds.getFrequency(hi) - ds.getFrequency(lo) + ds.getFFTBinSizeHz();
		return (float) (hz / 1000.0);
	}

	private static float percentile(DatasetSpectrum ds, double startMHz, double endMHz, float p)
	{
		long lo = Math.round(startMHz * 1_000_000d);
		long hi = Math.round(endMHz * 1_000_000d);
		int n = 0;
		for (int i = 0; i < ds.spectrumLength(); i++)
		{
			double hz = ds.getFrequency(i);
			if (hz < lo || hz > hi)
				continue;
			float v = ds.getPower(i);
			if (Float.isFinite(v) && !DatasetSpectrum.isChartHole(v))
				n++;
		}
		if (n == 0)
			return Float.NaN;
		float[] vals = new float[n];
		int w = 0;
		for (int i = 0; i < ds.spectrumLength(); i++)
		{
			double hz = ds.getFrequency(i);
			if (hz < lo || hz > hi)
				continue;
			float v = ds.getPower(i);
			if (Float.isFinite(v) && !DatasetSpectrum.isChartHole(v))
				vals[w++] = v;
		}
		java.util.Arrays.sort(vals);
		int idx = Math.min(vals.length - 1, Math.max(0, (int) Math.floor(p * (vals.length - 1))));
		return vals[idx];
	}

	private static boolean rangesOverlap(double a0, double a1, double b0, double b1)
	{
		return a1 > b0 && a0 < b1;
	}

	private static final class Peak
	{
		final int index;
		final float dbm;
		final float mhz;

		Peak(int index, float dbm, float mhz)
		{
			this.index = index;
			this.dbm = dbm;
			this.mhz = mhz;
		}
	}
}
