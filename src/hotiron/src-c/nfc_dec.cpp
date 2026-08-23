#include "nfc_dec.h"

#include <cmath>
#include <cstdint>
#include <cstring>
#include <memory>
#include <vector>

#include <hw/SignalBuffer.h>
#include <hw/SignalType.h>
#include <lab/data/RawFrame.h>
#include <lab/nfc/NfcDecoder.h>

namespace {

const char *tech_name(unsigned int tech)
{
	switch (tech)
	{
	case lab::NfcATech:
		return "A";
	case lab::NfcBTech:
		return "B";
	case lab::NfcFTech:
		return "F";
	case lab::NfcVTech:
		return "V";
	default:
		return "";
	}
}

const char *nfca_poll_name(unsigned char cmd)
{
	switch (cmd)
	{
	case 0x26:
		return "REQA";
	case 0x50:
		return "HLTA";
	case 0x52:
		return "WUPA";
	case 0x60:
	case 0x61:
		return "AUTH";
	case 0x93:
		return "SEL1";
	case 0x95:
		return "SEL2";
	case 0x97:
		return "SEL3";
	case 0xE0:
		return "RATS";
	default:
		return "A";
	}
}

void fill_name(nfc_dec_frame *dst, const lab::RawFrame &frame, unsigned int last_poll)
{
	std::memset(dst->name, 0, sizeof(dst->name));
	if (frame.frameType() == lab::NfcCarrierOn)
	{
		std::strncpy(dst->name, "field on", sizeof(dst->name) - 1);
		return;
	}
	if (frame.frameType() == lab::NfcCarrierOff)
	{
		std::strncpy(dst->name, "field off", sizeof(dst->name) - 1);
		return;
	}
	const char *tech = tech_name(frame.techType());
	if (frame.limit() == 0)
	{
		std::strncpy(dst->name, tech[0] ? tech : "frame", sizeof(dst->name) - 1);
		return;
	}
	unsigned char cmd = frame[0];
	if (frame.frameType() == lab::NfcPollFrame && frame.techType() == lab::NfcATech)
	{
		std::strncpy(dst->name, nfca_poll_name(cmd), sizeof(dst->name) - 1);
		return;
	}
	if (frame.frameType() == lab::NfcListenFrame && frame.techType() == lab::NfcATech)
	{
		if (last_poll == 0x26 || last_poll == 0x52)
			std::strncpy(dst->name, "ATQA", sizeof(dst->name) - 1);
		else if (last_poll == 0x93 || last_poll == 0x95 || last_poll == 0x97)
			std::strncpy(dst->name, "UID", sizeof(dst->name) - 1);
		else
			std::strncpy(dst->name, "A", sizeof(dst->name) - 1);
		return;
	}
	if (frame.frameType() == lab::NfcPollFrame && frame.techType() == lab::NfcBTech && cmd == 0x05)
	{
		std::strncpy(dst->name, "REQB", sizeof(dst->name) - 1);
		return;
	}
	std::strncpy(dst->name, tech[0] ? tech : "frame", sizeof(dst->name) - 1);
}

} // namespace

struct nfc_dec
{
	lab::NfcDecoder decoder;
	uint32_t sample_rate = 10000000;
	unsigned long long offset = 0;
	unsigned int last_poll = 0;
	bool ready = false;
};

nfc_dec *nfc_dec_create(void)
{
	auto *dec = new nfc_dec();
	dec->decoder.setEnableDebug(false);
	dec->decoder.setSampleRate(dec->sample_rate);
	dec->decoder.initialize();
	dec->ready = true;
	return dec;
}

void nfc_dec_destroy(nfc_dec *dec)
{
	if (dec == nullptr)
		return;
	dec->decoder.cleanup();
	delete dec;
}

void nfc_dec_set_sample_rate(nfc_dec *dec, uint32_t sample_rate)
{
	if (dec == nullptr || sample_rate == 0)
		return;
	dec->sample_rate = sample_rate;
	dec->decoder.setSampleRate(sample_rate);
	dec->decoder.initialize();
}

int nfc_dec_process_iq(nfc_dec *dec, const int8_t *iq, int nbytes, nfc_dec_frame *out, int max_frames)
{
	if (dec == nullptr || iq == nullptr || nbytes < 2 || out == nullptr || max_frames <= 0)
		return 0;
	int pairs = nbytes / 2;
	hw::SignalBuffer buffer(static_cast<unsigned int>(pairs), 1, 1, dec->sample_rate, dec->offset, 0,
			hw::SignalType::SIGNAL_TYPE_RADIO_SAMPLES);
	float *dst = buffer.push(static_cast<unsigned int>(pairs));
	if (dst == nullptr)
		return 0;
	for (int i = 0; i < pairs; i++)
	{
		float i_s = static_cast<float>(iq[2 * i]) / 128.0f;
		float q_s = static_cast<float>(iq[2 * i + 1]) / 128.0f;
		dst[i] = std::sqrt(i_s * i_s + q_s * q_s);
	}
	buffer.flip();
	dec->offset += static_cast<unsigned long long>(pairs);

	std::list<lab::RawFrame> frames = dec->decoder.nextFrames(buffer);
	int n = 0;
	for (const lab::RawFrame &frame : frames)
	{
		if (n >= max_frames)
			break;
		nfc_dec_frame *dstf = &out[n];
		std::memset(dstf, 0, sizeof(*dstf));
		dstf->tech = frame.techType();
		dstf->type = frame.frameType();
		dstf->phase = frame.framePhase();
		dstf->flags = frame.frameFlags();
		dstf->rate = frame.frameRate();
		unsigned int lim = frame.limit();
		if (lim > NFC_DEC_PAYLOAD_MAX)
			lim = NFC_DEC_PAYLOAD_MAX;
		dstf->nbytes = lim;
		dstf->t0 = frame.timeStart();
		dstf->t1 = frame.timeEnd();
		fill_name(dstf, frame, dec->last_poll);
		for (unsigned int i = 0; i < lim; i++)
			dstf->payload[i] = frame[i];
		if (frame.frameType() == lab::NfcPollFrame && lim > 0)
			dec->last_poll = frame[0];
		n++;
	}
	return n;
}
