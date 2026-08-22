#ifndef ATSC_SHIM_RS_H
#define ATSC_SHIM_RS_H
#ifdef __cplusplus
extern "C" {
#endif
void* init_rs_char(int symsize, int gfpoly, int fcr, int prim, int nroots);
int decode_rs_char(void* rs, unsigned char* data, int* eras_pos, int no_eras);
void free_rs_char(void* rs);
#ifdef __cplusplus
}
#endif
#endif
