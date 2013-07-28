#ifndef _SHA1_H
#define _SHA1_H

#include "environment.h"
#define HW 5
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

typedef struct {
    word32 state[5];
    word32 count[2];
    byte buffer[64];
} sha1_ctx;

void sha1_initial( sha1_ctx * c );
void sha1_process( sha1_ctx * c, byte * data, size_t len );
void sha1_final( sha1_ctx * c, word32[HW] );

#endif //END _SHA1_H
