package hotiron.core.jfc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class XYSeriesImmutableTest {

    @Test
    void testConstructionAndAccess() {
        float[] x = {2400f, 2400.1f};
        float[] y = {-80f, -75f};
        XYSeriesImmutable series = new XYSeriesImmutable("test", x, y);
        assertEquals(2, series.getItemCount());
        assertEquals(2400.0, series.getXX(0), 0.001);
        assertEquals(-75.0, series.getYY(1), 0.001);
    }

    @Test
    void testMismatchedLengthThrows() {
        float[] x = {1f, 2f};
        float[] y = {3f};
        assertThrows(IllegalArgumentException.class, () -> new XYSeriesImmutable("bad", x, y));
    }

    @Test
    void testGetDataItemReturnsNullAsDesigned() {
        XYSeriesImmutable series = new XYSeriesImmutable("t", new float[]{1}, new float[]{2});
        assertNull(series.getDataItem(0));
    }
}
