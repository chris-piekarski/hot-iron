package hotiron.jna;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
/**
 * JNA bindings for {@code libhackrf-sweep}. Hand-maintained — do not regenerate
 * with JNAerator. Keep in sync with {@code src-c/hackrf_sweep.h} and
 * {@code src-c/hackrf_fm.h}.
 */
public class HackrfSweepLibrary implements Library {
	public interface hackrf_sweep_lib_start__fft_power_callback_callback extends Callback {
		void apply(byte full_sweep_done, int bins, DoubleByReference freqStart, float fft_bin_Hz, FloatByReference powerdBm);
	};
	public interface hackrf_fm_lib_start__iq_callback_callback extends Callback {
		void apply(Pointer iq, int nbytes);
	};
	/**
	 * only ONE instance running is supported at any time<br>
	 * Original signature : <code>int hackrf_sweep_lib_start(hackrf_sweep_lib_start__fft_power_callback_callback*, uint32_t, uint32_t, uint32_t, uint32_t, unsigned int, unsigned int, unsigned int, unsigned int)</code>
	 */
	public static native int hackrf_sweep_lib_start(HackrfSweepLibrary.hackrf_sweep_lib_start__fft_power_callback_callback _fft_power_callback, int freq_min, int freq_max, int fft_bin_width, int num_samples, int lna_gain, int vga_gain, int _antennaPowerEnable, int _enableAntennaLNA);
	/** Original signature : <code>void hackrf_sweep_lib_stop()</code> */
	public static native void hackrf_sweep_lib_stop();
	/** Call before start. {@code serial_number} null/empty = first device. */
	public static native void hackrf_sweep_lib_config(String serial_number, int clkout_enable);

	public static native int hackrf_fm_lib_start(HackrfSweepLibrary.hackrf_fm_lib_start__iq_callback_callback iq_callback,
			long freq_hz, int sample_rate, int lna_gain, int vga_gain, int antenna_power, int antenna_lna);

	public static native void hackrf_fm_lib_stop();

	public static native void hackrf_fm_lib_config(String serial_number, int clkout_enable);

	public static native Pointer atsc_rx_create(double input_rate_hz);

	public static native void atsc_rx_destroy(Pointer rx);

	public static native void atsc_rx_set_invert(Pointer rx, int invert);

	public static native int atsc_rx_process(Pointer rx, byte[] iq, int nbytes, byte[] ts_out, int ts_cap,
			FloatByReference snr_db);

	public static native int atsc_rx_locked(Pointer rx);

	public static native int atsc_rx_packets(Pointer rx);

	public static native int atsc_rx_bad_packets(Pointer rx);

	public static native int atsc_rx_debug(Pointer rx, long[] counters, int counterCap,
			float[] gauges, int gaugeCap);
}
