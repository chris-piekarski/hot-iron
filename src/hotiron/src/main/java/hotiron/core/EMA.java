package hotiron.core;

public class EMA
{
	public static double calculate(double currentValue, double previousEMA, double order)
	{
		double k = 2 / (order + 1);
		double result = currentValue * k + previousEMA * (1 - k);

		return (result);
	}

	public static double calculateTimeDependent(double currentValue, double previousEMA, long timeDiffFromPreviousValueMillis, double orderInMillis)
	{
		double order = timeDiffFromPreviousValueMillis <= 0 ? 1 : orderInMillis / timeDiffFromPreviousValueMillis;
		double k = 2 / (order + 1);
		double result;

		result = currentValue * k + previousEMA * (1 - k);

		return (result);
	}

	/**
	 * Remaining fraction after {@code dtMillis} of a half-life. {@code 1}
	 * means no decay (zero/invalid interval).
	 */
	public static double decayFactor(long dtMillis, long halfLifeMillis)
	{
		if (dtMillis <= 0 || halfLifeMillis <= 0)
			return 1.0;
		return Math.pow(0.5, dtMillis / (double) halfLifeMillis);
	}

	/**
	 * Move {@code peak} halfway to {@code live} each half-life. A new high
	 * ({@code live > peak}) or a zero half-life snaps to {@code live}.
	 */
	public static float decayToward(float live, float peak, long dtMillis, long halfLifeMillis)
	{
		if (halfLifeMillis <= 0 || dtMillis <= 0 || live > peak || !Float.isFinite(peak))
			return live;
		return (float) (live + (peak - live) * decayFactor(dtMillis, halfLifeMillis));
	}

	private double	ema	= 0;

	private int		order;

	public EMA(int order)
	{
		this.order = order;
	}

	public double addNewValue(double value)
	{
		return (ema = calculate(value, ema, order));
	}

	public double getEma()
	{
		return ema;
	}
}
