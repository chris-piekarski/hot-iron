package hotiron.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Stable BLE sniff table: one row per MAC, append on first seen,
 * update values in place, drop after {@link #HOLD_MS} quiet.
 */
public final class BleMacTracker
{
	public static final long HOLD_MS = 12_000L;

	private final LinkedHashMap<String, Slot> byAddr = new LinkedHashMap<>();
	private List<BleMacRow> last = List.of();

	public synchronized void reset()
	{
		byAddr.clear();
		last = List.of();
	}

	public synchronized List<BleMacRow> rows()
	{
		return last;
	}

	public synchronized BleMacRow get(String address)
	{
		if (address == null || address.isBlank())
			return null;
		for (BleMacRow r : last)
		{
			if (address.equals(r.address))
				return r;
		}
		return null;
	}

	public synchronized boolean ingest(BleFrame frame, long nowMs)
	{
		if (frame == null)
			return expire(nowMs);
		String addr = frame.address == null || frame.address.isBlank() ? "" : frame.address;
		if (addr.isEmpty())
			return expire(nowMs);
		Slot s = byAddr.get(addr);
		if (s == null)
		{
			s = new Slot();
			byAddr.put(addr, s);
		}
		s.address = addr;
		s.name = frame.name;
		BleAdv adv = BleAdv.parseHex(frame.hex);
		if (!adv.localName.isEmpty())
			s.localName = adv.localName;
		if (!adv.service.isEmpty())
			s.service = adv.service;
		s.channel = frame.channel;
		s.rssiDbm = frame.rssiDbm;
		s.hits++;
		s.lastMs = frame.timestampMs > 0 ? frame.timestampMs : nowMs;
		s.advertising = frame.advertising;
		expire(nowMs);
		return true;
	}

	public synchronized boolean expire(long nowMs)
	{
		boolean gone = false;
		for (Iterator<Slot> it = byAddr.values().iterator(); it.hasNext();)
		{
			Slot s = it.next();
			if (nowMs - s.lastMs > HOLD_MS)
			{
				it.remove();
				gone = true;
			}
		}
		last = snapshot();
		return gone;
	}

	private List<BleMacRow> snapshot()
	{
		List<BleMacRow> out = new ArrayList<>(byAddr.size());
		for (Slot s : byAddr.values())
			out.add(new BleMacRow(s.address, s.name, s.channel, s.rssiDbm, s.hits, s.lastMs, s.advertising, s.localName,
					s.service));
		return List.copyOf(out);
	}

	private static final class Slot
	{
		String address = "";
		String name = "pdu";
		String localName = "";
		String service = "";
		int channel;
		int rssiDbm;
		int hits;
		long lastMs;
		boolean advertising;
	}
}
