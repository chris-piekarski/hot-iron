#ifndef ATSC_SHIM_MATH_H
#define ATSC_SHIM_MATH_H
#include <array>
#include <cmath>
#include <complex>
#ifndef GR_M_PI
#define GR_M_PI 3.14159265358979323846
#endif
namespace gr {
using gr_complex = std::complex<float>;
inline void fast_cc_multiply(gr_complex& out, const gr_complex& a, const gr_complex& b)
{
	float ar = a.real();
	float ai = a.imag();
	float br = b.real();
	float bi = b.imag();
	out.real(ar * br - ai * bi);
	out.imag(ar * bi + ai * br);
}

inline const std::array<float, 257>& fast_atan_table()
{
	static const std::array<float, 257> table = [] {
		std::array<float, 257> values{};
		for (std::size_t i = 0; i < values.size(); i++)
			values[i] = std::atan((float) i / 255.f);
		return values;
	}();
	return table;
}

inline float fast_atan2f(float y, float x)
{
	constexpr float RESOLUTION = 1.f / 255.f;
	constexpr float HALF_PI = 1.57079632679489661923f;
	constexpr float PI = 3.14159265358979323846f;
	float ay = std::fabs(y);
	float ax = std::fabs(x);
	if (!(ay > 0.f || ax > 0.f))
		return 0.f;
	float z = ay < ax ? ay / ax : ax / ay;
	float base;
	if (z < RESOLUTION)
		base = z;
	else
	{
		float scaled = z * 255.f;
		int index = ((int) scaled) & 0xff;
		float fraction = scaled - index;
		const std::array<float, 257>& table = fast_atan_table();
		base = table[(std::size_t) index]
				+ (table[(std::size_t) index + 1] - table[(std::size_t) index]) * fraction;
	}
	if (ax > ay)
	{
		if (x >= 0.f)
			return y >= 0.f ? base : -base;
		return y >= 0.f ? PI - base : base - PI;
	}
	if (y >= 0.f)
		return x >= 0.f ? HALF_PI - base : HALF_PI + base;
	return x >= 0.f ? -HALF_PI + base : -HALF_PI - base;
}
} // namespace gr
#endif
