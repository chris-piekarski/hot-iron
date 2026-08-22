#include "atsc_rx_filter.h"

#include <gnuradio/dtv/atsc_consts.h>

#include <algorithm>
#include <cmath>
#include <stdexcept>

namespace
{
constexpr double PI = 3.14159265358979323846;
constexpr double RRC_ALPHA = 0.1152;
constexpr int RRC_SYMS = 8;
}

AtscRxFilter::AtscRxFilter(double input_rate_hz, double output_rate_hz)
{
	if (input_rate_hz <= 0 || output_rate_hz <= 0)
		throw std::invalid_argument("ATSC sample rates must be positive");

	const double atsc_symbol_rate = gr::dtv::ATSC_SYMBOL_RATE;
	const double rrc_symbol_rate = atsc_symbol_rate / 2.0;
	const double sps = output_rate_hz / atsc_symbol_rate;
	const double filter_rate = input_rate_hz * NFILTERS;
	const int ntaps = (int) ((2 * RRC_SYMS + 1) * sps * NFILTERS);
	const double gain = NFILTERS * rrc_symbol_rate / filter_rate;
	std::vector<float> prototype = root_raised_cosine(
			gain, filter_rate, rrc_symbol_rate, RRC_ALPHA, ntaps);

	std::vector<float> diff(prototype.size(), 0.f);
	for (std::size_t i = 0; i + 1 < prototype.size(); i++)
		diff[i] = prototype[i + 1] - prototype[i];

	taps_per_filter_ = (unsigned) ((prototype.size() + NFILTERS - 1) / NFILTERS);
	filters_ = split_taps(prototype, taps_per_filter_);
	diff_filters_ = split_taps(diff, taps_per_filter_);

	const double rate = output_rate_hz / input_rate_hz;
	const double exact_decimation = NFILTERS / rate;
	decimation_ = (unsigned) std::floor(exact_decimation);
	fractional_rate_ = exact_decimation - decimation_;
	phase_ = (unsigned) ((prototype.size() / 2) % NFILTERS);

	/* GNU Radio's history supplies taps_per_filter - 1 zero samples at startup. */
	input_.assign(taps_per_filter_ - 1, std::complex<float>(0.f, 0.f));
}

void AtscRxFilter::process(const std::complex<float>* input, std::size_t count,
		std::vector<std::complex<float>>& out)
{
	if (input == nullptr || count == 0)
		return;
	input_.insert(input_.end(), input, input + count);
	produce(out);
}

void AtscRxFilter::process_int8(const int8_t* interleaved_iq, std::size_t complex_count,
		bool invert, std::vector<std::complex<float>>& out)
{
	if (interleaved_iq == nullptr || complex_count == 0)
		return;
	input_.reserve(input_.size() + complex_count);
	const float q_scale = invert ? -1.f / 128.f : 1.f / 128.f;
	for (std::size_t i = 0; i < complex_count; i++)
		input_.emplace_back(interleaved_iq[2 * i] / 128.f, interleaved_iq[2 * i + 1] * q_scale);
	produce(out);
}

void AtscRxFilter::produce(std::vector<std::complex<float>>& out)
{
	while (input_offset_ + taps_per_filter_ <= input_.size())
	{
		out.push_back(filter(phase_));

		accumulator_ += fractional_rate_;
		unsigned carry = (unsigned) std::floor(accumulator_);
		accumulator_ -= carry;
		unsigned next = phase_ + decimation_ + carry;
		input_offset_ += next / NFILTERS;
		phase_ = next % NFILTERS;
	}

	/* Keep the partial FIR window and discard samples no future output can use. */
	if (input_offset_ > 8192)
	{
		input_.erase(input_.begin(), input_.begin() + (long) input_offset_);
		input_offset_ = 0;
	}
}

std::complex<float> AtscRxFilter::filter(unsigned phase) const
{
	std::complex<float> sum(0.f, 0.f);
	const std::vector<float>& taps = filters_[phase];
	const std::vector<float>& diff = diff_filters_[phase];
	const float mu = (float) accumulator_;
	for (unsigned i = 0; i < taps_per_filter_; i++)
	{
		unsigned tap = taps_per_filter_ - 1 - i;
		sum += input_[input_offset_ + i] * (taps[tap] + diff[tap] * mu);
	}
	return sum;
}

std::vector<std::vector<float>> AtscRxFilter::split_taps(const std::vector<float>& taps,
		unsigned taps_per_filter)
{
	std::vector<std::vector<float>> bank(
			NFILTERS, std::vector<float>(taps_per_filter, 0.f));
	for (unsigned phase = 0; phase < NFILTERS; phase++)
		for (unsigned i = 0; i < taps_per_filter; i++)
		{
			std::size_t source = phase + (std::size_t) i * NFILTERS;
			if (source < taps.size())
				bank[phase][i] = taps[source];
		}
	return bank;
}

std::vector<float> AtscRxFilter::root_raised_cosine(double gain, double sampling_freq,
		double symbol_rate, double alpha, int ntaps)
{
	/* This is gr::filter::firdes::root_raised_cosine, kept local to avoid
	 * pulling the GNU Radio runtime into the application. */
	ntaps |= 1;
	const double spb = sampling_freq / symbol_rate;
	std::vector<float> taps((std::size_t) ntaps);
	double scale = 0;
	const int center = ntaps / 2;
	for (int i = 0; i < ntaps; i++)
	{
		const double xindx = i - center;
		const double x1 = PI * xindx / spb;
		double x2 = 4 * alpha * xindx / spb;
		double x3 = x2 * x2 - 1;
		double num;
		double den;
		if (std::fabs(x3) >= 0.000001)
		{
			if (i != center)
				num = std::cos((1 + alpha) * x1)
						+ std::sin((1 - alpha) * x1) / (4 * alpha * xindx / spb);
			else
				num = std::cos((1 + alpha) * x1) + (1 - alpha) * PI / (4 * alpha);
			den = x3 * PI;
		}
		else
		{
			if (alpha == 1)
			{
				taps[(std::size_t) i] = -1;
				scale += taps[(std::size_t) i];
				continue;
			}
			x3 = (1 - alpha) * x1;
			x2 = (1 + alpha) * x1;
			num = std::sin(x2) * (1 + alpha) * PI
					- std::cos(x3) * ((1 - alpha) * PI * spb) / (4 * alpha * xindx)
					+ std::sin(x3) * spb * spb / (4 * alpha * xindx * xindx);
			den = -32 * PI * alpha * alpha * xindx / spb;
		}
		taps[(std::size_t) i] = (float) (4 * alpha * num / den);
		scale += taps[(std::size_t) i];
	}
	for (float& tap : taps)
		tap = (float) (tap * gain / scale);
	return taps;
}
