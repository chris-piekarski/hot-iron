package hotiron.core;

import java.util.ArrayDeque;
import java.util.Optional;

/**
 * Stack of frequency windows for Grafana-style zoom-out (double-click / wheel).
 */
public final class SpectrumZoomHistory
{
	public static final int MAX_DEPTH = 32;
	private final ArrayDeque<FrequencyRange> stack = new ArrayDeque<>();

	public void push(FrequencyRange range)
	{
		if (range == null)
			return;
		FrequencyRange top = stack.peek();
		if (top != null && top.equals(range))
			return;
		stack.push(range);
		while (stack.size() > MAX_DEPTH)
			stack.removeLast();
	}

	public Optional<FrequencyRange> pop()
	{
		if (stack.isEmpty())
			return Optional.empty();
		return Optional.of(stack.pop());
	}

	public boolean canZoomOut()
	{
		return !stack.isEmpty();
	}

	public void clear()
	{
		stack.clear();
	}

	public int size()
	{
		return stack.size();
	}
}
