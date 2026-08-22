#ifndef ATSC_SHIM_NCO_H
#define ATSC_SHIM_NCO_H
#include <cmath>
#include <cstdint>
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
namespace gr {
template <typename FREQ, typename PHASE>
class nco
{
	double d_sin_phase = 0;
	double d_cos_phase = 1;
	double d_sin_freq = 0;
	double d_cos_freq = 1;
	uint32_t d_steps = 0;

	static void rotate(double& s, double& c, double radians)
	{
		/* FPLL corrections are tiny; this avoids two libm calls per sample. */
		double x2 = radians * radians;
		double sin_x = radians * (1.0 - x2 / 6.0 + x2 * x2 / 120.0);
		double cos_x = 1.0 - x2 / 2.0 + x2 * x2 / 24.0;
		double next_s = s * cos_x + c * sin_x;
		double next_c = c * cos_x - s * sin_x;
		s = next_s;
		c = next_c;
	}

public:
	void set_freq(double f)
	{
		d_sin_freq = std::sin(f);
		d_cos_freq = std::cos(f);
	}
	void set_phase(double p)
	{
		d_sin_phase = std::sin(p);
		d_cos_phase = std::cos(p);
	}
	void step()
	{
		double next_s = d_sin_phase * d_cos_freq + d_cos_phase * d_sin_freq;
		double next_c = d_cos_phase * d_cos_freq - d_sin_phase * d_sin_freq;
		d_sin_phase = next_s;
		d_cos_phase = next_c;
		if ((++d_steps & 4095u) == 0)
		{
			double scale = 1.0 / std::sqrt(
					d_sin_phase * d_sin_phase + d_cos_phase * d_cos_phase);
			d_sin_phase *= scale;
			d_cos_phase *= scale;
			scale = 1.0 / std::sqrt(d_sin_freq * d_sin_freq + d_cos_freq * d_cos_freq);
			d_sin_freq *= scale;
			d_cos_freq *= scale;
		}
	}
	void sincos(float* s, float* c)
	{
		*s = (float) d_sin_phase;
		*c = (float) d_cos_phase;
	}
	void adjust_phase(float x) { rotate(d_sin_phase, d_cos_phase, x); }
	void adjust_freq(float x) { rotate(d_sin_freq, d_cos_freq, x); }
};
} // namespace gr
#endif
