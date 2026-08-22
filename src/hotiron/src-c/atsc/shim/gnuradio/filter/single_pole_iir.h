#ifndef ATSC_SHIM_SINGLE_POLE_IIR_H
#define ATSC_SHIM_SINGLE_POLE_IIR_H
#include <complex>
namespace gr {
namespace filter {
template <typename T, typename TAP, typename OUT>
class single_pole_iir
{
	T d_prev{};
	TAP d_alpha{};

public:
	void set_taps(TAP a) { d_alpha = a; }
	T filter(T x)
	{
		d_prev = (T) ((1 - d_alpha) * d_prev + d_alpha * x);
		return d_prev;
	}
};

template <>
class single_pole_iir<std::complex<float>, float, float>
{
	std::complex<float> d_prev{ 0, 0 };
	float d_alpha = 0;

public:
	void set_taps(float a) { d_alpha = a; }
	std::complex<float> filter(std::complex<float> x)
	{
		d_prev = (1.f - d_alpha) * d_prev + d_alpha * x;
		return d_prev;
	}
};

template <>
class single_pole_iir<std::complex<float>, std::complex<float>, float>
{
	std::complex<float> d_prev{ 0, 0 };
	float d_alpha = 0;

public:
	void set_taps(float a) { d_alpha = a; }
	std::complex<float> filter(std::complex<float> x)
	{
		d_prev = (1.f - d_alpha) * d_prev + d_alpha * x;
		return d_prev;
	}
};
} // namespace filter
} // namespace gr
#endif
