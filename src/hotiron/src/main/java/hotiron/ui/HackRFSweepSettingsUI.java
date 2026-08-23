package hotiron.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JSpinner.ListEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.UIManager;

import hotiron.HotIron;
import hotiron.Version;
import hotiron.core.AutoSweepPolicy;
import hotiron.core.FrequencyAllocationTable;
import hotiron.core.FrequencyAllocations;
import hotiron.core.FrequencyRange;
import hotiron.core.HackRFSettings;
import hotiron.core.HackRFSettings.HackRFEventAdapter;
import hotiron.core.McpStatus;
import hotiron.core.RadioIdentity;
import net.miginfocom.swing.MigLayout;
import hotiron.mvc.MVCController;
import javax.swing.border.EmptyBorder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;

public class HackRFSweepSettingsUI extends JPanel
{
	/**
	 * 
	 */
	private HackRFSettings hRF;
	private static final long serialVersionUID = 7721079457485020637L;
	private JLabel txtHackrfConnected;
	private JLabel txtMcpStatus;
	private boolean radioSweeping;
	private boolean syncingRadioCombo;
	private FrequencyRangePanel frequencyRangePanel;
	private QuickFrequencySelectorPanel quickFrequencySelector;
	private JSpinner spinnerFFTBinHz;
	private JSlider sliderGain;
	private JSpinner spinner_numberOfSamples;
	private JCheckBox chckbxAntennaPower;
	private JCheckBox chckbxAntennaLNA;
	private JSlider slider_waterfallPaletteStart;
	private JSlider slider_waterfallPaletteSize;
	private JCheckBox chckbxShowPeaks;
	private JCheckBox chckbxAutoScalePower;
	private JCheckBox chckbxAutoGain;
	private JCheckBox chckbxAutoSweep;
	private JCheckBox chckbxRemoveSpurs;
	private JButton btnPause;
	private JButton btnRestart;
	private JButton btnStop;
	private JButton btnListen;
	private JButton btnWatch;
	private StationKnob stationKnob;
	private TunerPanel tunerPanel;
	private TvTunerPanel tvTunerPanel;
	private JSlider sliderVolume;
	private JComboBox<String> comboRadio;
	private JCheckBox checkBoxClkout;
	static final String FIRST_RADIO = "First radio";
	private SpinnerListModel spinnerModelFFTBinHz;
	private FrequencySelectorRangeBinder frequencyRangeSelector;
	private JSpinner spinnerPeakFallSpeed;
	private JComboBox<FrequencyAllocationTable> comboBoxFrequencyAllocationBands;
	private JSlider sliderGainVGA;
	private JSlider sliderGainLNA;
	private JLabel lblPeakFall;
	private JComboBox<BigDecimal> comboBoxLineThickness;
	private JLabel lblPersistentDisplay;
	private JCheckBox checkBoxPersistentDisplay;
	private JCheckBox checkBoxWaterfallEnabled;
	private JLabel lblDecayRate;
	private JComboBox comboBoxDecayRate;
	private JLabel lblDebugDisplay;
	private JCheckBox checkBoxDebugDisplay;

	/**
	 * Create the panel.
	 */
	public HackRFSweepSettingsUI(HackRFSettings hackRFSettings)
	{
		if (hackRFSettings == null)
			throw new IllegalArgumentException("hackRFSettings");
		this.hRF	= hackRFSettings;
		AnalyzerLookAndFeel.install();
		setBorder(new EmptyBorder(4, 8, 16, 8));
		JPanel panelMainSettings	= new JPanel(new MigLayout("", "[123.00px,grow,leading]", "[][grow]"));

		panelMainSettings.setBorder(new EmptyBorder(UIManager.getInsets("TabbedPane.tabAreaInsets")));;

		JLabel lblQuickSelect = new JLabel("Quick Select");
		panelMainSettings.add(lblQuickSelect, "cell 0 0,alignx left,aligny center");

		quickFrequencySelector = new QuickFrequencySelectorPanel();
		panelMainSettings.add(quickFrequencySelector, "cell 0 1,growx,aligny center");

		JLabel lblSweepRange = new JLabel("Sweep range");
		panelMainSettings.add(lblSweepRange, "cell 0 2,growx,aligny center");

		frequencyRangePanel = new FrequencyRangePanel();
		panelMainSettings.add(frequencyRangePanel, "cell 0 3,growx,aligny center");
		
		txtHackrfConnected = new JLabel();
		txtHackrfConnected.setText(RadioIdentity.ABSENT.statusHtml());
		ExclusiveToolTip.setText(txtHackrfConnected, RadioIdentity.ABSENT.tooltip(false));
		txtHackrfConnected.setVerticalAlignment(SwingConstants.TOP);
		txtHackrfConnected.setBorder(null);

		txtMcpStatus = new JLabel();
		txtMcpStatus.setText(McpStatus.OFF.statusHtml());
		ExclusiveToolTip.setText(txtMcpStatus, McpStatus.OFF.tooltip(System.currentTimeMillis()));
		txtMcpStatus.setVerticalAlignment(SwingConstants.TOP);
		txtMcpStatus.setBorder(null);

		comboRadio = new JComboBox<>(new String[] { FIRST_RADIO });
		ExclusiveToolTip.setText(comboRadio, "Which HackRF to open. First radio = libhackrf default.");

		checkBoxClkout = new JCheckBox("CLKOUT 10 MHz");
		ExclusiveToolTip.setText(checkBoxClkout, "Drive CLKOUT so another radio can lock. CLKIN is used automatically when a 10 MHz signal is present.");

		btnRestart = new JButton("Restart");
		ExclusiveToolTip.setText(btnRestart, "Stop and start the sweep again. Use this if the plot freezes after a setting change.");
		btnStop = new JButton("Stop");
		ExclusiveToolTip.setText(btnStop, "Halt the native sweep and release USB so other tools can open the radio.");
		btnPause = new JButton("Pause");
		ExclusiveToolTip.setText(btnPause, "Freeze the display. The radio keeps sweeping; click Resume to show live data again.");
		tunerPanel = new TunerPanel();
		btnListen = tunerPanel.listenButton();
		stationKnob = tunerPanel.knob();
		sliderVolume = tunerPanel.volumeSlider();
		tvTunerPanel = new TvTunerPanel();
		btnWatch = tvTunerPanel.watchButton();
		ExclusiveToolTip.install(txtHackrfConnected);
		ExclusiveToolTip.install(txtMcpStatus);
		ExclusiveToolTip.install(comboRadio);
		ExclusiveToolTip.install(checkBoxClkout);
		ExclusiveToolTip.install(btnRestart);
		ExclusiveToolTip.install(btnStop);
		ExclusiveToolTip.install(btnPause);
		JPanel radioButtons = new JPanel(new GridLayout(1, 2, 4, 0));
		radioButtons.add(btnRestart);
		radioButtons.add(btnStop);

		JPanel radioStrip = new JPanel(new MigLayout("insets 0, wrap 1, gapy 4", "[grow,fill]", ""));
		radioStrip.add(txtHackrfConnected);
		radioStrip.add(txtMcpStatus);
		radioStrip.add(comboRadio);
		radioStrip.add(checkBoxClkout);
		radioStrip.add(radioButtons);
		radioStrip.add(tunerPanel);
		radioStrip.add(tvTunerPanel);
		radioStrip.add(btnPause);
		panelMainSettings.add(radioStrip, "cell 0 4,growx");

		
		
		
		JTabbedPane tabbedPane	= new JTabbedPane(JTabbedPane.TOP);
		setLayout(new BorderLayout());
		add(panelMainSettings, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);

		JPanel tab1	= new JPanel(new MigLayout("", "[123.00px,grow,leading]", "[][][0][][][0][][][0][][][0][][][0][][0][][grow,fill]"));
		
		JPanel tab2	= new JPanel(new MigLayout("", "[123.00px,grow,leading]", "[][0][][][0][][][0][][][][][0][][0][][][0][0][][][0][][0][grow,fill]"));
		
		tab1.setBorder(new EmptyBorder(4, 0, 12, 0));
		tab2.setBorder(new EmptyBorder(4, 0, 12, 0));
		tabbedPane.addTab("HackRF Settings", tab1);
		tabbedPane.addTab("Chart options", tab2);

		
		JLabel lblNewLabel_2 = new JLabel("LNA Gain [dB]");
		tab1.add(lblNewLabel_2, "cell 0 3");
		
		sliderGainLNA = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 2);
		sliderGainLNA.setFont(new Font("Monospaced", Font.BOLD, 16));
		tab1.add(sliderGainLNA, "cell 0 4,growx");
		
		JLabel lblVgfaGaindb = new JLabel("VGA Gain [dB]");
		tab1.add(lblVgfaGaindb, "cell 0 6");
		
		sliderGainVGA = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 2);
		sliderGainVGA.setFont(new Font("Monospaced", Font.BOLD, 16));
		tab1.add(sliderGainVGA, "cell 0 7,growx");

		

		JLabel lblFftBinhz = new JLabel("FFT Bin [Hz]");
		tab1.add(lblFftBinhz, "flowx,cell 0 9,growx");

		chckbxAutoSweep = new JCheckBox("Auto");
		chckbxAutoSweep.setSelected(true);
		ExclusiveToolTip.setText(chckbxAutoSweep,
				"Pick FFT bin and samples from the sweep span so zoomed-in windows stay detailed and wide scans stay fast.");
		tab1.add(chckbxAutoSweep, "cell 0 9,alignx right");

		spinnerFFTBinHz = new JSpinner();
		spinnerFFTBinHz.setFont(new Font("Monospaced", Font.BOLD, 16));
		spinnerModelFFTBinHz = new SpinnerListModel(AutoSweepPolicy.binLabels());
		spinnerFFTBinHz.setModel(spinnerModelFFTBinHz);
		ExclusiveToolTip.setText(spinnerFFTBinHz, "Resolution bandwidth. Locked while Auto is on.");
		tab1.add(spinnerFFTBinHz, "cell 0 10,growx");
		((ListEditor) spinnerFFTBinHz.getEditor()).getTextField().setHorizontalAlignment(JTextField.RIGHT);
		

		JLabel lblGain = new JLabel("Gain [dB]");
		tab1.add(lblGain, "flowx,cell 0 0,growx");

		chckbxAutoGain = new JCheckBox("Auto");
		chckbxAutoGain.setSelected(true);
		ExclusiveToolTip.setText(chckbxAutoGain, "Pick LNA/VGA for this band so the plot is not all blue or all red.");
		tab1.add(chckbxAutoGain, "cell 0 0,alignx right");

		sliderGain = new JSlider(JSlider.HORIZONTAL, 0, 100, 2);
		sliderGain.setFont(new Font("Monospaced", Font.BOLD, 16));
		tab1.add(sliderGain, "flowy,cell 0 1,growx");

		JLabel lbl_gainValue = new JLabel(hRF.getGain() + "dB");
		tab1.add(lbl_gainValue, "cell 0 1,alignx right");

		hRF.getGain().addListener((gain) -> lbl_gainValue.setText(String.format(" %ddB  [LNA: %ddB  VGA: %ddB]",
				gain, hRF.getGainLNA().getValue(), hRF.getGainVGA().getValue())));
		

		JLabel lblNumberOfSamples = new JLabel("Number of samples");
		ExclusiveToolTip.setText(lblNumberOfSamples,
				"Samples captured per tuning step. Higher values average more FFT blocks for a smoother, slower sweep. Locked while Auto is on.");
		tab1.add(lblNumberOfSamples, "cell 0 12");

		spinner_numberOfSamples = new JSpinner();
		spinner_numberOfSamples.setModel(new SpinnerListModel(new String[] { "8192", "16384", "32768", "65536", "131072", "262144" }));
		ExclusiveToolTip.setText(spinner_numberOfSamples,
				"8192 = one FFT block; each doubling adds linear-power averaging and roughly doubles dwell time.");
		spinner_numberOfSamples.setFont(new Font("Monospaced", Font.BOLD, 16));
		((ListEditor) spinner_numberOfSamples.getEditor()).getTextField().setHorizontalAlignment(JTextField.RIGHT);
		((ListEditor) spinner_numberOfSamples.getEditor()).getTextField().setEditable(false);
		tab1.add(spinner_numberOfSamples, "cell 0 13,growx");

		JButton btnAbout = new JButton("Visit homepage");
		btnAbout.addActionListener(e -> {
			try {
				DesktopBrowse.open(Version.url);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		
		JLabel labelVersion = new JLabel("Version: "+Version.version);
		tab1.add(labelVersion, "flowx,cell 0 17");
		tab1.add(btnAbout, "cell 0 17,alignx right");
		
		JLabel lblAntennaPower = new JLabel("Antenna power");
		tab1.add(lblAntennaPower, "flowx,cell 0 15,growx");
		
		chckbxAntennaPower = new JCheckBox("");
		chckbxAntennaPower.setHorizontalTextPosition(SwingConstants.LEADING);
		tab1.add(chckbxAntennaPower, "cell 0 15,alignx right");

		JLabel lblLNAEnable = new JLabel("Antenna LNA +14dB");
		tab1.add(lblLNAEnable, "flowx,cell 0 16,growx");

		chckbxAntennaLNA = new JCheckBox("");
		chckbxAntennaLNA.setHorizontalTextPosition(SwingConstants.LEADING);
		tab1.add(chckbxAntennaLNA, "cell 0 16,alignx right");
		
		JLabel lblWaterfallEnabled = new JLabel("Waterfall enabled");
		tab2.add(lblWaterfallEnabled, "flowx,cell 0 0,growx");

		


		JLabel lblWaterfallPaletteStart = new JLabel("Waterfall palette start [dB]");
		tab2.add(lblWaterfallPaletteStart, "cell 0 2");

		slider_waterfallPaletteStart = new JSlider();
		slider_waterfallPaletteStart.setMinimum(-100);
		slider_waterfallPaletteStart.setMaximum(0);
		slider_waterfallPaletteStart.setValue(-30);
		tab2.add(slider_waterfallPaletteStart, "cell 0 3,growx");


		JLabel lblWaterfallPaletteLength = new JLabel("Waterfall palette length [dB]");
		tab2.add(lblWaterfallPaletteLength, "cell 0 5");

		slider_waterfallPaletteSize = new JSlider(HotIron.SPECTRUM_PALETTE_SIZE_MIN, 100);
		tab2.add(slider_waterfallPaletteSize, "cell 0 6,growx");
		
		JLabel lblSpectrLineThickness = new JLabel("Spectr. Line Thickness");
		tab2.add(lblSpectrLineThickness, "flowx,cell 0 8,growx");
		
		JLabel lblAutoScalePower = new JLabel("Auto-scale dB axis");
		tab2.add(lblAutoScalePower, "flowx,cell 0 9,growx");

		chckbxAutoScalePower = new JCheckBox("");
		tab2.add(chckbxAutoScalePower, "cell 0 9,alignx right");

		JLabel lblShowPeaks = new JLabel("Show peaks");
		tab2.add(lblShowPeaks, "flowx,cell 0 10,growx");
		
		
		chckbxShowPeaks = new JCheckBox("");
		tab2.add(chckbxShowPeaks, "cell 0 10,alignx right");
		
		JLabel lblSpurFiltermay = new JLabel("Spur filter (may distort real signals)");
		tab2.add(lblSpurFiltermay, "flowx,cell 0 13,growx");
		
		chckbxRemoveSpurs = new JCheckBox("");
		tab2.add(chckbxRemoveSpurs, "cell 0 13,alignx right");
		
		lblPeakFall = new JLabel("  Peak half-life [s]");
		tab2.add(lblPeakFall, "flowx,cell 0 11,growx");
		
		spinnerPeakFallSpeed = new JSpinner();
		spinnerPeakFallSpeed.setModel(new SpinnerNumberModel(10, 0, 500, 1));
		tab2.add(spinnerPeakFallSpeed, "cell 0 11,alignx right");
		
		lblPersistentDisplay = new JLabel("Persistent Display");
		tab2.add(lblPersistentDisplay, "flowx,cell 0 15,growx");
		
		lblDecayRate = new JLabel("  Persistence half-life [s]");
		tab2.add(lblDecayRate, "flowx,cell 0 16,growx");
		
		JLabel lblDisplayFrequencyAllocation = new JLabel("Frequency Allocation Bands");
		tab2.add(lblDisplayFrequencyAllocation, "cell 0 19");
		
		
		FrequencyAllocations frequencyAllocations	= new FrequencyAllocations();
		Vector<FrequencyAllocationTable> freqAllocValues	= new Vector<>();
		freqAllocValues.add(null);
		freqAllocValues.addAll(frequencyAllocations.getTable().values());
		DefaultComboBoxModel<FrequencyAllocationTable> freqAllocModel	= new  DefaultComboBoxModel<>(freqAllocValues);
		comboBoxFrequencyAllocationBands = new JComboBox<FrequencyAllocationTable>(freqAllocModel);
		tab2.add(comboBoxFrequencyAllocationBands, "cell 0 20,growx");
		
		comboBoxLineThickness = new JComboBox(new BigDecimal[] {
				new BigDecimal("1"), new BigDecimal("1.5"), new BigDecimal("2"), new BigDecimal("3")
				});
		tab2.add(comboBoxLineThickness, "cell 0 8,alignx right");
		
		checkBoxPersistentDisplay = new JCheckBox("");
		tab2.add(checkBoxPersistentDisplay, "cell 0 15,alignx right");
		
		checkBoxWaterfallEnabled = new JCheckBox("");
		tab2.add(checkBoxWaterfallEnabled, "cell 0 0,alignx right");
		
		comboBoxDecayRate = new JComboBox(
				new Vector<>(IntStream.rangeClosed(hRF.getPersistentDisplayDecayRate().getMin(),
						hRF.getPersistentDisplayDecayRate().getMax()).
						boxed().collect(Collectors.toList())));
		tab2.add(comboBoxDecayRate, "cell 0 16,alignx right");
		
		lblDebugDisplay = new JLabel("Debug display");
		tab2.add(lblDebugDisplay, "flowx,cell 0 22,growx");
		
		checkBoxDebugDisplay = new JCheckBox("");
		tab2.add(checkBoxDebugDisplay, "cell 0 22,alignx right");
		
		
		bindViewToModel();
	}

	private void bindViewToModel() {
		frequencyRangeSelector = new FrequencySelectorRangeBinder(frequencyRangePanel, quickFrequencySelector);

		new MVCController(spinnerFFTBinHz, hRF.getFFTBinHz(), 
				viewValue -> Integer.parseInt(viewValue.toString().replaceAll("\\s", "")), 
				modelValue -> {
					Optional<?> val = spinnerModelFFTBinHz.getList().stream().filter(value -> modelValue <= Integer.parseInt(value.toString().replaceAll("\\s", ""))).findFirst();
					if (val.isPresent())
						return val.get();
					else
						return spinnerModelFFTBinHz.getList().get(0);
				});
		new MVCController(sliderGain, hRF.getGain());
		new MVCController(chckbxAutoGain, hRF.isAutoGain());
		new MVCController(chckbxAutoSweep, hRF.isAutoSweep());
		Runnable syncGainEditors = () -> {
			boolean manual = !hRF.isAutoGain().getValue();
			sliderGain.setEnabled(manual);
			sliderGainLNA.setEnabled(manual);
			sliderGainVGA.setEnabled(manual);
		};
		hRF.isAutoGain().addListener(() -> SwingUtilities.invokeLater(syncGainEditors));
		syncGainEditors.run();
		Runnable syncSweepEditors = () -> {
			boolean manual = !hRF.isAutoSweep().getValue();
			spinnerFFTBinHz.setEnabled(manual);
			spinner_numberOfSamples.setEnabled(manual);
		};
		hRF.isAutoSweep().addListener(() -> SwingUtilities.invokeLater(syncSweepEditors));
		syncSweepEditors.run();
		new MVCController(spinner_numberOfSamples, hRF.getSamples(), val -> Integer.parseInt(val.toString()), val -> val.toString());
		new MVCController(chckbxAntennaPower, hRF.getAntennaPowerEnable());
		new MVCController(chckbxAntennaLNA, hRF.getAntennaLNA());
		new MVCController(slider_waterfallPaletteStart, hRF.getSpectrumPaletteStart());
		new MVCController(slider_waterfallPaletteSize, hRF.getSpectrumPaletteSize());
		new MVCController(	(Consumer<FrequencyRange> valueChangedCall) ->  
								frequencyRangeSelector.addPropertyChangeListener((PropertyChangeEvent evt) -> valueChangedCall.accept(frequencyRangeSelector.getFrequencyRange()) ) ,
							(FrequencyRange newComponentValue) -> {
								frequencyRangeSelector.applyPreset(newComponentValue.getStartMHz(),
										newComponentValue.getEndMHz());
							},
							hRF.getFrequency()
		);
		// After the binder applies the preset (same vetoable list, registered
		// later) so stopListen restarts the sweep on the new range, not the
		// parked FM/TV IQ window.
		quickFrequencySelector.addVetoableChangeListener(evt -> {
			if (Boolean.TRUE.equals(hRF.isListening().getValue()))
				hRF.stopListen();
		}); 
		new MVCController(chckbxShowPeaks, hRF.isChartsPeaksVisible());
		new MVCController(chckbxAutoScalePower, hRF.isPowerAutoScale());
		new MVCController(chckbxRemoveSpurs, hRF.isSpurRemoval());
		
		new MVCController((valueChangedCall) -> btnPause.addActionListener((event) -> valueChangedCall.accept(!hRF.isCapturingPaused().getValue())), 
				isCapt -> btnPause.setText(!isCapt ? "Pause"  : "Resume"), 
				hRF.isCapturingPaused());

		btnRestart.addActionListener(e -> hRF.restartSweep());
		btnStop.addActionListener(e -> {
			hRF.releaseRadio();
			refreshRadioCombo();
		});
		btnListen.addActionListener(e -> {
			if (Boolean.TRUE.equals(hRF.isListening().getValue())
					&& hRF.getListenService().getValue() == hotiron.core.ListenService.FM)
				hRF.stopListen();
			else
				hRF.startListen();
		});
		btnWatch.addActionListener(e -> {
			if (Boolean.TRUE.equals(hRF.isListening().getValue())
					&& hRF.getListenService().getValue() == hotiron.core.ListenService.TV)
				hRF.stopListen();
			else
				hRF.startWatch();
		});
		new MVCController(sliderVolume, hRF.getListenVolume());
		new MVCController(tvTunerPanel.volumeSlider(), hRF.getListenVolume());
		tunerPanel.setOnTune(dir -> {
			hotiron.core.FmChannel next = hotiron.core.FmStationDial.tune(
					hRF.getListenKHz().getValue(), dir);
			if (next.centerKHz != hRF.getListenKHz().getValue())
				hRF.getListenKHz().setValue(next.centerKHz);
		});
		tunerPanel.setOnSeek(dir -> {
			hotiron.core.FmChannel next = hotiron.core.FmStationDial.seek(
					hRF.getDetectedFmStations().getValue(), hRF.getListenKHz().getValue(), dir);
			if (next.centerKHz != hRF.getListenKHz().getValue())
				hRF.getListenKHz().setValue(next.centerKHz);
		});
		tunerPanel.setOnScan(hRF::startFmScan);
		tvTunerPanel.setOnTune(dir -> {
			hotiron.core.TvChannel next = hotiron.core.TvStationDial.tune(
					hRF.getTvChannel().getValue(), dir);
			if (next.fccChannel != hRF.getTvChannel().getValue())
				hRF.getTvChannel().setValue(next.fccChannel);
		});
		tvTunerPanel.setOnSeek(dir -> {
			hotiron.core.TvChannel next = hotiron.core.TvStationDial.seek(
					hRF.getDetectedTvStations().getValue(), hRF.getTvChannel().getValue(), dir);
			if (next.fccChannel != hRF.getTvChannel().getValue())
				hRF.getTvChannel().setValue(next.fccChannel);
		});
		tvTunerPanel.setOnScan(hRF::startTvScan);
		Runnable syncListen = () -> {
			boolean parked = Boolean.TRUE.equals(hRF.isListening().getValue());
			boolean released = Boolean.TRUE.equals(hRF.isRadioReleased().getValue());
			boolean fm = parked && hRF.getListenService().getValue() == hotiron.core.ListenService.FM;
			boolean tv = parked && hRF.getListenService().getValue() == hotiron.core.ListenService.TV;
			int kHz = hRF.getListenKHz().getValue();
			tunerPanel.setKHz(kHz);
			tunerPanel.setListening(fm);
			tunerPanel.setStations(hRF.getDetectedFmStations().getValue());
			tvTunerPanel.setChannel(hRF.getTvChannel().getValue());
			tvTunerPanel.setWatching(tv);
			btnPause.setEnabled(!parked && !released);
		};
		hRF.isListening().addListener(() -> SwingUtilities.invokeLater(syncListen));
		hRF.getListenService().addListener(s -> SwingUtilities.invokeLater(syncListen));
		hRF.getBandScan().addListener(scan -> SwingUtilities.invokeLater(() -> {
			tunerPanel.setScanning(scan == hotiron.core.BandScan.FM);
			tvTunerPanel.setScanning(scan == hotiron.core.BandScan.TV);
		}));
		tunerPanel.setScanning(hRF.getBandScan().getValue() == hotiron.core.BandScan.FM);
		tvTunerPanel.setScanning(hRF.getBandScan().getValue() == hotiron.core.BandScan.TV);
		hRF.getListenKHz().addListener(() -> SwingUtilities.invokeLater(syncListen));
		hRF.getTvChannel().addListener(ch -> SwingUtilities.invokeLater(syncListen));
		hRF.getDetectedFmStations().addListener(hits -> SwingUtilities.invokeLater(() -> tunerPanel.setStations(hits)));
		syncListen.run();
		new MVCController(checkBoxClkout, hRF.getClkoutEnable());
		refreshRadioCombo();
		comboRadio.addActionListener(e -> {
			if (syncingRadioCombo)
				return;
			Object sel = comboRadio.getSelectedItem();
			String serial = (sel == null || FIRST_RADIO.equals(sel)) ? "" : sel.toString();
			if (!serial.equals(hRF.getSelectedSerial().getValue()))
				hRF.getSelectedSerial().setValue(serial);
		});
		hRF.isRadioReleased().addListener(released -> SwingUtilities.invokeLater(() -> {
			btnStop.setEnabled(!released);
			boolean listen = Boolean.TRUE.equals(hRF.isListening().getValue());
			btnPause.setEnabled(!released && !listen);
			btnStop.setText(released ? "Stopped" : "Stop");
		}));
		hRF.isRadioReleased().callObservers();
	
		new MVCController(spinnerPeakFallSpeed, hRF.getPeakFallRate(), in -> (Integer)in, in -> in);
	
		new MVCController(comboBoxFrequencyAllocationBands, hRF.getFrequencyAllocationTable());
		
		sliderGainLNA.setModel(new DefaultBoundedRangeModel(hRF.getGainLNA().getValue(), 0, hRF.getGainLNA().getMin(), hRF.getGainLNA().getMax()));
		sliderGainVGA.setModel(new DefaultBoundedRangeModel(hRF.getGainVGA().getValue(), 0, hRF.getGainVGA().getMin(), hRF.getGainVGA().getMax()));
		
		sliderGainLNA.setSnapToTicks(true);
		sliderGainLNA.setMinorTickSpacing(hRF.getGainLNA().getStep());
		
		sliderGainVGA.setSnapToTicks(true);
		sliderGainVGA.setMinorTickSpacing(hRF.getGainVGA().getStep());
		
		new MVCController(sliderGainLNA, hRF.getGainLNA());
		new MVCController(sliderGainVGA, hRF.getGainVGA());

		new MVCController(comboBoxLineThickness, hRF.getSpectrumLineThickness());
		
		new MVCController(checkBoxPersistentDisplay, hRF.isPersistentDisplayVisible());
		
		new MVCController(checkBoxWaterfallEnabled, hRF.isWaterfallVisible());
		
		new MVCController(checkBoxDebugDisplay, hRF.isDebugDisplay());
		
		hRF.isChartsPeaksVisible().addListener((enabled) -> {
			SwingUtilities.invokeLater(()->{
				spinnerPeakFallSpeed.setEnabled(enabled);
				spinnerPeakFallSpeed.setVisible(enabled);
				lblPeakFall.setVisible(enabled);
			});
		});
		hRF.isChartsPeaksVisible().callObservers();
		
		new MVCController(comboBoxDecayRate, hRF.getPersistentDisplayDecayRate());
		hRF.isPersistentDisplayVisible().addListener((visible) -> {
			SwingUtilities.invokeLater(()->{
				comboBoxDecayRate.setVisible(visible);
				lblDecayRate.setVisible(visible);
			});
		});
		hRF.isPersistentDisplayVisible().callObservers();
		
		hRF.getRadioIdentity().addListener(id -> SwingUtilities.invokeLater(this::refreshRadioStatus));
		hRF.getMcpStatus().addListener(s -> SwingUtilities.invokeLater(this::refreshMcpStatus));
		hRF.registerListener(new HackRFSettings.HackRFEventAdapter()
		{
			@Override public void hardwareStatusChanged(boolean hardwareSendingData)
			{
				radioSweeping = hardwareSendingData;
				refreshRadioStatus();
			}
		});
		refreshRadioStatus();
		refreshMcpStatus();
		ExclusiveToolTip.bindColumn(this);
	}

	private void refreshRadioStatus() {
		RadioIdentity id = hRF.getRadioIdentity().getValue();
		if (id == null)
			id = RadioIdentity.ABSENT;
		txtHackrfConnected.setText(id.statusHtml());
		ExclusiveToolTip.setText(txtHackrfConnected, id.tooltip(radioSweeping));
	}

	private void refreshMcpStatus() {
		McpStatus mcp = hRF.getMcpStatus().getValue();
		if (mcp == null)
			mcp = McpStatus.OFF;
		txtMcpStatus.setText(mcp.statusHtml());
		ExclusiveToolTip.setText(txtMcpStatus, mcp.tooltip(System.currentTimeMillis()));
	}

	private void refreshRadioCombo() {
		if (comboRadio == null)
			return;
		syncingRadioCombo = true;
		try {
			String current = hRF.getSelectedSerial().getValue();
			comboRadio.removeAllItems();
			comboRadio.addItem(FIRST_RADIO);
			for (String serial : hRF.listRadioSerials())
				comboRadio.addItem(serial);
			if (current != null && !current.isEmpty())
				comboRadio.setSelectedItem(current);
			else
				comboRadio.setSelectedItem(FIRST_RADIO);
		} finally {
			syncingRadioCombo = false;
		}
	}

	JButton pauseButton() {
		return btnPause;
	}

	JButton listenButton() {
		return btnListen;
	}

	JButton watchButton() {
		return btnWatch;
	}

	public TvTunerPanel tvTunerPanel() {
		return tvTunerPanel;
	}

	StationKnob stationKnob() {
		return stationKnob;
	}

	TunerPanel tunerPanel() {
		return tunerPanel;
	}

	JButton restartButton() {
		return btnRestart;
	}

	JButton stopButton() {
		return btnStop;
	}

	JComboBox<String> radioCombo() {
		return comboRadio;
	}

	JCheckBox clkoutCheckBox() {
		return checkBoxClkout;
	}

	JLabel connectedLabel() {
		return txtHackrfConnected;
	}

	JLabel mcpStatusLabel() {
		return txtMcpStatus;
	}

	FrequencyRangePanel frequencyRangePanel() {
		return frequencyRangePanel;
	}

	QuickFrequencySelectorPanel quickFrequencySelector() {
		return quickFrequencySelector;
	}

	JSpinner fftBinSpinner() {
		return spinnerFFTBinHz;
	}

	JCheckBox showPeaksCheckbox() {
		return chckbxShowPeaks;
	}

	JCheckBox autoScaleCheckbox() {
		return chckbxAutoScalePower;
	}

	JCheckBox autoGainCheckbox() {
		return chckbxAutoGain;
	}

	JCheckBox autoSweepCheckbox() {
		return chckbxAutoSweep;
	}

	JSpinner samplesSpinner() {
		return spinner_numberOfSamples;
	}

	JSlider gainSlider() {
		return sliderGain;
	}

	JSpinner peakFallSpinner() {
		return spinnerPeakFallSpeed;
	}

	JCheckBox persistentDisplayCheckbox() {
		return checkBoxPersistentDisplay;
	}

	JComboBox decayRateCombo() {
		return comboBoxDecayRate;
	}

}
