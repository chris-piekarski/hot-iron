package hotiron.core;

/**
 * How much we know about a US TV brick. Occupancy is a sweep mean.
 * {@link #PICTURE} is the only grade that means MPEG-2 frames decoded
 * on this HackRF.
 */
public enum TvChannelGrade
{
	/** MPEG-2 frames decoded while Watch/Qualify was parked. */
	PICTURE,
	/** Occupied 6 MHz brick plus an ATSC 1.0 pilot (~+310 kHz). */
	ATSC_LIKE,
	/** Occupied 6 MHz brick, no ATSC 1.0 pilot. */
	OCCUPIED,
	/** Watched or qualified; never reached picture. */
	NO_LOCK;

	public boolean watchMemory()
	{
		return this == PICTURE || this == NO_LOCK;
	}

	/** Higher wins when merging a live occupancy hit onto memory. */
	public int trust()
	{
		switch (this)
		{
		case PICTURE:
			return 3;
		case NO_LOCK:
			return 2;
		case ATSC_LIKE:
			return 1;
		default:
			return 0;
		}
	}

	/** Seek order: picture, then ATSC-like, then occupied. */
	public int seekRank()
	{
		switch (this)
		{
		case PICTURE:
			return 0;
		case ATSC_LIKE:
			return 1;
		case OCCUPIED:
			return 2;
		default:
			return 3;
		}
	}

	public String jsonName()
	{
		switch (this)
		{
		case PICTURE:
			return "picture";
		case ATSC_LIKE:
			return "atsc_like";
		case NO_LOCK:
			return "no_lock";
		default:
			return "occupied";
		}
	}

	public String rosterLabel()
	{
		switch (this)
		{
		case PICTURE:
			return "picture";
		case ATSC_LIKE:
			return "ATSC-like";
		case NO_LOCK:
			return "no lock";
		default:
			return "occupied";
		}
	}
}
