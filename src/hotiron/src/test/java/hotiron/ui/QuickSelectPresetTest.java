package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import hotiron.core.FmChannelPlan;
import hotiron.core.WifiChannelPlan;

/**
 * Guard the FCC / ITU / Part 97 numbers so a casual edit cannot silently
 * restore the old Wi-Fi 5 "5030 MHz" MLS window or pre-repack UHF TV.
 */
class QuickSelectPresetTest {

    @Test
    void everyPresetIsAValidHackrfSweepWindow() {
        assertEquals(16, QuickSelectPreset.values().length);
        for (QuickSelectPreset preset : QuickSelectPreset.values()) {
            assertTrue(preset.startMHz >= 1, preset.label + " below HackRF floor");
            assertTrue(preset.endMHz <= 7250, preset.label + " above selector max");
            assertTrue(preset.startMHz < preset.endMHz, preset.label);
            assertFalse(preset.detail.isBlank(), preset.label + " needs a citation");
            assertEquals(preset.startMHz + "–" + preset.endMHz + " MHz", preset.tooltip());
            assertTrue(preset.tooltip().length() < 28, preset.label + " hover is a range, not the citation");
            assertEquals(preset, QuickSelectPreset.findByLabel(preset.label).orElseThrow());
            assertEquals(preset, QuickSelectPreset.findByRange(preset.startMHz, preset.endMHz).orElseThrow());
        }
        assertTrue(QuickSelectPreset.findByLabel("AM").isEmpty());
        assertTrue(QuickSelectPreset.findByRange(2412, 2462).isEmpty());
    }

    @Test
    void fccAndItuEnvelopesMatchCitedRules() {
        assertRange(QuickSelectPreset.ALL, 1, 7250);
        assertRange(QuickSelectPreset.WIFI_2, 2402, 2472);
        assertRange(QuickSelectPreset.WIFI_5, 5170, 5895);
        assertRange(QuickSelectPreset.LTE_1, 1695, 2200);
        assertRange(QuickSelectPreset.LTE_2, 617, 960);
        assertRange(QuickSelectPreset.NFC, 13, 14);
        assertRange(QuickSelectPreset.FM, 88, 108);
        assertRange(QuickSelectPreset.HF, 3, 30);
        assertRange(QuickSelectPreset.VHF, 30, 300);
        assertRange(QuickSelectPreset.UHF, 300, 3000);
        assertRange(QuickSelectPreset.V_TV, 54, 216);
        assertRange(QuickSelectPreset.U_TV, 470, 608);
        assertRange(QuickSelectPreset.HAM_6M, 50, 54);
        assertRange(QuickSelectPreset.HAM_2M, 144, 148);
        assertRange(QuickSelectPreset.HAM_70CM, 420, 450);
        assertRange(QuickSelectPreset.HAM_33CM, 902, 928);
    }

    @Test
    void wifi5DoesNotStartInMlsAviation() {
        assertTrue(QuickSelectPreset.WIFI_5.startMHz >= 5150);
        assertTrue(QuickSelectPreset.WIFI_2.endMHz <= 2472);
        assertTrue(QuickSelectPreset.U_TV.endMHz <= 608);
    }

    @Test
    void wifiSelectorsMatchOccupiedTwentyMhzEnvelopes() {
        assertEquals(WifiChannelPlan.WIFI_24_VIEW_START_MHZ, QuickSelectPreset.WIFI_2.startMHz);
        assertEquals(WifiChannelPlan.WIFI_24_VIEW_END_MHZ, QuickSelectPreset.WIFI_2.endMHz);
        assertEquals(2402, QuickSelectPreset.WIFI_2.startMHz, "ch 1 occupied start");
        assertEquals(2472, QuickSelectPreset.WIFI_2.endMHz, "ch 11 occupied end");
        assertEquals(70, QuickSelectPreset.WIFI_2.endMHz - QuickSelectPreset.WIFI_2.startMHz);
        assertEquals(WifiChannelPlan.WIFI_5_VIEW_START_MHZ, QuickSelectPreset.WIFI_5.startMHz);
        assertEquals(WifiChannelPlan.WIFI_5_VIEW_END_MHZ, QuickSelectPreset.WIFI_5.endMHz);
        assertEquals(FmChannelPlan.VIEW_START_MHZ, QuickSelectPreset.FM.startMHz);
        assertEquals(FmChannelPlan.VIEW_END_MHZ, QuickSelectPreset.FM.endMHz);
    }

    @Test
    void presetSpansStaySane() {
        assertEquals(1, QuickSelectPreset.NFC.endMHz - QuickSelectPreset.NFC.startMHz);
        assertEquals(7249, QuickSelectPreset.ALL.endMHz - QuickSelectPreset.ALL.startMHz);
        assertEquals(20, QuickSelectPreset.FM.endMHz - QuickSelectPreset.FM.startMHz);
        assertEquals(27, QuickSelectPreset.HF.endMHz - QuickSelectPreset.HF.startMHz);
        assertEquals(270, QuickSelectPreset.VHF.endMHz - QuickSelectPreset.VHF.startMHz);
        assertEquals(2700, QuickSelectPreset.UHF.endMHz - QuickSelectPreset.UHF.startMHz);
        assertEquals(4, QuickSelectPreset.HAM_6M.endMHz - QuickSelectPreset.HAM_6M.startMHz);
        assertEquals(4, QuickSelectPreset.HAM_2M.endMHz - QuickSelectPreset.HAM_2M.startMHz);
        assertEquals(30, QuickSelectPreset.HAM_70CM.endMHz - QuickSelectPreset.HAM_70CM.startMHz);
        assertEquals(26, QuickSelectPreset.HAM_33CM.endMHz - QuickSelectPreset.HAM_33CM.startMHz);
        assertEquals(138, QuickSelectPreset.U_TV.endMHz - QuickSelectPreset.U_TV.startMHz);
    }

    @Test
    void visibleInViewSkipsABandThatFillsThePlot() {
        assertTrue(QuickSelectPreset.visibleInView(88, 108).stream()
                .noneMatch(p -> p == QuickSelectPreset.FM));
        assertTrue(QuickSelectPreset.visibleInView(2402, 2472).stream()
                .noneMatch(p -> p == QuickSelectPreset.WIFI_2));
        assertTrue(QuickSelectPreset.visibleInView(300, 3000).stream()
                .noneMatch(p -> p == QuickSelectPreset.UHF));
    }

    @Test
    void visibleInViewShowsChildrenWhenZoomedOutPastAPreset() {
        java.util.List<QuickSelectPreset> wide = QuickSelectPreset.visibleInView(1, 7250);
        assertTrue(wide.contains(QuickSelectPreset.FM));
        assertTrue(wide.contains(QuickSelectPreset.WIFI_2));
        assertTrue(wide.contains(QuickSelectPreset.WIFI_5));
        assertTrue(wide.contains(QuickSelectPreset.LTE_1));
        assertTrue(wide.contains(QuickSelectPreset.UHF));
        assertFalse(wide.contains(QuickSelectPreset.ALL));

        java.util.List<QuickSelectPreset> uhf = QuickSelectPreset.visibleInView(300, 3000);
        assertTrue(uhf.contains(QuickSelectPreset.WIFI_2));
        assertTrue(uhf.contains(QuickSelectPreset.HAM_70CM));
        assertTrue(uhf.contains(QuickSelectPreset.U_TV));
        assertFalse(uhf.contains(QuickSelectPreset.FM));

        java.util.List<QuickSelectPreset> vhf = QuickSelectPreset.visibleInView(30, 300);
        assertTrue(vhf.contains(QuickSelectPreset.FM));
        assertTrue(vhf.contains(QuickSelectPreset.HAM_6M));
        assertTrue(vhf.contains(QuickSelectPreset.HAM_2M));
        assertFalse(vhf.contains(QuickSelectPreset.VHF));
    }

    @Test
    void labelPriorityPutsSpecificBandsBeforeItuSurveys() {
        java.util.List<QuickSelectPreset> ordered = QuickSelectPreset.labelPriority(
                java.util.List.of(QuickSelectPreset.UHF, QuickSelectPreset.FM, QuickSelectPreset.WIFI_2));
        assertEquals(QuickSelectPreset.FM, ordered.get(0));
        assertEquals(QuickSelectPreset.UHF, ordered.get(ordered.size() - 1));
        assertTrue(QuickSelectPreset.HF.surveyEnvelope());
        assertFalse(QuickSelectPreset.FM.surveyEnvelope());
    }

    private static void assertRange(QuickSelectPreset preset, int start, int end) {
        assertEquals(start, preset.startMHz, preset.label + " start");
        assertEquals(end, preset.endMHz, preset.label + " end");
    }
}
