package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

import hotiron.core.FrequencyRange;

class FrequencySelectorRangeBinderTest {

    private static JButton buttonNamed(QuickFrequencySelectorPanel panel, String text) {
        JButton button = panel.findButton(text);
        assertNotNull(button, "No button labeled " + text);
        return button;
    }

    @Test
    void testConstructionAndInitialRange() {
        FrequencyRangePanel range = new FrequencyRangePanel();
        range.setRange(new FrequencyRange(2400, 2500));
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(range, quick);
        assertEquals(2400, binder.getFrequencyRange().getStartMHz());
        assertEquals(2500, binder.getFrequencyRange().getEndMHz());
    }

    @Test
    void quickSelectButtonsSetKnownRanges() {
        FrequencyRangePanel range = new FrequencyRangePanel();
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(range, quick);
        for (QuickSelectPreset preset : QuickSelectPreset.values()) {
            buttonNamed(quick, preset.label).doClick();
            assertEquals(preset.startMHz, binder.getFrequencyRange().getStartMHz(), preset.label + " start");
            assertEquals(preset.endMHz, binder.getFrequencyRange().getEndMHz(), preset.label + " end");
            assertEquals(preset.label, quick.getValue());
        }
    }

    @Test
    void clickingSamePresetAgainRestoresRange() {
        FrequencyRangePanel range = new FrequencyRangePanel();
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(range, quick);
        buttonNamed(quick, QuickSelectPreset.WIFI_2.label).doClick();
        range.setRange(new FrequencyRange(2412, 2462));
        buttonNamed(quick, QuickSelectPreset.WIFI_2.label).doClick();
        assertEquals(QuickSelectPreset.WIFI_2.startMHz, binder.getFrequencyRange().getStartMHz());
        assertEquals(QuickSelectPreset.WIFI_2.endMHz, binder.getFrequencyRange().getEndMHz());
    }

    @Test
    void quickSelectNotifiesRangeListenerOnce() {
        FrequencyRangePanel range = new FrequencyRangePanel();
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(range, quick);
        AtomicInteger notifications = new AtomicInteger();
        binder.addPropertyChangeListener(evt -> notifications.incrementAndGet());
        buttonNamed(quick, "LTE-1").doClick();
        assertEquals(1, notifications.get(), "preset must be one sweep restart, not start+end");
        assertEquals(QuickSelectPreset.LTE_1.startMHz, binder.getFrequencyRange().getStartMHz());
        assertEquals(QuickSelectPreset.LTE_1.endMHz, binder.getFrequencyRange().getEndMHz());
    }
}
