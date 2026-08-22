package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;
import hotiron.core.RadioIdentity;

class HackRFSweepSettingsUITest {

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void noArgConstructorDoesNotThrow() {
        assertDoesNotThrow(() -> { new HackRFSweepSettingsUI(); });
    }

    @Test
    void bindsFftBinPausePeaksPersistenceAndHardwareStatus() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();

        assertEquals("100 000", ui.fftBinSpinner().getValue().toString());
        assertTrue(ui.autoScaleCheckbox().isSelected(), "dB auto-scale is on so FM/Wi-Fi peaks fill the axis");
        assertTrue(ui.autoGainCheckbox().isSelected(), "auto gain is the default");
        assertFalse(ui.gainSlider().isEnabled(), "gain sliders stay locked while auto is on");
        assertEquals("Pause", ui.pauseButton().getText());
        assertTrue(ui.peakFallSpinner().isVisible());
        assertTrue(ui.decayRateCombo().isVisible());
        assertTrue(ui.connectedLabel().getText().contains("No radio detected"));

        SwingUtilities.invokeAndWait(() -> ui.pauseButton().doClick());
        flushEdt();
        assertTrue(settings.isCapturingPaused().getValue());
        assertEquals("Resume", ui.pauseButton().getText());

        SwingUtilities.invokeAndWait(() -> ui.autoScaleCheckbox().setSelected(false));
        flushEdt();
        assertFalse(settings.isPowerAutoScale().getValue());

        SwingUtilities.invokeAndWait(() -> ui.autoGainCheckbox().setSelected(false));
        flushEdt();
        assertFalse(settings.isAutoGain().getValue());
        assertTrue(ui.gainSlider().isEnabled(), "unchecking Auto unlocks the gain sliders");

        settings.isChartsPeaksVisible().setValue(false);
        flushEdt();
        assertFalse(ui.peakFallSpinner().isVisible());

        settings.isPersistentDisplayVisible().setValue(false);
        flushEdt();
        assertFalse(ui.decayRateCombo().isVisible());

        settings.getRadioIdentity().setValue(
                RadioIdentity.of("HackRF One", "0000000000000000a1b2c3d4e5f60708", "v2026.01.3", "1.16"));
        settings.fireHardwareStatusChanged(true);
        flushEdt();
        assertTrue(ui.connectedLabel().getText().contains("HackRF One"));
        assertTrue(ui.connectedLabel().getText().contains("SN e5f60708"));
        assertTrue(ui.connectedLabel().getText().contains("FW 2026.01.3"));
        assertFalse(ui.connectedLabel().getText().contains("HackRF connected"));
        assertTrue(ui.connectedLabel().getToolTipText().contains("Sweep running"));
    }

    @Test
    void mcpStatusShowsOffThenClients() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertTrue(ui.mcpStatusLabel().getText().contains("MCP  off"));
        hotiron.core.McpStatus.Client c = new hotiron.core.McpStatus.Client(
                "claude-code", "1", "127.0.0.1:9", "spectrum_summary", 1L, 2L, 3L);
        settings.getMcpStatus().setValue(hotiron.core.McpStatus.listening(
                "127.0.0.1", 8765, false, java.util.List.of(c), "spectrum_summary", 2L));
        flushEdt();
        assertTrue(ui.mcpStatusLabel().getText().contains("127.0.0.1:8765"));
        assertTrue(ui.mcpStatusLabel().getText().contains("claude-code"));
        assertTrue(ui.mcpStatusLabel().getText().contains("spectrum_summary"));
    }

    @Test
    void hardwareStripRestartStopPickerAndClkout() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        settings.listedSerials.add("aabbccdd");
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();

        assertEquals("Restart", ui.restartButton().getText());
        assertEquals("Stop", ui.stopButton().getText());
        assertEquals(HackRFSweepSettingsUI.FIRST_RADIO, ui.radioCombo().getItemAt(0));
        assertEquals("aabbccdd", ui.radioCombo().getItemAt(1));

        SwingUtilities.invokeAndWait(() -> ui.stopButton().doClick());
        flushEdt();
        assertEquals(1, settings.releaseRadioCalls);
        assertTrue(settings.isRadioReleased().getValue());
        assertEquals("Stopped", ui.stopButton().getText());
        assertFalse(ui.pauseButton().isEnabled());

        SwingUtilities.invokeAndWait(() -> ui.restartButton().doClick());
        flushEdt();
        assertEquals(1, settings.restartSweepCalls);
        assertFalse(settings.isRadioReleased().getValue());

        SwingUtilities.invokeAndWait(() -> ui.clkoutCheckBox().setSelected(true));
        flushEdt();
        assertTrue(settings.getClkoutEnable().getValue());
    }

    @Test
    void listenButtonParksTheRadioWithoutRelease() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals("Listen", ui.listenButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertEquals(1, settings.startListenCalls);
        assertTrue(settings.isListening().getValue());
        assertFalse(settings.isRadioReleased().getValue());
        assertTrue(ui.listenButton().getText().contains("Listening"));
        assertFalse(ui.pauseButton().isEnabled());
        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue());
        assertEquals(1, settings.restartSweepCalls);
        assertEquals(0, settings.releaseRadioCalls);
    }

    @Test
    void watchButtonParksAsTvWithoutRelease() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals("Watch", ui.watchButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.watchButton().doClick());
        flushEdt();
        assertEquals(1, settings.startWatchCalls);
        assertTrue(settings.isListening().getValue());
        assertEquals(hotiron.core.ListenService.TV, settings.getListenService().getValue());
        assertTrue(ui.watchButton().getText().contains("Watching"));
        assertNotNull(ui.tvTunerPanel().previewPanel());
        assertEquals(90, ui.tvTunerPanel().previewPanel().getMinimumSize().height);
        SwingUtilities.invokeAndWait(() -> ui.watchButton().doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue());
    }

    @Test
    void stationKnobJumpsBetweenDetectedStations() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        settings.getDetectedFmStations().setValue(java.util.List.of(
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(88.1), -40f),
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(97.3), -30f),
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(101.1), -35f)));
        settings.getListenKHz().setValue(97300);
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals(97300, ui.stationKnob().getKHz());
        SwingUtilities.invokeAndWait(() -> ui.stationKnob().nudge(+1));
        flushEdt();
        assertEquals(101100, settings.getListenKHz().getValue());
        SwingUtilities.invokeAndWait(() -> ui.stationKnob().nudge(-1));
        flushEdt();
        assertEquals(97300, settings.getListenKHz().getValue());
    }

    @Test
    void tuneIsOneChannelAndSeekSkipsToTheNextHit() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        settings.getDetectedFmStations().setValue(java.util.List.of(
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(88.1), -40f),
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(97.3), -30f),
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(101.1), -35f)));
        settings.getListenKHz().setValue(97300);
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        SwingUtilities.invokeAndWait(() -> ui.tunerPanel().tuneUpButton().doClick());
        flushEdt();
        assertEquals(97500, settings.getListenKHz().getValue());
        SwingUtilities.invokeAndWait(() -> ui.tunerPanel().seekUpButton().doClick());
        flushEdt();
        assertEquals(101100, settings.getListenKHz().getValue());
    }

    @Test
    void sweepRangePanelReplacesDigitWheelsAndUpdatesTheModel() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertNotNull(ui.frequencyRangePanel());
        assertEquals(0, countComponents(ui, hotiron.ui.FrequencySelectorPanel.class));
        SwingUtilities.invokeAndWait(() -> ui.frequencyRangePanel().setRange(
                new hotiron.core.FrequencyRange(88, 108)));
        flushEdt();
        assertEquals(88, settings.getFrequency().getValue().getStartMHz());
        assertEquals(108, settings.getFrequency().getValue().getEndMHz());
        SwingUtilities.invokeAndWait(() -> ui.frequencyRangePanel().panRightButton().doClick());
        flushEdt();
        assertEquals(93, settings.getFrequency().getValue().getStartMHz());
        assertEquals(113, settings.getFrequency().getValue().getEndMHz());
    }

    private static int countComponents(java.awt.Container root, Class<?> type) {
        int n = type.isInstance(root) ? 1 : 0;
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof java.awt.Container)
                n += countComponents((java.awt.Container) child, type);
            else if (type.isInstance(child))
                n++;
        }
        return n;
    }
}
