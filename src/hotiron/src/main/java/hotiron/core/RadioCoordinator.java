package hotiron.core;

/**
 * USB apply path for radio settings. Spinner {@code setValue} is
 * {@link Source#OPERATOR}. {@link #applyAutoSweep} / {@link #applyAutoGain}
 * run as {@link Source#AUTO_POLICY} so they do not turn Auto off or
 * double-fire USB. MCP parks via {@link HackRFSettings#startListen()} /
 * {@code startWatch()}; dial and channel still flow through {@link #bind()}.
 */
public final class RadioCoordinator
{
	public enum Source
	{
		OPERATOR, AUTO_POLICY
	}

	public interface Usb
	{
		void applyNow();

		void applyDebounced();

		Usb NOOP = new Usb()
		{
			@Override
			public void applyNow()
			{
			}

			@Override
			public void applyDebounced()
			{
			}
		};
	}

	private final HackRFSettings settings;
	private final Usb usb;
	private final AutoGainPolicy.Loop autoGain;
	private boolean bound;
	private Source source = Source.OPERATOR;
	private boolean usbCoalesced;
	private boolean splittingGain;

	public RadioCoordinator(HackRFSettings settings, Usb usb, AutoGainPolicy.Loop autoGain)
	{
		if (settings == null)
			throw new IllegalArgumentException("settings");
		this.settings = settings;
		this.usb = usb == null ? Usb.NOOP : usb;
		this.autoGain = autoGain == null ? new AutoGainPolicy.Loop() : autoGain;
	}

	/** Current write source; {@link Source#OPERATOR} except inside Auto applies. */
	public Source source()
	{
		return source;
	}

	public synchronized void bind()
	{
		if (bound)
			return;
		bound = true;
		settings.getFrequency().addListener(this::onFrequencyRadio);
		settings.getAntennaPowerEnable().addListener(this::applyNow);
		settings.getAntennaLNA().addListener(this::applyNow);
		settings.getFFTBinHz().addListener(this::onFftOrSamples);
		settings.getSamples().addListener(this::onFftOrSamples);
		settings.isAutoSweep().addListener(this::onAutoSweepEnabled);
		settings.getSelectedSerial().addListener(this::applyNow);
		settings.getClkoutEnable().addListener(this::applyNow);
		settings.getListenKHz().addListener(this::onListenKHz);
		settings.getTvChannel().addListener(this::onTvChannel);
		settings.getGain().addListener(this::onGainTotal);
		settings.getGainLNA().addListener(this::onGainSplit);
		settings.getGainVGA().addListener(this::onGainSplit);
		settings.isAutoGain().addListener(this::onAutoGainEnabled);
	}

	/**
	 * Write FFT Bin / samples for {@code range} as Auto policy. Does not
	 * turn Auto off. {@code restart} requests USB after a real change.
	 */
	public boolean applyAutoSweep(FrequencyRange range, boolean restart)
	{
		boolean[] changed = { false };
		runAs(Source.AUTO_POLICY, true, () -> changed[0] = AutoSweepPolicy.apply(settings, range));
		if (restart && changed[0])
			applyNow();
		return changed[0];
	}

	/**
	 * Write LNA-then-VGA total as Auto policy. Does not turn Auto off.
	 * {@code restart} false coalesces with a pending frequency apply.
	 */
	public void applyAutoGain(int totalGain, boolean restart)
	{
		int snapped = GainPolicy.clampTotal(totalGain);
		runAs(Source.AUTO_POLICY, !restart, () -> {
			if (settings.getGain().getValue() != snapped)
				settings.getGain().setValue(snapped);
		});
	}

	public void recalculateGains(int totalGain)
	{
		int lnaGain = GainPolicy.lnaGain(totalGain);
		int vgaGain = GainPolicy.vgaGain(totalGain);
		settings.getGainLNA().setValue(lnaGain);
		settings.getGainVGA().setValue(vgaGain);
		settings.getGain().setValue(lnaGain + vgaGain);
	}

	public void maybeSeedAutoGain(FrequencyRange range, boolean scanActive, long nowMs)
	{
		if (range == null || !SweepFramePolicy.maySeedAutoGain(settings, scanActive))
			return;
		Integer seed = autoGain.seedIfBandShifted(range.getStartMHz(), range.getEndMHz(),
				settings.getGain().getValue());
		if (seed == null)
			return;
		autoGain.markSettling(nowMs);
		applyAutoGain(seed.intValue(), false);
	}

	public void considerAutoGain(DatasetSpectrum ds, FrequencyRange range, boolean scanActive, long nowMs)
	{
		if (ds == null || range == null)
			return;
		if (!SweepFramePolicy.mayConsiderAutoGain(settings, scanActive))
			return;
		AutoGainPolicy.Observation obs = AutoGainPolicy.observe(ds, settings.getGain().getValue(),
				range.getStartMHz(), range.getEndMHz());
		Integer next = autoGain.consider(obs, nowMs);
		if (next == null || next.intValue() == settings.getGain().getValue())
			return;
		autoGain.markSettling(nowMs);
		applyAutoGain(next.intValue(), true);
	}

	private void onFrequencyRadio()
	{
		if (parked())
			return;
		applyAutoSweep(settings.getFrequency().getValue(), false);
		applyDebounced();
	}

	private void onFftOrSamples()
	{
		if (!isAuto() && Boolean.TRUE.equals(settings.isAutoSweep().getValue()))
			settings.isAutoSweep().setValue(false);
		if (!parked() && !usbCoalesced)
			applyNow();
	}

	private void onAutoSweepEnabled(Boolean on)
	{
		if (!Boolean.TRUE.equals(on))
			return;
		applyAutoSweep(settings.getFrequency().getValue(), true);
	}

	private void onListenKHz()
	{
		if (parked() && settings.getListenService().getValue() == ListenService.FM)
			applyNow();
	}

	private void onTvChannel()
	{
		if (parked() && settings.getListenService().getValue() == ListenService.TV)
			applyNow();
	}

	private void onGainTotal(Integer gainTotal)
	{
		if (splittingGain)
			return;
		recalculateGains(gainTotal);
		if (!usbCoalesced)
			applyNow();
		if (!isAuto() && Boolean.TRUE.equals(settings.isAutoGain().getValue()))
			settings.isAutoGain().setValue(false);
	}

	private void onGainSplit()
	{
		int totalGain = settings.getGainLNA().getValue() + settings.getGainVGA().getValue();
		splittingGain = true;
		try
		{
			settings.getGain().setValue(totalGain);
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
		finally
		{
			splittingGain = false;
		}
		if (!isAuto() && Boolean.TRUE.equals(settings.isAutoGain().getValue()))
			settings.isAutoGain().setValue(false);
		if (!usbCoalesced && !isAuto())
			applyNow();
	}

	private void onAutoGainEnabled(Boolean on)
	{
		if (!Boolean.TRUE.equals(on))
			return;
		autoGain.reset();
		FrequencyRange range = settings.getFrequency().getValue();
		Integer seed = autoGain.seedIfBandShifted(range.getStartMHz(), range.getEndMHz(),
				settings.getGain().getValue());
		if (seed == null)
			return;
		autoGain.markSettling(System.currentTimeMillis());
		applyAutoGain(seed.intValue(), true);
	}

	private void applyNow()
	{
		if (released())
			return;
		usb.applyNow();
	}

	private void applyDebounced()
	{
		if (released())
			return;
		usb.applyDebounced();
	}

	private void runAs(Source src, boolean coalesceUsb, Runnable body)
	{
		Source prev = source;
		boolean prevCoalesce = usbCoalesced;
		source = src;
		usbCoalesced = coalesceUsb;
		try
		{
			body.run();
		}
		finally
		{
			source = prev;
			usbCoalesced = prevCoalesce;
		}
	}

	private boolean isAuto()
	{
		return source == Source.AUTO_POLICY;
	}

	private boolean parked()
	{
		return settings.radioMode().parked();
	}

	private boolean released()
	{
		return settings.radioMode() == RadioMode.STOPPED;
	}
}
