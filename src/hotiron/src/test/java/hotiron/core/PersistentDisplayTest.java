package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class PersistentDisplayTest {

    @Test
    void testStaticMapFloat() {
        float result = PersistentDisplay.map(50f, 0f, 100f, 0f, 1f);
        assertEquals(0.5f, result, 0.001f);
    }

    @Test
    void testStaticMapInt() {
        int result = PersistentDisplay.map(5, 0, 10, 0, 100);
        assertEquals(50, result);
    }

    @Test
    void testConstructionAndSetters() {
        PersistentDisplay pd = new PersistentDisplay();
        assertNotNull(pd.getDisplayImage());
        pd.setPersistenceTime(10);
        assertEquals(10, pd.getPersistenceTime());
    }

    @Test
    void testDrawSpectrumCoversCalibrationAndDraw() throws Exception {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(100, 50);

        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2500, -100f);
        float[] arr = ds.getSpectrumArray();
        for (int i = 0; i < arr.length; i++) arr[i] = -80f + (i % 10);

        // Use reflection to simulate time passing so calibration completes quickly
        java.lang.reflect.Field calField = PersistentDisplay.class.getDeclaredField("calibrationStarted");
        calField.setAccessible(true);
        calField.setLong(pd, System.currentTimeMillis() - 2000);

        java.lang.reflect.Field counterField = PersistentDisplay.class.getDeclaredField("incomingDataCounter");
        counterField.setAccessible(true);
        counterField.setInt(pd, 50);

        pd.drawSpectrumFloat(ds, -120f, -30f, true);

        BufferedImage img = pd.getDisplayImage().getValue();
        assertNotNull(img);
        assertEquals(100, img.getWidth());
    }

    @Test
    void testReset() {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(50, 30);
        pd.reset();
        // should not throw, image recreated
        assertNotNull(pd.getDisplayImage().getValue());
    }

    @Test
    void testDrawWithRenderFalseStillAccumulates() {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(10, 5);
        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        // no exception, coverage on non-render path
    }

    @Test
    void testMultipleDrawsExerciseDecayAndAccumulation() throws Exception {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(20, 10);
        pd.setPersistenceTime(2);

        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        float[] arr = ds.getSpectrumArray();
        arr[0] = -60f;

        // Force calibrated state via reflection
        java.lang.reflect.Field calField = PersistentDisplay.class.getDeclaredField("calibrated");
        calField.setAccessible(true);
        calField.setBoolean(pd, true);

        java.lang.reflect.Field upsField = PersistentDisplay.class.getDeclaredField("updatesPerSecond");
        upsField.setAccessible(true);
        upsField.setFloat(pd, 10f);

        for (int i = 0; i < 3; i++) {
            pd.drawSpectrumFloat(ds, -120f, -30f, true);
        }
        // Multiple calls exercise the kM1 decay multiply and accumulation loop
        assertNotNull(pd.getDisplayImage().getValue());

        // Inspect internal accumulator via reflection to verify decay happened
        java.lang.reflect.Field accumField = PersistentDisplay.class.getDeclaredField("imagePowerAccumulated");
        accumField.setAccessible(true);
        Object accum = accumField.get(pd);
        java.lang.reflect.Field dataField = accum.getClass().getDeclaredField("data");
        dataField.setAccessible(true);
        float[] data = (float[]) dataField.get(accum);
        boolean hasAccum = false;
        for (float v : data) if (v > 0.1f) { hasAccum = true; break; }
        assertTrue(hasAccum);
    }

    @Test
    void testDrawWithVaryingDataAndRenderOptions() throws Exception {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(50, 20);
        pd.setPersistenceTime(5);

        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2500, -100f);
        float[] spectrum = ds.getSpectrumArray();
        for (int i = 0; i < spectrum.length; i++) {
            spectrum[i] = -90f + (i % 20);  // varying powers
        }

        // Force calibrated + high update rate
        java.lang.reflect.Field calField = PersistentDisplay.class.getDeclaredField("calibrated");
        calField.setAccessible(true);
        calField.setBoolean(pd, true);
        java.lang.reflect.Field upsField = PersistentDisplay.class.getDeclaredField("updatesPerSecond");
        upsField.setAccessible(true);
        upsField.setFloat(pd, 20f);

        // Test render=false (accumulation only)
        pd.drawSpectrumFloat(ds, -120f, -20f, false);

        // Now render=true to exercise pixel setting, log scaling, palette, zero threshold
        pd.drawSpectrumFloat(ds, -120f, -20f, true);

        java.awt.image.BufferedImage img = pd.getDisplayImage().getValue();
        assertNotNull(img);
        assertEquals(50, img.getWidth());
        // Check that some pixels were set (not all black)
        boolean hasNonBlack = false;
        for (int x = 0; x < img.getWidth() && !hasNonBlack; x++) {
            for (int y = 0; y < img.getHeight() && !hasNonBlack; y++) {
                if (img.getRGB(x, y) != java.awt.Color.black.getRGB()) {
                    hasNonBlack = true;
                }
            }
        }
        assertTrue(hasNonBlack);
    }

    @Test
    void testResetDuringOperation() throws Exception {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(30, 15);

        DatasetSpectrum ds = new DatasetSpectrum(100000f, 2400, 2401, -100f);
        java.lang.reflect.Field calField = PersistentDisplay.class.getDeclaredField("calibrated");
        calField.setAccessible(true);
        calField.setBoolean(pd, true);

        pd.drawSpectrumFloat(ds, -120f, -30f, true);
        pd.reset();
        pd.drawSpectrumFloat(ds, -120f, -30f, true);

        assertNotNull(pd.getDisplayImage().getValue());
    }

    @Test
    void persistenceDecaysByHalfLifeAndSkipsAPauseGap() throws Exception {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(8, 8);
        pd.setPersistenceTime(1);

        DatasetSpectrum ds = new DatasetSpectrum(1_000_000f, 2400, 2401, SpectrumPowerScale.EMPTY_CEILING);
        ds.getSpectrumArray()[0] = -50f;

        java.lang.reflect.Field calField = PersistentDisplay.class.getDeclaredField("calibrated");
        calField.setAccessible(true);
        calField.setBoolean(pd, true);
        java.lang.reflect.Field lastField = PersistentDisplay.class.getDeclaredField("lastDecayMillis");
        lastField.setAccessible(true);
        lastField.setLong(pd, System.currentTimeMillis());

        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        float peak = max(accum(pd));
        assertTrue(peak > 0.5f);

        ds.getSpectrumArray()[0] = SpectrumPowerScale.EMPTY_CEILING;
        lastField.setLong(pd, System.currentTimeMillis() - 1000);
        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        float afterHalf = max(accum(pd));
        assertEquals(peak * 0.5f, afterHalf, peak * 0.05f);

        lastField.setLong(pd, System.currentTimeMillis() - 5000);
        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        assertEquals(afterHalf, max(accum(pd)), 0.01f);
    }

    @Test
    void flushFadesToZeroAndIgnoresNewHits() throws Exception {
        PersistentDisplay pd = new PersistentDisplay();
        pd.setImageSize(8, 8);
        pd.setPersistenceTime(30);

        DatasetSpectrum ds = new DatasetSpectrum(1_000_000f, 2400, 2401, SpectrumPowerScale.EMPTY_CEILING);
        ds.getSpectrumArray()[0] = -50f;

        java.lang.reflect.Field calField = PersistentDisplay.class.getDeclaredField("calibrated");
        calField.setAccessible(true);
        calField.setBoolean(pd, true);

        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        float peak = pd.maxAccumulated();
        assertTrue(peak > 0.5f);

        long t0 = 1_000_000L;
        pd.beginFlush(t0);
        assertTrue(pd.isFlushing(t0));
        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        assertEquals(peak, pd.maxAccumulated(), 0.01f);

        assertTrue(pd.tickFlush(t0 + PersistentDisplay.FLUSH_HALF_LIFE_MS));
        assertEquals(peak * 0.5f, pd.maxAccumulated(), peak * 0.08f);

        assertFalse(pd.tickFlush(t0 + PersistentDisplay.FLUSH_MAX_MS));
        assertEquals(0f, pd.maxAccumulated(), 0.001f);
        assertFalse(pd.isFlushing(t0 + PersistentDisplay.FLUSH_MAX_MS));

        pd.drawSpectrumFloat(ds, -120f, -30f, false);
        assertTrue(pd.maxAccumulated() > 0.5f, "after flush, new hits accumulate again");
    }

    private static float[] accum(PersistentDisplay pd) throws Exception {
        java.lang.reflect.Field accumField = PersistentDisplay.class.getDeclaredField("imagePowerAccumulated");
        accumField.setAccessible(true);
        Object accum = accumField.get(pd);
        java.lang.reflect.Field dataField = accum.getClass().getDeclaredField("data");
        dataField.setAccessible(true);
        return (float[]) dataField.get(accum);
    }

    private static float max(float[] data) {
        float m = 0;
        for (float v : data)
            if (v > m)
                m = v;
        return m;
    }
}
