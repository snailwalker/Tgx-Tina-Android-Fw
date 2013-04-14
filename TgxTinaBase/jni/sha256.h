/*
 * sha256.h
 *
 *  Created on: 2012-8-30
 *      Author: Zhangzhuo
 */
#include <stdio.h>
#include <string.h>
#include "environment.h"

#if BYTE_ORDER == LITTLE_ENDIAN
#define SWAP(n) \
    (((n) << 24) | (((n) & 0xff00) << 8) | (((n) >> 8) & 0xff00) | ((n) >> 24))
#else
#define SWAP(n) (n)
#endif
#ifndef USUAL_TYPES
#define USUAL_TYPES
typedef unsigned char byte; /*  8 bit */
typedef unsigned short word16; /* 16 bit */
#ifdef _BSD_UNIX_X64
typedef unsigned int word32; /* 32 bit */
typedef unsigned long word64; /* 64 bit */
#else
typedef unsigned long word32; /* 32 bit */
typedef unsigned long long word64; /* 64 bit */
#endif
#endif /* ?USUAL_TYPES */
#ifndef SHA256_H_
#define SHA256_H_

typedef struct sha256_ctx {
	word32 state[8];

	word32 total[2];
	word32 buflen;
	byte buffer[128] __attribute__ ((__aligned__ (__alignof__ (word32))));
} sha256_ctx;

void sha256_initial(sha256_ctx * ctx);
void sha256_final(sha256_ctx *ctx, word32 resbuf[8]);
void sha256_process(sha256_ctx *ctx, const void *buffer, size_t len);

#endif /* SHA256_H_ */
