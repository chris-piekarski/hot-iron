/* -*- c++ -*- */
/*
 * Copyright 2014 Free Software Foundation, Inc.
 *
 * This file is part of GNU Radio
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 */

#include "atsc_equalizer_impl.h"
#include "atsc_pnXXX_impl.h"
#include "atsc_types.h"
#include <gnuradio/io_signature.h>
#include <volk/volk.h>
#include <algorithm>
#include <cmath>
#include <cstring>
#include <fstream>
#include <vector>

namespace gr {
namespace dtv {

namespace {

// Solves (A) w = b in-place for symmetric positive definite A (n x n, row-major), ported from OnAir atsc_rx.cpp.
bool solve_spd(std::vector<double>& A, std::vector<double>& b, int n)
{
    for (int j = 0; j < n; j++) {
        double d = A[(size_t)j * n + j];
        for (int k = 0; k < j; k++)
            d -= A[(size_t)j * n + k] * A[(size_t)j * n + k];
        if (d <= 1e-12 || !std::isfinite(d))
            return false;
        d = std::sqrt(d);
        A[(size_t)j * n + j] = d;
        for (int i = j + 1; i < n; i++) {
            double s = A[(size_t)i * n + j];
            for (int k = 0; k < j; k++)
                s -= A[(size_t)i * n + k] * A[(size_t)j * n + k];
            A[(size_t)i * n + j] = s / d;
        }
    }
    for (int i = 0; i < n; i++) {
        double s = b[(size_t)i];
        for (int k = 0; k < i; k++)
            s -= A[(size_t)i * n + k] * b[(size_t)k];
        b[(size_t)i] = s / A[(size_t)i * n + i];
    }
    for (int i = n - 1; i >= 0; i--) {
        double s = b[(size_t)i];
        for (int k = i + 1; k < n; k++)
            s -= A[(size_t)k * n + i] * b[(size_t)k];
        b[(size_t)i] = s / A[(size_t)i * n + i];
    }
    return true;
}

inline float slice_8vsb(float y)
{
    return std::max(-7.0f, std::min(7.0f, 2.0f * std::floor(y * 0.5f) + 1.0f));
}

double eval_field_sync_mse(const float* input_samples,
                           const float* training_pattern,
                           int nsamples,
                           int ntaps,
                           int ndfe,
                           const std::vector<float>& ff,
                           const std::vector<float>& fb,
                           float bias)
{
    double mse = 0.0;
    int count = 0;
    for (int j = ndfe; j < nsamples; j++) {
        float y = 0.0f;
        volk_32f_x2_dot_prod_32f(&y, &input_samples[j], ff.data(), ntaps);
        y += bias;
        if (!fb.empty()) {
            float fbs = 0.0f;
            for (int m = 0; m < ndfe; m++)
                fbs += fb[(size_t)m] * training_pattern[j - 1 - m];
            y += fbs;
        }
        double e = (double)y - (double)training_pattern[j];
        mse += e * e;
        count++;
    }
    return count > 0 ? mse / count : 1e9;
}

} // namespace

atsc_equalizer::sptr atsc_equalizer::make()
{
    return gnuradio::make_block_sptr<atsc_equalizer_impl>();
}

static float bin_map(int bit) { return bit ? +5 : -5; }

static void init_field_sync_common(float* p, int mask)
{
    int i = 0;

    p[i++] = bin_map(1); // data segment sync pulse
    p[i++] = bin_map(0);
    p[i++] = bin_map(0);
    p[i++] = bin_map(1);

    for (int j = 0; j < 511; j++) // PN511
        p[i++] = bin_map(atsc_pn511[j]);

    for (int j = 0; j < 63; j++) // PN63
        p[i++] = bin_map(atsc_pn63[j]);

    for (int j = 0; j < 63; j++) // PN63, toggled on field 2
        p[i++] = bin_map(atsc_pn63[j] ^ mask);

    for (int j = 0; j < 63; j++) // PN63
        p[i++] = bin_map(atsc_pn63[j]);
}

atsc_equalizer_impl::atsc_equalizer_impl()
    : gr::block("dtv_atsc_equalizer",
                io_signature::make2(
                    2, 2, ATSC_DATA_SEGMENT_LENGTH * sizeof(float), sizeof(plinfo)),
                io_signature::make2(
                    2, 2, ATSC_DATA_SEGMENT_LENGTH * sizeof(float), sizeof(plinfo)))
{
    init_field_sync_common(training_sequence1, 0);
    init_field_sync_common(training_sequence2, 1);

    d_taps.resize(NTAPS, 0.0f);
    // Pass-through until trained. All-zero taps mute 8VSB into the trellis.
    d_taps[NPRETAPS] = 1.0f;
    d_fb_taps.assign(NDFE, 0.0f);
    d_dec_hist.assign(NDFE, 0.0f);
    d_bias = 0.0f;
    d_gain_corr = 1.0f;
    d_snr_db = 0.0f;
    d_trained = false;

    const int alignment_multiple = volk_get_alignment() / sizeof(float);
    set_alignment(std::max(1, alignment_multiple));
}

atsc_equalizer_impl::~atsc_equalizer_impl() {}

std::vector<float> atsc_equalizer_impl::taps() const { return d_taps; }

std::vector<float> atsc_equalizer_impl::data() const
{
    std::vector<float> ret(&data_mem2[0], &data_mem2[ATSC_DATA_SEGMENT_LENGTH - 1]);
    return ret;
}

void atsc_equalizer_impl::filterN(const float* input_samples,
                                  float* output_samples,
                                  int nsamples)
{
    static const float sync_lv[4] = { +5.0f, -5.0f, -5.0f, +5.0f };
    static const float SYNC_BETA = 0.00002f;
    const bool use_fb = !d_fb_taps.empty();
    double data_mse = 0.0;
    int data_cnt = 0;
    double sxy = 0.0;
    double sxx = 0.0;

    for (int j = 0; j < nsamples; j++) {
        float ff = 0.0f;
        volk_32f_x2_dot_prod_32f(&ff, &input_samples[j], &d_taps[0], NTAPS);
        float fb = 0.0f;
        if (use_fb) {
            for (int m = 0; m < NDFE; m++)
                fb += d_fb_taps[(size_t)m] * d_dec_hist[(size_t)m];
        }
        float y = (ff + fb + d_bias) * d_gain_corr;
        output_samples[j] = y;

        float dec;
        if (j < 4) {
            dec = sync_lv[j];
            float e = y - dec;
            if ( std::isfinite(e) && std::fabs(e) < 8.0f ) {
                float tmp_taps[NTAPS];
                volk_32f_s32f_multiply_32f(tmp_taps, &input_samples[j], SYNC_BETA * e, NTAPS);
                volk_32f_x2_subtract_32f(&d_taps[0], &d_taps[0], tmp_taps, NTAPS);
                d_bias -= SYNC_BETA * 0.5f * e;
            }
        } else {
            dec = slice_8vsb(y);
            double err = (double)y - (double)dec;
            data_mse += err * err;
            data_cnt++;
            if (std::fabs(err) < 1.0) {
                sxy += (double)y * (double)dec;
                sxx += (double)dec * (double)dec;
            }
        }

        for (int m = NDFE - 1; m > 0; m--)
            d_dec_hist[(size_t)m] = d_dec_hist[(size_t)(m - 1)];
        d_dec_hist[0] = dec;
    }

    if (data_cnt > 0) {
        double mse = data_mse / data_cnt;
        float seg_snr = (float)(10.0 * std::log10(21.0 / std::max(mse, 1e-4)));
        if (std::isfinite(seg_snr))
            d_snr_db = d_snr_db == 0.0f ? seg_snr : (d_snr_db + 0.05f * (seg_snr - d_snr_db));
    }
    if (d_trained && sxx > 1000.0 && sxy > 0.0) {
        double g = sxy / sxx;
        if (g > 0.5 && g < 2.0) {
            double target = std::max(0.92, std::min(1.12, (double)d_gain_corr / std::pow(g, 0.15)));
            d_gain_corr = (float)(0.9 * d_gain_corr + 0.1 * target);
        }
    }
}

void atsc_equalizer_impl::adaptN(const float* input_samples,
                                 const float* training_pattern,
                                 float* output_samples,
                                 int nsamples)
{
    // 1. Baseline LMS candidate (always safe even on degenerate synthetic inputs).
    std::vector<float> lms_ff = d_taps;
    std::vector<float> zero_fb(NDFE, 0.0f);
    static const double BETA = 0.00005;
    for (int j = 0; j < nsamples; j++) {
        float y = 0.0f;
        volk_32f_x2_dot_prod_32f(&y, &input_samples[j], lms_ff.data(), NTAPS);
        float e = y - training_pattern[j];
        float tmp_taps[NTAPS];
        volk_32f_s32f_multiply_32f(tmp_taps, &input_samples[j], (float)(BETA * e), NTAPS);
        volk_32f_x2_subtract_32f(lms_ff.data(), lms_ff.data(), tmp_taps, NTAPS);
    }

    std::vector<float> best_ff = lms_ff;
    std::vector<float> best_fb = zero_fb;
    float best_bias = 0.0f;
    double best_mse = eval_field_sync_mse(
        input_samples, training_pattern, nsamples, NTAPS, NDFE, best_ff, best_fb, best_bias);

    // Keep current taps if they already have lower MSE on this field sync.
    if (d_trained) {
        double cur_mse = eval_field_sync_mse(
            input_samples, training_pattern, nsamples, NTAPS, NDFE, d_taps, d_fb_taps, d_bias);
        if (cur_mse < best_mse) {
            best_mse = cur_mse;
            best_ff = d_taps;
            best_fb = d_fb_taps;
            best_bias = d_bias;
        }
    }

    // 2. OnAir Cholesky Least-Squares Feed-Forward solver (64 FF + 1 bias = 65 unknowns).
    {
        const int n = NTAPS + 1;
        std::vector<double> A((size_t)n * n, 0.0);
        std::vector<double> b((size_t)n, 0.0);
        double row[NTAPS + 1];
        for (int j = 0; j < nsamples; j++) {
            for (int k = 0; k < NTAPS; k++)
                row[k] = (double)input_samples[j + k];
            row[NTAPS] = 1.0;
            const double tgt = (double)training_pattern[j];
            for (int r = 0; r < n; r++) {
                const double xr = row[r];
                b[(size_t)r] += xr * tgt;
                for (int c = 0; c <= r; c++)
                    A[(size_t)r * n + c] += xr * row[c];
            }
        }
        double tr = 0.0;
        for (int i = 0; i < NTAPS; i++)
            tr += A[(size_t)i * n + i];
        const double ridge = std::max(1e-3, 2e-3 * (tr / NTAPS));
        for (int i = 0; i < n; i++)
            A[(size_t)i * n + i] += ridge;
        if (solve_spd(A, b, n)) {
            std::vector<float> ls_ff(NTAPS);
            for (int k = 0; k < NTAPS; k++)
                ls_ff[(size_t)k] = (float)b[(size_t)k];
            float ls_bias = (float)b[(size_t)NTAPS];
            double ls_mse = eval_field_sync_mse(
                input_samples, training_pattern, nsamples, NTAPS, NDFE, ls_ff, zero_fb, ls_bias);
            if (ls_mse < best_mse) {
                best_mse = ls_mse;
                best_ff = std::move(ls_ff);
                best_fb = zero_fb;
                best_bias = ls_bias;
            }
        }
    }

    // 3. OnAir Cholesky Least-Squares Decision-Feedback solver (64 FF + 32 FB + 1 bias) for heavy echoes.
    if (best_mse > 0.8) {
        const int n = NTAPS + NDFE + 1;
        std::vector<double> A((size_t)n * n, 0.0);
        std::vector<double> b((size_t)n, 0.0);
        double row[NTAPS + NDFE + 1];
        for (int j = NDFE; j < nsamples; j++) {
            for (int k = 0; k < NTAPS; k++)
                row[k] = (double)input_samples[j + k];
            for (int m = 0; m < NDFE; m++)
                row[NTAPS + m] = (double)training_pattern[j - 1 - m];
            row[NTAPS + NDFE] = 1.0;
            const double tgt = (double)training_pattern[j];
            for (int r = 0; r < n; r++) {
                const double xr = row[r];
                b[(size_t)r] += xr * tgt;
                for (int c = 0; c <= r; c++)
                    A[(size_t)r * n + c] += xr * row[c];
            }
        }
        double tr_ff = 0.0;
        for (int i = 0; i < NTAPS; i++)
            tr_ff += A[(size_t)i * n + i];
        double tr_fb = 0.0;
        for (int i = NTAPS; i < NTAPS + NDFE; i++)
            tr_fb += A[(size_t)i * n + i];
        const double ridge_ff = std::max(1e-3, 3e-3 * (tr_ff / NTAPS));
        const double ridge_fb = std::max(1e-2, 2e-2 * (tr_fb / NDFE));
        for (int i = 0; i < NTAPS; i++)
            A[(size_t)i * n + i] += ridge_ff;
        for (int i = NTAPS; i < NTAPS + NDFE; i++)
            A[(size_t)i * n + i] += ridge_fb;
        A[(size_t)(n - 1) * n + (n - 1)] += ridge_ff;

        if (solve_spd(A, b, n)) {
            std::vector<float> dfe_ff(NTAPS);
            std::vector<float> dfe_fb(NDFE);
            double fb_energy = 0.0;
            for (int k = 0; k < NTAPS; k++)
                dfe_ff[(size_t)k] = (float)b[(size_t)k];
            for (int m = 0; m < NDFE; m++) {
                dfe_fb[(size_t)m] = (float)b[(size_t)(NTAPS + m)];
                fb_energy += (double)dfe_fb[(size_t)m] * dfe_fb[(size_t)m];
            }
            float dfe_bias = (float)b[(size_t)(NTAPS + NDFE)];
            // Only adopt DFE when feedback loop is stable (fb_energy < 0.75) and clearly beats FF.
            if (fb_energy < 0.75) {
                double dfe_mse = eval_field_sync_mse(
                    input_samples, training_pattern, nsamples, NTAPS, NDFE, dfe_ff, dfe_fb, dfe_bias);
                if (dfe_mse < best_mse * 0.85) {
                    best_mse = dfe_mse;
                    best_ff = std::move(dfe_ff);
                    best_fb = std::move(dfe_fb);
                    best_bias = dfe_bias;
                }
            }
        }
    }

    if (d_trained && best_mse < 12.0) {
        for (int k = 0; k < NTAPS; k++)
            d_taps[(size_t)k] = 0.4f * d_taps[(size_t)k] + 0.6f * best_ff[(size_t)k];
        for (int m = 0; m < NDFE; m++)
            d_fb_taps[(size_t)m] = 0.4f * d_fb_taps[(size_t)m] + 0.6f * best_fb[(size_t)m];
        d_bias = 0.4f * d_bias + 0.6f * best_bias;
    } else {
        d_taps = std::move(best_ff);
        d_fb_taps = std::move(best_fb);
        d_bias = best_bias;
        d_trained = (best_mse < 16.0);
    }

    float fs_snr = (float)(10.0 * std::log10(25.0 / std::max(best_mse, 1e-4)));
    if (std::isfinite(fs_snr))
        d_snr_db = d_snr_db == 0.0f ? fs_snr : (0.7f * d_snr_db + 0.3f * fs_snr);

    // Populate output_samples and warm DFE decision history to the end of the field sync segment.
    std::fill(d_dec_hist.begin(), d_dec_hist.end(), 0.0f);
    for (int j = 0; j < ATSC_DATA_SEGMENT_LENGTH; j++) {
        float ff = 0.0f;
        volk_32f_x2_dot_prod_32f(&ff, &input_samples[j], &d_taps[0], NTAPS);
        float fb = 0.0f;
        for (int m = 0; m < NDFE; m++)
            fb += d_fb_taps[(size_t)m] * d_dec_hist[(size_t)m];
        float y = (ff + fb + d_bias) * d_gain_corr;
        if (j < nsamples)
            output_samples[j] = y;
        float dec = j < nsamples ? training_pattern[j] : slice_8vsb(y);
        for (int m = NDFE - 1; m > 0; m--)
            d_dec_hist[(size_t)m] = d_dec_hist[(size_t)(m - 1)];
        d_dec_hist[0] = dec;
    }
}

int atsc_equalizer_impl::general_work(int noutput_items,
                                      gr_vector_int& ninput_items,
                                      gr_vector_const_void_star& input_items,
                                      gr_vector_void_star& output_items)
{
    auto in = static_cast<const float*>(input_items[0]);
    auto out = static_cast<float*>(output_items[0]);
    auto in_pl = static_cast<const plinfo*>(input_items[1]);
    auto out_pl = static_cast<plinfo*>(output_items[1]);

    int output_produced = 0;
    int i = 0;

    std::vector<tag_t> tags;

    plinfo pli_in;
    if (d_buff_not_filled) {
        memset(&data_mem[0], 0, NPRETAPS * sizeof(float));
        memcpy(&data_mem[NPRETAPS],
               in + i * ATSC_DATA_SEGMENT_LENGTH,
               ATSC_DATA_SEGMENT_LENGTH * sizeof(float));

        d_flags = in_pl[i].flags();
        d_segno = in_pl[i].segno();

        d_buff_not_filled = false;
        i++;
    }

    for (; i < noutput_items; i++) {

        memcpy(&data_mem[ATSC_DATA_SEGMENT_LENGTH + NPRETAPS],
               in + i * ATSC_DATA_SEGMENT_LENGTH,
               (NTAPS - NPRETAPS) * sizeof(float));

        if (d_segno == -1) {
            if (d_flags & 0x0010) {
                adaptN(data_mem, training_sequence2, data_mem2, KNOWN_FIELD_SYNC_LENGTH);
            } else if (!(d_flags & 0x0010)) {
                adaptN(data_mem, training_sequence1, data_mem2, KNOWN_FIELD_SYNC_LENGTH);
            }
        } else {
            filterN(data_mem, data_mem2, ATSC_DATA_SEGMENT_LENGTH);

            memcpy(&out[output_produced * ATSC_DATA_SEGMENT_LENGTH],
                   data_mem2,
                   ATSC_DATA_SEGMENT_LENGTH * sizeof(float));

            plinfo pli_out(d_flags, d_segno);
            out_pl[output_produced++] = pli_out;
        }

        memcpy(data_mem, &data_mem[ATSC_DATA_SEGMENT_LENGTH], NPRETAPS * sizeof(float));
        memcpy(&data_mem[NPRETAPS],
               in + i * ATSC_DATA_SEGMENT_LENGTH,
               ATSC_DATA_SEGMENT_LENGTH * sizeof(float));

        d_flags = in_pl[i].flags();
        d_segno = in_pl[i].segno();
    }

    consume_each(noutput_items);
    return output_produced;
}

void atsc_equalizer_impl::setup_rpc()
{
#ifdef GR_CTRLPORT
    add_rpc_variable(
        rpcbasic_sptr(new rpcbasic_register_get<atsc_equalizer, std::vector<float>>(
            alias(),
            "taps",
            &atsc_equalizer::taps,
            pmt::make_f32vector(1, -10),
            pmt::make_f32vector(1, 10),
            pmt::make_f32vector(1, 0),
            "",
            "Equalizer Taps",
            RPC_PRIVLVL_MIN,
            DISPTIME)));

    add_rpc_variable(
        rpcbasic_sptr(new rpcbasic_register_get<atsc_equalizer, std::vector<float>>(
            alias(),
            "data",
            &atsc_equalizer::data,
            pmt::make_f32vector(1, -10),
            pmt::make_f32vector(1, 10),
            pmt::make_f32vector(1, 0),
            "",
            "Post-equalizer Data",
            RPC_PRIVLVL_MIN,
            DISPTIME)));
#endif /* GR_CTRLPORT */
}

} /* namespace dtv */
} /* namespace gr */
