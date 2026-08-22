#ifndef ATSC_SHIM_MMSE_INTERP_H
#define ATSC_SHIM_MMSE_INTERP_H
#include <algorithm>
#include <array>
#include <cmath>
namespace gr {
namespace filter {
class mmse_fir_interpolator_ff
{
	static constexpr int NTAPS = 8;
	static constexpr int NSTEPS = 128;
	using TapBank = std::array<std::array<float, NTAPS>, NSTEPS + 1>;

	static TapBank build_taps()
	{
		/*
		 * Same MMSE design as GNU Radio's generated interpolator_taps.h:
		 * minimize error over -1/(4Ts)..+1/(4Ts), for positions -4..3.
		 */
		const double omega = 3.14159265358979323846 / 2.0;
		double gram[NTAPS][NTAPS];
		for (int i = 0; i < NTAPS; i++)
			for (int j = 0; j < NTAPS; j++)
			{
				int delta = j - i;
				gram[i][j] = delta == 0 ? 2 * omega
						: 2 * std::sin(omega * delta) / delta;
			}

		TapBank bank{};
		for (int step = 0; step <= NSTEPS; step++)
		{
			double augmented[NTAPS][NTAPS + 1];
			double mu = (double) step / NSTEPS;
			for (int i = 0; i < NTAPS; i++)
			{
				for (int j = 0; j < NTAPS; j++)
					augmented[i][j] = gram[i][j];
				double x = (i - 4) + mu;
				augmented[i][NTAPS] = std::fabs(x) < 1e-15 ? 2 * omega
						: 2 * std::sin(omega * x) / x;
			}

			for (int column = 0; column < NTAPS; column++)
			{
				int pivot = column;
				for (int row = column + 1; row < NTAPS; row++)
					if (std::fabs(augmented[row][column])
							> std::fabs(augmented[pivot][column]))
						pivot = row;
				if (pivot != column)
					for (int j = column; j <= NTAPS; j++)
						std::swap(augmented[column][j], augmented[pivot][j]);
				double scale = augmented[column][column];
				for (int j = column; j <= NTAPS; j++)
					augmented[column][j] /= scale;
				for (int row = 0; row < NTAPS; row++)
				{
					if (row == column)
						continue;
					scale = augmented[row][column];
					for (int j = column; j <= NTAPS; j++)
						augmented[row][j] -= scale * augmented[column][j];
				}
			}
			for (int i = 0; i < NTAPS; i++)
				bank[(std::size_t) step][(std::size_t) i] =
						(float) augmented[i][NTAPS];
		}
		return bank;
	}

	static const TapBank& taps()
	{
		static const TapBank bank = build_taps();
		return bank;
	}

public:
	unsigned ntaps() const { return NTAPS; }
	float interpolate(const float* input, float mu) const
	{
		if (mu < 0)
			mu = 0;
		if (mu > 1)
			mu = 1;
		int step = (int) std::lround(mu * NSTEPS);
		const std::array<float, NTAPS>& selected = taps()[(std::size_t) step];
		float out = 0;
		/* GNU Radio's fir_filter reverses the published tap row. */
		for (int i = 0; i < NTAPS; i++)
			out += input[i] * selected[(std::size_t) (NTAPS - 1 - i)];
		return out;
	}
};
} // namespace filter
} // namespace gr
#endif
