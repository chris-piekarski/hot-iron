#include "sweep_power_average.h"

#include <math.h>
#include <stdio.h>

#define CHECK(condition, message) \
	do { \
		if (!(condition)) { \
			fprintf(stderr, "sweep power average self-test failed: %s\n", message); \
			return 1; \
		} \
	} while (0)

static int near(float actual, float expected)
{
	return fabsf(actual - expected) < 0.0001f;
}

int main(void)
{
	sweep_power_average average = {0};
	double sums[2] = {0};
	float output[2] = {0};
	const float first[2] = {1.0f, 0.01f};
	const float second[2] = {9.0f, 0.09f};
	uint32_t num_bytes = 0;

	CHECK(sweep_samples_to_bytes(8192, 8192, &num_bytes) && num_bytes == 16384,
	      "one hardware block should map to 16384 bytes");
	CHECK(sweep_samples_to_bytes(262144, 8192, &num_bytes) && num_bytes == 524288,
	      "maximum UI dwell should map to 32 hardware blocks");
	CHECK(!sweep_samples_to_bytes(10000, 8192, &num_bytes),
	      "reject partial blocks before configuring firmware");

	CHECK(!sweep_power_average_init(&average, 2, 4096, 8192, sums, 2),
	      "reject fewer than one hardware block");
	CHECK(!sweep_power_average_init(&average, 2, 10000, 8192, sums, 2),
	      "reject partial hardware blocks");
	CHECK(!sweep_power_average_init(&average, 2, 8192, 8192, sums, 1),
	      "reject undersized caller-owned accumulation storage");

	CHECK(sweep_power_average_init(&average, 2, 8192, 8192, sums, 2),
	      "initialize one-block average");
	CHECK(sweep_power_average_begin_frequency(&average, 100),
	      "first block should begin a frequency dwell");
	CHECK(sweep_power_average_push(&average, first, output),
	      "one block should complete immediately");
	CHECK(near(output[0], 0.0f) && near(output[1], -20.0f),
	      "one-block output should preserve dB power");
	CHECK(sweep_power_average_begin_frequency(&average, 100),
	      "same frequency should begin a new dwell after completion");
	sweep_power_average_destroy(&average);

	CHECK(sweep_power_average_init(&average, 2, 16384, 8192, sums, 2),
	      "initialize two-block average");
	CHECK(sweep_power_average_begin_frequency(&average, 100),
	      "first block should begin a two-block dwell");
	CHECK(!sweep_power_average_push(&average, first, output),
	      "first of two blocks should remain pending");
	CHECK(!sweep_power_average_begin_frequency(&average, 100),
	      "second block at the same frequency should continue the dwell");
	CHECK(sweep_power_average_push(&average, second, output),
	      "second block should complete the average");
	CHECK(near(output[0], 10.0f * log10f(5.0f)) &&
		      near(output[1], 10.0f * log10f(0.05f)),
	      "blocks must be averaged in linear power");

	CHECK(sweep_power_average_begin_frequency(&average, 100),
	      "completed average should begin a new same-frequency dwell");
	CHECK(!sweep_power_average_push(&average, first, output),
	      "new dwell should remain pending after one block");
	CHECK(sweep_power_average_begin_frequency(&average, 200),
	      "frequency change should discard a partial dwell");
	CHECK(!sweep_power_average_push(&average, second, output),
	      "replacement frequency should start from one block");
	CHECK(!sweep_power_average_begin_frequency(&average, 200),
	      "replacement frequency should continue its dwell");
	CHECK(sweep_power_average_push(&average, second, output),
	      "replacement frequency should complete independently");
	CHECK(near(output[0], 10.0f * log10f(9.0f)) &&
		      near(output[1], 10.0f * log10f(0.09f)),
	      "discarded dwell must not leak power into the next frequency");
	sweep_power_average_destroy(&average);

	puts("sweep power average self-test passed");
	return 0;
}
