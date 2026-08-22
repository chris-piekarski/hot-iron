/*
 * Offline lock check: synthetic 8VSB-like IF → atsc_rx_process.
 * Not a full ATSC modulator (no trellis/RS), so MPEG packets may stay 0;
 * segment outputs from atsc_sync should be > 0 if FPLL+timing work.
 */
#include "atsc_rx.h"
#include "atsc_dc_blocker.h"
#include "atsc_rx_filter.h"
#include <gnuradio/dtv/atsc_consts.h>
#include <gnuradio/filter/mmse_fir_interpolator_ff.h>
#include <gnuradio/math.h>
#include <gnuradio/nco.h>
#include <volk/volk.h>
#include <algorithm>
#include <cmath>
#include <complex>
#include <cstdint>
#include <cstdlib>
#include <cstdio>
#include <vector>

static constexpr double PI = 3.14159265358979323846;
static constexpr double SYM = 4.5e6 / 286 * 684;
static constexpr double PILOT = -3e6 + 0.309e6;

static int volk_selftest()
{
	float a[67];
	float b[67];
	float product[67];
	float difference[67];
	float expected = 0;
	for (int i = 0; i < 67; i++)
	{
		a[i] = (i - 31) * 0.03125f;
		b[i] = (17 - i) * 0.015625f;
		expected += a[i] * b[i];
	}
	float actual = 0;
	volk_32f_x2_dot_prod_32f(&actual, a, b, 67);
	volk_32f_s32f_multiply_32f(product, a, 2.5f, 67);
	volk_32f_x2_subtract_32f(difference, product, b, 67);
	if (std::fabs(actual - expected) > 1e-4f
			|| std::fabs(difference[66] - (a[66] * 2.5f - b[66])) > 1e-6f)
	{
		fprintf(stderr, "selftest: SIMD VOLK mismatch expected=%g actual=%g\n", expected, actual);
		return 1;
	}
	fprintf(stderr, "selftest: SIMD VOLK dot error=%g\n", std::fabs(actual - expected));
	return 0;
}

static int dc_blocker_selftest()
{
	AtscDcBlocker dc(32);
	float output = 0;
	for (int i = 0; i < 4096; i++)
		output = dc.filter(1.f);
	if (std::fabs(output) > 1e-4f)
	{
		fprintf(stderr, "selftest: long DC blocker leaked constant input=%g\n", output);
		return 1;
	}

	AtscDcBlocker ac(32);
	double power = 0;
	int count = 0;
	for (int i = 0; i < 4096; i++)
	{
		float value = ac.filter((float) std::sin(i * 0.2));
		if (i > 256)
		{
			power += value * value;
			count++;
		}
	}
	float rms = (float) std::sqrt(power / count);
	if (rms < 0.5f || rms > 0.9f)
	{
		fprintf(stderr, "selftest: long DC blocker damaged AC input rms=%g\n", rms);
		return 1;
	}
	fprintf(stderr, "selftest: long DC blocker constant=%g ac_rms=%g\n", output, rms);
	return 0;
}

static int atan_selftest()
{
	gr::gr_complex a(2.f, 3.f);
	gr::gr_complex b(-4.f, 5.f);
	gr::gr_complex expected = a * b;
	gr::fast_cc_multiply(a, a, b);
	if (std::abs(a - expected) > 1e-6f)
	{
		fprintf(stderr, "selftest: fast complex multiply is not alias-safe\n");
		return 1;
	}
	double max_error = 0;
	for (int yi = -100; yi <= 100; yi++)
		for (int xi = -100; xi <= 100; xi++)
		{
			float y = yi / 17.f;
			float x = xi / 19.f;
			max_error = std::max(max_error,
					(double) std::fabs(gr::fast_atan2f(y, x) - std::atan2(y, x)));
		}
	if (max_error > 1e-5)
	{
		fprintf(stderr, "selftest: fast atan2 error=%g\n", max_error);
		return 1;
	}
	fprintf(stderr, "selftest: fast atan2 error=%g\n", max_error);
	return 0;
}

static int mmse_selftest()
{
	gr::filter::mmse_fir_interpolator_ff interp;
	float impulse[8]{};
	impulse[3] = 1;
	if (std::fabs(interp.interpolate(impulse, 0.f) - 1.f) > 1e-7f)
		return 1;
	impulse[3] = 0;
	impulse[4] = 1;
	if (std::fabs(interp.interpolate(impulse, 1.f) - 1.f) > 1e-7f)
		return 1;

	float input[8]{ 1, 2, 3, 4, 5, 6, 7, 8 };
	const float published[8]{ -6.77751e-03f, 3.94578e-02f, -1.42658e-01f,
			6.09836e-01f, 6.09836e-01f, -1.42658e-01f, 3.94578e-02f, -6.77751e-03f };
	float expected = 0;
	for (int i = 0; i < 8; i++)
		expected += input[i] * published[7 - i];
	float actual = interp.interpolate(input, 0.5f);
	if (std::fabs(actual - expected) > 1e-5f)
	{
		fprintf(stderr, "selftest: MMSE interpolator mismatch expected=%g actual=%g\n",
				expected, actual);
		return 1;
	}
	fprintf(stderr, "selftest: MMSE interpolator matches GNU Radio taps\n");
	return 0;
}

static int nco_selftest()
{
	gr::nco<float, float> osc;
	double phase = 0.2;
	double freq = -1.234;
	osc.set_phase(phase);
	osc.set_freq(freq);
	double max_error = 0;
	for (int i = 0; i < 100000; i++)
	{
		osc.step();
		phase += freq;
		float s;
		float c;
		osc.sincos(&s, &c);
		max_error = std::max(max_error,
				std::max(std::fabs(s - std::sin(phase)), std::fabs(c - std::cos(phase))));
		float phase_adjust = (float) (0.01 * std::sin(i * 0.013));
		float freq_adjust = (float) (0.00002 * std::cos(i * 0.007));
		osc.adjust_phase(phase_adjust);
		osc.adjust_freq(freq_adjust);
		phase += phase_adjust;
		freq += freq_adjust;
		phase = std::remainder(phase, 2 * PI);
	}
	if (max_error > 1e-4)
	{
		fprintf(stderr, "selftest: recursive NCO error=%g\n", max_error);
		return 1;
	}
	fprintf(stderr, "selftest: recursive NCO error=%g\n", max_error);
	return 0;
}

static std::vector<std::complex<float>> filter_in_chunks(
		const std::vector<std::complex<float>>& in, int chunk, double input_rate)
{
	AtscRxFilter filter(input_rate, gr::dtv::ATSC_SYMBOL_RATE * 1.1);
	std::vector<std::complex<float>> out;
	for (size_t off = 0; off < in.size();)
	{
		size_t n = std::min((size_t) chunk, in.size() - off);
		filter.process(in.data() + off, n, out);
		off += n;
	}
	return out;
}

static std::vector<std::complex<float>> filter_int8_in_chunks(
		const std::vector<int8_t>& in, int complex_chunk, double input_rate)
{
	AtscRxFilter filter(input_rate, gr::dtv::ATSC_SYMBOL_RATE * 1.1);
	std::vector<std::complex<float>> out;
	size_t complex_count = in.size() / 2;
	for (size_t off = 0; off < complex_count;)
	{
		size_t n = std::min((size_t) complex_chunk, complex_count - off);
		filter.process_int8(in.data() + 2 * off, n, false, out);
		off += n;
	}
	return out;
}

static double tone_rms(double hz, double input_rate)
{
	const int n = 200000;
	std::vector<std::complex<float>> in((size_t) n);
	for (int i = 0; i < n; i++)
	{
		double phase = 2 * PI * hz * i / input_rate;
		in[(size_t) i] = std::complex<float>((float) std::cos(phase), (float) std::sin(phase));
	}
	std::vector<std::complex<float>> out = filter_in_chunks(in, 4093, input_rate);
	double power = 0;
	size_t first = std::min((size_t) 2048, out.size());
	for (size_t i = first; i < out.size(); i++)
		power += std::norm(out[i]);
	return out.size() > first ? std::sqrt(power / (out.size() - first)) : 0;
}

static int filter_selftest(double input_rate)
{
	std::vector<std::complex<float>> in(65536);
	uint32_t state = 0x12345678u;
	for (std::complex<float>& sample : in)
	{
		state = state * 1664525u + 1013904223u;
		float i = (float) (int32_t) state / 2147483648.f;
		state = state * 1664525u + 1013904223u;
		float q = (float) (int32_t) state / 2147483648.f;
		sample = { i, q };
	}
	std::vector<std::complex<float>> whole =
			filter_in_chunks(in, (int) in.size(), input_rate);
	std::vector<std::complex<float>> chunked = filter_in_chunks(in, 257, input_rate);
	if (whole.size() != chunked.size())
	{
		fprintf(stderr, "selftest: resampler chunk sizes changed output length\n");
		return 1;
	}
	long expected_size = std::lround(in.size()
			* (gr::dtv::ATSC_SYMBOL_RATE * 1.1 / input_rate));
	if (std::llabs((long long) whole.size() - expected_size) > 2)
	{
		fprintf(stderr, "selftest: resampler rate mismatch input_rate=%g expected=%ld actual=%zu\n",
				input_rate, expected_size, whole.size());
		return 1;
	}
	float max_error = 0;
	for (size_t i = 0; i < whole.size(); i++)
		max_error = std::max(max_error, std::abs(whole[i] - chunked[i]));
	if (max_error > 1e-6f)
	{
		fprintf(stderr, "selftest: resampler is not streaming-continuous (error=%g)\n", max_error);
		return 1;
	}
	std::vector<int8_t> in8(in.size() * 2);
	for (size_t i = 0; i < in.size(); i++)
	{
		in8[2 * i] = (int8_t) std::lround(std::max(-127.f, std::min(127.f, in[i].real() * 127.f)));
		in8[2 * i + 1] =
				(int8_t) std::lround(std::max(-127.f, std::min(127.f, in[i].imag() * 127.f)));
	}
	std::vector<std::complex<float>> int8_whole =
			filter_int8_in_chunks(in8, (int) in.size(), input_rate);
	std::vector<std::complex<float>> int8_chunked =
			filter_int8_in_chunks(in8, 257, input_rate);
	if (int8_whole.size() != int8_chunked.size())
	{
		fprintf(stderr, "selftest: int8 resampler chunk sizes changed output length\n");
		return 1;
	}
	for (size_t i = 0; i < int8_whole.size(); i++)
		max_error = std::max(max_error, std::abs(int8_whole[i] - int8_chunked[i]));
	if (max_error > 1e-6f)
	{
		fprintf(stderr, "selftest: int8 resampler is not streaming-continuous (error=%g)\n", max_error);
		return 1;
	}

	double pass = tone_rms(1e6, input_rate);
	double stop = tone_rms(input_rate * 0.4, input_rate);
	if (pass <= 0 || stop >= pass * 0.05)
	{
		fprintf(stderr, "selftest: RRC did not reject aliases (pass=%g stop=%g)\n", pass, stop);
		return 1;
	}
	fprintf(stderr, "selftest: PFB RRC rate=%g pass=%g stop=%g outputs=%zu\n",
			input_rate, pass, stop, whole.size());
	return 0;
}

int main()
{
	if (volk_selftest() != 0)
		return 1;
	if (dc_blocker_selftest() != 0)
		return 1;
	if (atan_selftest() != 0)
		return 1;
	if (mmse_selftest() != 0)
		return 1;
	if (nco_selftest() != 0)
		return 1;
	if (filter_selftest(20e6) != 0)
		return 1;
	if (filter_selftest(16e6) != 0)
		return 1;

	const double fs = 12e6;
	const int seconds = 1;
	const int n = (int) (fs * seconds);
	void* rx = atsc_rx_create(fs);
	if (!rx)
	{
		fprintf(stderr, "create failed\n");
		return 1;
	}
	std::vector<int8_t> iq(4096);
	std::vector<uint8_t> ts(188 * 64);
	float snr = 0;
	int wrote = 0;
	int off = 0;
	for (int i = 0; i < n; i++)
	{
		double t = i / fs;
		int si = (int) (t * SYM);
		int pos = si % 832;
		float lvl;
		if (pos == 0)
			lvl = 5.f;
		else if (pos == 1 || pos == 2)
			lvl = -5.f;
		else if (pos == 3)
			lvl = 5.f;
		else
			lvl = ((si >> 2) & 1) ? 3.f : -3.f;
		lvl += 1.25f;
		double w = 2.0 * PI * PILOT * t;
		float I = lvl * (float) std::cos(w);
		float Q = lvl * (float) std::sin(w);
		int i8 = (int) std::lround(I * 12.f);
		int q8 = (int) std::lround(Q * 12.f);
		if (i8 > 127)
			i8 = 127;
		if (i8 < -127)
			i8 = -127;
		if (q8 > 127)
			q8 = 127;
		if (q8 < -127)
			q8 = -127;
		iq[off++] = (int8_t) i8;
		iq[off++] = (int8_t) q8;
		if (off == (int) iq.size())
		{
			wrote += atsc_rx_process(rx, iq.data(), off, ts.data(), (int) ts.size(), &snr);
			off = 0;
		}
	}
	if (off)
		wrote += atsc_rx_process(rx, iq.data(), off, ts.data(), (int) ts.size(), &snr);
	int64_t counters[ATSC_RX_DEBUG_COUNTERS]{};
	float gauges[ATSC_RX_DEBUG_GAUGES]{};
	if (atsc_rx_debug(rx, counters, ATSC_RX_DEBUG_COUNTERS,
				gauges, ATSC_RX_DEBUG_GAUGES) != 1
			|| counters[9] != n || !(gauges[0] > 0))
	{
		fprintf(stderr, "selftest: debug snapshot invalid iq=%lld agc=%g\n",
				(long long) counters[9], gauges[0]);
		atsc_rx_destroy(rx);
		return 1;
	}
	fprintf(stderr, "selftest: packets_from_process=%d locked=%d packets=%d\n", wrote,
			atsc_rx_locked(rx), atsc_rx_packets(rx));
	atsc_rx_destroy(rx);
	return 0;
}
