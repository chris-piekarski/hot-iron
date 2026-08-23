#ifndef HACKRF_ATSC_RX_H
#define HACKRF_ATSC_RX_H

#include <stdint.h>
#include "../hotiron_api.h"

HOTIRON_EXTERN_C_BEGIN

HOTIRON_API void* HOTIRON_CALL atsc_rx_create(double input_rate_hz);
HOTIRON_API void HOTIRON_CALL atsc_rx_destroy(void* rx);
HOTIRON_API void HOTIRON_CALL atsc_rx_set_invert(void* rx, int invert);
/* Convert int8 IQ (interleaved) into MPEG-TS. Returns bytes written (multiple of 188). */
HOTIRON_API int HOTIRON_CALL atsc_rx_process(
	void* rx,
	const int8_t* iq,
	int nbytes,
	uint8_t* ts_out,
	int ts_cap,
	float* snr_db);
HOTIRON_API int HOTIRON_CALL atsc_rx_locked(void* rx);
HOTIRON_API int HOTIRON_CALL atsc_rx_packets(void* rx);
/*
 * Debug schema v1.
 * counters: packets, bad, good, segments, fieldSegments, pendingBaseband,
 *           rsGoodWindow, rsWindow, inverted, totalIqSamples
 * gauges:   agcGain, rmsIq, rmsBaseband, rsGoodRatioDb,
 *           equalizerMainTap, equalizerPeakTap
 */
#define ATSC_RX_DEBUG_COUNTERS 10
#define ATSC_RX_DEBUG_GAUGES 6
HOTIRON_API int HOTIRON_CALL atsc_rx_debug(
	void* rx,
	int64_t* counters,
	int counter_cap,
	float* gauges,
	int gauge_cap);

HOTIRON_EXTERN_C_END

#endif /* HACKRF_ATSC_RX_H */
