
#ifndef HACKRF_SWEEP_H_
#define HACKRF_SWEEP_H_

#include <stdint.h>

#ifdef _WIN32
   #define ADD_EXPORTS

  /* You should define ADD_EXPORTS *only* when building the DLL. */
  #ifdef ADD_EXPORTS
    #define ADDAPI __declspec(dllexport)
  #else
    #define ADDAPI __declspec(dllimport)
  #endif

  /* Define calling convention in one place, for convenience. */
  #define ADDCALL __cdecl

#else /* _WIN32 not defined. */

  /* Define with no value on non-Windows OSes. */
  #define ADDAPI
  #define ADDCALL

#endif


/**
 * Only one instance is supported at a time. num_samples is the number captured
 * per tuning step and must be a multiple of 8192. Each hardware block gets one
 * FFT; multiple blocks are averaged in linear power before callback delivery.
 */
ADDAPI int hackrf_sweep_lib_start( void (*_fft_power_callback)(char full_sweep_done, int bins, double* freqStart,  float fft_bin_Hz, float* powerdBm),
uint32_t freq_min, uint32_t freq_max, uint32_t fft_bin_width, uint32_t num_samples, unsigned int lna_gain, unsigned int vga_gain, unsigned int _antennaPowerEnable, unsigned int _enableAntennaLNA);
ADDAPI void hackrf_sweep_lib_stop();
/* Call before start(). serial_number NULL/empty = first device. clkout_enable: 1 = 10 MHz CLKOUT. */
ADDAPI void hackrf_sweep_lib_config(const char* serial_number, unsigned int clkout_enable);

#endif /* HACKRF_SWEEP_H_ */
