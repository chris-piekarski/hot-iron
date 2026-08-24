package hotiron;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import hotiron.core.AnalyzerSettings;
import hotiron.core.FrequencyAllocationTable;
import hotiron.core.FrequencyRange;
import hotiron.core.HackRFSettings;
import hotiron.core.RadioIdentity;
import hotiron.mvc.ModelValue;
import hotiron.mvc.ModelValue.ModelValueBoolean;
import hotiron.mvc.ModelValue.ModelValueInt;

/**
 * {@link AnalyzerSettings} plus call counters for UI tests. No native / JFrame.
 */
public class FakeHackRFSettings implements HackRFSettings {
	public final AnalyzerSettings inner = new AnalyzerSettings();
	public int restartSweepCalls;
	public int releaseRadioCalls;
	public int startListenCalls;
	public int startWatchCalls;
	public int startSniffCalls;
	public int startBleSniffCalls;
	public int stopBleSniffCalls;
	public List<String> listedSerials = new ArrayList<String>();

	public FakeHackRFSettings() {
		inner.setHardware(new AnalyzerSettings.Hardware() {
			@Override
			public void restartSweep() {
				restartSweepCalls++;
			}

			@Override
			public void releaseRadio() {
				releaseRadioCalls++;
			}

			@Override
			public void startListen() {
				startListenCalls++;
			}

			@Override
			public void startWatch() {
				startWatchCalls++;
			}

			@Override
			public void startSniff() {
				startSniffCalls++;
			}

			@Override
			public void startBleSniff() {
				startBleSniffCalls++;
			}

			@Override
			public void stopBleSniff() {
				stopBleSniffCalls++;
			}

			@Override
			public List<String> listRadioSerials() {
				return listedSerials;
			}
		});
	}

	@Override
	public ModelValueBoolean getAntennaPowerEnable() {
		return inner.getAntennaPowerEnable();
	}

	@Override
	public ModelValueBoolean getAntennaLNA() {
		return inner.getAntennaLNA();
	}

	@Override
	public ModelValueInt getFFTBinHz() {
		return inner.getFFTBinHz();
	}

	@Override
	public ModelValue<FrequencyRange> getFrequency() {
		return inner.getFrequency();
	}

	@Override
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable() {
		return inner.getFrequencyAllocationTable();
	}

	@Override
	public ModelValueInt getGain() {
		return inner.getGain();
	}

	@Override
	public ModelValueInt getGainLNA() {
		return inner.getGainLNA();
	}

	@Override
	public ModelValueInt getPersistentDisplayDecayRate() {
		return inner.getPersistentDisplayDecayRate();
	}

	@Override
	public ModelValueBoolean isDebugDisplay() {
		return inner.isDebugDisplay();
	}

	@Override
	public ModelValueInt getSamples() {
		return inner.getSamples();
	}

	@Override
	public ModelValueInt getSpectrumPaletteSize() {
		return inner.getSpectrumPaletteSize();
	}

	@Override
	public ModelValueBoolean isPersistentDisplayVisible() {
		return inner.isPersistentDisplayVisible();
	}

	@Override
	public ModelValueBoolean isWaterfallVisible() {
		return inner.isWaterfallVisible();
	}

	@Override
	public ModelValueInt getSpectrumPaletteStart() {
		return inner.getSpectrumPaletteStart();
	}

	@Override
	public ModelValueInt getPeakFallRate() {
		return inner.getPeakFallRate();
	}

	@Override
	public ModelValue<BigDecimal> getSpectrumLineThickness() {
		return inner.getSpectrumLineThickness();
	}

	@Override
	public ModelValueInt getGainVGA() {
		return inner.getGainVGA();
	}

	@Override
	public ModelValueBoolean isCapturingPaused() {
		return inner.isCapturingPaused();
	}

	@Override
	public ModelValue<RadioIdentity> getRadioIdentity() {
		return inner.getRadioIdentity();
	}

	@Override
	public ModelValue<hotiron.core.McpStatus> getMcpStatus() {
		return inner.getMcpStatus();
	}

	@Override
	public ModelValue<String> getSelectedSerial() {
		return inner.getSelectedSerial();
	}

	@Override
	public ModelValueBoolean getClkoutEnable() {
		return inner.getClkoutEnable();
	}

	@Override
	public ModelValueBoolean isRadioReleased() {
		return inner.isRadioReleased();
	}

	@Override
	public void restartSweep() {
		inner.restartSweep();
	}

	@Override
	public void releaseRadio() {
		inner.releaseRadio();
	}

	@Override
	public void startListen() {
		inner.startListen();
	}

	@Override
	public void startWatch() {
		inner.startWatch();
	}

	@Override
	public void startSniff() {
		inner.startSniff();
	}

	@Override
	public void startBleSniff() {
		inner.startBleSniff();
	}

	@Override
	public void stopBleSniff() {
		inner.stopBleSniff();
	}

	@Override
	public ModelValueBoolean isBleSniffing() {
		return inner.isBleSniffing();
	}

	@Override
	public void stopListen() {
		inner.stopListen();
	}

	@Override
	public void startFmScan() {
		inner.startFmScan();
	}

	@Override
	public void startTvScan() {
		inner.startTvScan();
	}

	@Override
	public void startNfcScan() {
		inner.startNfcScan();
	}

	@Override
	public void stopScan() {
		inner.stopScan();
	}

	@Override
	public ModelValue<hotiron.core.BandScan> getBandScan() {
		return inner.getBandScan();
	}

	@Override
	public ModelValueBoolean isListening() {
		return inner.isListening();
	}

	@Override
	public ModelValueInt getListenKHz() {
		return inner.getListenKHz();
	}

	@Override
	public ModelValueInt getListenVolume() {
		return inner.getListenVolume();
	}

	@Override
	public ModelValue<java.util.List<hotiron.core.FmStationHit>> getDetectedFmStations() {
		return inner.getDetectedFmStations();
	}

	@Override
	public ModelValue<hotiron.core.ListenService> getListenService() {
		return inner.getListenService();
	}

	@Override
	public ModelValueInt getTvChannel() {
		return inner.getTvChannel();
	}

	@Override
	public ModelValue<java.util.List<hotiron.core.TvStationHit>> getDetectedTvStations() {
		return inner.getDetectedTvStations();
	}

	@Override
	public List<String> listRadioSerials() {
		return inner.listRadioSerials();
	}

	@Override
	public ModelValueBoolean isChartsPeaksVisible() {
		return inner.isChartsPeaksVisible();
	}

	@Override
	public ModelValueBoolean isPowerAutoScale() {
		return inner.isPowerAutoScale();
	}

	@Override
	public ModelValueBoolean isAutoGain() {
		return inner.isAutoGain();
	}

	@Override
	public ModelValueBoolean isAutoSweep() {
		return inner.isAutoSweep();
	}

	@Override
	public ModelValueBoolean isSpurRemoval() {
		return inner.isSpurRemoval();
	}

	@Override
	public void registerListener(HackRFEventListener listener) {
		inner.registerListener(listener);
	}

	@Override
	public void removeListener(HackRFEventListener listener) {
		inner.removeListener(listener);
	}

	public void fireHardwareStatusChanged(boolean hardwareSendingData) {
		inner.fireHardwareStatusChanged(hardwareSendingData);
	}

	public void fireCaptureStateChanged(boolean isCapturing) {
		inner.fireCaptureStateChanged(isCapturing);
	}
}
