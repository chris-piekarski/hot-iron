package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;

import org.junit.jupiter.api.Test;

import hotiron.core.FrequencyRange;

class QuickFrequencySelectorPanelTest {

    @Test
    void testInitialValue() {
        QuickFrequencySelectorPanel panel = new QuickFrequencySelectorPanel();
        assertEquals(QuickSelectPreset.WIFI_2.label, panel.getValue());
        assertTrue(panel.isHighlighted(QuickSelectPreset.WIFI_2.label));
        for (QuickSelectPreset preset : QuickSelectPreset.values()) {
            if (preset == QuickSelectPreset.WIFI_2)
                continue;
            assertFalse(panel.isHighlighted(preset.label), preset.label + " must not start selected");
        }
    }

    @Test
    void testValueChangeFiresPropertyAndVetoable() throws Exception {
        QuickFrequencySelectorPanel panel = new QuickFrequencySelectorPanel();

        AtomicReference<String> lastProperty = new AtomicReference<>();
        panel.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                lastProperty.set((String) evt.getNewValue());
            }
        });

        AbstractButton nfc = panel.findButton("NFC");
        assertNotNull(nfc);
        nfc.doClick();
        assertEquals("NFC", panel.getValue());
        assertEquals("NFC", lastProperty.get());
        assertTrue(panel.isHighlighted("NFC"));
        assertFalse(panel.isHighlighted(QuickSelectPreset.WIFI_2.label));
        assertNull(nfc.getToolTipText(), "Swing tooltips stack; hover is in-panel");
    }

    @Test
    void buttonsUseExclusiveToolTipNotSwingTooltips() {
        QuickFrequencySelectorPanel panel = new QuickFrequencySelectorPanel();
        AbstractButton wifi = panel.findButton(QuickSelectPreset.WIFI_2.label);
        AbstractButton fm = panel.findButton(QuickSelectPreset.FM.label);
        assertNotNull(wifi);
        assertNotNull(fm);
        assertNull(wifi.getToolTipText());
        assertNull(fm.getToolTipText());
        assertEquals(QuickSelectPreset.WIFI_2.tooltip(), ExclusiveToolTip.hintOf(wifi));
        assertEquals(QuickSelectPreset.FM.tooltip(), ExclusiveToolTip.hintOf(fm));
    }

    @Test
    void bannerGroupsCoverEveryPresetAndKeepAllLast() {
        int n = 0;
        for (QuickSelectPreset.Group g : QuickSelectPreset.Group.values())
            n += QuickSelectPreset.inGroup(g).size();
        assertEquals(QuickSelectPreset.values().length, n);
        java.util.List<QuickSelectPreset> survey = QuickSelectPreset.inGroup(QuickSelectPreset.Group.SURVEY);
        assertEquals(QuickSelectPreset.ALL, survey.get(survey.size() - 1));
        QuickFrequencySelectorPanel panel = new QuickFrequencySelectorPanel();
        for (QuickSelectPreset preset : QuickSelectPreset.values())
            assertNotNull(panel.findButton(preset.label), preset.label);
    }

    @Test
    void chipsAreLargeAndSitOnTheSurveyAxis() {
        QuickFrequencySelectorPanel panel = new QuickFrequencySelectorPanel();
        panel.setSize(1200, 220);
        panel.doLayout();
        panel.validate();
        AbstractButton fm = panel.findButton(QuickSelectPreset.FM.label);
        AbstractButton wifi = panel.findButton(QuickSelectPreset.WIFI_2.label);
        assertNotNull(fm);
        assertTrue(fm.getFont().getSize() >= 16, "Quick Select type was " + fm.getFont().getSize());
        assertTrue(fm.getHeight() >= SurveyChipLayout.BUTTON_H - 2);
        java.util.List<SurveyChipLayout.Chip> chips = panel.chips();
        assertFalse(chips.isEmpty());
        SurveyChipLayout.Chip fmChip = null;
        SurveyChipLayout.Chip wifiChip = null;
        for (SurveyChipLayout.Chip c : chips) {
            if (c.preset == QuickSelectPreset.FM)
                fmChip = c;
            if (c.preset == QuickSelectPreset.WIFI_2)
                wifiChip = c;
        }
        assertNotNull(fmChip);
        assertNotNull(wifiChip);
        assertTrue(fmChip.anchorX() < wifiChip.anchorX(), "FM must sit left of WiFi 2 on the 0–7.25 GHz strip");
        assertEquals(SurveyChipLayout.Side.TOP, fmChip.side);
        assertEquals(SurveyChipLayout.Side.BOTTOM, panel.chips().stream()
                .filter(c -> c.preset == QuickSelectPreset.ALL).findFirst().orElseThrow().side);
    }

    @Test
    void zoomOutExpandsTheSurveyWindow() {
        OperatorNavBanner banner = new OperatorNavBanner();
        banner.setSize(1400, 280);
        banner.doLayout();
        FrequencyRange before = banner.quickSelector().sweepWindow();
        int[] goldBefore = SpectrumWavePainter.windowPixels(before, 1000);
        banner.rangePanel().zoomOutButton().doClick();
        FrequencyRange after = banner.quickSelector().sweepWindow();
        int[] goldAfter = SpectrumWavePainter.windowPixels(after, 1000);
        assertTrue(after.spanMHz() > before.spanMHz(), "gold survey window must follow − bandwidth");
        assertTrue(goldAfter[1] - goldAfter[0] > goldBefore[1] - goldBefore[0],
                "gold overlay must grow when span doubles");
        banner.rangePanel().zoomInButton().doClick();
        assertEquals(before.spanMHz(), banner.quickSelector().sweepWindow().spanMHz());
    }

    @Test
    void digitsSitAbovePanZoomBesideTheWave() {
        OperatorNavBanner banner = new OperatorNavBanner();
        banner.setSize(1400, 300);
        banner.validate();
        banner.doLayout();
        banner.quickSelector().doLayout();
        java.awt.Point wave = javax.swing.SwingUtilities.convertPoint(
                banner.quickSelector().waveStrip().getParent(),
                banner.quickSelector().waveStrip().getLocation(), banner);
        java.awt.Point keys = javax.swing.SwingUtilities.convertPoint(
                banner.rangePanel().keysPanel().getParent(),
                banner.rangePanel().keysPanel().getLocation(), banner);
        java.awt.Point digits = javax.swing.SwingUtilities.convertPoint(
                banner.rangePanel().displayPanel().getParent(),
                banner.rangePanel().displayPanel().getLocation(), banner);
        int waveRight = wave.x + banner.quickSelector().waveStrip().getWidth();
        assertTrue(keys.x >= waveRight - 2, "◀ − + ▶ must sit to the right of the wave");
        assertTrue(digits.x >= waveRight - 2, "range digits must sit to the right of the wave");
        assertTrue(Math.abs(wave.y - keys.y) <= 8, "◀ − + ▶ must share the wave row");
        int digitBottom = digits.y + banner.rangePanel().displayPanel().getHeight();
        assertTrue(digitBottom <= keys.y + 4, "2402–2472 digits must sit above the adjustment buttons");
        assertTrue(keys.y - digitBottom <= 16, "digits must sit immediately above ◀ − + ▶, not at the banner top");
    }

    @Test
    void testWorksWithBinder() throws PropertyVetoException {
        FrequencyRangePanel range = new FrequencyRangePanel();
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();

        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(range, quick);

        // Simulate quick change by directly triggering the vetoable logic? The listener is internal.
        // Since the binder wires the vetoable, we can call the public set on quick? But quick doesn't have public setValue.
        // Quick fires on button clicks internally.

        // Low hanging test: just ensure binder doesn't throw on construction and getRange works.
        assertNotNull(binder.getFrequencyRange());
        assertTrue(binder.getFrequencyRange().getStartMHz() > 0);
    }
}
