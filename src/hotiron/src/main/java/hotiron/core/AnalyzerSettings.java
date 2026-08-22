package hotiron.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hotiron.mvc.ModelValue;
import hotiron.mvc.ModelValue.ModelValueBoolean;
import hotiron.mvc.ModelValue.ModelValueInt;

/**
 * All operator settings. Radio fields (frequency, gain, FFT, USB) are
 * distinct from display fields (peaks, auto-scale, palette). The analyzer
 * frame binds hardware actions through {@link Hardware}; it does not own
 * the {@link ModelValue}s.
 */
public final class AnalyzerSettings implements HackRFSettings
{
	public interface Hardware
	{
		void restartSweep();

		void releaseRadio();

		void startListen();

		void startWatch();

		List<String> listRadioSerials();

		Hardware NOOP = new Hardware()
		{
			@Override
			public void restartSweep()
			{
			}

			@Override
			public void releaseRadio()
			{
			}

			@Override
			public void startListen()
			{
			}

			@Override
			public void startWatch()
			{
			}

			@Override
			public List<String> listRadioSerials()
			{
				return List.of();
			}
		};
	}

	private volatile Hardware hardware = Hardware.NOOP;
	private final ArrayList<HackRFEventListener> listeners = new ArrayList<HackRFEventListener>();

	private final ModelValueBoolean antennaPower = new ModelValueBoolean("Ant power", false);
	private final ModelValueBoolean antennaLNA = new ModelValueBoolean("Antenna LNA +14dB", false);
	private final ModelValueInt fftBinHz = new ModelValueInt("FFT Bin [Hz]", 100000);
	private final ModelValueBoolean filterSpectrum = new ModelValueBoolean("Filter", false);
	private final ModelValue<FrequencyRange> frequency = new ModelValue<FrequencyRange>("Frequency range",
			new FrequencyRange(WifiChannelPlan.WIFI_24_VIEW_START_MHZ, WifiChannelPlan.WIFI_24_VIEW_END_MHZ));
	private final ModelValue<FrequencyAllocationTable> frequencyAllocationTable = new ModelValue<FrequencyAllocationTable>(
			"Frequency allocation table", null);
	private final ModelValueInt gainLNA = new ModelValueInt("LNA Gain", 0, 8, 0, 40);
	private final ModelValueInt gainTotal = new ModelValueInt("Gain [dB]", 40);
	private final ModelValueInt gainVGA = new ModelValueInt("VGA Gain", 0, 2, 0, 60);
	private final ModelValueBoolean capturingPaused = new ModelValueBoolean("Capturing paused", false);
	private final ModelValue<RadioIdentity> radioIdentity = new ModelValue<RadioIdentity>("Radio",
			RadioIdentity.ABSENT);
	private final ModelValue<McpStatus> mcpStatus = new ModelValue<McpStatus>("MCP", McpStatus.OFF);
	private final ModelValue<String> selectedSerial = new ModelValue<String>("Serial", "");
	private final ModelValueBoolean clkoutEnable = new ModelValueBoolean("CLKOUT", false);
	private final ModelValueBoolean radioReleased = new ModelValueBoolean("Radio released", false);
	private final ModelValueInt persistentDisplayPersTime = new ModelValueInt("Persistence time", 30, 1, 1, 60);
	private final ModelValueInt peakFallRateSecs = new ModelValueInt("Peak fall rate", 15);
	private final ModelValueBoolean persistentDisplay = new ModelValueBoolean("Persistent display", true);
	private final ModelValueInt samples = new ModelValueInt("Samples", SweepSamples.SAMPLES_PER_BLOCK,
			SweepSamples.SAMPLES_PER_BLOCK, SweepSamples.SAMPLES_PER_BLOCK, SweepSamples.MAX_SAMPLES)
	{
		@Override
		public void setValue(Integer value)
		{
			super.setValue(SweepSamples.requireValid(value));
		}
	};
	private final ModelValueBoolean showPeaks = new ModelValueBoolean("Show peaks", true);
	private final ModelValueBoolean powerAutoScale = new ModelValueBoolean("Auto-scale dB axis", true);
	private final ModelValueBoolean autoGain = new ModelValueBoolean("Auto gain", true);
	private final ModelValueBoolean debugDisplay = new ModelValueBoolean("Debug", false);
	private final ModelValue<BigDecimal> spectrumLineThickness = new ModelValue<BigDecimal>("Spectrum line thickness",
			new BigDecimal("1"));
	private final ModelValueInt spectrumPaletteSize = new ModelValueInt("Spectrum palette size", 0);
	private final ModelValueInt spectrumPaletteStart = new ModelValueInt("Spectrum palette start", 0);
	private final ModelValueBoolean spurRemoval = new ModelValueBoolean("Spur removal", false);
	private final ModelValueBoolean waterfallVisible = new ModelValueBoolean("Waterfall visible", true);
	private final ModelValueBoolean listening = new ModelValueBoolean("Listening", false);
	private final ModelValue<ListenService> listenService = new ModelValue<ListenService>("Listen service",
			ListenService.FM);
	private final ModelValueInt listenKHz = new ModelValueInt("Listen [kHz]", 97300, 200,
			FmChannelPlan.FIRST_CENTER_KHZ, FmChannelPlan.LAST_CENTER_KHZ);
	private final ModelValueInt tvChannel = new ModelValueInt("TV channel", 14, 1, TvChannelPlan.FIRST_FCC_CHANNEL,
			TvChannelPlan.LAST_FCC_CHANNEL);
	private final ModelValueInt listenVolume = new ModelValueInt("Volume", 80, 1, 0, 100);
	private final ModelValue<List<FmStationHit>> detectedFmStations = new ModelValue<List<FmStationHit>>(
			"Detected FM", List.of());
	private final ModelValue<List<TvStationHit>> detectedTvStations = new ModelValue<List<TvStationHit>>(
			"Detected TV", List.of());

	public void setHardware(Hardware hardware)
	{
		this.hardware = hardware == null ? Hardware.NOOP : hardware;
	}

	public boolean isRadioSetting(ModelValue<?> value)
	{
		return value == frequency || value == fftBinHz || value == samples || value == gainLNA || value == gainVGA
				|| value == gainTotal || value == antennaPower || value == antennaLNA || value == selectedSerial
				|| value == clkoutEnable;
	}

	public void fireHardwareStatusChanged(boolean hardwareSendingData)
	{
		List<HackRFEventListener> copy;
		synchronized (listeners)
		{
			copy = new ArrayList<HackRFEventListener>(listeners);
		}
		for (HackRFEventListener listener : copy)
			listener.hardwareStatusChanged(hardwareSendingData);
	}

	public void fireCaptureStateChanged(boolean isCapturing)
	{
		List<HackRFEventListener> copy;
		synchronized (listeners)
		{
			copy = new ArrayList<HackRFEventListener>(listeners);
		}
		for (HackRFEventListener listener : copy)
			listener.captureStateChanged(isCapturing);
	}

	@Override
	public ModelValueBoolean getAntennaPowerEnable()
	{
		return antennaPower;
	}

	@Override
	public ModelValueBoolean getAntennaLNA()
	{
		return antennaLNA;
	}

	@Override
	public ModelValueInt getFFTBinHz()
	{
		return fftBinHz;
	}

	@Override
	public ModelValue<FrequencyRange> getFrequency()
	{
		return frequency;
	}

	@Override
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable()
	{
		return frequencyAllocationTable;
	}

	@Override
	public ModelValueInt getGain()
	{
		return gainTotal;
	}

	@Override
	public ModelValueInt getGainLNA()
	{
		return gainLNA;
	}

	@Override
	public ModelValueInt getPersistentDisplayDecayRate()
	{
		return persistentDisplayPersTime;
	}

	@Override
	public ModelValueBoolean isDebugDisplay()
	{
		return debugDisplay;
	}

	@Override
	public ModelValueInt getSamples()
	{
		return samples;
	}

	@Override
	public ModelValueInt getSpectrumPaletteSize()
	{
		return spectrumPaletteSize;
	}

	@Override
	public ModelValueBoolean isPersistentDisplayVisible()
	{
		return persistentDisplay;
	}

	@Override
	public ModelValueBoolean isWaterfallVisible()
	{
		return waterfallVisible;
	}

	@Override
	public ModelValueInt getSpectrumPaletteStart()
	{
		return spectrumPaletteStart;
	}

	@Override
	public ModelValueInt getPeakFallRate()
	{
		return peakFallRateSecs;
	}

	@Override
	public ModelValue<BigDecimal> getSpectrumLineThickness()
	{
		return spectrumLineThickness;
	}

	@Override
	public ModelValueInt getGainVGA()
	{
		return gainVGA;
	}

	@Override
	public ModelValueBoolean isCapturingPaused()
	{
		return capturingPaused;
	}

	@Override
	public ModelValue<RadioIdentity> getRadioIdentity()
	{
		return radioIdentity;
	}

	@Override
	public ModelValue<McpStatus> getMcpStatus()
	{
		return mcpStatus;
	}

	@Override
	public ModelValue<String> getSelectedSerial()
	{
		return selectedSerial;
	}

	@Override
	public ModelValueBoolean getClkoutEnable()
	{
		return clkoutEnable;
	}

	@Override
	public ModelValueBoolean isRadioReleased()
	{
		return radioReleased;
	}

	@Override
	public void restartSweep()
	{
		listening.setValue(false);
		radioReleased.setValue(false);
		hardware.restartSweep();
	}

	@Override
	public void releaseRadio()
	{
		listening.setValue(false);
		radioReleased.setValue(true);
		hardware.releaseRadio();
	}

	@Override
	public void startListen()
	{
		listenService.setValue(ListenService.FM);
		listening.setValue(true);
		radioReleased.setValue(false);
		hardware.startListen();
	}

	@Override
	public void startWatch()
	{
		listenService.setValue(ListenService.TV);
		listening.setValue(true);
		radioReleased.setValue(false);
		hardware.startWatch();
	}

	@Override
	public void stopListen()
	{
		if (!Boolean.TRUE.equals(listening.getValue()))
			return;
		listening.setValue(false);
		if (!Boolean.TRUE.equals(radioReleased.getValue()))
			hardware.restartSweep();
	}

	@Override
	public ModelValueBoolean isListening()
	{
		return listening;
	}

	@Override
	public ModelValueInt getListenKHz()
	{
		return listenKHz;
	}

	@Override
	public ModelValueInt getListenVolume()
	{
		return listenVolume;
	}

	@Override
	public ModelValue<List<FmStationHit>> getDetectedFmStations()
	{
		return detectedFmStations;
	}

	@Override
	public ModelValue<ListenService> getListenService()
	{
		return listenService;
	}

	@Override
	public ModelValueInt getTvChannel()
	{
		return tvChannel;
	}

	@Override
	public ModelValue<List<TvStationHit>> getDetectedTvStations()
	{
		return detectedTvStations;
	}

	public RadioMode radioMode()
	{
		return RadioMode.from(Boolean.TRUE.equals(radioReleased.getValue()),
				Boolean.TRUE.equals(listening.getValue()), listenService.getValue());
	}

	@Override
	public List<String> listRadioSerials()
	{
		List<String> serials = hardware.listRadioSerials();
		return serials == null ? Collections.emptyList() : serials;
	}

	@Override
	public ModelValueBoolean isChartsPeaksVisible()
	{
		return showPeaks;
	}

	@Override
	public ModelValueBoolean isPowerAutoScale()
	{
		return powerAutoScale;
	}

	@Override
	public ModelValueBoolean isAutoGain()
	{
		return autoGain;
	}

	@Override
	public ModelValueBoolean isFilterSpectrum()
	{
		return filterSpectrum;
	}

	@Override
	public ModelValueBoolean isSpurRemoval()
	{
		return spurRemoval;
	}

	@Override
	public void registerListener(HackRFEventListener listener)
	{
		if (listener == null)
			return;
		synchronized (listeners)
		{
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(HackRFEventListener listener)
	{
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}
}
