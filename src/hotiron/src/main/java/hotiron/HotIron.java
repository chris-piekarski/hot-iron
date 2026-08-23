package hotiron;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.Align;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;

import hotiron.capture.ScreenCapture;
import hotiron.core.AnalyzerSettings;
import hotiron.core.BandMark;
import hotiron.core.DatasetSpectrumPeak;
import hotiron.core.FmBandLayer;
import hotiron.core.FmChannel;
import hotiron.core.FmChannelPlan;
import hotiron.core.FmListenEngine;
import hotiron.core.FmStationDial;
import hotiron.core.FmStationHit;
import hotiron.core.FmStationTracker;
import hotiron.core.FrequencyAxis;
import hotiron.core.FrequencyAllocationTable;
import hotiron.core.FrequencyAllocations;
import hotiron.core.FrequencyBand;
import hotiron.core.FrequencyRange;
import hotiron.core.AutoGainPolicy;
import hotiron.core.AutoSweepPolicy;
import hotiron.core.GainPolicy;
import hotiron.core.FmListenGainPolicy;
import hotiron.core.TvWatchGainPolicy;
import hotiron.core.AudioSink;
import hotiron.core.AudioSinks;
import hotiron.core.AudioSpectrum;
import hotiron.core.PersistentDisplay;
import hotiron.core.RecordingAudioSink;
import hotiron.core.WfmDemodulator;
import hotiron.core.RadioIdentity;
import hotiron.core.RuntimePerformanceWatch;
import hotiron.core.SpectrumPowerScale;
import hotiron.core.SpectrumSweepEngine;
import hotiron.core.SweepConfig;
import hotiron.core.SpectrumZoom;
import hotiron.core.SpectrumZoomHistory;
import hotiron.core.SpurFilter;
import hotiron.core.jfc.XYSeriesImmutable;
import hotiron.core.jfc.XYSeriesCollectionImmutable;
import hotiron.mcp.FmListenSpectrum;
import hotiron.mcp.TvWatchSpectrum;
import hotiron.nativebridge.HackRFDeviceQuery;
import hotiron.nativebridge.HackRFFmNativeBridge;
import hotiron.nativebridge.HackRFSweepNativeBridge;
import hotiron.ui.BandHeaderPainter;
import hotiron.ui.HackRFSweepSettingsUI;
import hotiron.ui.ListenHud;
import hotiron.ui.SweepStatusBar;
import hotiron.ui.WaterfallPlot;
import hotiron.ui.WatchHud;
import hotiron.ui.FmChannelOverlay;
import hotiron.ui.QuickSelectBandOverlay;
import hotiron.ui.SpectrumZoomOverlay;
import hotiron.ui.WifiChannelOverlay;
import hotiron.mvc.ModelValue;

public class HotIron {

	/**
	 * Color palette for UI
	 */
	protected static class ColorScheme {
		Color	palette0	= Color.white;
		Color	palette1	= new Color(0xe5e5e5);
		Color	palette2	= new Color(0xFCA311);
		Color	palette3	= new Color(0x14213D);
		Color	palette4	= Color.BLACK;
	}

	public static final int	SPECTRUM_PALETTE_SIZE_MIN	= 5;
	private static boolean	captureGIF					= false;

	public static void main(String[] args) throws IOException {
		//		System.out.println(new File("").getAbsolutePath());
		boolean mcpTcp = false;
		boolean mcpStdio = false;
		int mcpPort = hotiron.mcp.SpectrumMcpServer.DEFAULT_PORT;
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				String a = args[i];
				if ("capturegif".equals(a))
					captureGIF = true;
				else if ("--mcp".equals(a) || "mcp".equals(a))
					mcpTcp = true;
				else if ("--mcp-stdio".equals(a))
					mcpStdio = true;
				else if (a != null && a.startsWith("--mcp-port="))
					mcpPort = Integer.parseInt(a.substring("--mcp-port=".length()));
			}
		}
		//		try { Thread.sleep(20000); System.out.println("Started..."); } catch (InterruptedException e) {}

		hotiron.ui.AnalyzerLookAndFeel.install();
		HotIron app = new HotIron();
		if (mcpTcp)
			app.startMcpTcp(mcpPort);
		if (mcpStdio)
			app.startMcpStdio();
	}

	public boolean									flagIsHWSendingData						= false;
	private float									alphaFreqAllocationTableBandsImage	= 0.5f;
	private float									alphaPersistentDisplayImage			= 1.0f;
	private JFreeChart								chart;

	private ModelValue<Rectangle2D>					chartDataArea						= new ModelValue<Rectangle2D>(
			"Chart data area", new Rectangle2D.Double(0, 0, 1, 1));
	private XYSeriesCollectionImmutable				chartDataset								= new XYSeriesCollectionImmutable();
	private final XYSeriesImmutable					parkedRfPeaksEmpty						=
			new XYSeriesImmutable("peaks", new float[0], new float[0]);
	private XYLineAndShapeRenderer					chartLineRenderer;
	private ChartPanel								chartPanel;
	private ColorScheme								colors								= new ColorScheme();
	private DatasetSpectrumPeak						datasetSpectrum;
	private final hotiron.mcp.SpectrumSnapshotStore snapshotStore = new hotiron.mcp.SpectrumSnapshotStore();
	private hotiron.mcp.SpectrumMcpServer	mcpServer;
	private volatile boolean						flagManualGain						= false;
	private volatile boolean						flagApplyingAutoGain				= false;
	private volatile boolean						flagCoalesceGainRestart				= false;
	private volatile boolean						flagApplyingAutoSweep				= false;
	private volatile boolean						flagCoalesceAutoSweep				= false;
	private final AutoGainPolicy.Loop				autoGainLoop						= new AutoGainPolicy.Loop();
	private volatile boolean						forceStopSweep						= false;
	/**
	 * Capture a GIF of the program for the GITHUB page
	 */
	private ScreenCapture							gifCap								= null;
	private final AnalyzerSettings					settings							= new AnalyzerSettings();
	private BufferedImage							imageFrequencyAllocationTableBands	= null;
	private ReentrantLock							lock								= new ReentrantLock();

	private volatile List<FmStationHit>				fmStations							= List.of();
	private final FmStationTracker					fmTracker							= new FmStationTracker();
	private volatile List<hotiron.core.TvStationHit> tvStations = List.of();
	private final hotiron.core.TvStationTracker tvTracker = new hotiron.core.TvStationTracker();
	private final hotiron.core.BandScanSession scanSession = new hotiron.core.BandScanSession();
	private final hotiron.core.TvWatchEngine tvEngine = new hotiron.core.TvWatchEngine();
	private final AtomicReference<BufferedImage> pendingTvFrame = new AtomicReference<>();
	private final AtomicBoolean tvFrameUpdateQueued = new AtomicBoolean();
	private final SpectrumZoomHistory				spectrumZoomHistory					= new SpectrumZoomHistory();
	private final SpectrumZoomOverlay				spectrumZoomOverlay					= new SpectrumZoomOverlay();
	private boolean									applyingSpectrumZoom;
	private boolean									spectrumZoomDragging;
	private int										spectrumZoomAnchorX;
	private PersistentDisplay						persistentDisplay					= new PersistentDisplay();
	private javax.swing.Timer						persistFlushTimer;
	private float									spectrumInitValue					= -150;
	private SpurFilter								spurFilter;
	private SpectrumSweepEngine						sweepEngine;
	private Thread									threadHackrfSweep;
	private ArrayBlockingQueue<Integer>				threadLaunchCommands				= new ArrayBlockingQueue<>(1);
	private final javax.swing.Timer					radioApplyTimer					= new javax.swing.Timer(
			SweepConfig.FREQUENCY_APPLY_DEBOUNCE_MS, e -> restartHackrfSweep());
	private Thread									threadLauncher;
	private Thread									threadProcessing;
	private TextTitle								titleFreqBand						= new TextTitle("",
			new Font("Dialog", Font.PLAIN, 11));
	private RuntimePerformanceWatch					perfWatch							= new RuntimePerformanceWatch();
	private JFrame									uiFrame;
	private WaterfallPlot							waterfallPlot;
	private JSplitPane								splitPane;
	private HackRFSweepSettingsUI					settingsPanel;
	private JLabel labelMessages;
	private SweepStatusBar sweepStatusBar;
	private final FmListenEngine fmEngine = new FmListenEngine();
	private volatile boolean fmAudioOk;

	public HotIron() {
		hotiron.ui.AnalyzerLookAndFeel.install();
		settings.setHardware(new AnalyzerSettings.Hardware() {
			@Override
			public void restartSweep() {
				radioApplyTimer.stop();
				restartHackrfSweep();
			}

			@Override
			public void releaseRadio() {
				radioApplyTimer.stop();
				forceStopSweep = true;
				if (sweepEngine != null)
					sweepEngine.requestStop();
				HackRFSweepNativeBridge.stop();
				HackRFFmNativeBridge.stop();
				fmEngine.stop();
				tvEngine.stop();
				if (threadHackrfSweep != null) {
					try {
						threadHackrfSweep.join(2000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				refreshRadioIdentity();
			}

			@Override
			public void startListen() {
				radioApplyTimer.stop();
				restartHackrfSweep();
			}

			@Override
			public void startWatch() {
				radioApplyTimer.stop();
				restartHackrfSweep();
			}

			@Override
			public java.util.List<String> listRadioSerials() {
				return HackRFDeviceQuery.listSerials();
			}
		});

		if (captureGIF) {
//			settings.getFrequency().setValue(new FrequencyRange(700, 2700));
			settings.getFrequency().setValue(new FrequencyRange(2400, 2700));
			settings.getGain().setValue(60);
			settings.isSpurRemoval().setValue(true);
			settings.isPersistentDisplayVisible().setValue(true);
			settings.isAutoSweep().setValue(false);
			settings.getFFTBinHz().setValue(500000);
			settings.getFrequencyAllocationTable().setValue(new FrequencyAllocations().getTable().values().stream().findFirst().get());
		}

		if (settings.isAutoGain().getValue()) {
			FrequencyRange boot = settings.getFrequency().getValue();
			int seed = AutoGainPolicy.seedGain(boot.getStartMHz(), boot.getEndMHz());
			settings.getGain().setValue(seed);
			recalculateGains(seed);
		} else {
			recalculateGains(settings.getGain().getValue());
		}

		setupChart();

		setupChartMouseMarkers();
		setupSpectrumZoom();

		waterfallPlot = new WaterfallPlot(chartPanel, 300);

		refreshRadioIdentity();
		settingsPanel = new HackRFSweepSettingsUI(settings);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, waterfallPlot);
		splitPane.setResizeWeight(0.8);
		splitPane.setBorder(null);

		labelMessages = new JLabel("dsadasd");
		labelMessages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		settings.isDebugDisplay().addListener((debug) -> {
			labelMessages.setVisible(debug);
		});
		settings.isDebugDisplay().callObservers();
		
		JPanel splitPanePanel	= new JPanel(new BorderLayout());
		splitPanePanel.add(splitPane, BorderLayout.CENTER);
		splitPanePanel.add(labelMessages, BorderLayout.SOUTH);

		uiFrame = new JFrame();
		uiFrame.setUndecorated(captureGIF);
		uiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		uiFrame.setLayout(new BorderLayout());
		uiFrame.setTitle("HotIron");
		((javax.swing.JComponent) uiFrame.getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 16, 8));
		uiFrame.add(splitPanePanel, BorderLayout.CENTER);
		uiFrame.setResizable(true);
		uiFrame.setMinimumSize(new Dimension(900, 560));
		JScrollPane settingsScroll = new JScrollPane(settingsPanel);
		settingsScroll.setBorder(null);
		settingsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		settingsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		settingsScroll.getVerticalScrollBar().setUnitIncrement(16);
		settingsScroll.setMinimumSize(new Dimension(260, 200));
		uiFrame.add(settingsScroll, BorderLayout.EAST);
		sweepStatusBar = new SweepStatusBar();
		settings.getMcpStatus().addListener(s -> javax.swing.SwingUtilities.invokeLater(() -> sweepStatusBar.setMcp(s)));
		sweepStatusBar.setMcp(settings.getMcpStatus().getValue());
		uiFrame.add(sweepStatusBar, BorderLayout.SOUTH);
		applyAppIcons(uiFrame);
		
		setupFrequencyAllocationTable();
		
		uiFrame.pack();
		uiFrame.setMinimumSize(new Dimension(900, 560));
		uiFrame.setResizable(true);
		placeInitialWindow(uiFrame);
		uiFrame.setVisible(true);

		sweepEngine = new SpectrumSweepEngine(settings, spectrumInitValue, new SweepUiHooks());
		radioApplyTimer.setRepeats(false);
		startLauncherThread();
		applyAutoSweep(settings.getFrequency().getValue(), false);
		restartHackrfSweep();

		/**
		 * register parameter observers
		 */
		setupParameterObservers();

		//shutdown on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopHackrfSweep()));

		if (captureGIF) {
			try {
				gifCap = new ScreenCapture(uiFrame, 35 * 1, 10, 5, 760, 660, new File("screenshot.gif"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startMcpTcp(int port) {
		try {
			ensureMcpServer();
			mcpServer.startLocalhost(port);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			settings.getMcpStatus().setValue(hotiron.core.McpStatus.bindFailed(
					hotiron.mcp.SpectrumMcpServer.BIND_HOST, port, e.getMessage()));
		}
	}

	public void startMcpStdio() {
		ensureMcpServer();
		Thread t = new Thread(() -> {
			try {
				mcpServer.runStdio();
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}, "spectrum-mcp-stdio");
		t.setDaemon(true);
		t.start();
	}

	private void ensureMcpServer() {
		if (mcpServer != null)
			return;
		mcpServer = new hotiron.mcp.SpectrumMcpServer(snapshotStore, ch -> {
			runOnEdt(() -> {
				settings.getTvChannel().setValue(ch);
				settings.startWatch();
			});
		}, mhz -> {
			runOnEdt(() -> {
				FmChannel ch = FmChannelPlan.clamp(mhz);
				settings.getListenKHz().setValue(ch.centerKHz);
				settings.startListen();
			});
		});
		mcpServer.addStatusListener(s -> settings.getMcpStatus().setValue(s));
	}

	private static void runOnEdt(Runnable tune)
	{
		if (SwingUtilities.isEventDispatchThread())
			tune.run();
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(tune);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			catch (InvocationTargetException e)
			{
				throw new IllegalArgumentException(
						e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
			}
		}
	}

	private static void applyAppIcons(JFrame frame) {
		List<Image> icons = loadAppIcons();
		if (icons.isEmpty())
			return;
		frame.setIconImages(icons);
		if (Taskbar.isTaskbarSupported()) {
			try {
				Taskbar.getTaskbar().setIconImage(icons.get(0));
			} catch (Exception ignored) {
			}
		}
	}

	private static List<Image> loadAppIcons() {
		List<Image> icons = new ArrayList<Image>();
		String[] resources = {
				"/hotiron/icon-256.png",
				"/hotiron/icon-128.png",
				"/hotiron/icon-64.png",
				"/hotiron/icon-48.png",
				"/hotiron/icon-32.png",
				"/hotiron/icon-16.png"
		};
		for (int i = 0; i < resources.length; i++) {
			URL url = HotIron.class.getResource(resources[i]);
			if (url != null)
				icons.add(new ImageIcon(url).getImage());
		}
		return icons;
	}

	/**
	 * WSLg often reports one huge virtual desktop (e.g. 15360x2160). Maximizing
	 * there yields a gray empty frame. Size to the default screen and sit at
	 * its origin instead.
	 */
	private static void placeInitialWindow(JFrame frame) {
		Rectangle screen;
		try {
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			screen = gd.getDefaultConfiguration().getBounds();
		} catch (Exception e) {
			screen = new Rectangle(0, 0, 1920, 1080);
		}
		int w = 1600;
		int h = 900;
		if (screen.width > 0 && screen.width <= 2560)
			w = Math.max(1000, screen.width - 80);
		if (screen.height > 0 && screen.height <= 1440)
			h = Math.max(700, screen.height - 80);
		if (screen.width > 2560 || screen.height > 1440) {
			w = 1600;
			h = 900;
		}
		frame.setExtendedState(Frame.NORMAL);
		frame.setSize(w, h);
		int x = screen.x + 40;
		int y = screen.y + 40;
		if (x + w > screen.x + screen.width && screen.width > w)
			x = screen.x + Math.max(0, (screen.width - w) / 2);
		frame.setLocation(x, y);
	}

	private void fireCapturingStateChanged() {
		SwingUtilities.invokeLater(() -> settings.fireCaptureStateChanged(!settings.isCapturingPaused().getValue()));
	}

	private void refreshRadioIdentity() {
		RadioIdentity identity = RadioIdentity.ABSENT;
		try {
			identity = HackRFDeviceQuery.query().toIdentity();
		} catch (Throwable t) {
			identity = RadioIdentity.ABSENT;
		}
		settings.getRadioIdentity().setValue(identity);
	}

	private void fireHardwareStateChanged(boolean sendingData) {
		if (this.flagIsHWSendingData != sendingData) {
			this.flagIsHWSendingData = sendingData;
			SwingUtilities.invokeLater(() -> settings.fireHardwareStatusChanged(sendingData));
		}
	}

	private FrequencyRange getFreq() {
		return settings.getFrequency().getValue();
	}

	private void cancelScanIfRangeLeft() {
		if (!scanSession.active())
			return;
		FrequencyRange expected = scanSession.currentWindow();
		if (expected != null && !expected.equals(getFreq()))
			settings.stopScan();
	}

	private void advanceBandScan(DatasetSpectrumPeak ds) {
		if (!scanSession.active())
			return;
		long now = System.currentTimeMillis();
		FrequencyRange window = scanSession.currentWindow();
		if (window != null && ds != null && window.getStartMHz() == ds.getFreqStartMHz()
				&& window.getEndMHz() == ds.getFreqStopMHz())
			scanSession.markLive(now);
		if (scanSession.shouldFinish(now)) {
			settings.stopScan();
			return;
		}
		scanSession.nextWindowIfDue(now).ifPresent(next -> {
			if (waterfallPlot != null)
				waterfallPlot.clearHistory();
			settings.getFrequency().setValue(next);
		});
	}

	private void publishDetectedStations(java.util.List<FmStationHit> hits) {
		if (FmStationDial.sameChannels(settings.getDetectedFmStations().getValue(), hits))
			return;
		settings.getDetectedFmStations().setValue(hits == null ? java.util.List.of() : java.util.List.copyOf(hits));
	}

	private void publishDetectedTvStations(java.util.List<hotiron.core.TvStationHit> hits) {
		if (hotiron.core.TvStationDial.sameChannels(settings.getDetectedTvStations().getValue(), hits))
			return;
		settings.getDetectedTvStations().setValue(hits == null ? java.util.List.of() : java.util.List.copyOf(hits));
	}

	private final class SweepUiHooks implements SpectrumSweepEngine.Hooks {
		private long lastChartUpdated = System.currentTimeMillis();
		private long frameCounterChart = 0;
		private final int limitChartRefreshFPS = 30;
		private final int limitPersistentRefreshEveryChartFrame = 2;
		private final XYSeries spectrumPeaksEmpty = new XYSeries("peaks");
		private SpectrumPowerScale powerScale;

		@Override
		public void onPacketAccepted() {
			fireHardwareStateChanged(true);
		}

		@Override
		public void onFirstDataset(DatasetSpectrumPeak ds, float fftBinHz) {
			// Keep the previous chart series until the first full sweep of the
			// new window lands — assigning the −150 dB init buffer here made
			// the plot flash a flat line during every retune.
			spurFilter = sweepEngine.getSpurFilter();
			final double start = getFreq().getStartMHz();
			final double end = getFreq().getEndMHz();
			SwingUtilities.invokeLater(() -> {
				if (chart != null)
					chart.getXYPlot().getDomainAxis().setRange(start, end);
			});
		}

		@Override
		public void onFullSweepProcessed(DatasetSpectrumPeak ds) {
			boolean axisChanged = datasetSpectrum == null || !datasetSpectrum.sameAxisAs(ds);
			datasetSpectrum = ds;
			if (axisChanged) {
				if (FmChannelPlan.overlapsBroadcast(ds.getFreqStartMHz(), ds.getFreqStopMHz()))
					fmTracker.reset();
				if (hotiron.core.TvChannelPlan.overlapsBroadcast(ds.getFreqStartMHz(), ds.getFreqStopMHz()))
					tvTracker.reset();
				powerScale = null;
				if (waterfallPlot != null)
					waterfallPlot.clearHistory();
			}
			long nowMs = System.currentTimeMillis();
			if (snapshotStore.shouldPublish(nowMs)) {
				FrequencyRange live = getFreq();
				java.util.List<FmStationHit> hits = fmStations;
				if (FmChannelPlan.overlapsBroadcast(live.getStartMHz(), live.getEndMHz())) {
					hits = fmTracker.update(ds, live.getStartMHz(), live.getEndMHz());
					fmStations = hits;
					publishDetectedStations(hits);
				}
				if (hotiron.core.TvChannelPlan.overlapsBroadcast(live.getStartMHz(), live.getEndMHz())) {
					tvStations = hotiron.core.TvStationDial.mergeLive(
							settings.getDetectedTvStations().getValue(),
							tvTracker.update(ds, live.getStartMHz(), live.getEndMHz()),
							live.getStartMHz(), live.getEndMHz());
					publishDetectedTvStations(tvStations);
				}
				snapshotStore.publishSweep(hotiron.mcp.SpectrumSnapshot.fromDataset(ds, nowMs,
						hotiron.mcp.SpectrumSnapshot.DEFAULT_MAX_POINTS, null), nowMs);
				double sps = waterfallPlot != null ? waterfallPlot.getFps() : 0;
				snapshotStore.publishContext(settings, hits, sps);
			}
			synchronized (perfWatch) {
				perfWatch.hwFullSpectrumRefreshes++;
			}
			// Narrow windows (FM 20 MHz) finish 400+ sweeps/s. Updating the
			// waterfall / EDT that often freezes the plot. Keep ingesting
			// bins; only paint at the chart frame rate.
			if (System.currentTimeMillis() - lastChartUpdated <= 1000 / limitChartRefreshFPS)
				return;
			lastChartUpdated = System.currentTimeMillis();
			frameCounterChart++;

			FrequencyRange sweepRange = getFreq();
			if (FmChannelPlan.overlapsBroadcast(sweepRange.getStartMHz(), sweepRange.getEndMHz())) {
				fmStations = fmTracker.update(ds, sweepRange.getStartMHz(), sweepRange.getEndMHz());
				publishDetectedStations(fmStations);
			}
			if (hotiron.core.TvChannelPlan.overlapsBroadcast(sweepRange.getStartMHz(), sweepRange.getEndMHz())) {
				tvStations = hotiron.core.TvStationDial.mergeLive(
						settings.getDetectedTvStations().getValue(),
						tvTracker.update(ds, sweepRange.getStartMHz(), sweepRange.getEndMHz()),
						sweepRange.getStartMHz(), sweepRange.getEndMHz());
				publishDetectedTvStations(tvStations);
			}
			advanceBandScan(ds);
			considerAutoGain(ds, sweepRange);

			if (System.currentTimeMillis() - perfWatch.lastStatisticsRefreshed > 1000) {
				synchronized (perfWatch) {
					perfWatch.waterfallDraw.nanosSum = waterfallPlot.getDrawTimeSumAndReset();
					perfWatch.waterfallDraw.count = waterfallPlot.getDrawingCounterAndReset();
					String stats = perfWatch.generateStatistics();
					SwingUtilities.invokeLater(() -> {
						labelMessages.setText(stats);
					});
					perfWatch.reset();
				}
			}

			int maxPts = chartVertexBudget();
			XYSeries spectrumSeries = datasetSpectrum.createSpectrumDataset("spectrum", maxPts);
			XYSeries spectrumPeaks = settings.isChartsPeaksVisible().getValue()
					? datasetSpectrum.createPeaksDataset("peaks", maxPts)
					: spectrumPeaksEmpty;
			final double yLow;
			final double yHigh;
			if (settings.isPowerAutoScale().getValue()) {
				SpectrumPowerScale target = SpectrumPowerScale.fromDataset(datasetSpectrum);
				long now = System.currentTimeMillis();
				if (powerScale == null || powerScale.isUnset())
					powerScale = (target.isUnset() ? SpectrumPowerScale.defaults() : target.displayTicks())
							.stamped(now);
				else
					powerScale = powerScale.follow(target, now);
				yLow = powerScale.lowDb;
				yHigh = powerScale.highDb;
			} else {
				powerScale = SpectrumPowerScale.defaults();
				yLow = SpectrumPowerScale.DEFAULT_LOW;
				yHigh = SpectrumPowerScale.DEFAULT_HIGH;
			}

			if (settings.isPersistentDisplayVisible().getValue()) {
				long start = System.nanoTime();
				boolean redraw = frameCounterChart % limitPersistentRefreshEveryChartFrame == 0;
				persistentDisplay.drawSpectrumFloat(datasetSpectrum, (float) yLow, (float) yHigh, redraw);
				synchronized (perfWatch) {
					perfWatch.persisentDisplay.addDrawingTime(System.nanoTime() - start);
				}
			}

			if (settings.isWaterfallVisible().getValue()) {
				long start = System.nanoTime();
				if (settings.isPowerAutoScale().getValue())
					waterfallPlot.applyPowerWindow(yLow, yHigh);
				else {
					int startDb = settings.getSpectrumPaletteStart().getValue();
					int sizeDb = settings.getSpectrumPaletteSize().getValue();
					waterfallPlot.setSpectrumPaletteStart(startDb);
					if (sizeDb >= SPECTRUM_PALETTE_SIZE_MIN)
						waterfallPlot.setSpectrumPaletteSize(sizeDb);
				}
				waterfallPlot.addNewData(datasetSpectrum);
				synchronized (perfWatch) {
					perfWatch.waterfallUpdate.addDrawingTime(System.nanoTime() - start);
				}
				waterfallPlot.repaint();
			}

			final double rbwHz = datasetSpectrum.getFFTBinSizeHz();
			final int bins = datasetSpectrum.spectrumLength();
			final double fps = waterfallPlot.getFps();
			final Double peakDbm = Double.valueOf(datasetSpectrum.calculateSpectrumPeakPower());
			SwingUtilities.invokeLater(() -> {
				if (sweepStatusBar != null)
					sweepStatusBar.setSweepInfo(rbwHz, bins, fps, peakDbm);
				chart.setNotify(false);
				NumberAxis yAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
				if (yAxis.getLowerBound() != yLow || yAxis.getUpperBound() != yHigh)
					yAxis.setRange(yLow, yHigh);
				chartDataset.removeAllSeries();
				chartDataset.addSeries(spectrumPeaks);
				chartDataset.addSeries(spectrumSeries);
				chart.setNotify(true);
				if (gifCap != null) {
					gifCap.captureFrame();
				}
			});
		}
	}

	private void recalculateGains(int totalGain) {
		int lnaGain = GainPolicy.lnaGain(totalGain);
		int vgaGain = GainPolicy.vgaGain(totalGain);
		this.settings.getGainLNA().setValue(lnaGain);
		this.settings.getGainVGA().setValue(vgaGain);
		this.settings.getGain().setValue(lnaGain + vgaGain);
	}

	private void applyAutoGain(int totalGain, boolean restart) {
		int snapped = GainPolicy.clampTotal(totalGain);
		flagApplyingAutoGain = true;
		flagCoalesceGainRestart = !restart;
		try {
			if (settings.getGain().getValue() != snapped)
				settings.getGain().setValue(snapped);
		} finally {
			flagApplyingAutoGain = false;
			flagCoalesceGainRestart = false;
		}
	}

	private void flushPersistentOverlay() {
		persistentDisplay.beginFlush();
		if (GraphicsEnvironment.isHeadless())
			return;
		if (persistFlushTimer == null)
			persistFlushTimer = new javax.swing.Timer(33, e -> {
				if (!persistentDisplay.tickFlush(System.currentTimeMillis()))
					persistFlushTimer.stop();
			});
		persistFlushTimer.restart();
	}

	private void applyAutoSweep(FrequencyRange range, boolean restart) {
		flagApplyingAutoSweep = true;
		flagCoalesceAutoSweep = true;
		boolean changed;
		try {
			changed = AutoSweepPolicy.apply(settings, range);
		} finally {
			flagApplyingAutoSweep = false;
			flagCoalesceAutoSweep = false;
		}
		if (restart && changed)
			restartHackrfSweep();
	}

	private void maybeSeedAutoGain(FrequencyRange range) {
		if (range == null || !settings.isAutoGain().getValue() || settings.isListening().getValue()
				|| scanSession.active())
			return;
		Integer seed = autoGainLoop.seedIfBandShifted(range.getStartMHz(), range.getEndMHz(),
				settings.getGain().getValue());
		if (seed == null)
			return;
		autoGainLoop.markSettling(System.currentTimeMillis());
		applyAutoGain(seed.intValue(), false);
	}

	private void considerAutoGain(DatasetSpectrumPeak ds, FrequencyRange range) {
		if (ds == null || range == null)
			return;
		if (!settings.isAutoGain().getValue() || settings.isCapturingPaused().getValue()
				|| settings.isRadioReleased().getValue() || settings.isListening().getValue()
				|| scanSession.active())
			return;
		AutoGainPolicy.Observation obs = AutoGainPolicy.observe(ds, settings.getGain().getValue(),
				range.getStartMHz(), range.getEndMHz());
		Integer next = autoGainLoop.consider(obs, System.currentTimeMillis());
		if (next == null || next.intValue() == settings.getGain().getValue())
			return;
		autoGainLoop.markSettling(System.currentTimeMillis());
		applyAutoGain(next.intValue(), true);
	}

	/**
	 * uses fifo queue to process launch commands, only the last launch command
	 * is important, delete others
	 */
	private synchronized void restartHackrfSweep() {
		if (settings.isRadioReleased().getValue())
			return;
		if (threadLaunchCommands.offer(0) == false) {
			threadLaunchCommands.clear();
			threadLaunchCommands.offer(0);
		}
	}

	private void scheduleFrequencyRadioApply() {
		if (settings.isRadioReleased().getValue())
			return;
		if (SwingUtilities.isEventDispatchThread())
			radioApplyTimer.restart();
		else
			SwingUtilities.invokeLater(radioApplyTimer::restart);
	}

	private int chartVertexBudget() {
		Rectangle2D area = chartDataArea.getValue();
		if (area != null && area.getWidth() > 8)
			return Math.max(256, (int) Math.round(area.getWidth()));
		return 2048;
	}

	/**
	 * no need to synchronize, executes only in the launcher thread
	 */
	private void restartHackrfSweepExecute() {
		stopHackrfSweep();
		if (!SweepConfig.shouldStartAfterStop(settings.isRadioReleased().getValue(),
				threadLaunchCommands.peek() != null))
			return;
		final boolean listen = settings.isListening().getValue();
		final boolean watch = listen && settings.getListenService().getValue() == hotiron.core.ListenService.TV;
		if (!listen)
			applyAutoSweep(settings.getFrequency().getValue(), false);
		threadHackrfSweep = new Thread(() -> {
			Thread.currentThread().setName(watch ? "hackrf_tv" : (listen ? "hackrf_fm" : "hackrf_sweep"));
			try {
				forceStopSweep = false;
				if (sweepEngine != null)
					sweepEngine.clearStop();
				if (watch)
					runTvWatch();
				else if (listen)
					runFmListen();
				else
					sweep();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		threadHackrfSweep.start();
	}

	private void setupChart() {
		int axisWidthLeft = 70;
		int axisWidthRight = 20;

		chart = ChartFactory.createXYLineChart("Spectrum analyzer", "Frequency [MHz]", "Power [dB]", chartDataset,
				PlotOrientation.VERTICAL, false, false, false);
		chart.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		XYPlot plot = chart.getXYPlot();
		NumberAxis domainAxis = ((NumberAxis) plot.getDomainAxis());
		NumberAxis rangeAxis = ((NumberAxis) plot.getRangeAxis());
		chartLineRenderer = new XYLineAndShapeRenderer();
		chartLineRenderer.setDefaultShapesVisible(false);
		chartLineRenderer.setDefaultStroke(new BasicStroke(settings.getSpectrumLineThickness().getValue().floatValue()));

		rangeAxis.setAutoRange(false);
		rangeAxis.setRange(SpectrumPowerScale.DEFAULT_LOW, SpectrumPowerScale.DEFAULT_HIGH);
		rangeAxis.setTickUnit(new NumberTickUnit(SpectrumPowerScale.TICK_DB, new DecimalFormat("###")));

		domainAxis.setAutoRange(false);
		domainAxis.setLowerMargin(0);
		domainAxis.setUpperMargin(0);
		domainAxis.setRange(getFreq().getStartMHz(), getFreq().getEndMHz());
		domainAxis.setNumberFormatOverride(new DecimalFormat(" #.### "));

		chartLineRenderer.setAutoPopulateSeriesStroke(false);
		chartLineRenderer.setAutoPopulateSeriesPaint(false);
		chartLineRenderer.setSeriesPaint(0, colors.palette2);

		plot.setDomainGridlinesVisible(false);
		plot.setRenderer(chartLineRenderer);

		/**
		 * sets empty space around the plot
		 */
		AxisSpace axisSpace = new AxisSpace();
		axisSpace.setLeft(axisWidthLeft);
		axisSpace.setRight(axisWidthRight);
		axisSpace.setTop(0);
		axisSpace.setBottom(50);
		plot.setFixedDomainAxisSpace(axisSpace);//sets width of the domain axis left/right
		plot.setFixedRangeAxisSpace(axisSpace);//sets heigth of range axis top/bottom

		rangeAxis.setAxisLineVisible(false);
		rangeAxis.setTickMarksVisible(false);

		plot.setAxisOffset(RectangleInsets.ZERO_INSETS); //no space between range axis and plot

		Font labelFont = new Font(Font.MONOSPACED, Font.BOLD, 16);
		rangeAxis.setLabelFont(labelFont);
		rangeAxis.setTickLabelFont(labelFont);
		rangeAxis.setLabelPaint(colors.palette1);
		rangeAxis.setTickLabelPaint(colors.palette1);
		domainAxis.setLabelFont(labelFont);
		domainAxis.setTickLabelFont(labelFont);
		domainAxis.setLabelPaint(colors.palette1);
		domainAxis.setTickLabelPaint(colors.palette1);
		chartLineRenderer.setDefaultPaint(Color.white);
		plot.setBackgroundPaint(colors.palette4);
		chart.setBackgroundPaint(colors.palette4);
		chartLineRenderer.setSeriesPaint(1, colors.palette1);

		chartPanel = new ChartPanel(chart);
		chartPanel.setDisplayToolTips(false);
		chartPanel.setMaximumDrawWidth(4096);
		chartPanel.setMaximumDrawHeight(2160);
		chartPanel.setMouseWheelEnabled(false);
		chartPanel.setDomainZoomable(false);
		chartPanel.setRangeZoomable(false);
		chartPanel.setPopupMenu(null);
		chartPanel.setMinimumSize(new Dimension(200, 200));

		/**
		 * Draws overlay of waterfall's color scale next to main spectrum chart
		 * to show
		 */
		chartPanel.addOverlay(new Overlay() {
			@Override
			public void addChangeListener(OverlayChangeListener listener) {
			}

			@Override
			public void paintOverlay(Graphics2D g, ChartPanel chartPanel) {
				Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
				int plotStartX = (int) area.getX();
				int plotWidth = (int) area.getWidth();

				Rectangle2D subplotArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();

				int y1 = (int) plot.getRangeAxis().valueToJava2D(waterfallPlot.getSpectrumPaletteStart(), subplotArea,
						plot.getRangeAxisEdge());
				int y2 = (int) plot.getRangeAxis().valueToJava2D(
						waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize(), subplotArea,
						plot.getRangeAxisEdge());

				int x = plotStartX + plotWidth;
				int w = 15;
				int h = y1 - y2;
				waterfallPlot.drawScale(g, x, y2, w, h);
			}

			@Override
			public void removeChangeListener(OverlayChangeListener listener) {
			}
		});

		/**
		 * Draw frequency bands as an overlay
		 */
		chartPanel.addOverlay(new Overlay() {
			@Override
			public void addChangeListener(OverlayChangeListener listener) {
			}

			@Override
			public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
				Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
				XYPlot xy = chart.getXYPlot();
				boolean fmParked = settings.isListening().getValue()
						&& settings.getListenService().getValue() == hotiron.core.ListenService.FM;
				if (fmParked)
				{
					double start = xy.getDomainAxis().getLowerBound();
					double end = xy.getDomainAxis().getUpperBound();
					FmChannelOverlay.paint(g2, area, start, end, java.util.List.of(),
							settings.getListenKHz().getValue());
					ListenHud.paint(g2, area, settings.getListenKHz().getValue() / 1000.0, fmAudioOk);
					return;
				}
				boolean tvParked = settings.isListening().getValue()
						&& settings.getListenService().getValue() == hotiron.core.ListenService.TV;
				if (tvParked)
				{
					double start = xy.getDomainAxis().getLowerBound();
					double end = xy.getDomainAxis().getUpperBound();
					hotiron.ui.TvChannelOverlay.paint(g2, area, start, end, java.util.List.of(),
							settings.getTvChannel().getValue());
					WatchHud.paint(g2, area, settings.getTvChannel().getValue(), tvEngine.locked(),
							tvEngine.snrDb(), tvEngine.packets(), tvEngine.frames(), tvEngine.previewFrames());
					return;
				}
				BufferedImage img = imageFrequencyAllocationTableBands;
				if (img != null) {
					g2.drawImage(img, (int) area.getX(), (int) area.getY(), null);
				}
				FrequencyRange range = getFreq();
				QuickSelectBandOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz());
				WifiChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz());
				FmChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz(), fmStations,
						settings.getListenKHz().getValue());
				hotiron.ui.TvChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz(), tvStations,
						settings.getTvChannel().getValue());
				if (settings.isListening().getValue())
				{
					if (settings.getListenService().getValue() == hotiron.core.ListenService.TV)
						WatchHud.paint(g2, area, settings.getTvChannel().getValue(), tvEngine.locked(),
								tvEngine.snrDb(), tvEngine.packets(), tvEngine.frames(), tvEngine.previewFrames());
					else
						ListenHud.paint(g2, area, settings.getListenKHz().getValue() / 1000.0, fmAudioOk);
				}
				spectrumZoomOverlay.paint(g2, area);
			}

			@Override
			public void removeChangeListener(OverlayChangeListener listener) {
			}
		});

		/**
		 * monitors chart data area for change due to no other way to extract
		 * that info from jfreechart when it changes
		 */
		chart.addChangeListener(event -> {
			Rectangle2D aN = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
			Rectangle2D aO = chartDataArea.getValue();
			if (aO.getX() != aN.getX() || aO.getY() != aN.getY() || aO.getWidth() != aN.getWidth()
					|| aO.getHeight() != aN.getHeight()) {
				chartDataArea.setValue(new Rectangle2D.Double(aN.getX(), aN.getY(), aN.getWidth(), aN.getHeight()));
			}
		});

		chart.addProgressListener(new ChartProgressListener() {
			private long chartRedrawStarted;

			@Override
			public void chartProgress(ChartProgressEvent arg0) {
				if (arg0.getType() == ChartProgressEvent.DRAWING_STARTED) {
					chartRedrawStarted = System.nanoTime();
				} else if (arg0.getType() == ChartProgressEvent.DRAWING_FINISHED) {
					synchronized (perfWatch) {
						perfWatch.chartDrawing.addDrawingTime(System.nanoTime() - chartRedrawStarted);
					}
				}
			}
		});
		
		
	}

	/**
	 * Displays a cross marker with current frequency and signal strength when
	 * mouse hovers over the frequency chart
	 */
	private void setupChartMouseMarkers() {
		ValueMarker freqMarker = new ValueMarker(0, Color.WHITE, new BasicStroke(1f));
		freqMarker.setLabelPaint(Color.white);
		freqMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		freqMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
		freqMarker.setLabelFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
		ValueMarker signalMarker = new ValueMarker(0, Color.WHITE, new BasicStroke(1f));
		signalMarker.setLabelPaint(Color.white);
		signalMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		signalMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
		signalMarker.setLabelFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

		chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
			DecimalFormat format = new DecimalFormat("0.#");

			@Override
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();

				XYPlot plot = chart.getXYPlot();
				Rectangle2D subplotArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
				double crosshairRange = plot.getRangeAxis().java2DToValue(y, subplotArea, plot.getRangeAxisEdge());
				signalMarker.setValue(crosshairRange);
				signalMarker.setLabel(String.format("%.1fdB", crosshairRange));
				double crosshairDomain = plot.getDomainAxis().java2DToValue(x, subplotArea, plot.getDomainAxisEdge());
				freqMarker.setValue(crosshairDomain);
				freqMarker.setLabel(String.format("%.1fMHz", crosshairDomain));

				FrequencyAllocationTable activeTable = settings.getFrequencyAllocationTable().getValue();
				if (activeTable != null) {
					FrequencyBand band = activeTable.lookupBand((long) (crosshairDomain * 1000000l));
					if (band == null)
						titleFreqBand.setText(" ");
					else {
						titleFreqBand.setText(String.format("%s - %s MHz  %s", format.format(band.getMHzStartIncl()),
								format.format(band.getMHzEndExcl()), band.getApplications().replaceAll("/", " / ")));
					}
				}
			}
		});
		chartPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				chart.getXYPlot().clearDomainMarkers();
				chart.getXYPlot().clearRangeMarkers();
				chart.getXYPlot().addRangeMarker(signalMarker);
				chart.getXYPlot().addDomainMarker(freqMarker);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				chart.getXYPlot().clearDomainMarkers();
				chart.getXYPlot().clearRangeMarkers();
				titleFreqBand.setText(" ");
			}
		});

		titleFreqBand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		titleFreqBand.setPosition(RectangleEdge.BOTTOM);
		titleFreqBand.setHorizontalAlignment(HorizontalAlignment.LEFT);
		titleFreqBand.setMargin(0.0, 2.0, 0.0, 2.0);
		titleFreqBand.setPaint(Color.white);
		chart.addSubtitle(titleFreqBand);
	}

	/**
	 * Grafana-style frequency zoom: drag a span to zoom in, double-click or
	 * scroll out to zoom out. Updates the sweep start/end so the radio
	 * retunes (same as changing the digits).
	 */
	private void setupSpectrumZoom() {
		chartPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e) || !inPlot(e) || inHeader(e))
					return;
				spectrumZoomAnchorX = e.getX();
				spectrumZoomDragging = true;
				spectrumZoomOverlay.setSelection(spectrumZoomAnchorX, spectrumZoomAnchorX);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!spectrumZoomDragging)
					return;
				spectrumZoomDragging = false;
				spectrumZoomOverlay.clear();
				chartPanel.repaint();
				Rectangle2D area = plotArea();
				if (area == null)
					return;
				FrequencyRange current = getFreq();
				SpectrumZoom.fromDrag(spectrumZoomAnchorX, e.getX(), area, current.getStartMHz(), current.getEndMHz())
						.ifPresent(HotIron.this::zoomIn);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (inHeader(e) && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
					tryListenFromHeader(e);
					return;
				}
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && inPlot(e) && !inHeader(e))
					zoomOut();
			}
		});
		chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!spectrumZoomDragging)
					return;
				spectrumZoomOverlay.setSelection(spectrumZoomAnchorX, e.getX());
				chartPanel.repaint();
			}
		});
		chartPanel.addMouseWheelListener((MouseWheelEvent e) -> {
			if (!inPlot(e))
				return;
			e.consume();
			Rectangle2D area = plotArea();
			if (area == null)
				return;
			FrequencyRange current = getFreq();
			double mhz = chart.getXYPlot().getDomainAxis().java2DToValue(e.getX(), area,
					chart.getXYPlot().getDomainAxisEdge());
			if (e.getWheelRotation() < 0)
				zoomIn(SpectrumZoom.around(current, mhz, SpectrumZoom.ZOOM_IN_FACTOR));
			else
				zoomOutAround(mhz);
		});
	}

	private boolean inPlot(MouseEvent e) {
		Rectangle2D area = plotArea();
		return area != null && area.contains(e.getX(), e.getY());
	}

	private boolean inHeader(MouseEvent e) {
		Rectangle2D area = plotArea();
		return area != null && e.getX() >= area.getMinX() && e.getX() <= area.getMaxX()
				&& e.getY() >= area.getMinY() && e.getY() <= area.getMinY() + BandHeaderPainter.HEADER_H;
	}

	private void tryListenFromHeader(MouseEvent e) {
		Rectangle2D area = plotArea();
		if (area == null)
			return;
		FrequencyRange range = getFreq();
		FrequencyAxis axis = FrequencyAxis.fromArea(area, range.getStartMHz(), range.getEndMHz());
		java.util.List<BandMark> fmMarks = FmBandLayer.marks(axis, fmStations, settings.getListenKHz().getValue());
		BandMark fmHit = BandHeaderPainter.hitTest(e.getX(), e.getY(), area, axis, fmMarks);
		if (fmHit != null)
		{
			FmChannel ch = FmChannelPlan.nearest(fmHit.labelMHz);
			if (ch == null)
				return;
			settings.getListenKHz().setValue(ch.centerKHz);
			settings.startListen();
			return;
		}
		java.util.List<BandMark> tvMarks = hotiron.core.TvBandLayer.marks(axis, tvStations,
				settings.getTvChannel().getValue());
		BandMark tvHit = BandHeaderPainter.hitTest(e.getX(), e.getY(), area, axis, tvMarks);
		if (tvHit == null)
			return;
		hotiron.core.TvChannel tv = hotiron.core.TvChannelPlan.containingMHz(tvHit.labelMHz);
		if (tv == null)
			return;
		settings.getTvChannel().setValue(tv.fccChannel);
		settings.startWatch();
	}

	private Rectangle2D plotArea() {
		if (chartPanel == null)
			return null;
		return chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
	}

	private void zoomIn(FrequencyRange next) {
		FrequencyRange current = getFreq();
		if (next == null || next.equals(current))
			return;
		spectrumZoomHistory.push(current);
		applySpectrumZoom(next);
	}

	private void zoomOut() {
		FrequencyRange current = getFreq();
		FrequencyRange next = spectrumZoomHistory.pop().orElseGet(() -> SpectrumZoom.expand(current));
		if (next.equals(current))
			return;
		applySpectrumZoom(next);
	}

	private void zoomOutAround(double centerMHz) {
		if (spectrumZoomHistory.canZoomOut()) {
			zoomOut();
			return;
		}
		FrequencyRange current = getFreq();
		FrequencyRange next = SpectrumZoom.around(current, centerMHz, SpectrumZoom.ZOOM_OUT_FACTOR);
		if (next.equals(current))
			return;
		applySpectrumZoom(next);
	}

	private void applySpectrumZoom(FrequencyRange next) {
		applyingSpectrumZoom = true;
		try {
			settings.getFrequency().setValue(next);
		} finally {
			applyingSpectrumZoom = false;
		}
	}

	private void setupFrequencyAllocationTable() {
		SwingUtilities.invokeLater(() -> {
			chartPanel.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					redrawFrequencySpectrumTable();
				}
			});
			chart.getXYPlot().getDomainAxis().addChangeListener((e) -> {
				redrawFrequencySpectrumTable();
			});
			chart.getXYPlot().getRangeAxis().addChangeListener(event -> {
				redrawFrequencySpectrumTable();
			});

		});
		settings.getFrequencyAllocationTable().addListener(this::redrawFrequencySpectrumTable);
	}

	private void setupParameterObservers() {
		Runnable restartHackrf = this::restartHackrfSweep;
		settings.getFrequency().addListener(() -> {
			cancelScanIfRangeLeft();
			if (settings.isListening().getValue())
				return;
			applyAutoSweep(settings.getFrequency().getValue(), false);
			scheduleFrequencyRadioApply();
		});
		settings.getBandScan().addListener(scan -> {
			if (scan == hotiron.core.BandScan.OFF)
			{
				scanSession.stop();
				return;
			}
			scanSession.start(scan, System.currentTimeMillis());
			if (waterfallPlot != null)
				waterfallPlot.clearHistory();
			if (scan == hotiron.core.BandScan.FM)
				fmTracker.reset();
			else
				tvTracker.reset();
		});
		settings.getFrequency().addListener((range) -> {
			flushPersistentOverlay();
			if (chart != null)
				chart.getXYPlot().getDomainAxis().setRange(range.getStartMHz(), range.getEndMHz());
			if (!applyingSpectrumZoom)
				spectrumZoomHistory.clear();
			maybeSeedAutoGain(range);
		});
		settings.getAntennaPowerEnable().addListener(restartHackrf);
		settings.getAntennaLNA().addListener(restartHackrf);
		settings.getFFTBinHz().addListener(() -> {
			if (!flagApplyingAutoSweep && settings.isAutoSweep().getValue())
				settings.isAutoSweep().setValue(false);
			if (!settings.isListening().getValue() && !flagCoalesceAutoSweep)
				restartHackrfSweep();
		});
		settings.getSamples().addListener(() -> {
			if (!flagApplyingAutoSweep && settings.isAutoSweep().getValue())
				settings.isAutoSweep().setValue(false);
			if (!settings.isListening().getValue() && !flagCoalesceAutoSweep)
				restartHackrfSweep();
		});
		settings.isAutoSweep().addListener((on) -> {
			if (!Boolean.TRUE.equals(on))
				return;
			applyAutoSweep(getFreq(), true);
		});
		settings.getSelectedSerial().addListener(restartHackrf);
		settings.getClkoutEnable().addListener(restartHackrf);
		settings.getListenKHz().addListener(() -> {
			if (settings.isListening().getValue()
					&& settings.getListenService().getValue() == hotiron.core.ListenService.FM)
				restartHackrfSweep();
			snapshotStore.publishContext(settings, fmStations, 0);
			if (chartPanel != null)
				SwingUtilities.invokeLater(chartPanel::repaint);
		});
		settings.getTvChannel().addListener(() -> {
			if (settings.isListening().getValue()
					&& settings.getListenService().getValue() == hotiron.core.ListenService.TV)
				restartHackrfSweep();
			snapshotStore.publishContext(settings, fmStations, 0);
			if (chartPanel != null)
				SwingUtilities.invokeLater(chartPanel::repaint);
		});
		settings.getListenVolume().addListener(v -> {
			fmEngine.setVolume(v);
			tvEngine.setVolume(v);
		});
		settings.isListening().addListener(() -> {
			flushPersistentOverlay();
			snapshotStore.publishContext(settings, fmStations, 0);
			if (chartPanel != null)
				SwingUtilities.invokeLater(chartPanel::repaint);
		});
		settings.getListenService().addListener(svc -> flushPersistentOverlay());
		settings.isCapturingPaused().addListener(this::fireCapturingStateChanged);

		settings.getGain().addListener((gainTotal) -> {
			if (flagManualGain) //flag is being adjusted manually by LNA or VGA, do not recalculate the gains
				return;
			recalculateGains(gainTotal);
			if (!flagCoalesceGainRestart)
				restartHackrfSweep();
			if (!flagApplyingAutoGain && settings.isAutoGain().getValue())
				settings.isAutoGain().setValue(false);
		});
		Runnable gainRecalc = () -> {
			int totalGain = settings.getGainLNA().getValue() + settings.getGainVGA().getValue();
			flagManualGain = true;
			try {
				settings.getGain().setValue(totalGain);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				flagManualGain = false;
			}
			if (!flagApplyingAutoGain && settings.isAutoGain().getValue())
				settings.isAutoGain().setValue(false);
			if (!flagCoalesceGainRestart && !flagApplyingAutoGain)
				restartHackrfSweep();
		};
		settings.getGainLNA().addListener(gainRecalc);
		settings.getGainVGA().addListener(gainRecalc);
		settings.isAutoGain().addListener((on) -> {
			if (!Boolean.TRUE.equals(on))
				return;
			autoGainLoop.reset();
			FrequencyRange range = getFreq();
			Integer seed = autoGainLoop.seedIfBandShifted(range.getStartMHz(), range.getEndMHz(),
					settings.getGain().getValue());
			if (seed == null)
				return;
			autoGainLoop.markSettling(System.currentTimeMillis());
			applyAutoGain(seed.intValue(), true);
		});

		settings.isSpurRemoval().addListener(() -> {
			SpurFilter filter = spurFilter;
			if (filter != null) {
				filter.recalibrate();
			}
		});
		settings.isChartsPeaksVisible().addListener(() -> {
			DatasetSpectrumPeak p = datasetSpectrum;
			if (p != null) {
				p.resetPeaks();
			}
		});
		settings.isPowerAutoScale().addListener((enabled) -> {
			SwingUtilities.invokeLater(() -> {
				if (chart == null || enabled)
					return;
				chart.getXYPlot().getRangeAxis().setRange(SpectrumPowerScale.DEFAULT_LOW,
						SpectrumPowerScale.DEFAULT_HIGH);
			});
		});
		settings.getSpectrumPaletteStart().setValue((int) waterfallPlot.getSpectrumPaletteStart());
		settings.getSpectrumPaletteSize().setValue((int) waterfallPlot.getSpectrumPaletteSize());
		settings.getSpectrumPaletteStart().addListener(waterfallPlot::setSpectrumPaletteStart);
		settings.getSpectrumPaletteSize().addListener((dB) -> {
			if (dB < SPECTRUM_PALETTE_SIZE_MIN)
				return;
			waterfallPlot.setSpectrumPaletteSize(dB);
		});
		settings.getPeakFallRate().addListener((fallRate) -> {
			datasetSpectrum.setPeakFalloutMillis(fallRate * 1000l);
		});

		settings.getSpectrumLineThickness().addListener((thickness) -> {
			SwingUtilities.invokeLater(() -> chartLineRenderer.setDefaultStroke(new BasicStroke(thickness.floatValue())));
		});
		
		settings.getPersistentDisplayDecayRate().addListener((time) -> {
			persistentDisplay.setPersistenceTime(time);
		});

		int persistentDisplayDownscaleFactor = 4;

		Runnable resetPersistentImage = () -> {
			boolean display = settings.isPersistentDisplayVisible().getValue();
			persistentDisplay.reset();
			chart.getXYPlot().setBackgroundImage(display ? persistentDisplay.getDisplayImage().getValue() : null);
			chart.getXYPlot().setBackgroundImageAlpha(alphaPersistentDisplayImage);
		};
		persistentDisplay.getDisplayImage().addListener((image) -> {
			SwingUtilities.invokeLater(() -> {
				if (settings.isPersistentDisplayVisible().getValue())
					chart.getXYPlot().setBackgroundImage(image);
			});
		});

		settings.isPersistentDisplayVisible().addListener((display) -> {
			SwingUtilities.invokeLater(resetPersistentImage::run);
		});

		chartDataArea.addListener((area) -> {
			SwingUtilities.invokeLater(() -> {
				/*
				 * Align the waterfall plot and the spectrum chart
				 */
				if (waterfallPlot != null)
					waterfallPlot.setDrawingOffsets((int) area.getX(), (int) area.getWidth());

				/**
				 * persistent display config
				 */
				persistentDisplay.setImageSize((int) area.getWidth() / persistentDisplayDownscaleFactor,
						(int) area.getWidth() / persistentDisplayDownscaleFactor);
				if (settings.isPersistentDisplayVisible().getValue()) {
					chart.getXYPlot().setBackgroundImage(persistentDisplay.getDisplayImage().getValue());
					chart.getXYPlot().setBackgroundImageAlpha(alphaPersistentDisplayImage);
				}
			});
		});
	}

	private void startLauncherThread() {
		threadLauncher = new Thread(() -> {
			Thread.currentThread().setName("Launcher-thread");
			while (true) {
				try {
					threadLaunchCommands.take();
					restartHackrfSweepExecute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		threadLauncher.start();
	}

	private void queueTvPreviewFrame(BufferedImage img) {
		if (img == null)
			return;
		pendingTvFrame.set(img);
		if (tvFrameUpdateQueued.compareAndSet(false, true))
			SwingUtilities.invokeLater(this::drainTvPreviewFrame);
	}

	private void drainTvPreviewFrame() {
		try
		{
			BufferedImage img = pendingTvFrame.getAndSet(null);
			if (img != null && settingsPanel != null && settings.isListening().getValue()
					&& settings.getListenService().getValue() == hotiron.core.ListenService.TV)
				settingsPanel.tvTunerPanel().setPreviewFrame(img);
		}
		finally
		{
			tvFrameUpdateQueued.set(false);
			if (pendingTvFrame.get() != null && tvFrameUpdateQueued.compareAndSet(false, true))
				SwingUtilities.invokeLater(this::drainTvPreviewFrame);
		}
	}

	private void runTvWatch() {
		hotiron.core.TvChannel ch = hotiron.core.TvChannelPlan
				.clamp(settings.getTvChannel().getValue());
		long loHz = ch.centerHz();
		waterfallPlot.setVideoMode(true, loHz);
		tvEngine.setVolume(settings.getListenVolume().getValue());
		final long[] lastRowMs = { 0L };
		final long[] lastSnapshotMs = { 0L };
		final long[] lastChartMs = { 0L };
		final long[] lastStationMs = { 0L };
		tvEngine.setSpectrumListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastRowMs[0] < 33)
				return;
			lastRowMs[0] = now;
			if (now - lastSnapshotMs[0] >= 100)
			{
				lastSnapshotMs[0] = now;
				TvWatchSpectrum snap = TvWatchSpectrum.fromRow(now, ch.fccChannel, loHz,
						tvEngine.iqSpectrum().sampleRate(), tvEngine.iqSpectrum().binHz(), row);
				snapshotStore.publishTvWatchSpectrum(snap);
				if (now - lastChartMs[0] >= 200)
				{
					lastChartMs[0] = now;
					showTvRfSpectrum(snap);
				}
				if (!snap.isEmpty() && now - lastStationMs[0] >= 200)
				{
					lastStationMs[0] = now;
					java.util.List<hotiron.core.TvStationHit> live =
							hotiron.core.TvChannelPlan.detectStations(snap.mhz, snap.dbfs);
					java.util.List<hotiron.core.TvStationHit> merged = hotiron.core.TvStationDial.mergeLive(
							settings.getDetectedTvStations().getValue(), live,
							snap.mhz[0], snap.mhz[snap.mhz.length - 1]);
					tvStations = merged;
					publishDetectedTvStations(merged);
				}
			}
			waterfallPlot.addVideoFrame(row, hotiron.core.IqSpectrum.DISPLAY_HZ, loHz);
			waterfallPlot.repaint();
			float peak = -150f;
			for (int i = 0; i < row.length; i++)
			{
				if (row[i] > peak)
					peak = row[i];
			}
			final float peakDb = peak;
			final int bins = row.length;
			SwingUtilities.invokeLater(() -> {
				if (sweepStatusBar != null && settings.isListening().getValue())
					sweepStatusBar.setSweepInfo(hotiron.core.IqSpectrum.BIN_HZ, bins,
							waterfallPlot.getFps(), Double.valueOf(peakDb), false, true);
			});
		});
		AudioSink sink = AudioSinks.openPlayback();
		int lna = settings.getGainLNA().getValue();
		int vga = settings.getGainVGA().getValue();
		if (settings.isAutoGain().getValue())
		{
			int total = TvWatchGainPolicy.seed(ch);
			lna = GainPolicy.lnaGain(total);
			vga = GainPolicy.vgaGain(total);
		}
		final int[] ifTotal = { lna + vga };
		final long[] lastTrimMs = { 0 };
		final boolean amp = TvWatchGainPolicy.antennaLna(settings.isAutoGain().getValue(),
				settings.getAntennaLNA().getValue());
		final long watchArmedMs = System.currentTimeMillis();
		tvEngine.start(this::queueTvPreviewFrame, sink);
		javax.swing.Timer hud = new javax.swing.Timer(200, e -> {
			boolean locked = tvEngine.locked();
			float snr = tvEngine.snrDb();
			snapshotStore.publishWatchStats(locked, snr, tvEngine.packets());
			snapshotStore.publishWatchDebug(tvEngine.debug());
			if (settingsPanel != null)
				settingsPanel.tvTunerPanel().setPreviewStatus(WatchHud.text(ch.fccChannel, locked, snr,
						tvEngine.packets(), tvEngine.frames(), tvEngine.previewFrames()));
			if (chartPanel != null)
				chartPanel.repaint();
			if (settings.isAutoGain().getValue())
			{
				hotiron.core.TvWatchDebug d = tvEngine.debug();
				long now = System.currentTimeMillis();
				long sinceArm = now - watchArmedMs;
				long sinceTrim = lastTrimMs[0] == 0 ? sinceArm : now - lastTrimMs[0];
				if (sinceArm >= TvWatchGainPolicy.FIRST_TRIM_MS
						&& sinceTrim >= TvWatchGainPolicy.TRIM_SETTLE_MS
						&& !TvWatchGainPolicy.shouldHoldIf(d.locked, d.rsGoodWindow, d.rmsIq)
						&& TvWatchGainPolicy.shouldTrimIf(ifTotal[0], d.rmsIq))
				{
					int next = TvWatchGainPolicy.retune(ifTotal[0], d.rmsIq);
					System.err.println("ATSC watch: IF " + ifTotal[0] + " -> " + next
							+ String.format(java.util.Locale.US, " (rms=%.3f)", d.rmsIq));
					ifTotal[0] = next;
					lastTrimMs[0] = now;
					tvEngine.requestGains(GainPolicy.lnaGain(next), GainPolicy.vgaGain(next));
				}
			}
		});
		hud.start();
		snapshotStore.publishContext(settings, fmStations, 0);
		if (chartPanel != null)
			SwingUtilities.invokeLater(chartPanel::repaint);
		try {
			System.err.println("ATSC watch: ch " + ch.fccChannel + " LO " + loHz + " Hz LNA " + lna
					+ " VGA " + vga + " amp=" + (amp ? "on" : "off"));
			HackRFFmNativeBridge.configure(settings.getSelectedSerial().getValue(),
					settings.getClkoutEnable().getValue());
			HackRFFmNativeBridge.start(iq -> tvEngine.offerIq(iq), loHz,
					hotiron.core.TvChannelPlan.IQ_RATE_HZ, lna, vga,
					settings.getAntennaPowerEnable().getValue(), amp);
		} finally {
			hud.stop();
			tvEngine.setSpectrumListener(null);
			tvEngine.stop();
			pendingTvFrame.set(null);
			snapshotStore.publishTvWatchSpectrum(TvWatchSpectrum.empty());
			waterfallPlot.setVideoMode(false, 0);
			if (chart != null)
				SwingUtilities.invokeLater(() -> chart.getXYPlot().getDomainAxis()
						.setRange(getFreq().getStartMHz(), getFreq().getEndMHz()));
			if (settingsPanel != null)
				SwingUtilities.invokeLater(() -> settingsPanel.tvTunerPanel().setWatching(false));
		}
	}

	private void showFmRfSpectrum(FmListenSpectrum snap) {
		if (snap == null || snap.isEmpty())
			return;
		final double high = Math.min(0d, Math.ceil((snap.peakDbfs + 5d) / 10d) * 10d);
		double candidateLow = Math.floor((snap.noiseDbfs - 10d) / 10d) * 10d;
		if (!Double.isFinite(candidateLow))
			candidateLow = -100d;
		final double low = Math.min(candidateLow, high - 40d);
		final XYSeriesImmutable rfSeries = new XYSeriesImmutable("FM RF", snap.mhz, snap.dbfs);
		SwingUtilities.invokeLater(() -> {
			if (chart == null || !settings.isListening().getValue()
					|| settings.getListenService().getValue() != hotiron.core.ListenService.FM
					|| Math.abs(settings.getListenKHz().getValue() / 1000.0 - snap.dialMHz) > 0.001)
				return;
			chart.setNotify(false);
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(snap.mhz[0],
					snap.mhz[snap.mhz.length - 1] + snap.binHz / 1_000_000d);
			plot.getRangeAxis().setRange(low, high);
			chartDataset.removeAllSeries();
			chartDataset.addSeries(parkedRfPeaksEmpty);
			chartDataset.addSeries(rfSeries);
			chart.setNotify(true);
		});
	}

	private void showTvRfSpectrum(TvWatchSpectrum snap) {
		if (snap == null || snap.isEmpty())
			return;
		final double high = Math.min(0d, Math.ceil((snap.peakDbfs + 5d) / 10d) * 10d);
		double candidateLow = Math.floor((snap.noiseDbfs - 10d) / 10d) * 10d;
		if (!Double.isFinite(candidateLow))
			candidateLow = -100d;
		final double low = Math.min(candidateLow, high - 40d);
		final XYSeriesImmutable rfSeries = new XYSeriesImmutable("TV RF", snap.mhz, snap.dbfs);
		SwingUtilities.invokeLater(() -> {
			if (chart == null || !settings.isListening().getValue()
					|| settings.getListenService().getValue() != hotiron.core.ListenService.TV
					|| settings.getTvChannel().getValue() != snap.tvChannel)
				return;
			chart.setNotify(false);
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(snap.mhz[0],
					snap.mhz[snap.mhz.length - 1] + snap.binHz / 1_000_000d);
			plot.getRangeAxis().setRange(low, high);
			chartDataset.removeAllSeries();
			chartDataset.addSeries(parkedRfPeaksEmpty);
			chartDataset.addSeries(rfSeries);
			chart.setNotify(true);
		});
	}

	private void runFmListen() {
		FmChannel ch = FmChannelPlan.clamp(settings.getListenKHz().getValue() / 1000.0);
		long loHz = (long) ch.centerKHz * 1000L - WfmDemodulator.OFFSET_HZ;
		if (loHz < 1_000_000L)
			loHz = 1_000_000L;
		final long captureCenterHz = loHz;
		fmEngine.setVolume(settings.getListenVolume().getValue());
		waterfallPlot.setAudioMode(true);
		final long[] lastRowMs = { 0L };
		final long[] lastRfMs = { 0L };
		final long[] lastStationMs = { 0L };
		fmEngine.setRfSpectrumListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastRfMs[0] < 50)
				return;
			lastRfMs[0] = now;
			FmListenSpectrum snap = FmListenSpectrum.fromRow(now, ch.centerMHz(), captureCenterHz,
					fmEngine.rfSpectrum().sampleRate(), fmEngine.rfSpectrum().binHz(), row);
			snapshotStore.publishFmListenSpectrum(snap);
			showFmRfSpectrum(snap);
			if (!snap.isEmpty() && now - lastStationMs[0] >= 200)
			{
				lastStationMs[0] = now;
				java.util.List<FmStationHit> live = FmChannelPlan.detectStations(snap.mhz, snap.dbfs);
				java.util.List<FmStationHit> merged = FmStationDial.mergeLive(
						settings.getDetectedFmStations().getValue(), live,
						snap.mhz[0], snap.mhz[snap.mhz.length - 1]);
				fmStations = merged;
				publishDetectedStations(merged);
			}
		});
		fmEngine.setSpectrumListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastRowMs[0] < 33)
				return;
			lastRowMs[0] = now;
			waterfallPlot.addAudioFrame(row, AudioSpectrum.DISPLAY_HZ);
			waterfallPlot.repaint();
			float peak = -150f;
			for (int i = 0; i < row.length; i++)
			{
				if (row[i] > peak)
					peak = row[i];
			}
			final float peakDb = peak;
			final int bins = row.length;
			SwingUtilities.invokeLater(() -> {
				if (sweepStatusBar != null && settings.isListening().getValue())
					sweepStatusBar.setSweepInfo(AudioSpectrum.BIN_HZ, bins, waterfallPlot.getFps(),
							Double.valueOf(peakDb), true);
			});
		});
		AudioSink sink = AudioSinks.openPlayback();
		fmAudioOk = !(sink instanceof RecordingAudioSink);
		fmEngine.start(sink);
		snapshotStore.publishContext(settings, fmStations, 0);
		if (chartPanel != null)
			SwingUtilities.invokeLater(chartPanel::repaint);
		try {
			int lna = settings.getGainLNA().getValue();
			int vga = settings.getGainVGA().getValue();
			if (settings.isAutoGain().getValue())
			{
				int total = FmListenGainPolicy.seed(ch);
				lna = GainPolicy.lnaGain(total);
				vga = GainPolicy.vgaGain(total);
			}
			System.err.println("FM listen: " + String.format(java.util.Locale.US, "%.1f", ch.centerMHz())
					+ " MHz LO " + loHz + " Hz LNA " + lna + " VGA " + vga);
			HackRFFmNativeBridge.configure(settings.getSelectedSerial().getValue(),
					settings.getClkoutEnable().getValue());
			HackRFFmNativeBridge.start(iq -> fmEngine.offerIq(iq), loHz, WfmDemodulator.IQ_RATE_HZ,
					lna, vga,
					settings.getAntennaPowerEnable().getValue(), settings.getAntennaLNA().getValue());
		} finally {
			fmEngine.setRfSpectrumListener(null);
			fmEngine.setSpectrumListener(null);
			fmEngine.stop();
			snapshotStore.publishFmListenSpectrum(FmListenSpectrum.empty());
			waterfallPlot.setAudioMode(false);
			if (chart != null)
				SwingUtilities.invokeLater(() -> chart.getXYPlot().getDomainAxis()
						.setRange(getFreq().getStartMHz(), getFreq().getEndMHz()));
		}
	}

	/**
	 * no need to synchronize, executes only in launcher thread
	 */
	private void stopHackrfSweep() {
		forceStopSweep = true;
		if (sweepEngine != null)
			sweepEngine.requestStop();
		HackRFFmNativeBridge.stop();
		fmEngine.stop();
		tvEngine.stop();
		if (threadHackrfSweep != null) {
			while (threadHackrfSweep.isAlive()) {
				forceStopSweep = true;
				HackRFSweepNativeBridge.stop();
				HackRFFmNativeBridge.stop();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
			}
			try {
				threadHackrfSweep.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threadHackrfSweep = null;
		}
		if (settings.isListening().getValue())
			System.out.println("QRX parked IQ.");
		else
			System.out.println("sweep QRT.");
		if (threadProcessing != null) {
			threadProcessing.interrupt();
			try {
				threadProcessing.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threadProcessing = null;
			System.out.println("processing QRT.");
		}
	}

	private void sweep() throws IOException {
		lock.lock();
		try {
			threadProcessing = new Thread(() -> {
				Thread.currentThread().setName("hackrf_sweep data processing thread");
				sweepEngine.runProcessingLoop();
			});
			threadProcessing.start();

			refreshRadioIdentity();
			System.out.println(
					"QRV sweep " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz() + " MHz");
			System.out.println("sweep params:  freq " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz()
					+ "MHz  FFTBin " + settings.getFFTBinHz().getValue() + "Hz  samples " + settings.getSamples().getValue()
					+ "  lna: " + settings.getGainLNA().getValue() + " vga: " + settings.getGainVGA().getValue() + " antPwr:"
					+ settings.getAntennaPowerEnable().getValue() + " antLNA:" + settings.getAntennaLNA().getValue());
			fireHardwareStateChanged(false);
			sweepEngine.runSweepLoop();
			fireHardwareStateChanged(false);
		} finally {
			lock.unlock();
			fireHardwareStateChanged(false);
		}
	}

	protected void redrawFrequencySpectrumTable() {
		Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
		FrequencyAllocationTable activeTable = settings.getFrequencyAllocationTable().getValue();
		if (activeTable == null) {
			imageFrequencyAllocationTableBands = null;
		} else if (area.getWidth() > 0 && area.getHeight() > 0) {
			imageFrequencyAllocationTableBands = activeTable.drawAllocationTable((int) area.getWidth(),
					(int) area.getHeight(), alphaFreqAllocationTableBandsImage, getFreq().getStartMHz() * 1000000l,
					getFreq().getEndMHz() * 1000000l,
					//colors.palette4, 
					Color.white,
					//colors.palette1
					Color.DARK_GRAY);
		}
	}
}
