#ifndef ATSC_SHIM_IO_SIGNATURE_H
#define ATSC_SHIM_IO_SIGNATURE_H
#include <cstddef>
namespace gr {
class io_signature
{
public:
	static io_signature make(int, int, int) { return {}; }
	static io_signature make2(int, int, int, int) { return {}; }
};
} // namespace gr
#endif
