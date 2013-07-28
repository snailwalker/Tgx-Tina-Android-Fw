/*
 * sha256.c
 *
 *  Created on: 2012-8-30
 *      Author: Zhangzhuo
 */
#include "sha256.h"

static const byte fillbuf[64] = { 0x80, 0 /** , 0, 0, ...  */};
static const word32 K[64] = { 0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
		0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152, 0xa831c66d,
		0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
		0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
		0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2 };

void sha256_initial(sha256_ctx * ctx) {
	ctx->state[0] = 0x6a09e667;
	ctx->state[1] = 0xbb67ae85;
	ctx->state[2] = 0x3c6ef372;
	ctx->state[3] = 0xa54ff53a;
	ctx->state[4] = 0x510e527f;
	ctx->state[5] = 0x9b05688c;
	ctx->state[6] = 0x1f83d9ab;
	ctx->state[7] = 0x5be0cd19;

	ctx->total[0] = ctx->total[1] = 0;
	ctx->buflen = 0;
}

void sha256_process_block(sha256_ctx *ctx, const void *data, size_t len) {
	const word32 *words = data;
	size_t nwords = len / sizeof(word32);
	int t;
	word32 a = ctx->state[0];
	word32 b = ctx->state[1];
	word32 c = ctx->state[2];
	word32 d = ctx->state[3];
	word32 e = ctx->state[4];
	word32 f = ctx->state[5];
	word32 g = ctx->state[6];
	word32 h = ctx->state[7];

	ctx->total[0] += len;
	if (ctx->total[0] < len) ++ctx->total[1];

	while (nwords > 0) {
		word32 W[64];
		word32 a_save = a;
		word32 b_save = b;
		word32 c_save = c;
		word32 d_save = d;
		word32 e_save = e;
		word32 f_save = f;
		word32 g_save = g;
		word32 h_save = h;

#define Ch(x, y, z) ((x & y) ^ (~x & z))
#define Maj(x, y, z) ((x & y) ^ (x & z) ^ (y & z))
#define S0(x) (CYCLIC (x, 2) ^ CYCLIC (x, 13) ^ CYCLIC (x, 22))
#define S1(x) (CYCLIC (x, 6) ^ CYCLIC (x, 11) ^ CYCLIC (x, 25))
#define R0(x) (CYCLIC (x, 7) ^ CYCLIC (x, 18) ^ (x >> 3))
#define R1(x) (CYCLIC (x, 17) ^ CYCLIC (x, 19) ^ (x >> 10))

#define CYCLIC(w, s) ((w >> s) | (w << (32 - s)))

		for (t = 0; t < 16; ++t) {
			W[t] = SWAP (*words);
			++words;
		}
		for (t = 16; t < 64; ++t)
			W[t] = R1 (W[t - 2]) + W[t - 7] + R0 (W[t - 15]) + W[t - 16];

		for (t = 0; t < 64; ++t) {
			word32 T1 = h + S1 (e) + Ch (e, f, g) + K[t] + W[t];
			word32 T2 = S0 (a) + Maj (a, b, c);
			h = g;
			g = f;
			f = e;
			e = d + T1;
			d = c;
			c = b;
			b = a;
			a = T1 + T2;
		}

		a += a_save;
		b += b_save;
		c += c_save;
		d += d_save;
		e += e_save;
		f += f_save;
		g += g_save;
		h += h_save;

		nwords -= 16;
	}

	ctx->state[0] = a;
	ctx->state[1] = b;
	ctx->state[2] = c;
	ctx->state[3] = d;
	ctx->state[4] = e;
	ctx->state[5] = f;
	ctx->state[6] = g;
	ctx->state[7] = h;
}

void sha256_final(sha256_ctx *ctx, word32 resbuf[8]) {
	word32 bytes = ctx->buflen;
	size_t pad;
	int i;

	ctx->total[0] += bytes;
	if (ctx->total[0] < bytes) ++ctx->total[1];

	pad = bytes >= 56 ? 64 + 56 - bytes : 56 - bytes;
	memcpy(&ctx->buffer[bytes], fillbuf, pad);

	*(word32 *) &ctx->buffer[bytes + pad + 4] = SWAP (ctx->total[0] << 3);
	*(word32 *) &ctx->buffer[bytes + pad] = SWAP ((ctx->total[1] << 3) |
			(ctx->total[0] >> 29));

	sha256_process_block(ctx, ctx->buffer, bytes + pad + 8);

	for (i = 0; i < 8; ++i)
		resbuf[i] = SWAP (ctx->state[i]);
}

void sha256_process(sha256_ctx *ctx, const void *buffer, size_t len) {
	if (ctx->buflen != 0) {
		size_t left_over = ctx->buflen;
		size_t add = 128 - left_over > len ? len : 128 - left_over;

		memcpy(&ctx->buffer[left_over], buffer, add);
		ctx->buflen += add;

		if (ctx->buflen > 64) {
			sha256_process_block(ctx, ctx->buffer, ctx->buflen & ~63);

			ctx->buflen &= 63;
			memcpy(ctx->buffer, &ctx->buffer[(left_over + add) & ~63], ctx->buflen);
		}

		buffer = (const char *) buffer + add;
		len -= add;
	}

	if (len >= 64) {
#if !_STRING_ARCH_unaligned
# if __GNUC__ >= 2
#  define UNALIGNED_P(p) (((uintptr_t) p) % __alignof__ (word32) != 0)
# else
#  define UNALIGNED_P(p) (((uintptr_t) p) % sizeof (word32) != 0)
# endif
		if (UNALIGNED_P (buffer)) while (len > 64) {
			sha256_process_block(ctx, memcpy(ctx->buffer, buffer, 64), 64);
			buffer = (const char *) buffer + 64;
			len -= 64;
		}
		else
#endif
		{
			sha256_process_block(ctx, buffer, len & ~63);
			buffer = (const char *) buffer + (len & ~63);
			len &= 63;
		}
	}

	if (len > 0) {
		size_t left_over = ctx->buflen;

		memcpy(&ctx->buffer[left_over], buffer, len);
		left_over += len;
		if (left_over >= 64) {
			sha256_process_block(ctx, ctx->buffer, 64);
			left_over -= 64;
			memcpy(ctx->buffer, &ctx->buffer[64], left_over);
		}
		ctx->buflen = left_over;
	}
}
