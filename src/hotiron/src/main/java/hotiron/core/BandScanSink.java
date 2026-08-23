package hotiron.core;

/**
 * Advance a {@link BandScanSession} from a full-sweep dataset. USB retune
 * is not dwell: {@link BandScanSession#markLive} only when the dataset axis
 * matches the current window.
 */
public final class BandScanSink
{
	public interface Effects
	{
		void clearWaterfall();

		void retune(FrequencyRange next);

		void finishScan();
	}

	private BandScanSink()
	{
	}

	public static void advance(BandScanSession session, DatasetSpectrum ds, long nowMs, Effects effects)
	{
		if (session == null || !session.active() || effects == null)
			return;
		FrequencyRange window = session.currentWindow();
		if (window != null && ds != null && window.getStartMHz() == ds.getFreqStartMHz()
				&& window.getEndMHz() == ds.getFreqStopMHz())
			session.markLive(nowMs);
		if (session.shouldFinish(nowMs))
		{
			effects.finishScan();
			return;
		}
		session.nextWindowIfDue(nowMs).ifPresent(next -> {
			effects.clearWaterfall();
			effects.retune(next);
		});
	}
}
