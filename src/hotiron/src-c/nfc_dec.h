#ifndef NFC_DEC_H_
#define NFC_DEC_H_

#include <stdint.h>
#include "hotiron_api.h"

#ifdef __cplusplus
extern "C" {
#endif

#define NFC_DEC_NAME_MAX 32
#define NFC_DEC_PAYLOAD_MAX 256

typedef struct nfc_dec nfc_dec;

typedef struct nfc_dec_frame
{
	uint32_t tech;
	uint32_t type;
	uint32_t phase;
	uint32_t flags;
	uint32_t rate;
	uint32_t nbytes;
	double t0;
	double t1;
	char name[NFC_DEC_NAME_MAX];
	uint8_t payload[NFC_DEC_PAYLOAD_MAX];
} nfc_dec_frame;

HOTIRON_API nfc_dec *HOTIRON_CALL nfc_dec_create(void);
HOTIRON_API void HOTIRON_CALL nfc_dec_destroy(nfc_dec *dec);
HOTIRON_API void HOTIRON_CALL nfc_dec_set_sample_rate(nfc_dec *dec, uint32_t sample_rate);
HOTIRON_API int HOTIRON_CALL nfc_dec_process_iq(nfc_dec *dec, const int8_t *iq, int nbytes, nfc_dec_frame *out,
		int max_frames);

#ifdef __cplusplus
}
#endif

#endif /* NFC_DEC_H_ */
