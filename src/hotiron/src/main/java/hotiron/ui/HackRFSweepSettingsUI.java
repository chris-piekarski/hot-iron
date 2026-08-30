package hotiron.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSpinner.ListEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import hotiron.core.AutoSweepPolicy;
import hotiron.core.BandContext;
import hotiron.core.BandToolKind;
import hotiron.core.FrequencyAllocationTable;
import hotiron.core.FrequencyAllocations;
import hotiron.core.FrequencyRange;
import hotiron.core.HackRFSettings;
import hotiron.core.HackRFSettings.HackRFEventAdapter;
import hotiron.core.McpStatus;
import hotiron.core.RadioIdentity;
import hotiron.mvc.MVCController;
import net.miginfocom.swing.MigLayout;

public class HackRFSweepSettingsUI extends JPanel
{
	/**
	 * 
	 */
	private HackRFSettings hRF;
	private static final long serialVersionUID = 7721079457485020637L;
	public static final int TOOLS_WIDTH = OperatorLayout.TOOLS_WIDTH;
	public static final int BAND_SLOT_HEIGHT = OperatorLayout.BAND_SLOT_HEIGHT;
	private JLabel txtHackrfConnected;
	private boolean radioSweeping;
	private boolean syncingRadioCombo;
	private OperatorNavBanner navBanner;
	private FrequencyRangePanel frequencyRangePanel;
	private QuickFrequencySelectorPanel quickFrequencySelector;
	private RadioSessionStrip radioStrip;
	private SweepStatusBar footer;
	private SpectrumGainRail gainRail;
	private ChartToggleBar chartToggles;
	private HardwarePane hardwarePane;
	private BandToolsSlot bandSlot;
	private JSpinner spinnerFFTBinHz;
	private JSpinner spinner_numberOfSamples;
	private JCheckBox chckbxAntennaPower;
	private JCheckBox chckbxAntennaLNA;
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
	private TunerPanel tunerPanel;
	private McpLogPane mcpLog;
	private TvTunerPanel tvTunerPanel;
	private NfcSniffPanel nfcSniffPanel;
	private BleSniffPanel bleSniffPanel;
	private JSlider sliderVolume;
	private JComboBox<String> comboRadio;
	private JCheckBox checkBoxClkout;
	static final String FIRST_RADIO = RadioSessionStrip.FIRST_RADIO;
	private SpinnerListModel spinnerModelFFTBinHz;
	private FrequencySelectorRangeBinder frequencyRangeSelector;
	private JSpinner spinnerPeakFallSpeed;
	private JComboBox<FrequencyAllocationTable> comboBoxFrequencyAllocationBands;
	private JSlider sliderGainVGA;
	private JSlider sliderGainLNA;
	private JCheckBox checkBoxPersistentDisplay;
	private JComboBox comboBoxDecayRate;
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
		setBorder(new EmptyBorder(4, 8, 8, 8));
		navBanner = new OperatorNavBanner();
		quickFrequencySelector = navBanner.quickSelector();
		frequencyRangePanel = navBanner.rangePanel();

		radioStrip = new RadioSessionStrip();
		txtHackrfConnected = radioStrip.connectedLabel();
		comboRadio = radioStrip.radioCombo();
		btnRestart = radioStrip.restartButton();
		btnStop = radioStrip.stopButton();
		btnPause = radioStrip.pauseButton();

		tunerPanel = new TunerPanel();
		btnListen = tunerPanel.listenButton();
		sliderVolume = tunerPanel.volumeSlider();
		tvTunerPanel = new TvTunerPanel();
		btnWatch = tvTunerPanel.watchButton();
		nfcSniffPanel = new NfcSniffPanel();
		bleSniffPanel = new BleSniffPanel();
		bandSlot = new BandToolsSlot(
				new BandTool(BandToolKind.FM, tunerPanel),
				new BandTool(BandToolKind.TV, tvTunerPanel),
				new BandTool(BandToolKind.NFC, nfcSniffPanel),
				new BandTool(BandToolKind.BLE, bleSniffPanel));

		gainRail = new SpectrumGainRail();
		chckbxAutoGain = gainRail.autoGainCheckbox();
		chckbxAntennaLNA = gainRail.antennaLnaCheckbox();
		sliderGainLNA = gainRail.lnaSlider();
		sliderGainVGA = gainRail.vgaSlider();
		gainRail.setCaption(hRF.getGain() + "dB");
		hRF.getGain().addListener((gain) -> gainRail.setCaption(String.format("%d dB", gain)));

		chartToggles = new ChartToggleBar();
		chckbxShowPeaks = chartToggles.peaksCheckbox();
		checkBoxPersistentDisplay = chartToggles.persistCheckbox();
		chckbxRemoveSpurs = chartToggles.spursCheckbox();
		chckbxAutoScalePower = chartToggles.autoDbCheckbox();
		comboBoxFrequencyAllocationBands = chartToggles.allocationCombo();
		FrequencyAllocations frequencyAllocations = new FrequencyAllocations();
		Vector<FrequencyAllocationTable> freqAllocValues = new Vector<>();
		freqAllocValues.add(null);
		freqAllocValues.addAll(frequencyAllocations.getTable().values());
		comboBoxFrequencyAllocationBands.setModel(new DefaultComboBoxModel<>(freqAllocValues));

		hardwarePane = new HardwarePane();
		spinnerFFTBinHz = hardwarePane.fftBinSpinner();
		spinner_numberOfSamples = hardwarePane.samplesSpinner();
		chckbxAntennaPower = hardwarePane.antennaPowerCheckbox();
		checkBoxClkout = hardwarePane.clkoutCheckbox();
		checkBoxDebugDisplay = hardwarePane.debugCheckbox();
		spinnerPeakFallSpeed = hardwarePane.peakFallSpinner();
		comboBoxDecayRate = hardwarePane.persistHalfLifeCombo();
		for (int i = hRF.getPersistentDisplayDecayRate().getMin(); i <= hRF.getPersistentDisplayDecayRate()
				.getMax(); i++)
			comboBoxDecayRate.addItem(Integer.valueOf(i));
		spinnerModelFFTBinHz = new SpinnerListModel(AutoSweepPolicy.binLabels());
		spinnerFFTBinHz.setModel(spinnerModelFFTBinHz);
		((ListEditor) spinnerFFTBinHz.getEditor()).getTextField().setHorizontalAlignment(JTextField.RIGHT);
		chckbxAutoSweep = new JCheckBox("FFT Auto");
		chckbxAutoSweep.setSelected(true);
		ExclusiveToolTip.setText(chckbxAutoSweep,
				"Pick FFT bin from the sweep span so zoomed-in windows stay detailed. Samples stay 8192.");
		ExclusiveToolTip.install(chckbxAutoSweep);
		radioStrip.setOverflow(hardwarePane);
		footer = new SweepStatusBar();
		footer.installSession(radioStrip);
		footer.installAutoSweep(chckbxAutoSweep);

		mcpLog = new McpLogPane();
		JLabel toolsTitle = new JLabel("Spectrum tools", SwingConstants.LEFT);
		setLayout(new BorderLayout(0, 6));
		add(toolsTitle, BorderLayout.NORTH);
		add(bandSlot, BorderLayout.CENTER);
		add(mcpLog, BorderLayout.SOUTH);

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
		new MVCController(chckbxAutoGain, hRF.isAutoGain());
		new MVCController(chckbxAutoSweep, hRF.isAutoSweep());
		Runnable syncGainEditors = () -> {
			boolean manual = !hRF.isAutoGain().getValue();
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
		tunerPanel.setOnSelect(kHz -> {
			if (kHz != hRF.getListenKHz().getValue())
				hRF.getListenKHz().setValue(kHz);
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
		tvTunerPanel.setOnSelect(fcc -> {
			if (fcc != hRF.getTvChannel().getValue())
				hRF.getTvChannel().setValue(fcc);
		});
		nfcSniffPanel.setOnSniff(() -> {
			if (Boolean.TRUE.equals(hRF.isListening().getValue())
					&& hRF.getListenService().getValue() == hotiron.core.ListenService.NFC)
				hRF.stopListen();
			else
				hRF.startSniff();
		});
		bleSniffPanel.setOnSniff(() -> {
			if (Boolean.TRUE.equals(hRF.isBleSniffing().getValue()))
				hRF.stopBleSniff();
			else
				hRF.startBleSniff();
		});
		Runnable syncListen = () -> {
			boolean parked = Boolean.TRUE.equals(hRF.isListening().getValue());
			boolean released = Boolean.TRUE.equals(hRF.isRadioReleased().getValue());
			boolean fm = parked && hRF.getListenService().getValue() == hotiron.core.ListenService.FM;
			boolean tv = parked && hRF.getListenService().getValue() == hotiron.core.ListenService.TV;
			boolean nfc = parked && hRF.getListenService().getValue() == hotiron.core.ListenService.NFC;
			int kHz = hRF.getListenKHz().getValue();
			tunerPanel.setKHz(kHz);
			tunerPanel.setListening(fm);
			tunerPanel.setStations(hRF.getDetectedFmStations().getValue());
			tvTunerPanel.setChannel(hRF.getTvChannel().getValue());
			tvTunerPanel.setWatching(tv);
			tvTunerPanel.setStations(hRF.getDetectedTvStations().getValue());
			tvTunerPanel.setQualifying(Boolean.TRUE.equals(hRF.isTvQualifying().getValue()),
					hRF.getTvQualifyChannel().getValue());
			nfcSniffPanel.setSniffing(nfc);
			bleSniffPanel.setSniffing(Boolean.TRUE.equals(hRF.isBleSniffing().getValue()));
			btnPause.setEnabled(!parked && !released);
			refreshBandTools();
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
		hRF.getDetectedTvStations().addListener(hits -> SwingUtilities.invokeLater(() -> {
			tvTunerPanel.setStations(hits);
			tvTunerPanel.setChannel(hRF.getTvChannel().getValue());
		}));
		hRF.isTvQualifying().addListener(on -> SwingUtilities.invokeLater(syncListen));
		hRF.getTvQualifyChannel().addListener(ch -> SwingUtilities.invokeLater(syncListen));
		hRF.isBleSniffing().addListener(() -> SwingUtilities.invokeLater(syncListen));
		hRF.getFrequency().addListener(r -> SwingUtilities.invokeLater(this::refreshBandTools));
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

		new MVCController(checkBoxPersistentDisplay, hRF.isPersistentDisplayVisible());
		new MVCController(checkBoxDebugDisplay, hRF.isDebugDisplay());
		hRF.isChartsPeaksVisible().addListener((enabled) -> {
			SwingUtilities.invokeLater(() -> spinnerPeakFallSpeed.setEnabled(enabled));
		});
		hRF.isChartsPeaksVisible().callObservers();
		new MVCController(comboBoxDecayRate, hRF.getPersistentDisplayDecayRate());
		hRF.isPersistentDisplayVisible().addListener((visible) -> {
			SwingUtilities.invokeLater(() -> comboBoxDecayRate.setEnabled(visible));
		});
		hRF.isPersistentDisplayVisible().callObservers();
		
		hRF.getRadioIdentity().addListener(id -> SwingUtilities.invokeLater(() -> {
			refreshRadioStatus();
			refreshRadioCombo();
		}));
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
		refreshBandTools();
	}

	private void refreshBandTools()
	{
		BandContext next = BandContext.of(hRF).stabilize(bandSlot.shown(), hRF.getFrequency().getValue());
		bandSlot.apply(next);
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		return new Dimension(TOOLS_WIDTH, d.height);
	}

	@Override
	public Dimension getMinimumSize()
	{
		Dimension d = super.getMinimumSize();
		return new Dimension(TOOLS_WIDTH, Math.max(200, d.height));
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(TOOLS_WIDTH, Integer.MAX_VALUE);
	}

	private void refreshRadioStatus() {
		RadioIdentity id = hRF.getRadioIdentity().getValue();
		if (id == null)
			id = RadioIdentity.ABSENT;
		txtHackrfConnected.setText(id.statusLine());
		ExclusiveToolTip.setText(txtHackrfConnected, id.tooltip(radioSweeping));
	}

	private void refreshMcpStatus() {
		McpStatus mcp = hRF.getMcpStatus().getValue();
		if (mcp == null)
			mcp = McpStatus.OFF;
		if (footer != null)
			footer.setMcp(mcp);
		if (mcpLog != null)
			mcpLog.apply(mcp);
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

	public NfcSniffPanel nfcSniffPanel() {
		return nfcSniffPanel;
	}

	public BleSniffPanel bleSniffPanel() {
		return bleSniffPanel;
	}

	FmTunerScale stationKnob() {
		return tunerPanel.scale();
	}

	public McpLogPane mcpLog() {
		return mcpLog;
	}

	TunerPanel tunerPanel() {
		return tunerPanel;
	}

	public void setFmSignalLevel(float level01)
	{
		tunerPanel.setLiveLevel(level01);
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
		return footer != null ? footer.mcpLabel() : null;
	}

	public SweepStatusBar footer() {
		return footer;
	}

	public OperatorNavBanner navBanner() {
		return navBanner;
	}

	FrequencyRangePanel frequencyRangePanel() {
		return frequencyRangePanel;
	}

	QuickFrequencySelectorPanel quickFrequencySelector() {
		return quickFrequencySelector;
	}

	BandToolsSlot bandSlot() {
		return bandSlot;
	}

	BandContext bandContext() {
		return bandSlot.shown();
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

	public JCheckBox autoSweepCheckbox() {
		return chckbxAutoSweep;
	}

	JSpinner samplesSpinner() {
		return spinner_numberOfSamples;
	}

	JSlider gainSlider() {
		return sliderGainLNA;
	}

	public SpectrumGainRail gainRail() {
		return gainRail;
	}

	public ChartToggleBar chartToggleBar() {
		return chartToggles;
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
