package hotiron.core;

import java.nio.charset.StandardCharsets;

/**
 * Lightweight MPEG-TS PSI probe: PAT → PMT PID, PMT → video/audio PIDs,
 * plus packet / PES-start counts on those PIDs.
 */
public final class MpegTsProbe
{
	private final SectionCollector pat = new SectionCollector();
	private final SectionCollector pmt = new SectionCollector();
	private long packets;
	private long syncErrors;
	private long patPackets;
	private int pmtPid = -1;
	private long pmtPackets;
	private int videoPid = -1;
	private int videoStreamType;
	private long videoPackets;
	private long videoPesStarts;
	private int audioPid = -1;
	private int audioStreamType;
	private long audioPackets;
	private long audioPesStarts;

	public synchronized void reset()
	{
		pat.reset();
		pmt.reset();
		packets = 0;
		syncErrors = 0;
		patPackets = 0;
		pmtPid = -1;
		pmtPackets = 0;
		videoPid = -1;
		videoStreamType = 0;
		videoPackets = 0;
		videoPesStarts = 0;
		audioPid = -1;
		audioStreamType = 0;
		audioPackets = 0;
		audioPesStarts = 0;
	}

	public synchronized void accept(byte[] ts, int nbytes)
	{
		if (ts == null)
			return;
		int lim = Math.min(nbytes, ts.length);
		lim -= lim % 188;
		for (int packet = 0; packet + 188 <= lim; packet += 188)
		{
			packets++;
			if (ts[packet] != 0x47)
			{
				syncErrors++;
				continue;
			}
			int pid = ((ts[packet + 1] & 0x1f) << 8) | (ts[packet + 2] & 0xff);
			boolean pusi = (ts[packet + 1] & 0x40) != 0;
			if (pid == 0)
			{
				patPackets++;
				int payload = payloadStart(ts, packet);
				if (payload >= 0 && pat.accept(ts, packet, payload, pusi) && pat.section[0] == 0x00)
					parsePat(pat.section, pat.sectionLen);
			}
			else if (pmtPid >= 0 && pid == pmtPid)
			{
				pmtPackets++;
				int payload = payloadStart(ts, packet);
				if (payload >= 0 && pmt.accept(ts, packet, payload, pusi) && pmt.section[0] == 0x02)
					parsePmt(pmt.section, pmt.sectionLen);
			}
			if (videoPid >= 0 && pid == videoPid)
			{
				videoPackets++;
				if (pusi)
					videoPesStarts++;
			}
			if (audioPid >= 0 && pid == audioPid)
			{
				audioPackets++;
				if (pusi)
					audioPesStarts++;
			}
		}
	}

	public synchronized Snapshot snapshot()
	{
		return new Snapshot(packets, syncErrors, patPackets, pmtPid, pmtPackets, videoPid,
				videoStreamType, videoPackets, videoPesStarts, audioPid, audioStreamType,
				audioPackets, audioPesStarts);
	}

	private void parsePat(byte[] section, int length)
	{
		if (length < 16 || (section[5] & 0x01) == 0)
			return;
		int end = length - 4;
		for (int i = 8; i + 4 <= end; i += 4)
		{
			int program = ((section[i] & 0xff) << 8) | (section[i + 1] & 0xff);
			int pid = ((section[i + 2] & 0x1f) << 8) | (section[i + 3] & 0xff);
			if (program != 0 && pid != 0)
			{
				if (pmtPid != pid)
				{
					pmtPid = pid;
					pmt.reset();
				}
				return;
			}
		}
	}

	private void parsePmt(byte[] section, int length)
	{
		if (length < 16 || (section[5] & 0x01) == 0)
			return;
		int programInfo = ((section[10] & 0x0f) << 8) | (section[11] & 0xff);
		int i = 12 + programInfo;
		int end = length - 4;
		int firstVideo = -1;
		int firstVideoType = 0;
		int bestAudio = -1;
		int bestAudioType = 0;
		int bestScore = Integer.MIN_VALUE;
		while (i + 5 <= end)
		{
			int type = section[i] & 0xff;
			int pid = ((section[i + 1] & 0x1f) << 8) | (section[i + 2] & 0xff);
			int esInfo = ((section[i + 3] & 0x0f) << 8) | (section[i + 4] & 0xff);
			int descStart = i + 5;
			int descEnd = Math.min(end, descStart + esInfo);
			i = descStart + esInfo;
			if (firstVideo < 0 && isVideo(type))
			{
				firstVideo = pid;
				firstVideoType = type;
			}
			if (isAudio(type))
			{
				Iso639 iso = readIso639(section, descStart, descEnd);
				int score = audioScore(type, iso.audioType, iso.lang);
				if (score > bestScore)
				{
					bestScore = score;
					bestAudio = pid;
					bestAudioType = type;
				}
			}
		}
		if (firstVideo >= 0)
		{
			videoPid = firstVideo;
			videoStreamType = firstVideoType;
		}
		if (bestAudio >= 0)
		{
			audioPid = bestAudio;
			audioStreamType = bestAudioType;
		}
	}

	static boolean isVideo(int streamType)
	{
		return streamType == 0x01 || streamType == 0x02 || streamType == 0x10 || streamType == 0x1b
				|| streamType == 0x24 || streamType == 0x42 || streamType == 0x80;
	}

	static boolean isAudio(int streamType)
	{
		return streamType == 0x03 || streamType == 0x04 || streamType == 0x0f || streamType == 0x11
				|| streamType == 0x81 || streamType == 0x87;
	}

	/**
	 * Prefer main AC-3 over visually-impaired / SAP / descriptive tracks
	 * that ATSC muxes often list first.
	 */
	static int audioScore(int streamType, int audioType, String lang)
	{
		int score = (streamType == 0x81 || streamType == 0x87) ? 20 : 10;
		if (audioType < 0 || audioType == 0)
			score += 50;
		else if (audioType == 1)
			score += 30;
		else if (audioType == 2)
			score += 5;
		else if (audioType == 3)
			score += 0;
		else
			score += 20;
		if (lang != null && lang.equalsIgnoreCase("eng"))
			score += 10;
		else if (lang == null || lang.isEmpty())
			score += 5;
		return score;
	}

	static Iso639 readIso639(byte[] section, int start, int end)
	{
		int d = start;
		Iso639 found = Iso639.none();
		while (d + 2 <= end)
		{
			int tag = section[d] & 0xff;
			int len = section[d + 1] & 0xff;
			int next = d + 2 + len;
			if (next > end)
				break;
			if (tag == 0x0A && len >= 4)
			{
				String lang = new String(section, d + 2, 3, StandardCharsets.US_ASCII);
				int audioType = section[d + 5] & 0xff;
				found = new Iso639(lang, audioType);
			}
			d = next;
		}
		return found;
	}

	static final class Iso639
	{
		final String lang;
		final int audioType;

		Iso639(String lang, int audioType)
		{
			this.lang = lang == null ? "" : lang;
			this.audioType = audioType;
		}

		static Iso639 none()
		{
			return new Iso639("", -1);
		}
	}

	static int payloadStart(byte[] ts, int packet)
	{
		int adaptation = (ts[packet + 3] >>> 4) & 0x03;
		if (adaptation == 0 || adaptation == 2)
			return -1;
		int payload = packet + 4;
		if (adaptation == 3)
		{
			if (payload >= packet + 188)
				return -1;
			payload += 1 + (ts[payload] & 0xff);
		}
		if (payload >= packet + 188)
			return -1;
		return payload;
	}

	public static final class Snapshot
	{
		public final long packets;
		public final long syncErrors;
		public final long patPackets;
		public final int pmtPid;
		public final long pmtPackets;
		public final int videoPid;
		public final int videoStreamType;
		public final long videoPackets;
		public final long videoPesStarts;
		public final int audioPid;
		public final int audioStreamType;
		public final long audioPackets;
		public final long audioPesStarts;

		public Snapshot(long packets, long syncErrors, long patPackets, int pmtPid, long pmtPackets,
				int videoPid, int videoStreamType, long videoPackets, long videoPesStarts,
				int audioPid, int audioStreamType, long audioPackets, long audioPesStarts)
		{
			this.packets = packets;
			this.syncErrors = syncErrors;
			this.patPackets = patPackets;
			this.pmtPid = pmtPid;
			this.pmtPackets = pmtPackets;
			this.videoPid = videoPid;
			this.videoStreamType = videoStreamType;
			this.videoPackets = videoPackets;
			this.videoPesStarts = videoPesStarts;
			this.audioPid = audioPid;
			this.audioStreamType = audioStreamType;
			this.audioPackets = audioPackets;
			this.audioPesStarts = audioPesStarts;
		}

		public static Snapshot empty()
		{
			return new Snapshot(0, 0, 0, -1, 0, -1, 0, 0, 0, -1, 0, 0, 0);
		}
	}

	static final class SectionCollector
	{
		final byte[] section = new byte[1024];
		int sectionLen;
		private int filled;
		private int expected = -1;
		private int lastContinuity = -1;
		private boolean collecting;

		void reset()
		{
			filled = 0;
			expected = -1;
			lastContinuity = -1;
			collecting = false;
			sectionLen = 0;
		}

		boolean accept(byte[] ts, int packet, int payload, boolean unitStart)
		{
			int continuity = ts[packet + 3] & 0x0f;
			if (lastContinuity >= 0 && continuity != ((lastContinuity + 1) & 0x0f))
				resetSection();
			lastContinuity = continuity;
			if (unitStart)
			{
				int pointer = ts[payload] & 0xff;
				int sectionStart = payload + 1 + pointer;
				if (collecting && pointer > 0
						&& append(ts, payload + 1, Math.min(sectionStart, packet + 188)))
					return true;
				resetSection();
				if (sectionStart < packet + 188)
				{
					collecting = true;
					return append(ts, sectionStart, packet + 188);
				}
				return false;
			}
			return collecting && append(ts, payload, packet + 188);
		}

		private boolean append(byte[] data, int start, int end)
		{
			while (start < end && collecting)
			{
				if (filled >= section.length)
				{
					resetSection();
					return false;
				}
				section[filled++] = data[start++];
				if (filled == 3)
				{
					int sectionLength = ((section[1] & 0x0f) << 8) | (section[2] & 0xff);
					if ((section[1] & 0x80) == 0 || sectionLength < 9 || sectionLength > 1021)
					{
						resetSection();
						return false;
					}
					expected = 3 + sectionLength;
				}
				if (expected > 0 && filled == expected)
				{
					boolean valid = TvWatchEngine.mpegCrc32(section, 0, expected) == 0;
					sectionLen = expected;
					resetSectionKeep();
					return valid;
				}
			}
			return false;
		}

		private void resetSection()
		{
			filled = 0;
			expected = -1;
			collecting = false;
			sectionLen = 0;
		}

		private void resetSectionKeep()
		{
			filled = 0;
			expected = -1;
			collecting = false;
		}
	}
}
