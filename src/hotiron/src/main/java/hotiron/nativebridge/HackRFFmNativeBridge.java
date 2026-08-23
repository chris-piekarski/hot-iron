package hotiron.nativebridge;

import com.sun.jna.CallbackThreadInitializer;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import hotiron.jna.HackrfSweepLibrary;
import hotiron.jna.HackrfSweepLibrary.hackrf_fm_lib_start__iq_callback_callback;
import hotiron.core.WfmDemodulator;

/**
 * Parked IQ RX. Loads the same {@code hackrf-sweep} library as the sweep
 * bridge. Exclusive: Java must stop and join sweep before {@link #start}.
 */
public final class HackRFFmNativeBridge
{
	public interface IqCallback
	{
		void newIq(byte[] iq);
	}

	private HackRFFmNativeBridge()
	{
	}

	static
	{
		/* Ensure the sweep bridge has registered Native.load of the .so. */
		HackRFSweepNativeBridge.class.getName();
	}

	public static synchronized void configure(String serialNumber, boolean clkoutEnable)
	{
		HackrfSweepLibrary.hackrf_fm_lib_config(
				serialNumber == null || serialNumber.isEmpty() ? null : serialNumber, clkoutEnable ? 1 : 0);
	}

	/**
	 * Blocking until {@link #stop}. {@code freqHz} is the HackRF LO
	 * (dial minus {@link WfmDemodulator#OFFSET_HZ}).
	 */
	public static synchronized int start(IqCallback callback, long freqHz, int sampleRate, int lnaGain, int vgaGain,
			boolean antennaPower, boolean antennaLna)
	{
		hackrf_fm_lib_start__iq_callback_callback nativeCb = new hackrf_fm_lib_start__iq_callback_callback()
		{
			@Override
			public void apply(Pointer iq, int nbytes)
			{
				if (callback == null || iq == null || nbytes <= 0)
					return;
				callback.newIq(iq.getByteArray(0, nbytes));
			}
		};
		Native.setCallbackThreadInitializer(nativeCb, new CallbackThreadInitializer(true));
		int rate = sampleRate > 0 ? sampleRate : WfmDemodulator.IQ_RATE_HZ;
		return HackrfSweepLibrary.hackrf_fm_lib_start(nativeCb, freqHz, rate, lnaGain, vgaGain, antennaPower ? 1 : 0,
				antennaLna ? 1 : 0);
	}

	public static void stop()
	{
		HackrfSweepLibrary.hackrf_fm_lib_stop();
	}

	/** Live LNA/VGA while parked IQ is running. Does not reopen USB. */
	public static int setGains(int lnaGain, int vgaGain)
	{
		return HackrfSweepLibrary.hackrf_fm_lib_set_gains(lnaGain, vgaGain);
	}
}
