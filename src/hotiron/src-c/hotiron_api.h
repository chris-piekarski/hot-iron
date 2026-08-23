#ifndef HOTIRON_API_H_
#define HOTIRON_API_H_

#ifdef _WIN32
	#define HOTIRON_API __declspec(dllexport)
	#define HOTIRON_CALL __cdecl
#else
	#define HOTIRON_API
	#define HOTIRON_CALL
#endif

#ifdef __cplusplus
	#define HOTIRON_EXTERN_C_BEGIN extern "C" {
	#define HOTIRON_EXTERN_C_END }
#else
	#define HOTIRON_EXTERN_C_BEGIN
	#define HOTIRON_EXTERN_C_END
#endif

#endif /* HOTIRON_API_H_ */
