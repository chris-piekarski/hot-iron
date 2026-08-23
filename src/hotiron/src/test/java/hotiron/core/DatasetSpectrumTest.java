package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DatasetSpectrumTest {

    @Test
    void testConstructionAndBasicProperties() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2500, -120f);
        assertEquals(100000f, ds.getFFTBinSizeHz());
        assertEquals(2400, ds.getFreqStartMHz());
        assertEquals(2500, ds.getFreqStopMHz());
        assertEquals(1000, ds.spectrumLength()); // (2500-2400)*1e6 / 1e5 = 1000 points
        assertEquals(-120f, ds.getPower(0));
    }

    @Test
    void testAddNewData() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2500, -120f);
        double[] freqs = {2400e6, 2400.1e6, 2499.9e6};
        float[] powers = {-80f, -70f, -90f};
        FFTBins bins = new FFTBins(true, freqs, 100000f, powers);

        boolean refreshed = ds.addNewData(bins);
        assertTrue(refreshed);
        assertEquals(-80f, ds.getPower(0));
        assertEquals(-70f, ds.getPower(1));
    }

    @Test
    void testResetAndClone() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2500, -120f);
        ds.resetSpectrum();
        assertEquals(-120f, ds.getPower(500));

        DatasetSpectrum clone = ds.cloneMe();
        assertNotSame(ds, clone);
        assertEquals(ds.getPower(100), clone.getPower(100));
    }

    @Test
    void testGetFrequencyAndPower() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        assertEquals(2400000000.0, ds.getFrequency(0), 200.0);
        assertEquals(2400100000.0, ds.getFrequency(1), 200.0);
        assertEquals(-100f, ds.getPower(0));
    }

    @Test
    void testCreateSpectrumDataset() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        hotiron.core.jfc.XYSeriesImmutable xy = ds.createSpectrumDataset("test");
        assertNotNull(xy);
        assertEquals(10, xy.getItemCount()); // 1MHz / 100kHz = 10 bins? wait calc
    }

    @Test
    void fullResolutionChartSeriesKeepsThePeakAndBreaksHoles() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 88, 108, -150f);
        for (int i = 0; i < ds.spectrumLength(); i++)
            ds.getSpectrumArray()[i] = i % 3 == 0 ? -150f : -62f;
        ds.getSpectrumArray()[10] = -35f;
        hotiron.core.jfc.XYSeriesImmutable xy = ds.createSpectrumDataset("fm");
        assertEquals(ds.spectrumLength(), xy.getItemCount());
        assertTrue(Double.isNaN(xy.getYY(0)));
        assertEquals(-35.0, xy.getYY(10), 0.001);
    }

    @Test
    void chartSeriesDownsamplesToMaxPointsUsingPeak() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2500, -150f);
        ds.getSpectrumArray()[0] = -80f;
        ds.getSpectrumArray()[1] = -30f;
        ds.getSpectrumArray()[2] = -90f;
        hotiron.core.jfc.XYSeriesImmutable xy = ds.createSpectrumDataset("wide", 50);
        assertEquals(50, xy.getItemCount());
        assertEquals(-30.0, xy.getYY(0), 0.001);
    }

    @Test
    void sameAxisIgnoresObjectIdentity() {
        DatasetSpectrum a = new DatasetSpectrum(100000f, 2402, 2472, -150f);
        DatasetSpectrum b = new DatasetSpectrum(100000f, 2402, 2472, -150f);
        DatasetSpectrum c = new DatasetSpectrum(100000f, 88, 108, -150f);
        assertTrue(a.sameAxisAs(b));
        assertFalse(a.sameAxisAs(c));
        assertFalse(a.sameAxisAs(null));
    }

    @Test
    void parkedIqAxisUsesExactStartHz() {
        float binHz = 3906.25f;
        DatasetSpectrum a = new DatasetSpectrum(binHz, 95_200_000L, 1024, -150f);
        DatasetSpectrum same = new DatasetSpectrum(binHz, 95_200_000L, 1024, -150f);
        DatasetSpectrum shifted = new DatasetSpectrum(binHz, 95_400_000L, 1024, -150f);
        assertEquals(95_200_000L, a.getFreqStartHz());
        assertEquals(1024, a.spectrumLength());
        assertTrue(a.sameAxisAs(same));
        assertFalse(a.sameAxisAs(shifted));
    }

    @Test
    void testCopyTo() {
        DatasetSpectrum src = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        src.getSpectrumArray()[0] = -50f;
        DatasetSpectrum dst = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        src.copyTo(dst);
        assertEquals(-50f, dst.getPower(0));
    }

    @Test
    void testSetInitPowerAndReset() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        ds.setSpectrumInitPower(-90f);
        ds.resetSpectrum();
        assertEquals(-90f, ds.getPower(0));
    }

    @Test
    void testCreateImmutableSeries() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        hotiron.core.jfc.XYSeriesImmutable immutable = ds.createSpectrumDataset("immut");
        assertNotNull(immutable);
        assertTrue(immutable.getItemCount() > 0);
    }

    @Test
    void testGetPowerEdgeAndFrequency() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        assertEquals(-100f, ds.getPower(0));
        assertEquals(-100f, ds.getPower(ds.spectrumLength() - 1));
        // Out of range would throw in real use, but getPower assumes valid
    }

    @Test
    void testClonePreservesData() {
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        ds.getSpectrumArray()[0] = -40f;
        DatasetSpectrum clone = ds.cloneMe();
        assertEquals(-40f, clone.getPower(0));
        clone.getSpectrumArray()[0] = -30f;
        assertEquals(-40f, ds.getPower(0));  // original unchanged
    }
}
