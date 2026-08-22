package hotiron.core;

import java.util.ArrayList;

/**
 * Accumulates per-stage draw/filter timings for the debug overlay.
 */
public class RuntimePerformanceWatch {
	public static class PerformanceEntry {
		public final String name;
		public long nanosSum;
		public int count;

		public PerformanceEntry(String name) {
			this.name = name;
		}

		public void addDrawingTime(long nanos) {
			nanosSum += nanos;
			count++;
		}

		public void reset() {
			count = 0;
			nanosSum = 0;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public int hwFullSpectrumRefreshes = 0;
	public volatile long lastStatisticsRefreshed = System.currentTimeMillis();
	public final PerformanceEntry persisentDisplay = new PerformanceEntry("Pers.disp");
	public final PerformanceEntry waterfallUpdate = new PerformanceEntry("Wtrfall.upd");
	public final PerformanceEntry waterfallDraw = new PerformanceEntry("Wtrfll.drw");
	public final PerformanceEntry chartDrawing = new PerformanceEntry("Spectr.chart");
	public final PerformanceEntry spurFilter = new PerformanceEntry("Spur.fil");

	private final ArrayList<PerformanceEntry> entries = new ArrayList<PerformanceEntry>();

	public RuntimePerformanceWatch() {
		entries.add(persisentDisplay);
		entries.add(waterfallUpdate);
		entries.add(waterfallDraw);
		entries.add(chartDrawing);
		entries.add(spurFilter);
	}

	public synchronized String generateStatistics() {
		long timeElapsed = System.currentTimeMillis() - lastStatisticsRefreshed;
		if (timeElapsed <= 0)
			timeElapsed = 1;
		StringBuilder b = new StringBuilder();
		long sumNanos = 0;
		for (PerformanceEntry entry : entries) {
			sumNanos += entry.nanosSum;
			float callsPerSec = entry.count / (timeElapsed / 1000f);
			b.append(entry.name).append(String.format(" %3dms (%5.1f calls/s) \n", entry.nanosSum / 1000000, callsPerSec));
		}
		b.append(String.format("Total: %4dms draw time/s: ", sumNanos / 1000000));
		return b.toString();
	}

	public synchronized void reset() {
		hwFullSpectrumRefreshes = 0;
		for (PerformanceEntry dataDrawingEntry : entries) {
			dataDrawingEntry.reset();
		}
		lastStatisticsRefreshed = System.currentTimeMillis();
	}
}
