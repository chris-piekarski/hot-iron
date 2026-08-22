#ifndef ATSC_SHIM_VOLK_H
#define ATSC_SHIM_VOLK_H
#include <algorithm>
#if defined(__SSE__)
#include <xmmintrin.h>
#endif
inline int volk_get_alignment() { return 16; }
inline void volk_32f_x2_dot_prod_32f(float* out, const float* a, const float* b, unsigned n)
{
	float s = 0;
#if defined(__SSE__)
	unsigned i = 0;
	__m128 sum = _mm_setzero_ps();
	for (; i + 4 <= n; i += 4)
		sum = _mm_add_ps(sum, _mm_mul_ps(_mm_loadu_ps(a + i), _mm_loadu_ps(b + i)));
	sum = _mm_add_ps(sum, _mm_movehl_ps(sum, sum));
	sum = _mm_add_ss(sum, _mm_shuffle_ps(sum, sum, _MM_SHUFFLE(1, 1, 1, 1)));
	s = _mm_cvtss_f32(sum);
	for (; i < n; i++)
		s += a[i] * b[i];
#else
	for (unsigned i = 0; i < n; i++)
		s += a[i] * b[i];
#endif
	*out = s;
}
inline void volk_32f_s32f_multiply_32f(float* o, const float* a, float s, unsigned n)
{
#if defined(__SSE__)
	unsigned i = 0;
	__m128 scale = _mm_set1_ps(s);
	for (; i + 4 <= n; i += 4)
		_mm_storeu_ps(o + i, _mm_mul_ps(_mm_loadu_ps(a + i), scale));
	for (; i < n; i++)
		o[i] = a[i] * s;
#else
	for (unsigned i = 0; i < n; i++)
		o[i] = a[i] * s;
#endif
}
inline void volk_32f_x2_subtract_32f(float* o, const float* a, const float* b, unsigned n)
{
#if defined(__SSE__)
	unsigned i = 0;
	for (; i + 4 <= n; i += 4)
		_mm_storeu_ps(o + i, _mm_sub_ps(_mm_loadu_ps(a + i), _mm_loadu_ps(b + i)));
	for (; i < n; i++)
		o[i] = a[i] - b[i];
#else
	for (unsigned i = 0; i < n; i++)
		o[i] = a[i] - b[i];
#endif
}
#endif
