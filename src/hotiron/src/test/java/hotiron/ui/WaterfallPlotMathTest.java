package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WaterfallPlotMathTest {

    @Test
    void normalizePowerClampsToPalette() {
        assertEquals(0.0, WaterfallPlot.normalizePower(-120, -90, 65), 1e-9);
        assertEquals(0.0, WaterfallPlot.normalizePower(-90, -90, 65), 1e-9);
        assertEquals(1.0, WaterfallPlot.normalizePower(0, -90, 65), 1e-9);
        assertEquals(0.5, WaterfallPlot.normalizePower(-57.5, -90, 65), 1e-9);
        assertEquals(0.0, WaterfallPlot.normalizePower(-50, -90, 0), 1e-9);
    }

    @Test
    void liveFmWindowPutsThePeakInTheHotHalfOfThePalette() {
        // Observed indoor FM: noise −83.5, peak −67.5. The historic
        // fixed −90…−25 scale parks that entire band in the blue third.
        assertTrue(WaterfallPlot.normalizePower(-67.5, -90, 65) < 0.40);
        int start = WaterfallPlot.paletteStartDb(-100);
        int size = WaterfallPlot.paletteSizeDb(-100, -50);
        assertEquals(-100, start);
        assertEquals(50, size);
        assertTrue(WaterfallPlot.normalizePower(-67.5, start, size) > 0.60);
        assertTrue(WaterfallPlot.normalizePower(-83.5, start, size) < 0.40);
    }

    @Test
    void paletteSizeDbNeverReturnsZero() {
        assertEquals(1, WaterfallPlot.paletteSizeDb(-50, -50));
        assertEquals(1, WaterfallPlot.paletteSizeDb(0, -4));
    }

    @Test
    void clampPixelXStaysInBuffer() {
        assertEquals(0, WaterfallPlot.clampPixelX(-3, 10));
        assertEquals(9, WaterfallPlot.clampPixelX(99, 10));
        assertEquals(4, WaterfallPlot.clampPixelX(4, 10));
        assertEquals(0, WaterfallPlot.clampPixelX(5, 0));
    }

    @Test
    void parkedIqRfWaterfallUsesTheRfPowerWindow() {
        assertEquals(-80, WaterfallPlot.parkedRowPaletteStart(true, false, -50), 0);
        assertEquals(80, WaterfallPlot.parkedRowPaletteSize(true, false, 40), 0);
        assertEquals(-52, WaterfallPlot.parkedRowPaletteStart(false, true, -52), 0);
        assertEquals(28, WaterfallPlot.parkedRowPaletteSize(false, true, 28), 0);
        assertEquals(-52, WaterfallPlot.parkedRowPaletteStart(true, true, -52), 0,
                "Listen/Watch RF pane is RF even if the sibling is audio");
    }

    @Test
    void atscBrickIsAWaterfallOnTheLiveRfWindowNotAClippedSlab() {
        // Default sweep palette −90…−25 clips a 0 dBFS 8VSB brick.
        assertEquals(1.0, WaterfallPlot.normalizePower(0, -90, 65), 1e-9);
        assertEquals(1.0, WaterfallPlot.normalizePower(-20, -90, 65), 1e-9);
        // Live parked window (−40…+10) keeps noise and the brick distinct.
        double start = WaterfallPlot.parkedRowPaletteStart(false, true, -40);
        double size = WaterfallPlot.parkedRowPaletteSize(false, true, 50);
        assertTrue(WaterfallPlot.normalizePower(-38, start, size) < 0.15);
        double brick = WaterfallPlot.normalizePower(0, start, size);
        assertTrue(brick > 0.70 && brick < 1.0);
        // Default −100…+20 also leaves headroom so Watch RF scrolls.
        assertTrue(WaterfallPlot.normalizePower(0, -100, 120) < 0.90);
        assertTrue(WaterfallPlot.normalizePower(0, -100, 120) > 0.70);
    }

    @Test
    void audioDbfsWindowDoesNotUseTheRfPalette() {
        assertEquals(-80, WaterfallPlot.AUDIO_PALETTE_START_DB, 0);
        assertEquals(80, WaterfallPlot.AUDIO_PALETTE_SIZE_DB, 0);
        assertTrue(WaterfallPlot.normalizePower(-12, WaterfallPlot.AUDIO_PALETTE_START_DB,
                WaterfallPlot.AUDIO_PALETTE_SIZE_DB) > 0.80);
        assertTrue(WaterfallPlot.normalizePower(-70, WaterfallPlot.AUDIO_PALETTE_START_DB,
                WaterfallPlot.AUDIO_PALETTE_SIZE_DB) < 0.20);
    }

    @Test
    void modeBannerNamesRfAndAudio() {
        assertEquals("RF waterfall", WaterfallPlot.modeBanner(false, false));
        assertEquals("parked IQ  ·  AUDIO  ·  0–16 kHz", WaterfallPlot.modeBanner(true, false));
        assertEquals("parked IQ  ·  VIDEO  ·  ±8 MHz", WaterfallPlot.modeBanner(false, true));
        assertEquals("parked IQ  ·  NFC  ·  12–15 MHz", WaterfallPlot.modeBanner(false, false, true));
        assertEquals("parked IQ  ·  RF  ·  ±2 MHz", WaterfallPlot.modeBannerListenRf());
        assertEquals("parked IQ  ·  RF  ·  ±8 MHz", WaterfallPlot.modeBannerWatchRf());
        assertEquals("parked IQ  ·  RF ±2 MHz + AUDIO 0–16 kHz", WaterfallPlot.modeBannerListenDual());
        assertEquals("parked IQ  ·  RF ±8 MHz + AUDIO 0–16 kHz", WaterfallPlot.modeBannerWatchDual());
    }

    @Test
    void formatAudioHzUsesKiloWhenNeeded() {
        assertEquals("440 Hz", WaterfallPlot.formatAudioHz(440));
        assertEquals("1.0 kHz", WaterfallPlot.formatAudioHz(1000));
        assertEquals("16.0 kHz", WaterfallPlot.formatAudioHz(16000));
    }

    @Test
    void translateXToFrequencyMapsAndClamps() {
        assertEquals(-1.0, WaterfallPlot.translateXToFrequency(10, 0, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.4e9, WaterfallPlot.translateXToFrequency(0, 100, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.5e9, WaterfallPlot.translateXToFrequency(100, 100, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.45e9, WaterfallPlot.translateXToFrequency(50, 100, 2.4e9, 2.5e9), 1e5);
        assertEquals(2.4e9, WaterfallPlot.translateXToFrequency(-10, 100, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.5e9, WaterfallPlot.translateXToFrequency(200, 100, 2.4e9, 2.5e9), 1.0);
    }
}
