#ifndef HACKRF_ATSC_DC_BLOCKER_H
#define HACKRF_ATSC_DC_BLOCKER_H

#include <cstddef>
#include <vector>

class AtscMovingAverager
{
public:
	explicit AtscMovingAverager(int length)
		: length_(length), delay_((std::size_t) length - 1, 0.f)
	{
	}

	float filter(float input)
	{
		out_d1_ = out_;
		out_ = delay_[index_];
		delay_[index_] = input;
		if (++index_ >= delay_.size())
			index_ = 0;
		float value = input - out_d1_ + out_d2_;
		out_d2_ = value;
		return value / length_;
	}

	float delayed_signal() const
	{
		return out_;
	}

private:
	float length_;
	std::vector<float> delay_;
	std::size_t index_ = 0;
	float out_ = 0;
	float out_d1_ = 0;
	float out_d2_ = 0;
};

/**
 * GNU Radio dc_blocker_ff(D, true), including its 2D-2 sample group delay.
 */
class AtscDcBlocker
{
public:
	explicit AtscDcBlocker(int length)
		: ma0_(length), ma1_(length), ma2_(length), ma3_(length),
		  delay_((std::size_t) length - 1, 0.f)
	{
	}

	float filter(float input)
	{
		float y1 = ma0_.filter(input);
		float y2 = ma1_.filter(y1);
		float y3 = ma2_.filter(y2);
		float y4 = ma3_.filter(y3);
		float delayed = delay_[index_];
		delay_[index_] = ma0_.delayed_signal();
		if (++index_ >= delay_.size())
			index_ = 0;
		return delayed - y4;
	}

private:
	AtscMovingAverager ma0_;
	AtscMovingAverager ma1_;
	AtscMovingAverager ma2_;
	AtscMovingAverager ma3_;
	std::vector<float> delay_;
	std::size_t index_ = 0;
};

#endif
