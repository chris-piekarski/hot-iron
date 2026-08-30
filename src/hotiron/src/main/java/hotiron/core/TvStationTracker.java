package hotiron.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Temporal smoother for {@link TvChannelPlan#detectStations}.
 */
public final class TvStationTracker
{
	public static final float ATTACK_PER_SEC = 1.2f;
	public static final float DECAY_PER_SEC = 0.35f;
	public static final float SHOW_AT = 0.50f;
	public static final float DROP_AT = 0.05f;
	static final double MAX_STEP_SEC = 0.25;
	static final double DEFAULT_STEP_SEC = 0.02;

	private final LongSupplier clockMs;
	private final Map<Integer, Track> tracks = new LinkedHashMap<>();

	public TvStationTracker()
	{
		this(System::currentTimeMillis);
	}

	public TvStationTracker(LongSupplier clockMs)
	{
		this.clockMs = clockMs == null ? System::currentTimeMillis : clockMs;
	}

	public synchronized void reset()
	{
		tracks.clear();
	}

	public synchronized List<TvStationHit> update(DatasetSpectrum ds, double startMHz, double endMHz)
	{
		return update(ds, startMHz, endMHz, clockMs.getAsLong());
	}

	public synchronized List<TvStationHit> update(DatasetSpectrum ds, double startMHz, double endMHz, long nowMs)
	{
		List<TvStationHit> raw = TvChannelPlan.detectStations(ds, startMHz, endMHz);
		Set<Integer> seen = new HashSet<>();
		for (TvStationHit hit : raw)
		{
			if (hit == null || hit.channel == null)
				continue;
			int id = hit.channel.fccChannel;
			seen.add(id);
			Track t = tracks.get(id);
			if (t == null)
			{
				t = new Track();
				t.channel = hit.channel;
				tracks.put(id, t);
			}
			double dt = stepSec(t.lastUpdateMs, nowMs);
			t.channel = hit.channel;
			t.powerDbm = hit.powerDbm;
			t.grade = hit.grade;
			t.pilotExcessDb = hit.pilotExcessDb;
			t.confidence = clamp01(t.confidence + (float) (dt * ATTACK_PER_SEC));
			t.lastUpdateMs = nowMs;
		}

		Iterator<Map.Entry<Integer, Track>> it = tracks.entrySet().iterator();
		while (it.hasNext())
		{
			Track t = it.next().getValue();
			if (seen.contains(t.channel.fccChannel))
				continue;
			double dt = stepSec(t.lastUpdateMs, nowMs);
			t.confidence = clamp01(t.confidence - (float) (dt * DECAY_PER_SEC));
			t.lastUpdateMs = nowMs;
			if (t.confidence < DROP_AT)
				it.remove();
		}

		List<TvStationHit> out = new ArrayList<>();
		for (Track t : tracks.values())
		{
			if (t.confidence >= SHOW_AT)
				out.add(new TvStationHit(t.channel, t.powerDbm, t.confidence,
						t.grade == null ? TvChannelGrade.OCCUPIED : t.grade, "", 0, Float.NaN,
						t.pilotExcessDb));
		}
		out.sort((a, b) -> Integer.compare(a.channel.fccChannel, b.channel.fccChannel));
		return List.copyOf(out);
	}

	private static double stepSec(long lastMs, long nowMs)
	{
		if (lastMs <= 0)
			return DEFAULT_STEP_SEC;
		double dt = (nowMs - lastMs) / 1000.0;
		if (dt < 0)
			return 0;
		return Math.min(dt, MAX_STEP_SEC);
	}

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
		TvChannel channel;
		float powerDbm;
		float confidence;
		TvChannelGrade grade = TvChannelGrade.OCCUPIED;
		float pilotExcessDb = Float.NaN;
		long lastUpdateMs;
	}
}
