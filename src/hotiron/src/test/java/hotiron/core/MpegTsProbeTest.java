package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class MpegTsProbeTest {

	@Test
	void patPmtAndVideoPesAreCounted() {
		byte[] pat = psiPacket(0, 0, patSection(0x30));
		byte[] pmt = psiPacket(0x30, 0, pmtSection(0x31, 0x34));
		byte[] video = pesPacket(0x31, 0);
		byte[] audio = pesPacket(0x34, 0);
		byte[] ts = concat(pat, pmt, video, video, audio);
		MpegTsProbe probe = new MpegTsProbe();
		probe.accept(ts, ts.length);
		MpegTsProbe.Snapshot s = probe.snapshot();
		assertEquals(5, s.packets);
		assertEquals(0, s.syncErrors);
		assertEquals(1, s.patPackets);
		assertEquals(0x30, s.pmtPid);
		assertEquals(1, s.pmtPackets);
		assertEquals(0x31, s.videoPid);
		assertEquals(0x02, s.videoStreamType);
		assertEquals(2, s.videoPackets);
		assertEquals(2, s.videoPesStarts);
		assertEquals(0x34, s.audioPid);
		assertEquals(0x81, s.audioStreamType);
		assertEquals(1, s.audioPackets);
	}

	@Test
	void corruptCrcDoesNotPublishPmtPid() {
		byte[] section = patSection(0x30);
		section[8] ^= 1;
		byte[] ts = psiPacket(0, 0, section);
		MpegTsProbe probe = new MpegTsProbe();
		probe.accept(ts, ts.length);
		assertEquals(-1, probe.snapshot().pmtPid);
		assertEquals(1, probe.snapshot().patPackets);
	}

	@Test
	void syncErrorsSkipThePacket() {
		byte[] ts = psiPacket(0, 0, patSection(0x30));
		ts[0] = 0x00;
		MpegTsProbe probe = new MpegTsProbe();
		probe.accept(ts, ts.length);
		assertEquals(1, probe.snapshot().syncErrors);
		assertEquals(-1, probe.snapshot().pmtPid);
	}

	@Test
	void resetClearsPids() {
		byte[] ts = concat(psiPacket(0, 0, patSection(0x30)), psiPacket(0x30, 0, pmtSection(0x31, 0x34)));
		MpegTsProbe probe = new MpegTsProbe();
		probe.accept(ts, ts.length);
		assertEquals(0x31, probe.snapshot().videoPid);
		probe.reset();
		assertEquals(-1, probe.snapshot().videoPid);
		assertEquals(0, probe.snapshot().packets);
	}

	@Test
	void prefersMainEnglishOverVisuallyImpairedSap() {
		byte[] ts = concat(psiPacket(0, 0, patSection(0x30)),
				psiPacket(0x30, 0, pmtSectionViThenMain(0x31, 0x65, 0x34)));
		MpegTsProbe probe = new MpegTsProbe();
		probe.accept(ts, ts.length);
		MpegTsProbe.Snapshot s = probe.snapshot();
		assertEquals(0x31, s.videoPid);
		assertEquals(0x34, s.audioPid, "main eng AC-3, not spa VI 0x65");
		assertEquals(0x81, s.audioStreamType);
	}

	@Test
	void audioScorePrefersMainEnglish() {
		assertTrue(MpegTsProbe.audioScore(0x81, 0, "eng") > MpegTsProbe.audioScore(0x81, 3, "spa"));
		assertTrue(MpegTsProbe.audioScore(0x81, 0, "") > MpegTsProbe.audioScore(0x81, 3, "eng"));
	}

	@Test
	void streamTypeHelpers() {
		assertTrue(MpegTsProbe.isVideo(0x02));
		assertTrue(MpegTsProbe.isVideo(0x1b));
		assertTrue(MpegTsProbe.isAudio(0x81));
		assertFalse(MpegTsProbe.isVideo(0x81));
		assertFalse(MpegTsProbe.isAudio(0x02));
	}

	static byte[] patSection(int pmtPid) {
		byte[] body = new byte[] { 0x00, (byte) 0xb0, 0x0d, 0x00, 0x01, (byte) 0xc1, 0x00, 0x00,
				0x00, 0x01, (byte) (0xe0 | ((pmtPid >> 8) & 0x1f)), (byte) pmtPid };
		return withCrc(body);
	}

	static byte[] pmtSection(int videoPid, int audioPid) {
		byte[] body = new byte[] { 0x02, (byte) 0xb0, 0x17, 0x00, 0x01, (byte) 0xc1, 0x00, 0x00,
				(byte) (0xe0 | ((videoPid >> 8) & 0x1f)), (byte) videoPid, (byte) 0xf0, 0x00, 0x02,
				(byte) (0xe0 | ((videoPid >> 8) & 0x1f)), (byte) videoPid, (byte) 0xf0, 0x00,
				(byte) 0x81, (byte) (0xe0 | ((audioPid >> 8) & 0x1f)), (byte) audioPid, (byte) 0xf0,
				0x00 };
		return withCrc(body);
	}

	/** VI Spanish listed first, then main English — common ATSC mux order. */
	static byte[] pmtSectionViThenMain(int videoPid, int viAudioPid, int mainAudioPid) {
		byte[] es = new byte[] {
				0x02, (byte) (0xe0 | ((videoPid >> 8) & 0x1f)), (byte) videoPid, (byte) 0xf0, 0x00,
				(byte) 0x81, (byte) (0xe0 | ((viAudioPid >> 8) & 0x1f)), (byte) viAudioPid,
				(byte) 0xf0, 0x06, 0x0A, 0x04, 's', 'p', 'a', 0x03,
				(byte) 0x81, (byte) (0xe0 | ((mainAudioPid >> 8) & 0x1f)), (byte) mainAudioPid,
				(byte) 0xf0, 0x06, 0x0A, 0x04, 'e', 'n', 'g', 0x00 };
		int sectionLen = 9 + 2 + es.length + 4;
		byte[] body = new byte[3 + sectionLen];
		body[0] = 0x02;
		body[1] = (byte) (0xb0 | ((sectionLen >> 8) & 0x0f));
		body[2] = (byte) sectionLen;
		body[3] = 0x00;
		body[4] = 0x01;
		body[5] = (byte) 0xc1;
		body[6] = 0x00;
		body[7] = 0x00;
		body[8] = (byte) (0xe0 | ((videoPid >> 8) & 0x1f));
		body[9] = (byte) videoPid;
		body[10] = (byte) 0xf0;
		body[11] = 0x00;
		System.arraycopy(es, 0, body, 12, es.length);
		return withCrc(Arrays.copyOf(body, 12 + es.length));
	}

	static byte[] withCrc(byte[] body) {
		byte[] s = Arrays.copyOf(body, body.length + 4);
		int crc = TvWatchEngine.mpegCrc32(s, 0, body.length);
		for (int i = 0; i < 4; i++)
			s[body.length + i] = (byte) (crc >>> (24 - 8 * i));
		return s;
	}

	static byte[] psiPacket(int pid, int continuity, byte[] section) {
		byte[] ts = new byte[188];
		ts[0] = 0x47;
		ts[1] = (byte) (0x40 | ((pid >> 8) & 0x1f));
		ts[2] = (byte) pid;
		ts[3] = (byte) (0x10 | (continuity & 0x0f));
		ts[4] = 0x00;
		System.arraycopy(section, 0, ts, 5, section.length);
		return ts;
	}

	static byte[] pesPacket(int pid, int continuity) {
		byte[] ts = new byte[188];
		ts[0] = 0x47;
		ts[1] = (byte) (0x40 | ((pid >> 8) & 0x1f));
		ts[2] = (byte) pid;
		ts[3] = (byte) (0x10 | (continuity & 0x0f));
		ts[4] = 0x00;
		ts[5] = 0x00;
		ts[6] = 0x01;
		ts[7] = (byte) 0xe0;
		return ts;
	}

	static byte[] concat(byte[]... parts) {
		int n = 0;
		for (byte[] p : parts)
			n += p.length;
		byte[] out = new byte[n];
		int o = 0;
		for (byte[] p : parts)
		{
			System.arraycopy(p, 0, out, o, p.length);
			o += p.length;
		}
		return out;
	}
}
