#ifndef ATSC_SHIM_SINCOS_H
#define ATSC_SHIM_SINCOS_H
#include <cmath>
namespace gr {
inline void sincos(double x, double* s, double* c)
{
	*s = std::sin(x);
	*c = std::cos(x);
}
} // namespace gr
#endif
