#ifndef HACKRF_FM_H_
#define HACKRF_FM_H_

#include <stdint.h>
#include "hotiron_api.h"

HOTIRON_EXTERN_C_BEGIN

/**
 * Parked WFM RX. Exclusive with hackrf_sweep_lib_start — Java must
 * stop the sweep and join before calling start.
 */
HOTIRON_API int HOTIRON_CALL hackrf_fm_lib_start(
	void (*iq_callback)(const int8_t* iq, int nbytes),
	uint64_t freq_hz,
	uint32_t sample_rate,
	unsigned int lna_gain,
	unsigned int vga_gain,
	unsigned int antenna_power,
	unsigned int antenna_lna);
HOTIRON_API void HOTIRON_CALL hackrf_fm_lib_stop(void);
HOTIRON_API void HOTIRON_CALL hackrf_fm_lib_config(const char* serial_number, unsigned int clkout_enable);
/* Change LNA/VGA while RX is running. Does not restart USB. */
HOTIRON_API int HOTIRON_CALL hackrf_fm_lib_set_gains(unsigned int lna_gain, unsigned int vga_gain);

HOTIRON_EXTERN_C_END

#endif /* HACKRF_FM_H_ */
