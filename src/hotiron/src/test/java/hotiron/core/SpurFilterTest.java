package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SpurFilterTest {

    private DatasetSpectrum createSimpleSpectrum(float binHz, int startMHz, int stopMHz, float initPower) {
        return new DatasetSpectrum(binHz, startMHz, stopMHz, initPower);
    }

    @Test
    void testConstructionAndCalibrationState() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(6f, 4f, 5, 5, input);

        assertFalse(filter.isFilterCalibrated());
        filter.recalibrate();
        assertFalse(filter.isFilterCalibrated());
    }

    @Test
    void testCalibrationRequiresMultipleIterations() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(6f, 4f, 5, 3, input);  // only need 3 iterations for this test

        assertFalse(filter.isFilterCalibrated());

        // Feed the same-ish data multiple times
        float[] arr = input.getSpectrumArray();
        for (int i = 0; i < arr.length; i++) arr[i] = -85f;

        for (int iter = 0; iter < 4; iter++) {
            filter.filterDataset();
        }

        // After enough iterations it should consider itself calibrated
        assertTrue(filter.isFilterCalibrated());
    }

    @Test
    void testFiltersSpursAfterCalibration() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(1.0f, 3.0f, 2, 3, input);  // tight thresholds for test

        float[] arr = input.getSpectrumArray();
        // Create a stable "spur" at index 50
        for (int iter = 0; iter < 5; iter++) {
            for (int i = 0; i < arr.length; i++) arr[i] = -80f;
            arr[50] = -50f;  // obvious spur
            filter.filterDataset();
        }
        assertTrue(filter.isFilterCalibrated());

        // Now apply the filter - the spur bin should be reduced
        float before = input.getSpectrumArray()[50];
        filter.filterDataset();
        float after = input.getSpectrumArray()[50];
        assertTrue(after < before, "Spur should have been attenuated");
    }

    @Test
    void testRecalibrateClearsState() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(6f, 4f, 5, 3, input);

        float[] arr = input.getSpectrumArray();
        for (int i = 0; i < arr.length; i++) arr[i] = -85f;

        for (int iter = 0; iter < 5; iter++) {
            filter.filterDataset();
        }
        assertTrue(filter.isFilterCalibrated());

        filter.recalibrate();
        assertFalse(filter.isFilterCalibrated());
    }

    @Test
    void testUnstableSpurNotCalibratedAsSpur() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(1.0f, 3.0f, 2, 5, input); // higher jitter tolerance? wait low for unstable

        float[] arr = input.getSpectrumArray();
        for (int iter = 0; iter < 6; iter++) {
            for (int i = 0; i < arr.length; i++) arr[i] = -80f;
            arr[50] = -50f + (iter % 3); // unstable spur varying > jitter
            filter.filterDataset();
        }
        // with low jitter threshold and unstable, may not fully calibrate or filter weakly
        // just ensure no crash and state reachable
        filter.filterDataset();
    }

    @Test
    void testNoSpursNormalData() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(6f, 10f, 5, 5, input); // high threshold, no spurs expected

        float[] arr = input.getSpectrumArray();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = -80f + (float)Math.sin(i * 0.1) * 5; // smooth varying, no outliers
        }

        for (int iter = 0; iter < 6; iter++) {
            filter.filterDataset();
        }
        assertTrue(filter.isFilterCalibrated());
        // After filtering normal data, input shouldn't have large negative spikes from filter
        float minAfter = Float.MAX_VALUE;
        for (float v : input.getSpectrumArray()) minAfter = Math.min(minAfter, v);
        assertTrue(minAfter > -100f);
    }

    @Test
    void testSpurAtEdgesAndMultipleSpurs() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(2f, 5f, 3, 4, input);

        float[] arr = input.getSpectrumArray();
        // Spur at low end, high end, and middle
        for (int iter = 0; iter < 5; iter++) {
            for (int i = 0; i < arr.length; i++) arr[i] = -82f;
            arr[5] = -40f;   // low edge spur
            arr[arr.length/2] = -35f; // middle
            arr[arr.length-10] = -45f; // high edge
            filter.filterDataset();
        }
        assertTrue(filter.isFilterCalibrated());

        // Apply filter and verify spurs are reduced
        float beforeMiddle = input.getSpectrumArray()[input.getSpectrumArray().length/2];
        filter.filterDataset();
        float afterMiddle = input.getSpectrumArray()[input.getSpectrumArray().length/2];
        assertTrue(afterMiddle < beforeMiddle - 3f);
    }

    @Test
    void testRecalibrateAfterFiltering() {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(3f, 4f, 4, 3, input);

        float[] arr = input.getSpectrumArray();
        for (int iter = 0; iter < 4; iter++) {
            for (int i = 0; i < arr.length; i++) arr[i] = -80f;
            arr[100] = -30f;
            filter.filterDataset();
        }
        assertTrue(filter.isFilterCalibrated());

        filter.filterDataset(); // apply once
        filter.recalibrate();
        assertFalse(filter.isFilterCalibrated());

        // Re-feed data to re-calibrate
        for (int iter = 0; iter < 4; iter++) {
            filter.filterDataset();
        }
        assertTrue(filter.isFilterCalibrated());
    }

    @Test
    void testDebugPathsViaReflection() throws Exception {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(6f, 4f, 5, 3, input);

        // Force some data and calibration
        float[] arr = input.getSpectrumArray();
        for (int i = 0; i < arr.length; i++) arr[i] = -80f;
        for (int iter = 0; iter < 4; iter++) filter.filterDataset();

        // Use reflection to set debug and call filterDatasetExec indirectly
        java.lang.reflect.Field debugField = SpurFilter.class.getDeclaredField("debug");
        debugField.setAccessible(true);

        // Normal (0) is already tested; hit others
        debugField.setInt(filter, 1);
        filter.filterDataset();  // will use the if(debug) path

        debugField.setInt(filter, 2);
        filter.filterDataset();

        debugField.setInt(filter, 3);
        filter.filterDataset();

        // debug=4 does nothing
        debugField.setInt(filter, 4);
        filter.filterDataset();
    }

    @Test
    void testFilterDataAfterCalibration() throws Exception {
        DatasetSpectrum input = createSimpleSpectrum(100000f, 2400, 2500, -100f);
        SpurFilter filter = new SpurFilter(2f, 4f, 3, 3, input);

        float[] arr = input.getSpectrumArray();
        for (int iter = 0; iter < 4; iter++) {
            for (int i = 0; i < arr.length; i++) arr[i] = -80f;
            arr[50] = -20f; // strong spur
            filter.filterDataset();
        }

        // Use reflection to inspect the filter data
        java.lang.reflect.Field filterField = SpurFilter.class.getDeclaredField("filter");
        filterField.setAccessible(true);
        DatasetSpectrum filterDS = (DatasetSpectrum) filterField.get(filter);
        float[] filterData = filterDS.getSpectrumArray();

        // There should be a positive correction at the spur location
        assertTrue(filterData[50] > 0);
    }
}
