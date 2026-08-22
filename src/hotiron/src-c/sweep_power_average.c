#include "sweep_power_average.h"

#include <math.h>
#include <string.h>

int sweep_samples_to_bytes(
	uint32_t samples_per_frequency,
	uint32_t samples_per_block,
	uint32_t* num_bytes)
{
	if (num_bytes == NULL || samples_per_block == 0 ||
	    samples_per_frequency < samples_per_block ||
	    samples_per_frequency % samples_per_block != 0 ||
	    samples_per_frequency > UINT32_MAX / 2) {
		return 0;
	}
	*num_bytes = samples_per_frequency * 2;
	return 1;
}

int sweep_power_average_init(
	sweep_power_average* average,
	size_t bin_count,
	uint32_t samples_per_frequency,
	uint32_t samples_per_block,
	double* sums,
	size_t sums_capacity)
{
	uint32_t num_bytes;

	if (average == NULL || bin_count == 0 || sums == NULL ||
	    sums_capacity < bin_count ||
	    !sweep_samples_to_bytes(
		    samples_per_frequency,
		    samples_per_block,
		    &num_bytes)) {
		return 0;
	}

	memset(average, 0, sizeof(*average));
	(void) num_bytes;
	average->sums = sums;
	average->bin_count = bin_count;
	average->block_target = samples_per_frequency / samples_per_block;
	memset(average->sums, 0, average->bin_count * sizeof(*average->sums));
	return 1;
}

void sweep_power_average_reset(sweep_power_average* average)
{
	if (average == NULL || average->sums == NULL) {
		return;
	}
	memset(average->sums, 0, average->bin_count * sizeof(*average->sums));
	average->block_count = 0;
}

void sweep_power_average_destroy(sweep_power_average* average)
{
	if (average == NULL) {
		return;
	}
	memset(average, 0, sizeof(*average));
}

int sweep_power_average_begin_frequency(
	sweep_power_average* average,
	uint64_t frequency)
{
	if (average == NULL || average->sums == NULL) {
		return 0;
	}
	if (average->block_count != 0 && average->frequency == frequency) {
		return 0;
	}
	if (average->block_count != 0) {
		sweep_power_average_reset(average);
	}
	average->frequency = frequency;
	return 1;
}

int sweep_power_average_push(
	sweep_power_average* average,
	const float* linear_power,
	float* output_db)
{
	size_t i;
	double divisor;

	if (average == NULL || average->sums == NULL || linear_power == NULL ||
	    output_db == NULL || average->block_target == 0) {
		return 0;
	}

	for (i = 0; i < average->bin_count; i++) {
		average->sums[i] += linear_power[i];
	}
	average->block_count++;
	if (average->block_count < average->block_target) {
		return 0;
	}

	divisor = average->block_count;
	for (i = 0; i < average->bin_count; i++) {
		output_db[i] = (float) (10.0 * log10(average->sums[i] / divisor));
	}
	sweep_power_average_reset(average);
	return 1;
}
