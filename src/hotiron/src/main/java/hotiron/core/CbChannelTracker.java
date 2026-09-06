package hotiron.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Temporal CB occupancy. A one-sweep flash is not labeled.
 */
public final class CbChannelTracker
{
	public static final float ATTACK_PER_SEC = 1.2f;
	public static final float DECAY_PER_SEC = 0.35f;
	public static final float SHOW_AT = 0.50f;
	public static final float DROP_AT = 0.05f;
	static final double MAX_STEP_SEC = 0.25;

	private final LongSupplier clockMs;
	private final Map<Integer, Track> tracks = new LinkedHashMap<>();

	public CbChannelTracker()
	{
		this(System::currentTimeMillis);
	}

	public CbChannelTracker(LongSupplier clockMs)
	{
		this.clockMs = clockMs == null ? System::currentTimeMillis : clockMs;
	}

	public synchronized void reset()
	{
		tracks.clear();
	}

	public synchronized List<CbChannelHit> update(DatasetSpectrum ds, double startMHz, double endMHz, float marginDb)
	{
		return update(ds, startMHz, endMHz, marginDb, clockMs.getAsLong());
	}

	public synchronized List<CbChannelHit> update(DatasetSpectrum ds, double startMHz, double endMHz, float marginDb,
			long nowMs)
	{
		if (!CbChannelPlan.viewIsCb(startMHz, endMHz))
		{
			tracks.clear();
			return List.of();
		}
		List<CbChannelHit> raw = CbChannelPlan.detect(ds, startMHz, endMHz, marginDb);
		Set<Integer> seen = new HashSet<>();
		final double dt = lastMs > 0
				? Math.min(MAX_STEP_SEC, Math.max(0, (nowMs - lastMs) / 1000.0))
				: 0.05;
		lastMs = nowMs;
		for (CbChannelHit hit : raw)
		{
			if (hit == null || hit.channel == null)
				continue;
			int id = hit.channel.number;
			seen.add(id);
			Track t = tracks.get(id);
			if (t == null)
			{
				t = new Track();
				t.channel = hit.channel;
				tracks.put(id, t);
			}
			t.powerDbm = hit.powerDbm;
			t.confidence = clamp01(t.confidence + (float) (dt * ATTACK_PER_SEC));
		}
		List<CbChannelHit> out = new ArrayList<>();
		tracks.entrySet().removeIf(e -> {
			Track t = e.getValue();
			if (!seen.contains(e.getKey()))
				t.confidence = clamp01(t.confidence - (float) (dt * DECAY_PER_SEC));
			if (t.confidence < DROP_AT)
				return true;
			if (t.confidence >= SHOW_AT)
				out.add(new CbChannelHit(t.channel, t.powerDbm, t.confidence));
			return false;
		});
		out.sort((a, b) -> Integer.compare(a.channel.number, b.channel.number));
		return List.copyOf(out);
	}

	private long lastMs;

	private static float clamp01(float v)
	{
		if (v < 0f)
			return 0f;
		if (v > 1f)
			return 1f;
		return v;
	}

	private static final class Track
	{
		CbChannel channel;
		float powerDbm;
		float confidence;
	}
}
