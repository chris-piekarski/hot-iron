#ifndef SWEEP_POWER_AVERAGE_H_
#define SWEEP_POWER_AVERAGE_H_

#include <stddef.h>
#include <stdint.h>

typedef struct sweep_power_average {
	size_t bin_count;
	uint32_t block_target;
	uint32_t block_count;
	uint64_t frequency;
	double* sums;
} sweep_power_average;

/**
 * Configure averaging for one FFT per fixed-size HackRF sweep block.
 *
 * Returns zero when samples_per_frequency is not a positive multiple of
 * samples_per_block or sums_capacity is too small.
 */
int sweep_power_average_init(
	sweep_power_average* average,
	size_t bin_count,
	uint32_t samples_per_frequency,
	uint32_t samples_per_block,
	double* sums,
	size_t sums_capacity);

void sweep_power_average_reset(sweep_power_average* average);
void sweep_power_average_destroy(sweep_power_average* average);

/** Validate a sample count and convert IQ samples to firmware sweep bytes. */
int sweep_samples_to_bytes(
	uint32_t samples_per_frequency,
	uint32_t samples_per_block,
	uint32_t* num_bytes);

/**
 * Start or continue a frequency dwell. Returns one for the first block in a
 * dwell. A frequency change before completion discards the partial average.
 */
int sweep_power_average_begin_frequency(
	sweep_power_average* average,
	uint64_t frequency);

/**
 * Add one FFT's linear power bins. Returns one when output_db contains the
 * completed linear-power average converted to dB, otherwise zero.
 */
int sweep_power_average_push(
	sweep_power_average* average,
	const float* linear_power,
	float* output_db);

#endif /* SWEEP_POWER_AVERAGE_H_ */
