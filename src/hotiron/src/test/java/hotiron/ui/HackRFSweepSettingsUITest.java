package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import hotiron.FakeHackRFSettings;
import hotiron.core.RadioIdentity;

class HackRFSweepSettingsUITest {

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void bindsFftBinPausePeaksPersistenceAndHardwareStatus() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();

        assertEquals(QuickSelectPreset.WIFI_2.startMHz, settings.getFrequency().getValue().getStartMHz());
        assertEquals(QuickSelectPreset.WIFI_2.endMHz, settings.getFrequency().getValue().getEndMHz());
        assertEquals(QuickSelectPreset.WIFI_2.startMHz, ui.frequencyRangePanel().getRange().getStartMHz());
        assertEquals(QuickSelectPreset.WIFI_2.endMHz, ui.frequencyRangePanel().getRange().getEndMHz());
        assertTrue(ui.quickFrequencySelector().isHighlighted(QuickSelectPreset.WIFI_2.label),
                "boot Quick Select is WiFi 2");
        assertEquals("20 000", ui.fftBinSpinner().getValue().toString());
        assertTrue(ui.autoScaleCheckbox().isSelected(), "dB auto-scale is on so FM/Wi-Fi peaks fill the axis");
        assertEquals("USA", settings.getFrequencyAllocationTable().getValue().toString());
        assertEquals("USA", ui.chartToggleBar().allocationCombo().getSelectedItem().toString());
        assertTrue(ui.autoGainCheckbox().isSelected(), "auto gain is the default");
        assertTrue(ui.autoSweepCheckbox().isSelected(), "auto FFT/samples is the default");
        assertFalse(ui.gainSlider().isEnabled(), "gain sliders stay locked while auto is on");
        assertFalse(ui.fftBinSpinner().isEnabled(), "FFT bin stays locked while auto is on");
        assertFalse(ui.samplesSpinner().isEnabled(), "samples stay locked while auto is on");
        assertEquals("Pause", ui.pauseButton().getText());
        assertTrue(ui.peakFallSpinner().isEnabled());
        assertTrue(ui.decayRateCombo().isEnabled());
        assertNotNull(ui.gainRail());
        assertNotNull(ui.chartToggleBar());
        assertNotNull(ui.footer());
        assertTrue(ui.footer().isAncestorOf(ui.pauseButton()));
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

        SwingUtilities.invokeAndWait(() -> ui.autoSweepCheckbox().setSelected(false));
        flushEdt();
        assertFalse(settings.isAutoSweep().getValue());
        assertTrue(ui.fftBinSpinner().isEnabled(), "unchecking Auto FFT unlocks the bin spinner");
        assertTrue(ui.samplesSpinner().isEnabled(), "unchecking Auto FFT unlocks the samples spinner");

        settings.isChartsPeaksVisible().setValue(false);
        flushEdt();
        assertFalse(ui.peakFallSpinner().isEnabled());

        settings.isPersistentDisplayVisible().setValue(false);
        flushEdt();
        assertFalse(ui.decayRateCombo().isEnabled());

        settings.getRadioIdentity().setValue(
                RadioIdentity.of("HackRF One", "0000000000000000a1b2c3d4e5f60708", "v2026.01.3", "1.16"));
        settings.fireHardwareStatusChanged(true);
        flushEdt();
        assertTrue(ui.connectedLabel().getText().contains("HackRF One"));
        assertTrue(ui.connectedLabel().getText().contains("SN e5f60708"));
        assertTrue(ui.connectedLabel().getText().contains("FW 2026.01.3"));
        assertFalse(ui.connectedLabel().getText().contains("HackRF connected"));
        assertNull(ui.connectedLabel().getToolTipText(), "status hover is an in-panel hint, not a Swing tooltip");
        assertTrue(ExclusiveToolTip.hintOf(ui.connectedLabel()).contains("Sweep running"));
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
        assertTrue(ui.mcpStatusLabel().getText().contains(":8765"));
        assertTrue(ui.mcpStatusLabel().getText().contains("1 client"));
        assertTrue(ExclusiveToolTip.hintOf(ui.mcpStatusLabel()).contains("claude-code"));
        assertTrue(ExclusiveToolTip.hintOf(ui.mcpStatusLabel()).contains("spectrum_summary"));
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
    void sniffButtonParksAsNfcWithoutRelease() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals("Sniff", ui.nfcSniffPanel().sniffButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.nfcSniffPanel().sniffButton().doClick());
        flushEdt();
        assertEquals(1, settings.startSniffCalls);
        assertTrue(settings.isListening().getValue());
        assertEquals(hotiron.core.ListenService.NFC, settings.getListenService().getValue());
        assertEquals(hotiron.core.RadioMode.NFC, settings.radioMode());
        assertEquals("Stop", ui.nfcSniffPanel().sniffButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.nfcSniffPanel().sniffButton().doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue());
    }

    @Test
    void bleSniffButtonDoesNotParkTheHackrf() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals("Sniff", ui.bleSniffPanel().sniffButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.bleSniffPanel().sniffButton().doClick());
        flushEdt();
        assertEquals(1, settings.startBleSniffCalls);
        assertTrue(settings.isBleSniffing().getValue());
        assertFalse(settings.isListening().getValue());
        assertEquals(hotiron.core.RadioMode.SWEEP, settings.radioMode());
        assertEquals(hotiron.core.BleBandPlan.VIEW_START_MHZ, settings.getFrequency().getValue().getStartMHz());
        assertEquals(hotiron.core.BleBandPlan.VIEW_END_MHZ, settings.getFrequency().getValue().getEndMHz());
        assertEquals("Stop", ui.bleSniffPanel().sniffButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.bleSniffPanel().sniffButton().doClick());
        flushEdt();
        assertEquals(1, settings.stopBleSniffCalls);
        assertFalse(settings.isBleSniffing().getValue());
    }

    @Test
    void seekButtonsJumpStrongStationsAndScaleStaysOnTheDial() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        settings.getDetectedFmStations().setValue(java.util.List.of(
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(88.1), -40f),
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(97.3), -30f),
                new hotiron.core.FmStationHit(hotiron.core.FmChannelPlan.nearest(101.1), -35f)));
        settings.getListenKHz().setValue(97300);
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals(97300, ui.stationKnob().getKHz());
        SwingUtilities.invokeAndWait(() -> ui.tunerPanel().seekUpButton().doClick());
        flushEdt();
        assertEquals(101100, settings.getListenKHz().getValue());
        SwingUtilities.invokeAndWait(() -> ui.tunerPanel().seekDownButton().doClick());
        flushEdt();
        assertEquals(97300, settings.getListenKHz().getValue());
        SwingUtilities.invokeAndWait(() -> ui.stationKnob().nudge(+1));
        flushEdt();
        assertEquals(97500, settings.getListenKHz().getValue(), "dial wheel is fine tune, not seek");
    }

    @Test
    void fmScanButtonStopsListenAndSweepsTheFmBand() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals("Scan", ui.tunerPanel().scanButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertTrue(settings.isListening().getValue());
        SwingUtilities.invokeAndWait(() -> ui.tunerPanel().scanButton().doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue());
        assertEquals(hotiron.core.BandScan.FM, settings.getBandScan().getValue());
        assertEquals(hotiron.core.FmChannelPlan.VIEW_START_MHZ, settings.getFrequency().getValue().getStartMHz());
        assertEquals(hotiron.core.FmChannelPlan.VIEW_END_MHZ, settings.getFrequency().getValue().getEndMHz());
        assertEquals("Scanning…", ui.tunerPanel().scanButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.tunerPanel().scanButton().doClick());
        flushEdt();
        assertEquals(hotiron.core.BandScan.OFF, settings.getBandScan().getValue());
        assertEquals("Scan", ui.tunerPanel().scanButton().getText());
    }

    @Test
    void tvScanButtonStopsWatchAndSweepsVhf() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        SwingUtilities.invokeAndWait(() -> ui.watchButton().doClick());
        flushEdt();
        assertTrue(settings.isListening().getValue());
        SwingUtilities.invokeAndWait(() -> ui.tvTunerPanel().scanButton().doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue());
        assertEquals(hotiron.core.BandScan.TV, settings.getBandScan().getValue());
        assertEquals(hotiron.core.TvChannelPlan.VHF_VIEW_START_MHZ, settings.getFrequency().getValue().getStartMHz());
        assertEquals(hotiron.core.TvChannelPlan.VHF_VIEW_END_MHZ, settings.getFrequency().getValue().getEndMHz());
        assertEquals("Scanning…", ui.tvTunerPanel().scanButton().getText());
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
    void quickSelectStopsListenAndSweepsThatBand() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertTrue(settings.isListening().getValue());
        int restarts = settings.restartSweepCalls;
        SwingUtilities.invokeAndWait(() -> ui.quickFrequencySelector()
                .findButton(QuickSelectPreset.FM.label).doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue(), "Listen audio must stop");
        assertEquals(QuickSelectPreset.FM.startMHz, settings.getFrequency().getValue().getStartMHz());
        assertEquals(QuickSelectPreset.FM.endMHz, settings.getFrequency().getValue().getEndMHz());
        assertEquals(restarts + 1, settings.restartSweepCalls);
    }

    @Test
    void quickSelectStopsWatchEvenOnTheSameBand() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        SwingUtilities.invokeAndWait(() -> ui.watchButton().doClick());
        flushEdt();
        assertTrue(settings.isListening().getValue());
        assertEquals(hotiron.core.ListenService.TV, settings.getListenService().getValue());
        int restarts = settings.restartSweepCalls;
        SwingUtilities.invokeAndWait(() -> ui.quickFrequencySelector()
                .findButton(QuickSelectPreset.WIFI_2.label).doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue(), "Watch must stop");
        assertEquals(QuickSelectPreset.WIFI_2.startMHz, settings.getFrequency().getValue().getStartMHz());
        assertEquals(QuickSelectPreset.WIFI_2.endMHz, settings.getFrequency().getValue().getEndMHz());
        assertEquals(restarts + 1, settings.restartSweepCalls);
    }

    @Test
    void sweepRangePanelReplacesDigitWheelsAndUpdatesTheModel() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertNotNull(ui.frequencyRangePanel());
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

    @Test
    void hoverHintDoesNotChangeSettingsColumnSize() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        Dimension before = ui.getPreferredSize();
        ExclusiveToolTip.dispatchForTest(ui.tunerPanel().scanButton(), MouseEvent.MOUSE_ENTERED);
        ExclusiveToolTip.dispatchForTest(ui.listenButton(), MouseEvent.MOUSE_ENTERED);
        ExclusiveToolTip.dispatchForTest(ui.restartButton(), MouseEvent.MOUSE_ENTERED);
        flushEdt();
        assertEquals(before, ui.getPreferredSize(), "hover must not reflow the settings column");
        assertNull(ui.listenButton().getToolTipText());
        assertNull(ui.restartButton().getToolTipText());
        assertTrue(ExclusiveToolTip.isShowing());
        ExclusiveToolTip.hide();
    }

    @Test
    void wifi2ShowsNoBandTool() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals(OperatorLayout.TOOLS_WIDTH, ui.getPreferredSize().width);
        assertFalse(ui.bandSlot().hosts(ui.bleSniffPanel()), "BLE tool is the BLE chip, not Wi-Fi 2");
        assertFalse(ui.bandSlot().hosts(ui.tunerPanel()));
        assertFalse(ui.bandSlot().hosts(ui.tvTunerPanel()));
        assertFalse(ui.bandSlot().hosts(ui.nfcSniffPanel()));
        assertFalse(ui.bandContext().shows(hotiron.core.BandToolKind.BLE));
        assertEquals(OperatorLayout.BAND_SLOT_HEIGHT, ui.bandSlot().getPreferredSize().height);
    }

    @Test
    void bandToolsFollowTheViewAndStayPinnedWhileParked() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        Dimension before = ui.getPreferredSize();

        SwingUtilities.invokeAndWait(() -> ui.quickFrequencySelector()
                .findButton(QuickSelectPreset.BLE.label).doClick());
        flushEdt();
        assertTrue(ui.bandSlot().hosts(ui.bleSniffPanel()));
        assertFalse(ui.bandSlot().hosts(ui.tunerPanel()));

        SwingUtilities.invokeAndWait(() -> ui.quickFrequencySelector()
                .findButton(QuickSelectPreset.FM.label).doClick());
        flushEdt();
        assertTrue(ui.bandSlot().hosts(ui.tunerPanel()));
        assertFalse(ui.bandSlot().hosts(ui.bleSniffPanel()));
        assertEquals(before.width, ui.getPreferredSize().width);
        assertEquals(OperatorLayout.BAND_SLOT_HEIGHT, ui.bandSlot().getPreferredSize().height);

        SwingUtilities.invokeAndWait(() -> ui.quickFrequencySelector()
                .findButton(QuickSelectPreset.V_TV.label).doClick());
        flushEdt();
        assertFalse(ui.bandSlot().hosts(ui.tunerPanel()), "V-TV is the TV tool, not FM beside it");
        assertTrue(ui.bandSlot().hosts(ui.tvTunerPanel()));

        SwingUtilities.invokeAndWait(() -> ui.quickFrequencySelector()
                .findButton(QuickSelectPreset.ALL.label).doClick());
        flushEdt();
        assertFalse(ui.bandSlot().hosts(ui.tunerPanel()));
        assertFalse(ui.bandSlot().hosts(ui.tvTunerPanel()));
        assertFalse(ui.bandSlot().hosts(ui.bleSniffPanel()));

        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertTrue(ui.bandSlot().hosts(ui.tunerPanel()), "Listen pins FM on All");
        assertEquals(before.width, ui.getPreferredSize().width);
    }

    @Test
    void navBannerOwnsQuickSelectAndRange() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertNotNull(ui.navBanner());
        assertSame(ui.quickFrequencySelector(), ui.navBanner().quickSelector());
        assertSame(ui.frequencyRangePanel(), ui.navBanner().rangePanel());
    }

    @Test
    void spectrumToolsColumnHostsTheMcpLog() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals(OperatorLayout.TOOLS_WIDTH, ui.getPreferredSize().width);
        assertNotNull(ui.mcpLog());
        ui.mcpLog().apply(hotiron.core.McpStatus.listening("127.0.0.1", 8765));
        assertTrue(ui.mcpLog().text().contains("listening"));
    }

}
