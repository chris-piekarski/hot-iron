package hotiron.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import hotiron.core.AnalyzerSettings;
import hotiron.core.DatasetSpectrum;
import hotiron.core.FrequencyRange;
import hotiron.core.MpegTsPlayer;
import hotiron.core.MpegTsProbe;
import hotiron.core.TvWatchDebug;

class SpectrumMcpToolsTest {

	private static SpectrumMcpTools toolsWithSweep() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2402, 2472, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -70f;
		ds.getSpectrumArray()[3] = -25f;
		store.publishSweep(SpectrumSnapshot.fromDataset(ds, 9L, 500, null), 9L);
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getFrequency().setValue(new FrequencyRange(2402, 2472));
		settings.getFFTBinHz().setValue(100000);
		settings.isPowerAutoScale().setValue(true);
		store.publishContext(settings, java.util.List.of(), 10.0);
		return new SpectrumMcpTools(store);
	}

	@Test
	void emptyStoreSummaryIsAnErrorPayload() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String rpc = tools.handleRpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_summary\"}}");
		assertTrue(rpc.contains("no sweep yet"));
		assertTrue(rpc.contains("\"isError\":true"));
	}

	@Test
	void toolsListAndInitializeAreValidJsonRpc() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String init = tools.handleRpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
		assertTrue(init.contains("hotiron"));
		assertTrue(init.contains("2024-11-05"));
		String list = tools.handleRpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
		assertTrue(list.contains("spectrum_snapshot"));
		assertTrue(list.contains("spectrum_summary"));
		assertTrue(list.contains("radio_identity"));
		assertTrue(list.contains("sweep_config"));
		assertTrue(list.contains("fm_stations"));
		assertTrue(list.contains("tv_stations"));
		assertTrue(list.contains("nfc_activity"));
		assertTrue(list.contains("nfc_frames"));
		assertTrue(list.contains("nfc_sniff"));
		assertTrue(list.contains("fm_spectrum"));
		assertTrue(list.contains("spectrum_occupancy"));
		assertTrue(list.contains("spectrum_history"));
		assertTrue(list.contains("spectrum_history_bins"));
		assertTrue(list.contains("tv_watch"));
		assertTrue(list.contains("tv_debug"));
		assertTrue(list.contains("tv_spectrum"));
		assertTrue(list.contains("tv_debug_history"));
		assertTrue(list.contains("fm_listen"));
		assertTrue(list.contains("auto_gain"));
		assertTrue(list.contains("sweep"));
		assertTrue(list.contains("ble_sniff"));
		assertTrue(list.contains("ble_frames"));
		assertTrue(list.contains("ble_activity"));
		assertNull(tools.handleRpc("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
	}

	@Test
	void snapshotAndSummaryMatchKnownBins() {
		SpectrumMcpTools tools = toolsWithSweep();
		String snap = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_snapshot\"}}");
		assertTrue(snap.contains("peakDbm"));
		assertTrue(snap.contains("-25"));
		assertFalse(snap.contains("-150"));
		String sum = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_summary\"}}");
		assertTrue(sum.contains("2402"));
		assertTrue(sum.contains("2472"));
	}

	@Test
	void sweepConfigSplitsRadioAndDisplay() {
		SpectrumMcpTools tools = toolsWithSweep();
		String cfg = tools.call("sweep_config", Map.of());
		assertTrue(cfg.contains("radio"));
		assertTrue(cfg.contains("display"));
		assertTrue(cfg.contains("autoScale"));
		assertTrue(cfg.contains("autoSweep"));
		assertTrue(cfg.contains("fftBinHz"));
		assertTrue(cfg.contains("100000"));
		assertTrue(cfg.contains("radioMode"));
		assertTrue(cfg.contains("sweep"));
		assertFalse(cfg.contains("persistent"));
	}

	@Test
	void sweepConfigReportsListenMode() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.startListen();
		store.publishContext(settings, java.util.List.of(), 0);
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		String cfg = tools.call("sweep_config", Map.of());
		assertTrue(cfg.contains("radioMode"));
		assertTrue(cfg.contains("listen"));
		assertTrue(cfg.contains("listenMHz"));
		assertTrue(cfg.contains("97.3"));
	}

	@Test
	void fmSpectrumReportsLiveParkedIq() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		assertTrue(tools.call("fm_spectrum", Map.of()).contains("no live FM spectrum"));
		store.publishFmListenSpectrum(new FmListenSpectrum(10, 97.3f, 97.2f,
				4_000_000f, 3906.25f, new float[] { 95.2f, 97.2f },
				new float[] { -70f, -20f }, -70f, -20f, 97.2f));
		String out = tools.call("fm_spectrum", Map.of());
		assertTrue(out.contains("dBFS"));
		assertTrue(out.contains("97.3000"));
		assertFalse(out.contains("\"isError\":true"));
	}

	@Test
	void sweepConfigReportsWatchModeAndTvChannel() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getTvChannel().setValue(14);
		settings.startWatch();
		store.publishContext(settings, java.util.List.of(), 0);
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		String cfg = tools.call("sweep_config", Map.of());
		assertTrue(cfg.contains("watch"));
		assertTrue(cfg.contains("tvChannel"));
		assertTrue(cfg.contains("14"));
		assertTrue(cfg.contains("tvLocked"));
	}

	@Test
	void tvWatchRequiresBinding() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> tools.call("tv_watch", Map.of("channel", 31)));
		assertTrue(ex.getMessage().contains("not bound"));
	}

	@Test
	void tvWatchParksTheNamedChannel() {
		int[] got = { 0 };
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		SpectrumMcpTools tools = new SpectrumMcpTools(store, ch -> got[0] = ch);
		String out = tools.call("tv_watch", Map.of("channel", 31));
		assertEquals(31, got[0]);
		assertTrue(out.contains("tvChannel"));
		assertTrue(out.contains("31"));
		assertTrue(out.contains("575"));
	}

	@Test
	void tvDebugReportsStageAndHistory() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getTvChannel().setValue(28);
		settings.startWatch();
		store.publishContext(settings, java.util.List.of(), 0);
		TvWatchDebug debug = new TvWatchDebug(1000, true, true, false, false, false,
				1000, 999, 1, 5000, 2500, 8, 1, 64, 40_000_000,
				0, 2, 0, 20, 500f, 0.15f, 1.4f, -30f, 0.8f, 1.1f);
		store.publishWatchDebug(debug);
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		String current = tools.call("tv_debug", Map.of());
		assertTrue(current.contains("rs_unusable"));
		assertTrue(current.contains("tvChannel"));
		assertTrue(current.contains("badPackets"));
		assertTrue(current.contains("999"));
		assertTrue(current.contains("agcGain"));
		assertTrue(current.contains("ffmpeg"));
		assertTrue(current.contains("lastStderr"));
		assertTrue(current.contains("videoPid"));
		assertTrue(current.contains("tsBytesWritten"));
		String history = tools.call("tv_debug_history", Map.of("maxSamples", 1));
		assertTrue(history.contains("sampleCount"));
		assertTrue(history.contains("fieldSyncFraction"));
		assertTrue(history.contains("pmtPid"), history);
	}

	@Test
	void tvDebugSplitsFfmpegStallFromMissingPmt() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		MpegTsPlayer.Stats ffmpeg = new MpegTsPlayer.Stats(true, false, true, false,
				MpegTsPlayer.Stats.EXIT_NONE, 10, 2000, 8000, 8000, 1, MpegTsPlayer.TS_QUEUE_CAP, 0,
				0, 0, 0, false, "Could not find codec parameters", MpegTsProbe.Snapshot.empty());
		store.publishWatchDebug(new TvWatchDebug(1000, true, true, true, true, false, 100, 1, 99,
				5000, 2500, 8, 50, 64, 40_000_000, 0, 2, 0, 20, 500f, 0.15f, 1.4f, -10f, 0.8f,
				1.1f, ffmpeg));
		String current = new SpectrumMcpTools(store).call("tv_debug", Map.of());
		assertTrue(current.contains("no_pmt"), current);
		assertTrue(current.contains("tsBytesWritten"), current);
		assertTrue(current.contains("Could not find codec parameters"), current);
		assertTrue(current.contains("videoAlive"), current);
	}

	@Test
	void tvSpectrumReportsLiveParkedIq() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		assertTrue(tools.call("tv_spectrum", Map.of()).contains("no live TV spectrum"));
		store.publishTvWatchSpectrum(new TvWatchSpectrum(10, 33, 587f,
				16_000_000f, 15_625f, new float[] { 579f, 587f },
				new float[] { -80f, -15f }, -80f, -15f, 587f));
		String out = tools.call("tv_spectrum", Map.of());
		assertTrue(out.contains("dBFS"));
		assertTrue(out.contains("tvChannel"));
		assertFalse(out.contains("\"isError\":true"));
	}

	@Test
	void fmListenRequiresBinding() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> tools.call("fm_listen", Map.of("mhz", 97.3)));
		assertTrue(ex.getMessage().contains("not bound"));
	}

	@Test
	void fmListenParksTheNamedDial() {
		double[] got = { 0 };
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore(), null, mhz -> got[0] = mhz);
		String out = tools.call("fm_listen", Map.of("mhz", 97.3));
		assertEquals(97.3, got[0], 1e-6);
		assertTrue(out.contains("listenMHz"));
		assertTrue(out.contains("97.3"));
		assertTrue(out.contains("97300"));
	}

	@Test
	void nfcSniffParksWhenBound()
	{
		int[] calls = { 0 };
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore(), null, null, () -> calls[0]++);
		String out = tools.call("nfc_sniff", Map.of());
		assertEquals(1, calls[0]);
		assertTrue(out.contains("radioMode"));
		assertTrue(out.contains("nfc"));
		assertTrue(out.contains("11560000"));
		SpectrumMcpTools unbound = new SpectrumMcpTools(new SpectrumSnapshotStore());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> unbound.call("nfc_sniff", Map.of()));
		assertTrue(ex.getMessage().contains("not bound"));
	}

	@Test
	void autoGainWritesTheExistingCheckbox()
	{
		boolean[] got = { true };
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore(), null, null, null,
				on -> got[0] = on, null);
		String out = tools.call("auto_gain", Map.of("enabled", false));
		assertFalse(got[0]);
		assertTrue(out.contains("autoGain"));
		assertTrue(out.contains("false"));
		SpectrumMcpTools unbound = new SpectrumMcpTools(new SpectrumSnapshotStore());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> unbound.call("auto_gain", Map.of("enabled", false)));
		assertTrue(ex.getMessage().contains("not bound"));
	}

	@Test
	void bleSniffAndActivityShareSpectrumAndFrames()
	{
		boolean[] enabled = { false };
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2400, 2484, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -70f;
		store.publishSweep(SpectrumSnapshot.fromDataset(ds, 11L, 500, null), 11L);
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getFrequency().setValue(new FrequencyRange(2400, 2484));
		store.publishContext(settings, java.util.List.of(), 20.0);
		store.publishBleStatus(true, "/dev/ttyACM0", "scan");
		store.publishBleFrame(new hotiron.core.BleFrame(11L, 37, -42, "ADV_IND", "AA:BB:CC:DD:EE:FF", "00", true));
		SpectrumMcpTools tools = new SpectrumMcpTools(store, null, null, null, null, null, on -> enabled[0] = on);
		String start = tools.call("ble_sniff", Map.of());
		assertTrue(enabled[0]);
		assertTrue(start.contains("2400"));
		assertTrue(start.contains("2484"));
		String frames = tools.call("ble_frames", Map.of("maxSamples", 10));
		assertTrue(frames.contains("ADV_IND"));
		assertTrue(frames.contains("sniffing"));
		String activity = tools.call("ble_activity", Map.of());
		assertTrue(activity.contains("ADV_IND"));
		assertTrue(activity.contains("peakDbm") || activity.contains("2400"));
		String snap = tools.call("spectrum_snapshot", Map.of());
		assertTrue(snap.contains("ble"));
		assertTrue(snap.contains("ADV_IND"));
		SpectrumMcpTools unbound = new SpectrumMcpTools(new SpectrumSnapshotStore());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> unbound.call("ble_sniff", Map.of()));
		assertTrue(ex.getMessage().contains("not bound"));
	}

	@Test
	void sweepRestartsTheWidebandPath()
	{
		int[] calls = { 0 };
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore(), null, null, null, null,
				() -> calls[0]++);
		String out = tools.call("sweep", Map.of());
		assertEquals(1, calls[0]);
		assertTrue(out.contains("sweep"));
	}

	@Test
	void nfcFramesReadsTheStoreRing()
	{
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		store.publishNfcFrame(new hotiron.core.NfcFrame(1, 0x0101, 0x0102, 0, 0, 106000, 0, 0, "REQA", "26"));
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		String json = tools.call("nfc_frames", Map.of("maxSamples", 10));
		assertTrue(json.contains("REQA"));
		assertTrue(json.contains("count"));
	}

	@Test
	void nfcActivityReportsPublishedClassification()
	{
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		SpectrumMcpTools tools = new SpectrumMcpTools(store);
		String hidden = tools.call("nfc_activity", Map.of());
		assertTrue(hidden.contains("hidden"));
		assertTrue(hidden.contains("trackingHint"));
		assertTrue(hidden.contains("Bluetooth"));
		store.publishNfc(hotiron.core.NfcActivity.quietVisible());
		assertEquals(hotiron.core.NfcActivity.Kind.QUIET, store.nfcActivity().kind);
		String quiet = tools.call("nfc_activity", Map.of());
		assertTrue(quiet.contains("quiet"));
		assertFalse(quiet.contains("\"isError\":true"));
	}

	@Test
	void occupancyAndHistoryAreJsonRpcTextResults() {
		SpectrumMcpTools tools = toolsWithSweep();
		String occ = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_occupancy\"}}");
		assertTrue(occ.contains("emitters"));
		assertTrue(occ.contains("occupiedFraction"));
		assertFalse(occ.contains("\"isError\":true"));
		String hist = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_history\",\"arguments\":{\"seconds\":15,\"maxSamples\":10}}}");
		assertTrue(hist.contains("samples"));
		assertTrue(hist.contains("2402"));
		String bins = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":16,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_history_bins\",\"arguments\":{\"seconds\":15,\"maxSamples\":5,\"maxPoints\":32}}}");
		assertTrue(bins.contains("points"));
		assertTrue(bins.contains("2402"));
		assertFalse(bins.contains("\"isError\":true"));
		String sum = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_summary\"}}");
		assertTrue(sum.contains("occupiedFraction"));
		assertTrue(sum.contains("emitterCount"));
	}

	@Test
	void occupancyOnEmptyStoreIsAnErrorPayload() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String occ = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_occupancy\"}}");
		assertTrue(occ.contains("no sweep yet"));
		assertTrue(occ.contains("\"isError\":true"));
		String hist = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_history\"}}");
		assertTrue(hist.contains("no sweep yet"));
		assertTrue(hist.contains("\"isError\":true"));
		String bins = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":19,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_history_bins\"}}");
		assertTrue(bins.contains("no sweep yet"));
		assertTrue(bins.contains("\"isError\":true"));
	}

	@Test
	void unknownToolIsJsonRpcError() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String rpc = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"explode\"}}");
		assertTrue(rpc.contains("\"error\""));
		assertTrue(rpc.contains("unknown tool"));
	}

	@Test
	void contentLengthRoundTrip() throws Exception {
		java.io.StringReader in = new java.io.StringReader(
				"Content-Length: 55\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
		java.io.BufferedReader reader = new java.io.BufferedReader(in);
		String msg = SpectrumMcpServer.readMessage(reader);
		assertTrue(msg.contains("tools/list"));
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.BufferedWriter w = new java.io.BufferedWriter(sw);
		SpectrumMcpServer.writeMessage(w, "{\"ok\":true}");
		assertTrue(sw.toString().startsWith("Content-Length:"));
		assertTrue(sw.toString().contains("{\"ok\":true}"));
	}
}
