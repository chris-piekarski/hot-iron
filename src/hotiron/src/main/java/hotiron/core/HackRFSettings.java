package hotiron.core;

import java.math.BigDecimal;

import hotiron.mvc.ModelValue;
import hotiron.mvc.ModelValue.ModelValueBoolean;
import hotiron.mvc.ModelValue.ModelValueInt;

public interface HackRFSettings {
	public static abstract class HackRFEventAdapter implements HackRFEventListener {
		@Override
		public void captureStateChanged(boolean isCapturing) {

		}

		@Override
		public void hardwareStatusChanged(boolean hardwareSendingData) {

		}
	}

	public static interface HackRFEventListener {
		public void captureStateChanged(boolean isCapturing);

		public void hardwareStatusChanged(boolean hardwareSendingData);
	}

	public ModelValueBoolean getAntennaPowerEnable();

	public ModelValueBoolean getAntennaLNA();

	public ModelValueInt getFFTBinHz();

	public ModelValue<FrequencyRange> getFrequency();

	public ModelValueInt getGain();

	public ModelValueInt getGainLNA();
	
	public ModelValueInt getPersistentDisplayDecayRate();
	
	public ModelValueBoolean isDebugDisplay();

	public ModelValueInt getSamples();

	public ModelValueInt getSpectrumPaletteSize();
	
	public ModelValueBoolean isPersistentDisplayVisible();
	public ModelValueBoolean isWaterfallVisible();

	public ModelValueInt getSpectrumPaletteStart();
	
	public ModelValueInt getPeakFallRate();
	
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable();

	public ModelValue<BigDecimal> getSpectrumLineThickness();
	
	public ModelValueInt getGainVGA();

	public ModelValueBoolean isCapturingPaused();

	/** Attached radio board / serial / firmware. {@link RadioIdentity#ABSENT} when none. */
	public ModelValue<RadioIdentity> getRadioIdentity();

	/** MCP listen state and connected clients. {@link McpStatus#OFF} when not started. */
	public ModelValue<McpStatus> getMcpStatus();

	/** Empty string = first radio found. */
	public ModelValue<String> getSelectedSerial();

	/** Drive 10 MHz CLKOUT (CLKIN is selected automatically when present). */
	public ModelValueBoolean getClkoutEnable();

	/** True when the native sweep is stopped and USB is released. */
	public ModelValueBoolean isRadioReleased();

	/** Exclusive USB mode from released / parked / {@link ListenService}. */
	public default RadioMode radioMode()
	{
		return RadioMode.of(this);
	}

	public void restartSweep();

	public void releaseRadio();

	/** Parked WFM receiver. Stops the sweep; USB stays owned. */
	public void startListen();

	/** Parked ATSC 1.0 receiver. Stops the sweep; USB stays owned. */
	public void startWatch();

	/** Leave listen mode and resume the sweep unless Stop was pressed. */
	public void stopListen();

	/** Sweep the FM band and pin those hits as the Seek list. */
	public void startFmScan();

	/** Sweep VHF then UHF TV and pin those hits as the Seek list. */
	public void startTvScan();

	/** Dwell 12–15 MHz then 27.12 / 40.68 MHz NFC harmonics. */
	public void startNfcScan();

	public void stopScan();

	public ModelValue<BandScan> getBandScan();

	public ModelValueBoolean isListening();

	/** US FM dial in kHz (88100–107900, step 200). */
	public ModelValueInt getListenKHz();

	public ModelValueInt getListenVolume();

	/** Live FM detections for the tuner knob (may be empty). */
	public ModelValue<java.util.List<FmStationHit>> getDetectedFmStations();

	public ModelValue<ListenService> getListenService();

	/** US TV channel 2–36 (47 CFR 73.603 / ATSC 1.0). */
	public ModelValueInt getTvChannel();

	public ModelValue<java.util.List<TvStationHit>> getDetectedTvStations();

	/** USB serials currently visible to libhackrf (may be empty). */
	public java.util.List<String> listRadioSerials();

	public ModelValueBoolean isChartsPeaksVisible();

	/** Live dB-axis auto-scale. Off = fixed −100…+20. */
	public ModelValueBoolean isPowerAutoScale();

	/** Live LNA/VGA AGC. Off = operator sliders. */
	public ModelValueBoolean isAutoGain();

	/** Live FFT Bin / samples from sweep span. Off = operator spinners. */
	public ModelValueBoolean isAutoSweep();

	public ModelValueBoolean isSpurRemoval();

	public void registerListener(HackRFEventListener listener);

	public void removeListener(HackRFEventListener listener);
}
