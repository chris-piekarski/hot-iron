#ifndef ATSC_SHIM_BLOCK_H
#define ATSC_SHIM_BLOCK_H
#include <memory>
#include <string>
#include <vector>
#include <gnuradio/io_signature.h>

namespace gr {
using gr_vector_const_void_star = std::vector<const void*>;
using gr_vector_void_star = std::vector<void*>;
using gr_vector_int = std::vector<int>;

struct tag_t
{
};

struct Logger
{
	template <typename... A>
	void debug(const char*, A&&...)
	{
	}
	template <typename... A>
	void warn(const char*, A&&...)
	{
	}
	template <typename... A>
	void info(const char*, A&&...)
	{
	}
};

class block
{
	Logger d_log;

public:
	int d_nconsumed = 0;
	Logger* d_logger = &d_log;
	Logger* d_debug_logger = &d_log;

	block() = default;
	template <typename... A>
	explicit block(A&&...)
	{
	}
	virtual ~block() = default;
	void consume_each(int n) { d_nconsumed = n; }
	void set_output_multiple(int) {}
	void set_alignment(int) {}
	virtual void setup_rpc() {}
	virtual void forecast(int, gr_vector_int&) {}
	virtual int work(int, gr_vector_const_void_star&, gr_vector_void_star&) { return 0; }
	virtual int general_work(int, gr_vector_int&, gr_vector_const_void_star&,
			gr_vector_void_star&)
	{
		return 0;
	}
};

class sync_block : public block
{
public:
	sync_block() = default;
	template <typename... A>
	explicit sync_block(A&&...)
	{
	}
};

class sync_interpolator : public sync_block
{
public:
	sync_interpolator() = default;
	template <typename... A>
	explicit sync_interpolator(A&&...)
	{
	}
};
} // namespace gr

namespace gnuradio {
template <typename T, typename... A>
std::shared_ptr<T> make_block_sptr(A&&... a)
{
	return std::make_shared<T>(std::forward<A>(a)...);
}
} // namespace gnuradio

#endif
