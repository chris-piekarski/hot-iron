package hotiron.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import hotiron.core.FmChannelPlan;
import hotiron.core.TvChannelPlan;
import hotiron.core.WifiChannelPlan;

/**
 * Quick Select sweep windows. Integer MHz start/end for the frequency
 * selectors (HackRF floor is 1 MHz). US FCC / ITU / 3GPP citations are
 * in {@link #detail}; ranges are survey envelopes, not exclusive allocations.
 */
public enum QuickSelectPreset
{
	ALL("All", 1, 7250,
			"Full HotIron/HackRF selectable survey range (1–7250 MHz). Auto FFT keeps this coarse so the waterfall stays fast."),
	WIFI_2("WiFi 2", WifiChannelPlan.WIFI_24_VIEW_START_MHZ, WifiChannelPlan.WIFI_24_VIEW_END_MHZ,
			"US 802.11 ch 1–11 occupied 20 MHz (2402–2472). Channel N starts at (2412+5×(N−1))−10; 2402 is ch 1, 2407 is ch 2, 2452–2472 is ch 11. Channels overlap."),
	BLE("BLE", hotiron.core.BleBandPlan.VIEW_START_MHZ, hotiron.core.BleBandPlan.VIEW_END_MHZ,
			"Bluetooth LE ISM 2400–2484 MHz so advertising 37/38/39 are on-screen (39 is 2480). Wi-Fi 2 stops at 2472."),
	WIFI_5("WiFi 5", WifiChannelPlan.WIFI_5_VIEW_START_MHZ, WifiChannelPlan.WIFI_5_VIEW_END_MHZ,
			"US 802.11 20 MHz ch 36–177 occupied (5170–5895). U-NII-1 legally starts at 5150; the first 20 MHz channel is 36 at 5170–5190."),
	LTE_1("LTE-1", 1695, 2200,
			"AWS + PCS + IMT: 3GPP B70/B66/B4/B3/B2/B25/B1/B65 (1695–2200 MHz)."),
	LTE_2("LTE-2", 617, 960,
			"600/700/800/850/900 cellular: 3GPP B71 DL through B8 DL (617–960 MHz)."),
	NFC("NFC", hotiron.core.NfcBandPlan.VIEW_START_MHZ, hotiron.core.NfcBandPlan.VIEW_END_MHZ,
			"HF RFID / NFC PHY at 13.56 MHz (47 CFR 15.225: 13.110–14.010). 12–15 MHz so Type A/B ±847.5 kHz sidebands are on-screen."),
	FM("FM", FmChannelPlan.VIEW_START_MHZ, FmChannelPlan.VIEW_END_MHZ,
			"US FM broadcast (47 CFR 73.201: 88–108 MHz). Overlay labels live peaks as station frequencies (97.3)."),
	HF("HF", 3, 30,
			"ITU HF (3–30 MHz). Not a single amateur allocation — US ham HF is discrete bands (see Part 97.301)."),
	VHF("VHF", 30, 300,
			"ITU VHF (30–300 MHz). Includes 6 m / 2 m amateur plus broadcast, aviation, and land mobile."),
	UHF("UHF", 300, 3000,
			"ITU UHF (300–3000 MHz). Includes 70 cm / 33 cm / 23 cm amateur plus cellular, ISM, and TV."),
	V_TV("V-TV", TvChannelPlan.VHF_VIEW_START_MHZ, TvChannelPlan.VHF_VIEW_END_MHZ,
			"US VHF TV envelope, ch 2–13 (54–72, 76–88, 174–216). Includes the 88–174 MHz gap (FM + aviation)."),
	U_TV("U-TV", TvChannelPlan.UHF_VIEW_START_MHZ, TvChannelPlan.UHF_VIEW_END_MHZ,
			"US UHF TV after the 600 MHz repack: ch 14–36 (470–608 MHz). Pre-1983 UHF TV ran to 890; 700/600 MHz were auctioned."),
	HAM_6M("6m", 50, 54,
			"Amateur 6 m (47 CFR 97.301: 50.0–54.0 MHz)."),
	HAM_2M("2m", 144, 148,
			"Amateur 2 m (47 CFR 97.301: 144.0–148.0 MHz). ITU Region 1 is 144–146."),
	HAM_70CM("70cm", 420, 450,
			"Amateur 70 cm (47 CFR 97.301: 420.0–450.0 MHz). ITU Region 1 is typically 430–440."),
	HAM_33CM("33cm", 902, 928,
			"Amateur 33 cm / 915 MHz ISM (47 CFR 97.301 and 15.247: 902–928 MHz).");

	public final String label;
	public final int startMHz;
	public final int endMHz;
	public final String detail;

	QuickSelectPreset(String label, int startMHz, int endMHz, String detail)
	{
		this.label = label;
		this.startMHz = startMHz;
		this.endMHz = endMHz;
		this.detail = detail;
	}

	public int spanMHz()
	{
		return endMHz - startMHz;
	}

	public boolean overlaps(double startMHz, double endMHz)
	{
		return this.endMHz > startMHz && this.startMHz < endMHz;
	}

	public double visibleLowMHz(double startMHz, double endMHz)
	{
		return Math.max(this.startMHz, startMHz);
	}

	public double visibleHighMHz(double startMHz, double endMHz)
	{
		return Math.min(this.endMHz, endMHz);
	}

	/** ITU survey buttons; drawn lighter under the specific allocations. */
	public boolean surveyEnvelope()
	{
		return this == HF || this == VHF || this == UHF;
	}

	/**
	 * Presets that sit inside {@code [startMHz, endMHz]} without filling it.
	 * Used when the plot is zoomed out past a single Quick Select window.
	 */
	public static List<QuickSelectPreset> visibleInView(double startMHz, double endMHz)
	{
		double view = endMHz - startMHz;
		if (view <= 0)
			return List.of();
		List<QuickSelectPreset> out = new ArrayList<>();
		for (QuickSelectPreset preset : values())
		{
			if (!preset.overlaps(startMHz, endMHz))
				continue;
			double visible = preset.visibleHighMHz(startMHz, endMHz) - preset.visibleLowMHz(startMHz, endMHz);
			if (visible <= 0)
				continue;
			if (visible / view >= 0.92)
				continue;
			out.add(preset);
		}
		out.sort((a, b) -> Integer.compare(b.spanMHz(), a.spanMHz()));
		return out;
	}

	public static List<QuickSelectPreset> labelPriority(List<QuickSelectPreset> bands)
	{
		List<QuickSelectPreset> copy = new ArrayList<>(bands);
		copy.sort((a, b) -> {
			if (a.surveyEnvelope() != b.surveyEnvelope())
				return a.surveyEnvelope() ? 1 : -1;
			return Integer.compare(a.spanMHz(), b.spanMHz());
		});
		return copy;
	}

	/** Short hover text. Citations stay in {@link #detail} / docs. */
	public String tooltip()
	{
		return startMHz + "–" + endMHz + " MHz";
	}

	public static Optional<QuickSelectPreset> findByLabel(String label)
	{
		if (label == null)
			return Optional.empty();
		for (QuickSelectPreset preset : values())
		{
			if (preset.label.equals(label))
				return Optional.of(preset);
		}
		return Optional.empty();
	}

	/** Exact start/end match for the selected-button highlight. */
	public static Optional<QuickSelectPreset> findByRange(int startMHz, int endMHz)
	{
		for (QuickSelectPreset preset : values())
		{
			if (preset.startMHz == startMHz && preset.endMHz == endMHz)
				return Optional.of(preset);
		}
		return Optional.empty();
	}
}
