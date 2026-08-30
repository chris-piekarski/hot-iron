package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import hotiron.core.SpectrumSurveyAxis;
import hotiron.ui.SurveyChipLayout.Chip;
import hotiron.ui.SurveyChipLayout.Side;

class SurveyChipLayoutTest
{
	@Test
	void servicesSitAboveSurveyEnvelopesBelow()
	{
		List<Chip> chips = SurveyChipLayout.place(1200, p -> 72);
		assertEquals(Side.TOP, chip(chips, QuickSelectPreset.FM).side);
		assertEquals(Side.TOP, chip(chips, QuickSelectPreset.WIFI_2).side);
		assertEquals(Side.BOTTOM, chip(chips, QuickSelectPreset.HF).side);
		assertEquals(Side.BOTTOM, chip(chips, QuickSelectPreset.VHF).side);
		assertEquals(Side.BOTTOM, chip(chips, QuickSelectPreset.UHF).side);
		assertEquals(Side.BOTTOM, chip(chips, QuickSelectPreset.ALL).side);
		assertEquals(1, chip(chips, QuickSelectPreset.ALL).row);
		assertEquals(0, chip(chips, QuickSelectPreset.HF).row);
		assertTrue(chip(chips, QuickSelectPreset.ALL).w >= 1190);
	}

	@Test
	void chipsSitLeftToRightWithTheSpectrum()
	{
		List<Chip> chips = SurveyChipLayout.place(1200, p -> 72);
		Chip nfc = chip(chips, QuickSelectPreset.NFC);
		Chip fm = chip(chips, QuickSelectPreset.FM);
		Chip wifi2 = chip(chips, QuickSelectPreset.WIFI_2);
		Chip wifi5 = chip(chips, QuickSelectPreset.WIFI_5);
		Chip wifi6 = chip(chips, QuickSelectPreset.WIFI_6);
		Chip air = chip(chips, QuickSelectPreset.AIR);
		Chip adsb = chip(chips, QuickSelectPreset.ADSB);
		Chip gnss = chip(chips, QuickSelectPreset.GNSS);
		assertTrue(nfc.anchorX() < fm.anchorX());
		assertTrue(fm.anchorX() < air.anchorX());
		assertTrue(air.anchorX() < adsb.anchorX());
		assertTrue(adsb.anchorX() < gnss.anchorX());
		assertTrue(gnss.anchorX() < wifi2.anchorX());
		assertTrue(wifi2.anchorX() < wifi5.anchorX());
		assertTrue(wifi5.anchorX() < wifi6.anchorX());
		assertEquals(Side.TOP, air.side);
		assertEquals(Side.TOP, adsb.side);
		assertEquals(Side.TOP, gnss.side);
		assertEquals(Side.TOP, wifi6.side);
	}

	@Test
	void wideBandsUseTheExactDividerWidth()
	{
		int w = 1200;
		Chip vhf = chip(SurveyChipLayout.place(w, p -> 72), QuickSelectPreset.VHF);
		assertEquals(SpectrumSurveyAxis.mhzToX(QuickSelectPreset.VHF.startMHz, w), vhf.spanX0);
		assertEquals(SpectrumSurveyAxis.mhzToX(QuickSelectPreset.VHF.endMHz, w), vhf.spanX1);
		assertEquals(vhf.spanX1 - vhf.spanX0, vhf.w);
		Chip hf = chip(SurveyChipLayout.place(w, p -> 72), QuickSelectPreset.HF);
		assertEquals(hf.spanX1, chip(SurveyChipLayout.place(w, p -> 72), QuickSelectPreset.VHF).spanX0);
	}

	@Test
	void buttonsStayOverTheirMhzSpan()
	{
		for (int w : new int[] { 1100, 1600, 2400, 3200 })
		{
			for (Chip c : SurveyChipLayout.place(w, p -> 72))
			{
				if (c.preset == QuickSelectPreset.ALL)
					continue;
				assertTrue(c.x < c.spanX1 && c.x + c.w > c.spanX0,
						c.preset.label + " button left its " + w + "px span");
				int spanMid = (c.spanX0 + c.spanX1) / 2;
				int btnMid = c.x + c.w / 2;
				assertTrue(Math.abs(btnMid - spanMid) <= Math.max(2, c.w / 2),
						c.preset.label + " slid off-center at " + w);
			}
		}
	}

	@Test
	void fmSitsInVtvNotBesideBle()
	{
		for (int w : new int[] { 1100, 1600, 2400, 3200, 3600 })
		{
			List<Chip> chips = SurveyChipLayout.place(w, p -> 72);
			Chip fm = chip(chips, QuickSelectPreset.FM);
			Chip vtv = chip(chips, QuickSelectPreset.V_TV);
			Chip ble = chip(chips, QuickSelectPreset.BLE);
			assertTrue(fm.anchorX() >= vtv.spanX0 && fm.anchorX() <= vtv.spanX1,
					"FM 88–108 is inside V-TV 54–216 at " + w);
			assertTrue(fm.x + fm.w <= ble.x || ble.x + ble.w <= fm.x,
					"FM must not sit on BLE at " + w + " (fm " + fm.x + "–" + (fm.x + fm.w)
							+ " ble " + ble.x + "–" + (ble.x + ble.w) + ")");
			int fmToVtv = Math.abs(fm.anchorX() - vtv.anchorX());
			int fmToBle = Math.abs(fm.anchorX() - ble.anchorX());
			assertTrue(fmToVtv < fmToBle, "FM is nearer V-TV than BLE at " + w);
		}
	}

	@Test
	void sameSideSameRowChipsDoNotOverlap()
	{
		List<Chip> chips = SurveyChipLayout.place(1100, p -> 72);
		for (Chip a : chips)
		{
			for (Chip b : chips)
			{
				if (a == b || a.side != b.side || a.row != b.row)
					continue;
				boolean overlap = a.x < b.x + b.w && b.x < a.x + a.w;
				assertFalse(overlap, a.preset.label + " overlaps " + b.preset.label);
			}
		}
	}

	@Test
	void everyPresetGetsAChip()
	{
		List<Chip> chips = SurveyChipLayout.place(1000, p -> SurveyChipLayout.MIN_BUTTON_W);
		assertEquals(QuickSelectPreset.values().length, chips.size());
	}

	@Test
	void heightIncludesWaveAndBothSides()
	{
		List<Chip> chips = SurveyChipLayout.place(800, p -> 72);
		assertTrue(SurveyChipLayout.heightFor(chips) > SurveyChipLayout.WAVE_H + SurveyChipLayout.BUTTON_H * 2);
	}

	@Test
	void allChipSpansTheRadio()
	{
		int w = 1200;
		Chip all = chip(SurveyChipLayout.place(w, p -> 72), QuickSelectPreset.ALL);
		assertEquals(SpectrumSurveyAxis.mhzToX(SpectrumSurveyAxis.MIN_MHZ, w), all.spanX0);
		assertEquals(SpectrumSurveyAxis.mhzToX(SpectrumSurveyAxis.MAX_MHZ, w), all.spanX1);
		assertEquals(0, all.spanX0);
		assertEquals(w - 1, all.spanX1);
	}

	private static Chip chip(List<Chip> chips, QuickSelectPreset preset)
	{
		Map<QuickSelectPreset, Chip> map = new HashMap<>();
		for (Chip c : chips)
			map.put(c.preset, c);
		Chip found = map.get(preset);
		assertNotNull(found, preset.label);
		return found;
	}
}
