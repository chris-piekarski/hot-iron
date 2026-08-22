package hotiron.core.jfc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class XYSeriesCollectionImmutableTest {

    @Test
    void testDelegatesToImmutable() {
        float[] x = {2400f};
        float[] y = {-80f};
        XYSeriesImmutable s = new XYSeriesImmutable("s", x, y);
        XYSeriesCollectionImmutable coll = new XYSeriesCollectionImmutable();
        coll.addSeries(s);

        assertEquals(2400.0, coll.getXValue(0, 0), 0.001);
        assertEquals(-80.0, coll.getYValue(0, 0), 0.001);
    }
}
