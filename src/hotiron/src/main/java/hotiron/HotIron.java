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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JSplitPane;

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
import hotiron.core.BleSniffEngine;
import hotiron.core.NfcActivity;
import hotiron.core.NfcFrame;
import hotiron.core.NfcSniffEngine;
import hotiron.core.NfcSniffGainPolicy;
import hotiron.core.FrequencyAxis;
import hotiron.core.FrequencyAllocationTable;
import hotiron.core.FrequencyAllocations;
import hotiron.core.FrequencyBand;
import hotiron.core.FrequencyRange;
import hotiron.core.AutoGainPolicy;
import hotiron.core.GainPolicy;
import hotiron.core.RadioCoordinator;
import hotiron.core.RadioHotPlug;
import hotiron.core.RadioMode;
import hotiron.core.RadioSession;
import hotiron.core.StationDetectSink;
import hotiron.core.SweepLiveLoop;
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
import hotiron.nativebridge.NfcDecNative;
import hotiron.ui.BandHeaderPainter;
import hotiron.ui.HackRFSweepSettingsUI;
import hotiron.ui.ListenHud;
import hotiron.ui.OperatorLayout;
import hotiron.ui.OperatorShell;
import hotiron.ui.NfcSniffHud;
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
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if ("capturegif".equals(args[i]))
					captureGIF = true;
			}
		}
		hotiron.mcp.McpFlags mcp = hotiron.mcp.McpFlags.parse(args);
		//		try { Thread.sleep(20000); System.out.println("Started..."); } catch (InterruptedException e) {}

		hotiron.ui.AnalyzerLookAndFeel.install();
		HotIron app = new HotIron();
		if (mcp.tcp)
			app.startMcpTcp(mcp.port);
		if (mcp.stdio)
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
	private SpectrumPowerScale						powerScale;
	private final hotiron.mcp.SpectrumSnapshotStore snapshotStore = new hotiron.mcp.SpectrumSnapshotStore();
	private hotiron.mcp.SpectrumMcpServer	mcpServer;
	private final AutoGainPolicy.Loop				autoGainLoop						= new AutoGainPolicy.Loop();
	private RadioCoordinator radio;
	private RadioSession radioSession;
	private final RadioHotPlug radioHotPlug = new RadioHotPlug();
	private Thread usbWatch;
	private volatile boolean						forceStopSweep						= false;
	/**
	 * Capture a GIF of the program for the GITHUB page
	 */
	private ScreenCapture							gifCap								= null;
	private final AnalyzerSettings					settings							= new AnalyzerSettings();
	private BufferedImage							imageFrequencyAllocationTableBands	= null;
	private ReentrantLock							lock								= new ReentrantLock();

	private volatile List<FmStationHit>				fmStations							= List.of();
	private volatile NfcActivity					nfcActivity							= NfcActivity.quiet();
	private volatile List<hotiron.core.TvStationHit> tvStations = List.of();
	private final StationDetectSink stationDetect = new StationDetectSink();
	private hotiron.core.TvQualifySession tvQualify;
	private long tvWatchEnteredMs;
	private boolean advancingTvQualify;
	private final hotiron.core.BandScanSession scanSession = new hotiron.core.BandScanSession();
	private SweepLiveLoop sweepLive;
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
	private javax.swing.Timer					radioApplyTimer;
	private Thread									threadProcessing;
	private TextTitle								titleFreqBand						= new TextTitle("",
			new Font("Dialog", Font.PLAIN, 11));
	private RuntimePerformanceWatch					perfWatch							= new RuntimePerformanceWatch();
	private JFrame									uiFrame;
	private WaterfallPlot							waterfallPlot;
	private WaterfallPlot							listenRfWaterfall;
	private JSplitPane								listenWaterfalls;
	private boolean									listenDualWaterfalls;
	private JSplitPane								splitPane;
	private HackRFSweepSettingsUI					settingsPanel;
	private JLabel labelMessages;
	private SweepStatusBar sweepStatusBar;
	private final FmListenEngine fmEngine = new FmListenEngine();
	private final NfcSniffEngine nfcEngine = new NfcSniffEngine();
	private final Object bleLock = new Object();
	private volatile BleSniffEngine bleEngine;
	private volatile NfcFrame lastNfcFrame;
	private volatile boolean nfcFieldOn;
	private volatile boolean fmAudioOk;

	public HotIron() {
		hotiron.ui.AnalyzerLookAndFeel.install();
		radioApplyTimer = new javax.swing.Timer(SweepConfig.FREQUENCY_APPLY_DEBOUNCE_MS, e -> {
			if (radioSession != null)
				radioSession.applyNow();
		});
		radioApplyTimer.setRepeats(false);
		radioSession = new RadioSession(settings, new RadioSession.Driver() {
			@Override
			public void stopAndJoin() {
				stopHackrfSweep();
			}

			@Override
			public void abort() {
				forceStopSweep = true;
				if (sweepEngine != null)
					sweepEngine.requestStop();
				HackRFSweepNativeBridge.stop();
				HackRFFmNativeBridge.stop();
				fmEngine.stop();
				tvEngine.stop();
				nfcEngine.stop();
				if (threadHackrfSweep != null) {
					try {
						threadHackrfSweep.join(2000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			@Override
			public void prepareSweep() {
				radio.applyAutoSweep(settings.getFrequency().getValue(), false);
			}

			@Override
			public void startExclusive(RadioMode mode) {
				startRadioThread(mode);
			}
		}, new RadioSession.Debounce() {
			@Override
			public void restart() {
				if (SwingUtilities.isEventDispatchThread())
					radioApplyTimer.restart();
				else
					SwingUtilities.invokeLater(radioApplyTimer::restart);
			}

			@Override
			public void stop() {
				radioApplyTimer.stop();
			}
		});
		radio = new RadioCoordinator(settings, new RadioCoordinator.Usb() {
			@Override
			public void applyNow() {
				radioSession.applyNow();
			}

			@Override
			public void applyDebounced() {
				radioSession.applyDebounced();
			}
		}, autoGainLoop);
		settings.setHardware(new AnalyzerSettings.Hardware() {
			@Override
			public void restartSweep() {
				radioSession.cancelDebounce();
				radioSession.applyNow();
			}

			@Override
			public void releaseRadio() {
				radioSession.release();
				refreshRadioIdentity();
			}

			@Override
			public void startListen() {
				radioSession.cancelDebounce();
				radioSession.applyNow();
			}

			@Override
			public void startWatch() {
				radioSession.cancelDebounce();
				radioSession.applyNow();
			}

			@Override
			public void startSniff() {
				radioSession.cancelDebounce();
				radioSession.applyNow();
			}

			@Override
			public void startBleSniff() {
				startBleSniffSession();
			}

			@Override
			public void stopBleSniff() {
				stopBleSniffSession();
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
			settings.getFrequencyAllocationTable().setValue(FrequencyAllocations.defaultTable());
		}

		if (settings.isAutoGain().getValue()) {
			FrequencyRange boot = settings.getFrequency().getValue();
			int seed = AutoGainPolicy.seedGain(boot.getStartMHz(), boot.getEndMHz());
			settings.getGain().setValue(seed);
			radio.recalculateGains(seed);
		} else {
			radio.recalculateGains(settings.getGain().getValue());
		}

		setupChart();

		setupChartMouseMarkers();
		setupSpectrumZoom();

		waterfallPlot = new WaterfallPlot(chartPanel, 300);
		listenRfWaterfall = new WaterfallPlot(chartPanel, 300);
		listenRfWaterfall.setAlignToChart(false);

		refreshRadioIdentity();
		settingsPanel = new HackRFSweepSettingsUI(settings);

		splitPane = OperatorShell.verticalPlots(
				OperatorShell.spectrumStack(settingsPanel.chartToggleBar(), chartPanel), waterfallPlot);

		labelMessages = new JLabel("dsadasd");
		labelMessages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		settings.isDebugDisplay().addListener((debug) -> {
			labelMessages.setVisible(debug);
		});
		settings.isDebugDisplay().callObservers();
		
		JPanel splitPanePanel	= new JPanel(new BorderLayout());
		splitPanePanel.add(OperatorShell.fieldOfPlay(splitPane, settingsPanel.gainRail()), BorderLayout.CENTER);
		splitPanePanel.add(labelMessages, BorderLayout.SOUTH);

		uiFrame = new JFrame();
		uiFrame.setUndecorated(captureGIF);
		uiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		uiFrame.setLayout(new BorderLayout());
		uiFrame.setTitle("HotIron");
		((javax.swing.JComponent) uiFrame.getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 16, 8));
		uiFrame.setResizable(true);
		sweepStatusBar = settingsPanel.footer();
		settings.getMcpStatus().addListener(s -> javax.swing.SwingUtilities.invokeLater(() -> sweepStatusBar.setMcp(s)));
		sweepStatusBar.setMcp(settings.getMcpStatus().getValue());
		OperatorShell.place(uiFrame, settingsPanel.navBanner(), splitPanePanel, settingsPanel, sweepStatusBar);
		applyAppIcons(uiFrame);
		
		setupFrequencyAllocationTable();
		
		uiFrame.pack();
		uiFrame.setMinimumSize(OperatorLayout.minFrame());
		uiFrame.setResizable(true);
		placeInitialWindow(uiFrame);
		uiFrame.setVisible(true);
		SwingUtilities.invokeLater(() -> OperatorShell.applyPlotSplit(splitPane));

		sweepEngine = new SpectrumSweepEngine(settings, spectrumInitValue, new SweepUiHooks());
		radioSession.startLauncher();
		radio.applyAutoSweep(settings.getFrequency().getValue(), false);
		radioSession.applyNow();
		startUsbWatch();

		/**
		 * register parameter observers
		 */
		setupParameterObservers();

		//shutdown on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			stopUsbWatch();
			if (radioSession != null)
				radioSession.stopLauncher();
			stopHackrfSweep();
			stopBleSniffSession();
		}));

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
		}, () -> runOnEdt(settings::startSniff), on -> runOnEdt(() -> {
			settings.isAutoGain().setValue(on);
			snapshotStore.publishContext(settings, fmStations, 0);
		}), () -> runOnEdt(settings::restartSweep), on -> runOnEdt(() -> {
			if (on)
				settings.startBleSniff();
			else
				settings.stopBleSniff();
		}));
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
	 * Size to the monitor that owns the window, not the WSLg virtual desktop.
	 * A spanned desktop (e.g. 15360x2160) still uses a compact fallback so
	 * maximize does not produce a gray empty frame. Keep packed height when
	 * that fallback is used so the settings panel is not immediately scrolled.
	 */
	private static void placeInitialWindow(JFrame frame) {
		Rectangle screen;
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			screen = ge.getMaximumWindowBounds();
			if (screen == null || screen.width <= 0 || screen.height <= 0 || spannedDesktop(screen)) {
				screen = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
			}
		} catch (Exception e) {
			screen = new Rectangle(0, 0, 1920, 1080);
		}
		Dimension size = initialWindowSize(screen, frame.getSize());
		int w = size.width;
		int h = size.height;
		frame.setExtendedState(Frame.NORMAL);
		frame.setSize(w, h);
		int x = screen.x + 40;
		int y = screen.y + 40;
		if (x + w > screen.x + screen.width && screen.width > w)
			x = screen.x + Math.max(0, (screen.width - w) / 2);
		frame.setLocation(x, y);
	}

	static boolean spannedDesktop(Rectangle screen) {
		Rectangle bounds = screen == null ? new Rectangle() : screen;
		return bounds.width > 7680 || (bounds.height > 0 && bounds.width > bounds.height * 4);
	}

	static Dimension initialWindowSize(Rectangle screen, Dimension packed) {
		Rectangle bounds = screen == null ? new Rectangle(0, 0, 1920, 1080) : screen;
		Dimension preferred = packed == null ? new Dimension() : packed;
		boolean spanned = spannedDesktop(bounds);

		int w = spanned ? 1600 : Math.max(1000, bounds.width - 80);
		int h = spanned ? Math.max(900, preferred.height) : Math.max(700, bounds.height - 80);
		if (bounds.width > 0)
			w = Math.min(w, Math.max(900, bounds.width - 80));
		if (bounds.height > 0)
			h = Math.min(h, Math.max(560, bounds.height - 80));
		return new Dimension(w, h);
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

	private void startUsbWatch() {
		if (usbWatch != null)
			return;
		usbWatch = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					boolean released = Boolean.TRUE.equals(settings.isRadioReleased().getValue());
					RadioIdentity id = settings.getRadioIdentity().getValue();
					boolean present = id != null && id.present;
					java.util.List<String> serials;
					if (present && !released)
						serials = HackRFDeviceQuery.usbEnumerated() ? java.util.List.of("enumerated")
								: java.util.List.of();
					else
						serials = HackRFDeviceQuery.listSerials();
					RadioHotPlug.Action action = radioHotPlug.observe(serials, released, present);
					if (action != RadioHotPlug.Action.IDLE)
						SwingUtilities.invokeLater(() -> applyUsbHotPlug(action));
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (Throwable t) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		}, "usb-hotplug");
		usbWatch.setDaemon(true);
		usbWatch.start();
	}

	private void stopUsbWatch() {
		Thread t = usbWatch;
		if (t == null)
			return;
		t.interrupt();
		usbWatch = null;
	}

	private void applyUsbHotPlug(RadioHotPlug.Action action) {
		if (action == RadioHotPlug.Action.MARK_ABSENT) {
			settings.getRadioIdentity().setValue(RadioIdentity.ABSENT);
			return;
		}
		if (action == RadioHotPlug.Action.START) {
			System.out.println("USB hotplug: starting sweep");
			settings.restartSweep();
		}
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

	private final class SweepUiHooks implements SpectrumSweepEngine.Hooks {
		private final XYSeries spectrumPeaksEmpty = new XYSeries("peaks");

		SweepUiHooks() {
			sweepLive = new SweepLiveLoop(settings, stationDetect, scanSession, new SweepLiveLoop.Publish() {
				@Override
				public boolean shouldPublish(long nowMs) {
					return snapshotStore.shouldPublish(nowMs);
				}

				@Override
				public void publish(hotiron.core.DatasetSpectrum ds, java.util.List<FmStationHit> fmHits,
						double sweepsPerSec, long nowMs) {
					snapshotStore.publishSweep(hotiron.mcp.SpectrumSnapshot.fromDataset(ds, nowMs,
							hotiron.mcp.SpectrumSnapshot.DEFAULT_MAX_POINTS, null), nowMs);
					snapshotStore.publishContext(settings, fmHits, sweepsPerSec);
					snapshotStore.publishNfc(stationDetect.lastNfc());
				}
			}, new SweepLiveLoop.Hooks() {
				@Override
				public void onAxisChanged(hotiron.core.DatasetSpectrum ds) {
					powerScale = null;
					if (waterfallPlot != null)
						waterfallPlot.clearHistory();
				}

				@Override
				public void onPaint(DatasetSpectrumPeak ds, FrequencyRange view, long nowMs, int frame) {
					paintFullSweep(ds, view, nowMs, frame, spectrumPeaksEmpty);
				}

				@Override
				public double sweepsPerSec() {
					return waterfallPlot != null ? waterfallPlot.getFps() : 0;
				}

				@Override
				public void clearWaterfall() {
					if (waterfallPlot != null)
						waterfallPlot.clearHistory();
				}

				@Override
				public void retune(FrequencyRange next) {
					settings.getFrequency().setValue(next);
				}

				@Override
				public void finishScan() {
					hotiron.core.BandScan kind = settings.getBandScan().getValue();
					settings.stopScan();
					if (kind == hotiron.core.BandScan.TV)
						maybeStartTvQualify();
				}
			});
		}

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
			datasetSpectrum = ds;
			sweepLive.accept(ds, getFreq(), System.currentTimeMillis());
			fmStations = stationDetect.lastFm();
			tvStations = stationDetect.lastTv();
			nfcActivity = stationDetect.lastNfc();
			synchronized (perfWatch) {
				perfWatch.hwFullSpectrumRefreshes++;
			}
		}
	}

	private void paintFullSweep(DatasetSpectrumPeak ds, FrequencyRange sweepRange, long nowMs, int frame,
			XYSeries spectrumPeaksEmpty) {
		radio.considerAutoGain(ds, sweepRange, scanSession.active(), nowMs);

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
		XYSeries spectrumSeries = ds.createSpectrumDataset("spectrum", maxPts);
		XYSeries spectrumPeaks = settings.isChartsPeaksVisible().getValue()
				? ds.createPeaksDataset("peaks", maxPts)
				: spectrumPeaksEmpty;
		final double yLow;
		final double yHigh;
		if (settings.isPowerAutoScale().getValue()) {
			SpectrumPowerScale target = SpectrumPowerScale.fromDataset(ds);
			if (powerScale == null || powerScale.isUnset())
				powerScale = (target.isUnset() ? SpectrumPowerScale.defaults() : target.displayTicks())
						.stamped(nowMs);
			else
				powerScale = powerScale.follow(target, nowMs);
			yLow = powerScale.lowDb;
			yHigh = powerScale.highDb;
		} else {
			powerScale = SpectrumPowerScale.defaults();
			yLow = SpectrumPowerScale.DEFAULT_LOW;
			yHigh = SpectrumPowerScale.DEFAULT_HIGH;
		}

		if (settings.isPersistentDisplayVisible().getValue()) {
			long start = System.nanoTime();
			boolean redraw = frame % 2 == 0;
			persistentDisplay.drawSpectrumFloat(ds, (float) yLow, (float) yHigh, redraw);
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
			waterfallPlot.addNewData(ds);
			synchronized (perfWatch) {
				perfWatch.waterfallUpdate.addDrawingTime(System.nanoTime() - start);
			}
			waterfallPlot.repaint();
		}

		final double rbwHz = ds.getFFTBinSizeHz();
		final int bins = ds.spectrumLength();
		final double fps = waterfallPlot.getFps();
		final Double peakDbm = Double.valueOf(ds.calculateSpectrumPeakPower());
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

	private int chartVertexBudget() {
		Rectangle2D area = chartDataArea.getValue();
		if (area != null && area.getWidth() > 8)
			return Math.max(256, (int) Math.round(area.getWidth()));
		return 2048;
	}

	private void startRadioThread(RadioMode mode) {
		final boolean watch = mode == RadioMode.WATCH;
		final boolean listen = mode == RadioMode.LISTEN;
		final boolean nfc = mode == RadioMode.NFC;
		threadHackrfSweep = new Thread(() -> {
			Thread.currentThread().setName(watch ? "hackrf_tv"
					: (listen ? "hackrf_fm" : (nfc ? "hackrf_nfc" : "hackrf_sweep")));
			try {
				forceStopSweep = false;
				if (sweepEngine != null)
					sweepEngine.clearStop();
				if (watch)
					runTvWatch();
				else if (listen)
					runFmListen();
				else if (nfc)
					runNfcSniff();
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
		chartLineRenderer.setDefaultStroke(new BasicStroke(OperatorLayout.SPECTRUM_LINE_WIDTH));

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
				boolean nfcParked = settings.isListening().getValue()
						&& settings.getListenService().getValue() == hotiron.core.ListenService.NFC;
				if (nfcParked)
				{
					double start = xy.getDomainAxis().getLowerBound();
					double end = xy.getDomainAxis().getUpperBound();
					hotiron.ui.NfcChannelOverlay.paint(g2, area, start, end, nfcActivity);
					NfcSniffHud.paint(g2, area, lastNfcFrame, nfcFieldOn);
					return;
				}
				BufferedImage img = imageFrequencyAllocationTableBands;
				if (img != null) {
					g2.drawImage(img, (int) area.getX(), (int) area.getY(), null);
				}
				FrequencyRange range = getFreq();
				QuickSelectBandOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz());
				WifiChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz());
				hotiron.ui.BleChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz());
				FmChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz(), fmStations,
						settings.getListenKHz().getValue());
				hotiron.ui.TvChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz(), tvStations,
						settings.getTvChannel().getValue());
				hotiron.ui.NfcChannelOverlay.paint(g2, area, range.getStartMHz(), range.getEndMHz(), nfcActivity);
				if (hotiron.core.NfcBandLayer.tagsReadable(range.getStartMHz(), range.getEndMHz())
						&& !settings.isListening().getValue())
					hotiron.ui.NfcHud.paint(g2, area, nfcActivity, settings.getBandScan().getValue());
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
		java.util.List<BandMark> nfcMarks = hotiron.core.NfcBandLayer.marks(axis, nfcActivity);
		BandMark nfcHit = BandHeaderPainter.hitTest(e.getX(), e.getY(), area, axis, nfcMarks);
		if (nfcHit != null)
		{
			settings.startNfcScan();
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
		radio.bind();
		settings.getFrequency().addListener(this::cancelScanIfRangeLeft);
		settings.isTvQualifying().addListener(on -> {
			if (!Boolean.TRUE.equals(on) && tvQualify != null)
				tvQualify.cancel();
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
				stationDetect.resetFm();
			else if (scan == hotiron.core.BandScan.TV)
				stationDetect.resetTv();
			else if (scan == hotiron.core.BandScan.NFC)
				stationDetect.resetNfc();
		});
		settings.getFrequency().addListener((range) -> {
			flushPersistentOverlay();
			if (chart != null)
				chart.getXYPlot().getDomainAxis().setRange(range.getStartMHz(), range.getEndMHz());
			if (!applyingSpectrumZoom)
				spectrumZoomHistory.clear();
			radio.maybeSeedAutoGain(range, scanSession.active(), System.currentTimeMillis());
		});
		settings.getListenKHz().addListener(() -> {
			flushPersistentOverlay();
			snapshotStore.publishContext(settings, fmStations, 0);
			if (chartPanel != null)
				SwingUtilities.invokeLater(chartPanel::repaint);
		});
		settings.getTvChannel().addListener(() -> {
			flushPersistentOverlay();
			snapshotStore.publishContext(settings, fmStations, 0);
			if (chartPanel != null)
				SwingUtilities.invokeLater(chartPanel::repaint);
			if (!advancingTvQualify && tvQualify != null && tvQualify.active()
					&& tvQualify.currentFcc() != settings.getTvChannel().getValue())
				abortTvQualifyKeepWatch();
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
			DatasetSpectrumPeak p = datasetSpectrum;
			if (p != null)
				p.setPeakFalloutMillis(fallRate * 1000l);
		});

		chartLineRenderer.setDefaultStroke(new BasicStroke(OperatorLayout.SPECTRUM_LINE_WIDTH));
		
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
				if (listenDualWaterfalls)
				{
					if (listenRfWaterfall != null)
						listenRfWaterfall.layoutToOwnWidth();
					if (waterfallPlot != null)
						waterfallPlot.layoutToOwnWidth();
				}
				else if (waterfallPlot != null)
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
		enterWatchWaterfalls(loHz);
		tvEngine.setVolume(settings.getListenVolume().getValue());
		final long[] lastRfMs = { 0L };
		final long[] lastSnapshotMs = { 0L };
		final long[] lastStationMs = { 0L };
		tvEngine.setSpectrumListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastRfMs[0] < 50)
				return;
			lastRfMs[0] = now;
			TvWatchSpectrum snap = TvWatchSpectrum.fromRow(now, ch.fccChannel, loHz,
					tvEngine.iqSpectrum().sampleRate(), tvEngine.iqSpectrum().binHz(), row);
			if (now - lastSnapshotMs[0] >= 100)
			{
				lastSnapshotMs[0] = now;
				snapshotStore.publishTvWatchSpectrum(snap);
			}
			showTvRfSpectrum(snap);
			pushParkedRfWaterfall(row, tvEngine.iqSpectrum().sampleRate(), loHz);
			if (!snap.isEmpty() && now - lastStationMs[0] >= 200)
			{
				lastStationMs[0] = now;
				java.util.List<hotiron.core.TvStationHit> live =
						hotiron.core.TvChannelPlan.detectStations(snap.mhz, snap.dbfs);
				java.util.List<hotiron.core.TvStationHit> merged = hotiron.core.TvStationDial.mergeLive(
						settings.getDetectedTvStations().getValue(), live,
						snap.mhz[0], snap.mhz[snap.mhz.length - 1]);
				tvStations = merged;
				StationDetectSink.publishTv(settings, merged);
			}
		});
		final long[] lastAudioMs = { 0L };
		tvEngine.setAudioSpectrumListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastAudioMs[0] < 33)
				return;
			lastAudioMs[0] = now;
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
					sweepStatusBar.setWatchDualInfo(AudioSpectrum.BIN_HZ, bins, waterfallPlot.getFps(),
							Double.valueOf(peakDb));
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
		tvWatchEnteredMs = watchArmedMs;
		tvEngine.start(this::queueTvPreviewFrame, sink);
		javax.swing.Timer hud = new javax.swing.Timer(200, e -> {
			boolean locked = tvEngine.locked();
			float snr = tvEngine.snrDb();
			int frames = tvEngine.frames();
			snapshotStore.publishWatchStats(locked, snr, tvEngine.packets());
			snapshotStore.publishWatchDebug(tvEngine.debug());
			if (frames > 0)
				stampTv(ch.fccChannel, hotiron.core.TvChannelGrade.PICTURE, "picture", frames, snr);
			advanceTvQualify(ch.fccChannel, frames, snr);
			if (settingsPanel != null)
				settingsPanel.tvTunerPanel().setPreviewStatus(WatchHud.text(ch.fccChannel, locked, snr,
						tvEngine.packets(), frames, tvEngine.previewFrames()));
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
			if (tvQualify == null || !tvQualify.active())
			{
				int frames = tvEngine.frames();
				if (frames == 0 && System.currentTimeMillis()
						- tvWatchEnteredMs >= hotiron.core.TvQualifySession.MIN_WATCH_MS_FOR_NO_LOCK)
					stampTv(ch.fccChannel, hotiron.core.TvChannelGrade.NO_LOCK, tvEngine.debug().stage(),
							0, tvEngine.snrDb());
			}
			tvEngine.setSpectrumListener(null);
			tvEngine.setAudioSpectrumListener(null);
			tvEngine.stop();
			pendingTvFrame.set(null);
			snapshotStore.publishTvWatchSpectrum(TvWatchSpectrum.empty());
			leaveWatchWaterfalls();
			if (chart != null)
				SwingUtilities.invokeLater(() -> chart.getXYPlot().getDomainAxis()
						.setRange(getFreq().getStartMHz(), getFreq().getEndMHz()));
			if (settingsPanel != null)
				SwingUtilities.invokeLater(() -> settingsPanel.tvTunerPanel().setWatching(false));
		}
	}

	private void stampTv(int fcc, hotiron.core.TvChannelGrade grade, String stage, int frames, float snrDb)
	{
		java.util.List<hotiron.core.TvStationHit> cur = settings.getDetectedTvStations().getValue();
		java.util.List<hotiron.core.TvStationHit> next = hotiron.core.TvStationDial.stamp(cur, fcc, grade,
				stage, frames, snrDb);
		tvStations = next;
		StationDetectSink.publishTv(settings, next);
	}

	private void maybeStartTvQualify()
	{
		java.util.List<Integer> queue = hotiron.core.TvQualifySession.queue(settings.getDetectedTvStations()
				.getValue());
		if (queue.isEmpty())
			return;
		SwingUtilities.invokeLater(() -> {
			if (Boolean.TRUE.equals(settings.isListening().getValue()))
				return;
			tvQualify = new hotiron.core.TvQualifySession(queue);
			tvQualify.start(System.currentTimeMillis());
			int fcc = tvQualify.currentFcc();
			settings.isTvQualifying().setValue(true);
			settings.getTvQualifyChannel().setValue(fcc);
			advancingTvQualify = true;
			try
			{
				if (settings.getTvChannel().getValue() != fcc)
					settings.getTvChannel().setValue(fcc);
			}
			finally
			{
				advancingTvQualify = false;
			}
			settings.startWatch();
		});
	}

	private void advanceTvQualify(int fcc, int frames, float snrDb)
	{
		hotiron.core.TvQualifySession q = tvQualify;
		if (q == null || !q.active() || q.currentFcc() != fcc)
			return;
		if (!q.shouldAdvance(System.currentTimeMillis(), frames))
			return;
		if (frames == 0)
			stampTv(fcc, hotiron.core.TvChannelGrade.NO_LOCK, tvEngine.debug().stage(), 0, snrDb);
		long now = System.currentTimeMillis();
		if (q.advance(now))
		{
			int next = q.currentFcc();
			settings.getTvQualifyChannel().setValue(next);
			advancingTvQualify = true;
			try
			{
				settings.getTvChannel().setValue(next);
			}
			finally
			{
				advancingTvQualify = false;
			}
		}
		else
			finishTvQualify();
	}

	private void abortTvQualifyKeepWatch()
	{
		if (tvQualify != null)
			tvQualify.cancel();
		tvQualify = null;
		if (Boolean.TRUE.equals(settings.isTvQualifying().getValue()))
			settings.isTvQualifying().setValue(false);
		settings.getTvQualifyChannel().setValue(0);
	}

	private void finishTvQualify()
	{
		if (tvQualify != null)
			tvQualify.cancel();
		tvQualify = null;
		if (Boolean.TRUE.equals(settings.isTvQualifying().getValue()))
			settings.isTvQualifying().setValue(false);
		settings.getTvQualifyChannel().setValue(0);
		if (Boolean.TRUE.equals(settings.isListening().getValue())
				&& settings.getListenService().getValue() == hotiron.core.ListenService.TV)
			settings.stopListen();
	}

	private void showFmRfSpectrum(FmListenSpectrum snap) {
		if (snap == null || snap.isEmpty())
			return;
		paintParkedRf(snap.mhz, snap.dbfs, snap.binHz, () -> settings.isListening().getValue()
				&& settings.getListenService().getValue() == hotiron.core.ListenService.FM
				&& Math.abs(settings.getListenKHz().getValue() / 1000.0 - snap.dialMHz) <= 0.001);
	}

	private void showTvRfSpectrum(TvWatchSpectrum snap) {
		if (snap == null || snap.isEmpty())
			return;
		paintParkedRf(snap.mhz, snap.dbfs, snap.binHz, () -> settings.isListening().getValue()
				&& settings.getListenService().getValue() == hotiron.core.ListenService.TV
				&& settings.getTvChannel().getValue() == snap.tvChannel);
	}

	private void showNfcRfSpectrum(NfcSniffEngine.ViewRow view) {
		if (view == null || view.isEmpty())
			return;
		paintParkedRf(view.mhz, view.dbfs, view.binHz, () -> settings.isListening().getValue()
				&& settings.getListenService().getValue() == hotiron.core.ListenService.NFC);
	}

	/**
	 * Same scrolling RF waterfall for Listen ±2 MHz and Watch ±8 MHz.
	 * Always map through the live (or default −100…+20) dB window — never
	 * the audio palette, and never the leftover sweep −90…−25 scale that
	 * clips an ATSC brick into a slab.
	 */
	private void pushParkedRfWaterfall(float[] row, float sampleRateHz, long centerHz)
	{
		if (listenRfWaterfall == null || row == null || row.length == 0)
			return;
		double lo = SpectrumPowerScale.DEFAULT_LOW;
		double hi = SpectrumPowerScale.DEFAULT_HIGH;
		if (powerScale != null)
		{
			lo = powerScale.lowDb;
			hi = powerScale.highDb;
		}
		listenRfWaterfall.applyPowerWindow(lo, hi);
		listenRfWaterfall.addListenRfFrame(row, sampleRateHz, centerHz);
		listenRfWaterfall.repaint();
	}

	/**
	 * Parked-IQ FFT uses the same peak half-life, persistence overlay, and
	 * snapshot-history ring as the wideband sweep. Listen and Watch also
	 * paint that FFT into a side-by-side RF waterfall next to AUDIO.
	 */
	private void paintParkedRf(float[] mhz, float[] dbfs, float binHz, java.util.function.BooleanSupplier stillLive) {
		if (mhz == null || dbfs == null || mhz.length == 0 || mhz.length != dbfs.length)
			return;
		Integer fallRate = settings.getPeakFallRate().getValue();
		long fallMs = fallRate == null ? 15_000L : fallRate.longValue() * 1000L;
		DatasetSpectrumPeak prev = datasetSpectrum;
		DatasetSpectrumPeak ds = DatasetSpectrumPeak.ingestParkedFrame(prev, mhz, dbfs, binHz, fallMs);
		if (ds == null)
			return;
		if (prev == null || !prev.sameAxisAs(ds))
			powerScale = null;
		datasetSpectrum = ds;

		long nowMs = System.currentTimeMillis();
		if (snapshotStore.shouldPublish(nowMs)) {
			snapshotStore.publishSweep(hotiron.mcp.SpectrumSnapshot.fromDataset(ds, nowMs,
					hotiron.mcp.SpectrumSnapshot.DEFAULT_MAX_POINTS, null), nowMs);
			double sps = waterfallPlot != null ? waterfallPlot.getFps() : 0;
			snapshotStore.publishContext(settings, fmStations, sps);
		}

		final double yLow;
		final double yHigh;
		if (settings.isPowerAutoScale().getValue()) {
			SpectrumPowerScale target = SpectrumPowerScale.fromDataset(ds);
			if (powerScale == null || powerScale.isUnset())
				powerScale = (target.isUnset() ? SpectrumPowerScale.defaults() : target.displayTicks())
						.stamped(nowMs);
			else
				powerScale = powerScale.follow(target, nowMs);
			yLow = powerScale.lowDb;
			yHigh = powerScale.highDb;
		} else {
			powerScale = SpectrumPowerScale.defaults();
			yLow = SpectrumPowerScale.DEFAULT_LOW;
			yHigh = SpectrumPowerScale.DEFAULT_HIGH;
		}

		if (settings.isPersistentDisplayVisible().getValue())
			persistentDisplay.drawSpectrumFloat(ds, (float) yLow, (float) yHigh, true);

		int maxPts = chartVertexBudget();
		final XYSeries spectrumSeries = ds.createSpectrumDataset("spectrum", maxPts);
		final XYSeries spectrumPeaks = settings.isChartsPeaksVisible().getValue()
				? ds.createPeaksDataset("peaks", maxPts)
				: parkedRfPeaksEmpty;
		final double domainLo = mhz[0];
		final double domainHi = mhz[mhz.length - 1] + binHz / 1_000_000d;
		SwingUtilities.invokeLater(() -> {
			if (chart == null || stillLive == null || !stillLive.getAsBoolean())
				return;
			chart.setNotify(false);
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(domainLo, domainHi);
			NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			if (yAxis.getLowerBound() != yLow || yAxis.getUpperBound() != yHigh)
				yAxis.setRange(yLow, yHigh);
			chartDataset.removeAllSeries();
			chartDataset.addSeries(spectrumPeaks);
			chartDataset.addSeries(spectrumSeries);
			chart.setNotify(true);
		});
	}

	private void enterListenWaterfalls(long captureCenterHz)
	{
		enterDualWaterfalls(captureCenterHz, WfmDemodulator.IQ_RATE_HZ, true);
	}

	private void enterWatchWaterfalls(long loHz)
	{
		enterDualWaterfalls(loHz, hotiron.core.TvChannelPlan.IQ_RATE_HZ, true);
	}

	private void enterDualWaterfalls(long centerHz, float spanHz, boolean audioRight)
	{
		runOnEdt(() -> {
			listenDualWaterfalls = true;
			powerScale = null;
			if (waterfallPlot != null)
			{
				waterfallPlot.setAlignToChart(false);
				if (audioRight)
					waterfallPlot.setAudioMode(true);
				else
					waterfallPlot.setVideoMode(true, centerHz);
			}
			if (listenRfWaterfall != null)
			{
				listenRfWaterfall.setListenRfMode(true, centerHz, spanHz);
				listenRfWaterfall.applyPowerWindow(SpectrumPowerScale.DEFAULT_LOW,
						SpectrumPowerScale.DEFAULT_HIGH);
			}
			listenWaterfalls = OperatorShell.listenWaterfalls(listenRfWaterfall, waterfallPlot);
			OperatorShell.showBottom(splitPane, listenWaterfalls);
			int w = listenWaterfalls.getWidth();
			if (w > 40)
				listenWaterfalls.setDividerLocation(w / 2);
			if (listenRfWaterfall != null)
				listenRfWaterfall.layoutToOwnWidth();
			if (waterfallPlot != null)
				waterfallPlot.layoutToOwnWidth();
		});
	}

	private void leaveListenWaterfalls()
	{
		leaveDualWaterfalls();
	}

	private void leaveWatchWaterfalls()
	{
		leaveDualWaterfalls();
	}

	private void leaveDualWaterfalls()
	{
		runOnEdt(() -> {
			listenDualWaterfalls = false;
			if (waterfallPlot != null)
			{
				waterfallPlot.setAudioMode(false);
				waterfallPlot.setVideoMode(false, 0);
				waterfallPlot.setAlignToChart(true);
			}
			if (listenRfWaterfall != null)
				listenRfWaterfall.setListenRfMode(false, 0, 0);
			OperatorShell.showBottom(splitPane, waterfallPlot);
			listenWaterfalls = null;
			Rectangle2D area = chartDataArea.getValue();
			if (area != null && waterfallPlot != null)
				waterfallPlot.setDrawingOffsets((int) area.getX(), (int) area.getWidth());
		});
	}

	private void runFmListen() {
		FmChannel ch = FmChannelPlan.clamp(settings.getListenKHz().getValue() / 1000.0);
		long loHz = (long) ch.centerKHz * 1000L - WfmDemodulator.OFFSET_HZ;
		if (loHz < 1_000_000L)
			loHz = 1_000_000L;
		final long captureCenterHz = loHz;
		fmEngine.setVolume(settings.getListenVolume().getValue());
		enterListenWaterfalls(captureCenterHz);
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
			pushParkedRfWaterfall(row, fmEngine.rfSpectrum().sampleRate(), captureCenterHz);
			if (!snap.isEmpty() && now - lastStationMs[0] >= 200)
			{
				lastStationMs[0] = now;
				java.util.List<FmStationHit> live = FmChannelPlan.detectStations(snap.mhz, snap.dbfs);
				java.util.List<FmStationHit> merged = FmStationDial.mergeLive(
						settings.getDetectedFmStations().getValue(), live,
						snap.mhz[0], snap.mhz[snap.mhz.length - 1]);
				fmStations = merged;
				StationDetectSink.publishFm(settings, merged);
			}
		});
		fmEngine.setLevelListener(level -> SwingUtilities.invokeLater(() -> {
			if (settingsPanel != null)
				settingsPanel.setFmSignalLevel((float) level);
		}));
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
					sweepStatusBar.setListenDualInfo(AudioSpectrum.BIN_HZ, bins, waterfallPlot.getFps(),
							Double.valueOf(peakDb));
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
			fmEngine.setLevelListener(null);
			fmEngine.stop();
			snapshotStore.publishFmListenSpectrum(FmListenSpectrum.empty());
			leaveListenWaterfalls();
			if (chart != null)
				SwingUtilities.invokeLater(() -> chart.getXYPlot().getDomainAxis()
						.setRange(getFreq().getStartMHz(), getFreq().getEndMHz()));
		}
	}

	private void startBleSniffSession()
	{
		synchronized (bleLock)
		{
			stopBleSniffSessionLocked();
			String path = BleSniffEngine.discoverPort();
			if (path == null)
			{
				snapshotStore.publishBleStatus(true, "", "no nRF ACM (attach J-Link 1366:1015)");
				SwingUtilities.invokeLater(() -> {
					if (settingsPanel != null)
						settingsPanel.bleSniffPanel().setStatus("no nRF ACM");
				});
				return;
			}
			try
			{
				BleSniffEngine.Port port = BleSniffEngine.openLinux(path);
				bleEngine = new BleSniffEngine(port, frame -> {
					snapshotStore.publishBleFrame(frame);
					SwingUtilities.invokeLater(() -> {
						if (settingsPanel != null)
							settingsPanel.bleSniffPanel().setFrames(snapshotStore.bleFrames());
					});
				}, status -> {
					snapshotStore.publishBleStatus(true, path, status);
					SwingUtilities.invokeLater(() -> {
						if (settingsPanel != null)
							settingsPanel.bleSniffPanel().setStatus(status);
					});
				});
				snapshotStore.publishBleStatus(true, path, "open " + path);
				bleEngine.start();
			}
			catch (IOException e)
			{
				String msg = e.getMessage() == null ? "port error" : e.getMessage();
				snapshotStore.publishBleStatus(true, path, msg);
				SwingUtilities.invokeLater(() -> {
					if (settingsPanel != null)
						settingsPanel.bleSniffPanel().setStatus(msg);
				});
			}
		}
	}

	private void stopBleSniffSession()
	{
		synchronized (bleLock)
		{
			stopBleSniffSessionLocked();
		}
	}

	private void stopBleSniffSessionLocked()
	{
		BleSniffEngine engine = bleEngine;
		bleEngine = null;
		if (engine != null)
			engine.close();
		snapshotStore.publishBleStatus(false, "", "idle");
		SwingUtilities.invokeLater(() -> {
			if (settingsPanel != null)
			{
				settingsPanel.bleSniffPanel().setSniffing(false);
				settingsPanel.bleSniffPanel().setStatus("idle");
			}
		});
	}

	private void runNfcSniff() {
		lastNfcFrame = null;
		nfcFieldOn = false;
		waterfallPlot.setNfcMode(true);
		final long[] lastRfMs = { 0L };
		final long[] lastEnvMs = { 0L };
		nfcEngine.setRfSpectrumListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastRfMs[0] < 33)
				return;
			lastRfMs[0] = now;
			NfcSniffEngine.ViewRow view = NfcSniffEngine.cropPhy(row, nfcEngine.rfSpectrum().binHz(),
					NfcSniffEngine.LO_HZ);
			showNfcRfSpectrum(view);
			if (view.isEmpty())
				return;
			waterfallPlot.addNfcFrame(view.dbfs, view.mhz[0], view.mhz[view.mhz.length - 1]);
			waterfallPlot.repaint();
			float peak = -150f;
			for (int i = 0; i < view.dbfs.length; i++)
			{
				if (view.dbfs[i] > peak)
					peak = view.dbfs[i];
			}
			final float peakDb = peak;
			final int bins = view.dbfs.length;
			final float binHz = view.binHz;
			SwingUtilities.invokeLater(() -> {
				if (sweepStatusBar != null && settings.isListening().getValue())
					sweepStatusBar.setSweepInfo(binHz, bins, waterfallPlot.getFps(), Double.valueOf(peakDb), false,
							false, true);
			});
		});
		nfcEngine.setEnvelopeListener(row -> {
			long now = System.currentTimeMillis();
			if (now - lastEnvMs[0] < 33)
				return;
			lastEnvMs[0] = now;
			final float[] snap = row;
			SwingUtilities.invokeLater(() -> {
				if (settingsPanel != null)
					settingsPanel.nfcSniffPanel().setEnvelope(snap);
			});
		});
		nfcEngine.setFrameListener(frame -> {
			if (frame.fieldOn())
				nfcFieldOn = true;
			else if (frame.fieldOff())
				nfcFieldOn = false;
			if (!frame.carrier())
				lastNfcFrame = frame;
			snapshotStore.publishNfcFrame(frame);
			final boolean field = nfcFieldOn;
			final NfcFrame shown = lastNfcFrame;
			SwingUtilities.invokeLater(() -> {
				if (settingsPanel != null)
				{
					settingsPanel.nfcSniffPanel().setFrames(snapshotStore.nfcFrames());
					settingsPanel.nfcSniffPanel().setStatus(NfcSniffHud.text(shown, field));
				}
				if (chartPanel != null)
					chartPanel.repaint();
			});
		});
		NfcDecNative decoder = NfcDecNative.open();
		if (decoder == null)
			System.err.println("NFC sniff: decoder unavailable (spectrum only)");
		nfcEngine.start(decoder == null ? NfcSniffEngine.Decoder.NONE : decoder);
		snapshotStore.publishContext(settings, fmStations, 0);
		if (chartPanel != null)
			SwingUtilities.invokeLater(chartPanel::repaint);
		if (settingsPanel != null)
			SwingUtilities.invokeLater(() -> settingsPanel.nfcSniffPanel().setSniffing(true));
		try {
			int lna = NfcSniffGainPolicy.seedLna();
			int vga = NfcSniffGainPolicy.seedVga();
			System.err.println("NFC sniff: LO " + NfcSniffEngine.LO_HZ + " Hz " + NfcSniffEngine.IQ_RATE_HZ
					+ " S/s LNA " + lna + " VGA " + vga);
			HackRFFmNativeBridge.configure(settings.getSelectedSerial().getValue(),
					settings.getClkoutEnable().getValue());
			HackRFFmNativeBridge.start(iq -> nfcEngine.offerIq(iq), NfcSniffEngine.LO_HZ, NfcSniffEngine.IQ_RATE_HZ,
					lna, vga, settings.getAntennaPowerEnable().getValue(), false);
		} finally {
			nfcEngine.setRfSpectrumListener(null);
			nfcEngine.setEnvelopeListener(null);
			nfcEngine.setFrameListener(null);
			nfcEngine.stop();
			waterfallPlot.setNfcMode(false);
			if (chart != null)
				SwingUtilities.invokeLater(() -> chart.getXYPlot().getDomainAxis()
						.setRange(getFreq().getStartMHz(), getFreq().getEndMHz()));
			if (settingsPanel != null)
				SwingUtilities.invokeLater(() -> settingsPanel.nfcSniffPanel().setSniffing(false));
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
		nfcEngine.stop();
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
