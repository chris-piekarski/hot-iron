package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class FrequencyAllocationTableTest {

    private FrequencyAllocationTable createSmallTable() {
        ArrayList<FrequencyBand> bands = new ArrayList<>();
        bands.add(new FrequencyBand(2400000000L, 2500000000L, "WiFi 2.4", "ISM"));
        bands.add(new FrequencyBand(5000000000L, 6000000000L, "WiFi 5", "ISM"));
        return new FrequencyAllocationTable("Test", bands);
    }

    @Test
    void testLookupBand() {
        FrequencyAllocationTable table = createSmallTable();
        FrequencyBand b = table.lookupBand(2450000000L);
        assertNotNull(b);
        assertEquals("WiFi 2.4", b.getName());
    }

    @Test
    void testGetFrequencyBandsInRange() {
        FrequencyAllocationTable table = createSmallTable();
        ArrayList<FrequencyBand> inRange = table.getFrequencyBands(2300000000L, 2600000000L);
        assertEquals(1, inRange.size());
        assertEquals("WiFi 2.4", inRange.get(0).getName());
    }

    @Test
    void testToString() {
        FrequencyAllocationTable table = createSmallTable();
        assertEquals("Test", table.toString());
    }

    @Test
    void testDrawAllocationTableExercisesGraphicsPath() {
        FrequencyAllocationTable table = createSmallTable();
        java.awt.image.BufferedImage img = table.drawAllocationTable(400, 30, 0.8f, 2300000000L, 2600000000L, java.awt.Color.WHITE, java.awt.Color.BLACK);
        assertNotNull(img);
        assertEquals(400, img.getWidth());
    }

    @Test
    void testGetFrequencyBandsEdgeCases() {
        FrequencyAllocationTable table = createSmallTable();
        // No overlap
        ArrayList<FrequencyBand> none = table.getFrequencyBands(1000000000L, 2000000000L);
        assertTrue(none.isEmpty());

        // Exact match on one band
        ArrayList<FrequencyBand> exact = table.getFrequencyBands(2400000000L, 2500000000L);
        assertEquals(1, exact.size());

        // Range covering both
        ArrayList<FrequencyBand> both = table.getFrequencyBands(2000000000L, 7000000000L);
        assertEquals(2, both.size());
    }

    @Test
    void testDrawWithDifferentAlphaAndColors() {
        FrequencyAllocationTable table = createSmallTable();
        java.awt.image.BufferedImage img1 = table.drawAllocationTable(200, 20, 0.5f, 2300000000L, 2600000000L, java.awt.Color.RED, java.awt.Color.BLUE);
        java.awt.image.BufferedImage img2 = table.drawAllocationTable(200, 20, 1.0f, 2300000000L, 2600000000L, java.awt.Color.GREEN, java.awt.Color.YELLOW);
        assertNotNull(img1);
        assertNotNull(img2);
        assertEquals(200, img1.getWidth());
    }
}
