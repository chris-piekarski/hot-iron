package hotiron.core;

import java.util.Objects;

/**
 * Snapshot of the radio settings that require a native restart.
 * Display-only options (peaks, auto-scale, palette) are not included.
 */
public final class SweepConfig
{
	public static final int FREQUENCY_APPLY_DEBOUNCE_MS = 120;

	public final int startMHz;
	public final int endMHz;
	public final int fftBinHz;
	public final int samples;
	public final int lnaGain;
	public final int vgaGain;
	public final boolean antennaPower;
	public final boolean antennaLna;
	public final boolean clkout;
	public final String serial;

	public SweepConfig(int startMHz, int endMHz, int fftBinHz, int samples, int lnaGain, int vgaGain,
			boolean antennaPower, boolean antennaLna, boolean clkout, String serial)
	{
		this.startMHz = startMHz;
		this.endMHz = endMHz;
		this.fftBinHz = fftBinHz;
		this.samples = SweepSamples.requireValid(samples);
		this.lnaGain = lnaGain;
		this.vgaGain = vgaGain;
		this.antennaPower = antennaPower;
		this.antennaLna = antennaLna;
		this.clkout = clkout;
		this.serial = serial == null ? "" : serial;
	}

	public static SweepConfig from(HackRFSettings settings)
	{
		if (settings == null)
			throw new IllegalArgumentException("settings");
		FrequencyRange range = settings.getFrequency().getValue();
		return new SweepConfig(range.getStartMHz(), range.getEndMHz(), settings.getFFTBinHz().getValue(),
				settings.getSamples().getValue(), settings.getGainLNA().getValue(),
				settings.getGainVGA().getValue(), settings.getAntennaPowerEnable().getValue(),
				settings.getAntennaLNA().getValue(), settings.getClkoutEnable().getValue(),
				settings.getSelectedSerial().getValue());
	}

	/**
	 * After joining the previous sweep, start only if the radio is still
	 * wanted and no newer apply is already queued.
	 */
	public static boolean shouldStartAfterStop(boolean released, boolean newerQueued)
	{
		return !released && !newerQueued;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof SweepConfig))
			return false;
		SweepConfig o = (SweepConfig) obj;
		return startMHz == o.startMHz && endMHz == o.endMHz && fftBinHz == o.fftBinHz && samples == o.samples
				&& lnaGain == o.lnaGain && vgaGain == o.vgaGain && antennaPower == o.antennaPower
				&& antennaLna == o.antennaLna && clkout == o.clkout && serial.equals(o.serial);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(startMHz, endMHz, fftBinHz, samples, lnaGain, vgaGain, antennaPower, antennaLna,
				clkout, serial);
	}

	@Override
	public String toString()
	{
		return startMHz + "-" + endMHz + "MHz fft=" + fftBinHz + " n=" + samples;
	}
}
