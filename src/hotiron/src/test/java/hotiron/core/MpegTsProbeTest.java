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

	@Test
	void tvctExtractsVirtualChannelCallsignAndSelectsPrimaryDtvProgram() {
		byte[] pat = psiPacket(0, 0, patTwoProgramsSection(3, 0x30, 1, 0x40));
		byte[] tvct = psiPacket(MpegTsProbe.PSIP_PID, 0, tvctSection("KATV", 7, 1, 1, 2, "COMET", 7, 2, 3, 2));
		byte[] pmtMain = psiPacket(0x40, 0, pmtSection(0x41, 0x44));
		MpegTsProbe probe = new MpegTsProbe();
		probe.accept(concat(pat, tvct, pmtMain), 188 * 3);
		MpegTsProbe.Snapshot s = probe.snapshot();
		assertEquals("7.1", s.virtualChannel);
		assertEquals("KATV", s.callSign);
		assertEquals("7.1 KATV", s.stationLabel());
		assertEquals(2, s.channels.size());
		assertEquals("7.1 KATV", s.channels.get(0).label());
		assertEquals("7.2 COMET", s.channels.get(1).label());
		assertEquals(0x40, s.pmtPid, "TVCT 7.1 maps to program 1 on PMT 0x40");
		assertEquals(0x41, s.videoPid);
		assertEquals(0x44, s.audioPid);
	}

	static byte[] patTwoProgramsSection(int progA, int pmtA, int progB, int pmtB) {
		byte[] body = new byte[] { 0x00, (byte) 0xb0, 0x11, 0x00, 0x01, (byte) 0xc1, 0x00, 0x00,
				(byte) (progA >> 8), (byte) progA, (byte) (0xe0 | ((pmtA >> 8) & 0x1f)), (byte) pmtA,
				(byte) (progB >> 8), (byte) progB, (byte) (0xe0 | ((pmtB >> 8) & 0x1f)), (byte) pmtB };
		return withCrc(body);
	}

	static byte[] tvctSection(String name1, int maj1, int min1, int prog1, int stype1,
			String name2, int maj2, int min2, int prog2, int stype2) {
		byte[] ch1 = tvctChannelEntry(name1, maj1, min1, prog1, stype1);
		byte[] ch2 = tvctChannelEntry(name2, maj2, min2, prog2, stype2);
		int sectionLen = 7 + ch1.length + ch2.length + 2 + 4;
		byte[] body = new byte[3 + sectionLen - 4];
		body[0] = (byte) MpegTsProbe.TABLE_TVCT;
		body[1] = (byte) (0xf0 | ((sectionLen >> 8) & 0x0f));
		body[2] = (byte) sectionLen;
		body[3] = 0x00;
		body[4] = 0x01;
		body[5] = (byte) 0xc1;
		body[6] = 0x00;
		body[7] = 0x00;
		body[8] = 0x00; // protocol_version
		body[9] = 0x02; // num_channels_in_section
		System.arraycopy(ch1, 0, body, 10, ch1.length);
		System.arraycopy(ch2, 0, body, 10 + ch1.length, ch2.length);
		body[10 + ch1.length + ch2.length] = (byte) 0xfc;
		body[10 + ch1.length + ch2.length + 1] = 0x00; // additional_descriptors_length = 0
		return withCrc(body);
	}

	static byte[] tvctChannelEntry(String name, int major, int minor, int prog, int stype) {
		byte[] c = new byte[32];
		for (int k = 0; k < 7 && k < name.length(); k++) {
			char ch = name.charAt(k);
			c[2 * k] = (byte) (ch >> 8);
			c[2 * k + 1] = (byte) ch;
		}
		c[14] = (byte) (0xf0 | ((major >> 6) & 0x0f));
		c[15] = (byte) (((major & 0x3f) << 2) | ((minor >> 8) & 0x03));
		c[16] = (byte) (minor & 0xff);
		c[17] = 0x04; // 8VSB modulation_mode
		c[24] = (byte) (prog >> 8);
		c[25] = (byte) prog;
		c[27] = (byte) (stype & 0x3f);
		c[30] = (byte) 0xfc;
		c[31] = 0x00; // descriptors_length = 0
		return c;
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
