#ifndef HACKRF_FM_H_
#define HACKRF_FM_H_

#include <stdint.h>

#ifdef _WIN32
  #define ADD_EXPORTS
  #ifdef ADD_EXPORTS
    #define ADDAPI __declspec(dllexport)
  #else
    #define ADDAPI __declspec(dllimport)
  #endif
  #define ADDCALL __cdecl
#else
  #define ADDAPI
  #define ADDCALL
#endif

/**
 * Parked WFM RX. Exclusive with hackrf_sweep_lib_start — Java must
 * stop the sweep and join before calling start.
 */
ADDAPI int hackrf_fm_lib_start(
	void (*iq_callback)(const int8_t* iq, int nbytes),
	uint64_t freq_hz,
	uint32_t sample_rate,
	unsigned int lna_gain,
	unsigned int vga_gain,
	unsigned int antenna_power,
	unsigned int antenna_lna);
ADDAPI void hackrf_fm_lib_stop(void);
ADDAPI void hackrf_fm_lib_config(const char* serial_number, unsigned int clkout_enable);

#endif
