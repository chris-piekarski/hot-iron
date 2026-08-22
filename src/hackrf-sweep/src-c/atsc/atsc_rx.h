#ifndef HACKRF_ATSC_RX_H
#define HACKRF_ATSC_RX_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void* atsc_rx_create(double input_rate_hz);
void atsc_rx_destroy(void* rx);
void atsc_rx_set_invert(void* rx, int invert);
/* Convert int8 IQ (interleaved) into MPEG-TS. Returns bytes written (multiple of 188). */
int atsc_rx_process(void* rx, const int8_t* iq, int nbytes, uint8_t* ts_out, int ts_cap,
		float* snr_db);
int atsc_rx_locked(void* rx);
int atsc_rx_packets(void* rx);
int atsc_rx_bad_packets(void* rx);
/*
 * Debug schema v1.
 * counters: packets, bad, good, segments, fieldSegments, pendingBaseband,
 *           rsGoodWindow, rsWindow, inverted, totalIqSamples
 * gauges:   agcGain, rmsIq, rmsBaseband, rsGoodRatioDb,
 *           equalizerMainTap, equalizerPeakTap
 */
#define ATSC_RX_DEBUG_COUNTERS 10
#define ATSC_RX_DEBUG_GAUGES 6
int atsc_rx_debug(void* rx, int64_t* counters, int counter_cap, float* gauges, int gauge_cap);

#ifdef __cplusplus
}
#endif

#endif
