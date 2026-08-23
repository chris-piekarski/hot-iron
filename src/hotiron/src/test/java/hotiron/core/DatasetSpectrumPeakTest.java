package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatasetSpectrumPeakTest {

    @Test
    void testConstructionAndInitialPeaks() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2500, -120f, 5f, 1000L);
        assertEquals(1000, peak.spectrumLength());
        assertEquals(-120f, peak.getPower(0));
        // peaks should be initialized to init power
    }

    @Test
    void testRefreshPeakSpectrumWithNewHigh() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        // set some spectrum data
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -50f;
        spectrum[1] = -60f;

        // simulate time passage
        peak.lastAdded = System.currentTimeMillis() - 100;

        peak.refreshPeakSpectrum();

        // high value should set both peak and hold
        assertEquals(-50f, peak.spectrumPeak[0], 0.001f);
        assertEquals(-50f, peak.spectrumPeakHold[0], 0.001f);
    }

    @Test
    void testPeakFallBelowThresholdHolds() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 5f, 1000L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -50f;

        peak.lastAdded = System.currentTimeMillis() - 100;
        peak.refreshPeakSpectrum();

        // now lower value but within threshold
        spectrum[0] = -53f;
        peak.lastAdded = System.currentTimeMillis() - 100;
        peak.refreshPeakSpectrum();

        // hold should still be the high value since diff < threshold
        assertEquals(-50f, peak.spectrumPeakHold[0], 0.001f);
    }

    @Test
    void testPeakFallExceedsThresholdUpdatesHold() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 5f, 1000L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -50f;

        peak.lastAdded = System.currentTimeMillis() - 100;
        peak.refreshPeakSpectrum();

        // much lower; use a large dt so EMA k is high enough to exceed the 5 dB threshold
        spectrum[0] = -70f;
        peak.lastAdded = System.currentTimeMillis() - 1000;
        peak.refreshPeakSpectrum();

        // now hold should have fallen
        assertTrue(peak.spectrumPeakHold[0] < -50f);
    }

    @Test
    void testCalculateSpectrumPeakPower() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        float[] hold = peak.spectrumPeakHold;
        hold[0] = -30f;
        hold[1] = -40f;

        double power = peak.calculateSpectrumPeakPower();
        // rough check: should be finite and reasonable
        assertTrue(power > -100 && power < 0);
    }

    @Test
    void createPeaksDatasetUsesHold() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        peak.spectrumPeakHold[0] = -30f;
        peak.setPeakFalloutMillis(250L);

        hotiron.core.jfc.XYSeriesImmutable series = peak.createPeaksDataset("peaks");
        assertEquals(peak.spectrumLength(), series.getItemCount());
        assertEquals(-30.0, series.getYY(0), 0.001);
    }

    @Test
    void testResetPeaks() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        peak.spectrumPeakHold[0] = -30f;
        peak.resetPeaks();
        assertEquals(-120f, peak.spectrumPeakHold[0]);
    }

    @Test
    void testCopyTo() {
        DatasetSpectrumPeak src = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        src.spectrumPeakHold[0] = -30f;
        src.spectrumPeak[0] = -25f;

        DatasetSpectrumPeak dst = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        src.copyTo(dst);

        assertEquals(-30f, dst.spectrumPeakHold[0]);
        assertEquals(-25f, dst.spectrumPeak[0]);
    }

    @Test
    void testMultipleRefreshesWithSimulatedTime() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 8f, 500L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -55f;
        spectrum[1] = -65f;

        long baseTime = System.currentTimeMillis();
        peak.lastAdded = baseTime - 200;
        peak.refreshPeakSpectrum();

        // Simulate time passing
        peak.lastAdded = baseTime - 50;
        spectrum[0] = -58f;  // slight drop
        peak.refreshPeakSpectrum();

        // Now larger time and bigger drop to trigger threshold
        peak.lastAdded = baseTime - 1000;
        spectrum[0] = -90f;
        peak.refreshPeakSpectrum();

        assertTrue(peak.spectrumPeakHold[0] < -55f);  // should have updated due to time + threshold
        assertTrue(peak.spectrumPeak[0] < -55f);
    }

    @Test
    void testResetPeaksAndRecalcPowerEdgeCases() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        peak.resetPeaks();
        double power = peak.calculateSpectrumPeakPower();
        assertTrue(Double.isFinite(power) || power < 0);  // should be low/negative for init power

        // Set some peaks and recalc
        peak.spectrumPeakHold[0] = -20f;
        peak.spectrumPeakHold[1] = -30f;
        double power2 = peak.calculateSpectrumPeakPower();
        assertTrue(power2 > power);
    }
}
