package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FrequencyBandTest {

    @Test
    void testBasicProperties() {
        FrequencyBand band = new FrequencyBand(2400000000L, 2500000000L, "WiFi 2.4", "ISM / WiFi");
        assertEquals(2400000000L, band.getHzStartIncl());
        assertEquals(2500000000L, band.getHzEndExcl());
        assertEquals(2400.0, band.getMHzStartIncl(), 0.001);
        assertEquals(2500.0, band.getMHzEndExcl(), 0.001);
        assertEquals("WiFi 2.4", band.getName());
        assertEquals("ISM / WiFi", band.getApplications());
        assertEquals("WiFi 2.4", band.toString());
    }

    @Test
    void testCompareTo() {
        FrequencyBand a = new FrequencyBand(1000, 2000, "A", "");
        FrequencyBand b = new FrequencyBand(1500, 2500, "B", "");
        FrequencyBand c = new FrequencyBand(1000, 2000, "C", "");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));
    }
}
