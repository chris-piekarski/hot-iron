
#ifndef HACKRF_SWEEP_H_
#define HACKRF_SWEEP_H_

#include <stdint.h>
#include "hotiron_api.h"

HOTIRON_EXTERN_C_BEGIN

/**
 * Only one instance is supported at a time. num_samples is the number captured
 * per tuning step and must be a multiple of 8192. Each hardware block gets one
 * FFT; multiple blocks are averaged in linear power before callback delivery.
 */
HOTIRON_API int HOTIRON_CALL hackrf_sweep_lib_start(
	void (*fft_power_callback)(
		char full_sweep_done,
		int bins,
		double* freq_start,
		float fft_bin_hz,
		float* power_dbm),
	uint32_t freq_min,
	uint32_t freq_max,
	uint32_t fft_bin_width,
	uint32_t num_samples,
	unsigned int lna_gain,
	unsigned int vga_gain,
	unsigned int antenna_power_enable,
	unsigned int antenna_lna_enable);
HOTIRON_API void HOTIRON_CALL hackrf_sweep_lib_stop(void);
/* Call before start(). serial_number NULL/empty = first device. clkout_enable: 1 = 10 MHz CLKOUT. */
HOTIRON_API void HOTIRON_CALL hackrf_sweep_lib_config(
	const char* serial_number,
	unsigned int clkout_enable);

HOTIRON_EXTERN_C_END

#endif /* HACKRF_SWEEP_H_ */