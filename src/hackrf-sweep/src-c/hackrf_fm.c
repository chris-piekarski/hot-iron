/*
 * Continuous IQ RX for the analyzer's FM listen mode.
 * Linked into libhackrf-sweep; not a patch on hackrf_sweep.c.
 */

#include "hackrf_fm.h"

#include <hackrf.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
static void fm_sleep_ms(int ms)
{
	Sleep(ms);
}
#else
#include <unistd.h>
static void fm_sleep_ms(int ms)
{
	usleep((useconds_t) ms * 1000);
}
#endif

static hackrf_device* device = NULL;
static volatile int do_exit = 0;
static void (*iq_cb)(const int8_t*, int) = NULL;
static char lib_serial[64];
static unsigned int lib_clkout = 0;

void hackrf_fm_lib_config(const char* serial_number, unsigned int clkout_enable)
{
	lib_clkout = clkout_enable ? 1u : 0u;
	if (serial_number == NULL || serial_number[0] == '\0') {
		lib_serial[0] = '\0';
		return;
	}
	strncpy(lib_serial, serial_number, sizeof(lib_serial) - 1);
	lib_serial[sizeof(lib_serial) - 1] = '\0';
}

void hackrf_fm_lib_stop(void)
{
	do_exit = 1;
}

static int rx_callback(hackrf_transfer* transfer)
{
	if (do_exit)
		return -1;
	if (iq_cb != NULL && transfer != NULL && transfer->buffer != NULL && transfer->valid_length > 0)
		iq_cb((const int8_t*) transfer->buffer, transfer->valid_length);
	return 0;
}

int hackrf_fm_lib_start(
	void (*iq_callback)(const int8_t* iq, int nbytes),
	uint64_t freq_hz,
	uint32_t sample_rate,
	unsigned int lna_gain,
	unsigned int vga_gain,
	unsigned int antenna_power,
	unsigned int antenna_lna)
{
	int result;
	uint32_t filter_bw;
	const char* serial = lib_serial[0] ? lib_serial : NULL;

	do_exit = 0;
	iq_cb = iq_callback;
	device = NULL;

	if (sample_rate < 2000000)
		sample_rate = 2000000;
	if (lna_gain > 40)
		lna_gain = 40;
	if (vga_gain > 62)
		vga_gain = 62;

	result = hackrf_init();
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_fm: hackrf_init failed: %s (%d)\n", hackrf_error_name(result), result);
		return result;
	}

	result = hackrf_open_by_serial(serial, &device);
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_fm: open failed: %s (%d)\n", hackrf_error_name(result), result);
		hackrf_exit();
		device = NULL;
		return result;
	}

	if (lib_clkout) {
		result = hackrf_set_clkout_enable(device, 1);
		if (result != HACKRF_SUCCESS)
			fprintf(stderr, "hackrf_fm: clkout failed: %s (%d)\n", hackrf_error_name(result), result);
	}

	result = hackrf_set_sample_rate(device, sample_rate);
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_fm: sample_rate failed: %s (%d)\n", hackrf_error_name(result), result);
		goto done;
	}

	/* FM uses 4 MS/s (~2 MHz IF). ATSC Watch uses 16 MS/s / 8 MHz analog
	 * bandwidth, leaving 1 MHz guard on each side of the 6 MHz 8VSB brick. */
	filter_bw = hackrf_compute_baseband_filter_bw(sample_rate / 2);
	result = hackrf_set_baseband_filter_bandwidth(device, filter_bw);
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_fm: filter bw failed: %s (%d)\n", hackrf_error_name(result), result);
		goto done;
	}

	result = hackrf_set_freq(device, freq_hz);
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_fm: set_freq failed: %s (%d)\n", hackrf_error_name(result), result);
		goto done;
	}

	result = hackrf_set_vga_gain(device, vga_gain);
	if (result != HACKRF_SUCCESS)
		fprintf(stderr, "hackrf_fm: vga failed: %s (%d)\n", hackrf_error_name(result), result);
	result = hackrf_set_lna_gain(device, lna_gain);
	if (result != HACKRF_SUCCESS)
		fprintf(stderr, "hackrf_fm: lna failed: %s (%d)\n", hackrf_error_name(result), result);

	result = hackrf_set_amp_enable(device, antenna_lna ? 1 : 0);
	if (result != HACKRF_SUCCESS)
		fprintf(stderr, "hackrf_fm: amp failed: %s (%d)\n", hackrf_error_name(result), result);
	result = hackrf_set_antenna_enable(device, antenna_power ? 1 : 0);
	if (result != HACKRF_SUCCESS)
		fprintf(stderr, "hackrf_fm: antenna power failed: %s (%d)\n", hackrf_error_name(result), result);

	result = hackrf_start_rx(device, rx_callback, NULL);
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_fm: start_rx failed: %s (%d)\n", hackrf_error_name(result), result);
		goto done;
	}

	fprintf(stderr, "hackrf_fm: listening at %llu Hz, %u S/s\n",
		(unsigned long long) freq_hz, sample_rate);

	/*
	 * start_rx is asynchronous. is_streaming() is often not TRUE on the
	 * first check; do not treat that as "done" or listen exits immediately
	 * and the Java thread falls back to a sweep.
	 */
	while (!do_exit)
		fm_sleep_ms(10);

	hackrf_stop_rx(device);

done:
	if (device != NULL) {
		hackrf_close(device);
		device = NULL;
	}
	hackrf_exit();
	iq_cb = NULL;
	fprintf(stderr, "hackrf_fm: stopped\n");
	return 0;
}
