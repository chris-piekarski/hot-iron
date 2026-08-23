#include "nfc_dec.h"

/*
 * Windows placeholder so JNA can register nfc_dec_* without linking
 * nfc-laboratory (Linux-only subset). Create returns NULL; sniff still
 * parks IQ, but there are no frames until lab-radio is cross-built.
 */
nfc_dec *HOTIRON_CALL nfc_dec_create(void)
{
	return 0;
}

void HOTIRON_CALL nfc_dec_destroy(nfc_dec *dec)
{
	(void)dec;
}

void HOTIRON_CALL nfc_dec_set_sample_rate(nfc_dec *dec, uint32_t sample_rate)
{
	(void)dec;
	(void)sample_rate;
}

int HOTIRON_CALL nfc_dec_process_iq(nfc_dec *dec, const int8_t *iq, int nbytes, nfc_dec_frame *out,
		int max_frames)
{
	(void)dec;
	(void)iq;
	(void)nbytes;
	(void)out;
	(void)max_frames;
	return 0;
}
