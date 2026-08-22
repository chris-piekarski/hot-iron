package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

class FrequencyAllocationsTest {

    @Test
    void testLoadsBuiltinTables() {
        FrequencyAllocations allocs = new FrequencyAllocations();
        HashMap<String, FrequencyAllocationTable> tables = allocs.getTable();

        assertTrue(tables.size() >= 2, "Should have at least Europe and USA tables");
        assertTrue(tables.containsKey("Europe"));
        assertTrue(tables.containsKey("USA"));

        FrequencyAllocationTable europe = tables.get("Europe");
        assertNotNull(europe);
        assertTrue(europe.getFrequencyBands(0L, Long.MAX_VALUE).size() > 10);
        assertNotNull(europe.lookupBand(100000000L));
    }
}
