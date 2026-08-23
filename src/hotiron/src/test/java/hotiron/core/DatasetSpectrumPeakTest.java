package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatasetSpectrumPeakTest {

    @Test
    void testConstructionAndInitialPeaks() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2500, -120f, 5f, 1000L);
        assertEquals(1000, peak.spectrumLength());
        assertEquals(-120f, peak.getPower(0));
    }

    @Test
    void testRefreshPeakSpectrumWithNewHigh() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -50f;
        spectrum[1] = -60f;
        peak.lastAdded = System.currentTimeMillis() - 100;
        peak.refreshPeakSpectrum();
        assertEquals(-50f, peak.spectrumPeak[0], 0.001f);
        assertEquals(-50f, peak.spectrumPeakHold[0], 0.001f);
    }

    @Test
    void halfLifeMovesHoldHalfwayTowardLive() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -30f;
        peak.lastAdded = System.currentTimeMillis() - 1;
        peak.refreshPeakSpectrum();
        assertEquals(-30f, peak.spectrumPeakHold[0], 0.001f);

        spectrum[0] = -90f;
        peak.lastAdded = System.currentTimeMillis() - 1000;
        peak.refreshPeakSpectrum();
        assertEquals(-60f, peak.spectrumPeakHold[0], 0.05f);
        assertEquals(peak.spectrumPeakHold[0], peak.spectrumPeak[0], 0.001f);
    }

    @Test
    void zeroHalfLifeFollowsLive() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 0L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -40f;
        peak.lastAdded = System.currentTimeMillis() - 50;
        peak.refreshPeakSpectrum();
        spectrum[0] = -80f;
        peak.lastAdded = System.currentTimeMillis() - 50;
        peak.refreshPeakSpectrum();
        assertEquals(-80f, peak.spectrumPeakHold[0], 0.001f);
    }

    @Test
    void chartHolesDoNotPullPeaksDown() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -150f, 10f, 1000L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -40f;
        peak.lastAdded = System.currentTimeMillis() - 1;
        peak.refreshPeakSpectrum();
        spectrum[0] = SpectrumPowerScale.EMPTY_CEILING;
        peak.lastAdded = System.currentTimeMillis() - 5000;
        peak.refreshPeakSpectrum();
        assertEquals(-40f, peak.spectrumPeakHold[0], 0.001f);
    }

    @Test
    void testCalculateSpectrumPeakPower() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        float[] hold = peak.spectrumPeakHold;
        hold[0] = -30f;
        hold[1] = -40f;

        double power = peak.calculateSpectrumPeakPower();
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
    void twoHalfLivesLeaveAQuarterOfTheGap() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 8f, 500L);
        float[] spectrum = peak.getSpectrumArray();
        spectrum[0] = -20f;
        peak.lastAdded = System.currentTimeMillis() - 1;
        peak.refreshPeakSpectrum();
        spectrum[0] = -80f;
        peak.lastAdded = System.currentTimeMillis() - 1000;
        peak.refreshPeakSpectrum();
        assertEquals(-65f, peak.spectrumPeakHold[0], 0.1f);
    }

    @Test
    void ingestParkedFrameReusesAxisAndDecaysPeaks() {
        float binHz = 15625f;
        float[] mhz = new float[8];
        float[] dbfs = new float[8];
        for (int i = 0; i < mhz.length; i++)
        {
            mhz[i] = 597f + i * (binHz / 1_000_000f);
            dbfs[i] = -40f;
        }
        dbfs[3] = -10f;
        DatasetSpectrumPeak first = DatasetSpectrumPeak.ingestParkedFrame(null, mhz, dbfs, binHz, 1000L);
        assertNotNull(first);
        assertEquals(8, first.spectrumLength());
        assertEquals(-10f, first.spectrumPeakHold[3], 0.001f);
        assertSame(first, DatasetSpectrumPeak.ingestParkedFrame(first, mhz, dbfs, binHz, 1000L));

        dbfs[3] = -70f;
        first.lastAdded = System.currentTimeMillis() - 1000;
        DatasetSpectrumPeak aged = DatasetSpectrumPeak.ingestParkedFrame(first, mhz, dbfs, binHz, 1000L);
        assertSame(first, aged);
        assertEquals(-40f, aged.spectrumPeakHold[3], 0.05f);

        float[] shifted = mhz.clone();
        shifted[0] += 0.2f;
        DatasetSpectrumPeak retuned = DatasetSpectrumPeak.ingestParkedFrame(first, shifted, dbfs, binHz, 1000L);
        assertNotSame(first, retuned);
        assertFalse(first.sameAxisAs(retuned));
    }

    @Test
    void testResetPeaksAndRecalcPowerEdgeCases() {
        DatasetSpectrumPeak peak = new DatasetSpectrumPeak(100000f, 2400, 2401, -120f, 10f, 1000L);
        peak.resetPeaks();
        double power = peak.calculateSpectrumPeakPower();
        assertTrue(Double.isFinite(power) || power < 0);

        peak.spectrumPeakHold[0] = -20f;
        peak.spectrumPeakHold[1] = -30f;
        double power2 = peak.calculateSpectrumPeakPower();
        assertTrue(power2 > power);
    }
}
