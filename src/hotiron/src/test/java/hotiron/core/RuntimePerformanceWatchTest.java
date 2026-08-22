package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RuntimePerformanceWatchTest {

    @Test
    void generateStatisticsIncludesEntryNamesAndResets() {
        RuntimePerformanceWatch watch = new RuntimePerformanceWatch();
        watch.lastStatisticsRefreshed = System.currentTimeMillis() + 5000;
        watch.chartDrawing.addDrawingTime(2_000_000L);
        watch.chartDrawing.addDrawingTime(3_000_000L);
        watch.hwFullSpectrumRefreshes = 4;

        String stats = watch.generateStatistics();
        assertTrue(stats.contains("Spectr.chart"));
        assertTrue(stats.contains("Pers.disp"));
        assertTrue(stats.contains("Total:"));

        watch.reset();
        assertEquals(0, watch.hwFullSpectrumRefreshes);
        assertEquals(0, watch.chartDrawing.count);
        assertEquals(0, watch.chartDrawing.nanosSum);
        assertTrue(watch.lastStatisticsRefreshed <= System.currentTimeMillis());
    }

    @Test
    void performanceEntryToStringIsName() {
        RuntimePerformanceWatch.PerformanceEntry entry = new RuntimePerformanceWatch.PerformanceEntry("Wtrfall.upd");
        assertEquals("Wtrfall.upd", entry.toString());
    }
}
