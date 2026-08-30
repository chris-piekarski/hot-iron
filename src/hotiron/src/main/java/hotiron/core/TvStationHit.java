package hotiron.core;

/**
 * A US TV channel seen as a 6 MHz brick. {@link #grade} is occupancy
 * until Watch or Qualify stamps {@link TvChannelGrade#PICTURE} /
 * {@link TvChannelGrade#NO_LOCK}.
 */
public final class TvStationHit
{
	public final TvChannel channel;
	public final float powerDbm;
	public final float confidence;
	public final TvChannelGrade grade;
	public final String stage;
	public final int frames;
	public final float snrDb;
	public final float pilotExcessDb;

	public TvStationHit(TvChannel channel, float powerDbm)
	{
		this(channel, powerDbm, 1f);
	}

	public TvStationHit(TvChannel channel, float powerDbm, float confidence)
	{
		this(channel, powerDbm, confidence, TvChannelGrade.OCCUPIED, "", 0, Float.NaN, Float.NaN);
	}

	public TvStationHit(TvChannel channel, float powerDbm, float confidence, TvChannelGrade grade,
			String stage, int frames, float snrDb, float pilotExcessDb)
	{
		this.channel = channel;
		this.powerDbm = powerDbm;
		this.confidence = confidence;
		this.grade = grade == null ? TvChannelGrade.OCCUPIED : grade;
		this.stage = stage == null ? "" : stage;
		this.frames = Math.max(0, frames);
		this.snrDb = snrDb;
		this.pilotExcessDb = pilotExcessDb;
	}

	public String label()
	{
		return channel.label();
	}

	public TvStationHit withConfidence(float confidence)
	{
		return new TvStationHit(channel, powerDbm, confidence, grade, stage, frames, snrDb, pilotExcessDb);
	}

	/**
	 * Keep the higher-trust grade; take live power / pilot / confidence.
	 */
	public static TvStationHit merge(TvStationHit remembered, TvStationHit live)
	{
		if (remembered == null)
			return live;
		if (live == null)
			return remembered;
		TvStationHit keep = remembered.grade.trust() >= live.grade.trust() ? remembered : live;
		TvStationHit fresh = remembered.grade.trust() >= live.grade.trust() ? live : remembered;
		float pilot = Float.isFinite(fresh.pilotExcessDb) ? fresh.pilotExcessDb : keep.pilotExcessDb;
		return new TvStationHit(keep.channel, fresh.powerDbm, fresh.confidence, keep.grade, keep.stage,
				keep.frames, keep.snrDb, pilot);
	}

	public TvStationHit stamp(TvChannelGrade grade, String stage, int frames, float snrDb)
	{
		TvChannelGrade g = grade == null ? this.grade : grade;
		if (this.grade == TvChannelGrade.PICTURE && g != TvChannelGrade.PICTURE)
			g = TvChannelGrade.PICTURE;
		return new TvStationHit(channel, powerDbm, confidence, g, stage, frames, snrDb, pilotExcessDb);
	}

	@Override
	public String toString()
	{
		return "ch" + label() + "@" + powerDbm + "dBm/" + confidence + "/" + grade.jsonName();
	}
}
