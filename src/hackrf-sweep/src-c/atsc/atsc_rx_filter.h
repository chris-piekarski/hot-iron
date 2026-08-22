#ifndef HACKRF_ATSC_RX_FILTER_H
#define HACKRF_ATSC_RX_FILTER_H

#include <complex>
#include <cstddef>
#include <cstdint>
#include <vector>

/*
 * GNU Radio-compatible ATSC matched filter and arbitrary resampler.
 *
 * The prototype RRC is split into 16 polyphase arms so filtering happens
 * before samples are discarded.  That ordering is required when converting
 * a wide HackRF capture to ATSC_SYMBOL_RATE * sps.
 */
class AtscRxFilter
{
public:
	static constexpr unsigned NFILTERS = 16;

	AtscRxFilter(double input_rate_hz, double output_rate_hz);

	/* Append filtered/resampled samples to out. State is retained across calls. */
	void process(const std::complex<float>* input, std::size_t count,
			std::vector<std::complex<float>>& out);
	void process_int8(const int8_t* interleaved_iq, std::size_t complex_count, bool invert,
			std::vector<std::complex<float>>& out);

	unsigned taps_per_filter() const
	{
		return taps_per_filter_;
	}

private:
	static std::vector<float> root_raised_cosine(double gain, double sampling_freq,
			double symbol_rate, double alpha, int ntaps);
	static std::vector<std::vector<float>> split_taps(const std::vector<float>& taps,
			unsigned taps_per_filter);
	std::complex<float> filter(unsigned phase) const;
	void produce(std::vector<std::complex<float>>& out);

	std::vector<std::vector<float>> filters_;
	std::vector<std::vector<float>> diff_filters_;
	std::vector<std::complex<float>> input_;
	std::size_t input_offset_ = 0;
	unsigned taps_per_filter_ = 0;
	unsigned phase_ = 0;
	unsigned decimation_ = 0;
	double fractional_rate_ = 0;
	double accumulator_ = 0;
};

#endif
