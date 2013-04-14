/*
 * ecc_crypt.c
 *
 *  Created on: 2012-8-8
 *      Author: Zhangzhuo
 */
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <stdlib.h>
#include <assert.h>

#include "sha1.h"
#include "sha256.h"
#include "byte_stream.h"
#include "ecc_crypt.h"

typedef struct {
	unsigned count;
	word32 seed[2 + HW * 3]; //(HW:5)17
} prng;

void prng_init(prng * p) {
	memset(p, 0, sizeof(*p));
}

void prng_set_secret_str(prng * p, const char* passwd) { //密钥的SHA-1写入到 seed的1-5位置
	sha1_ctx c;
	size_t keylen = strlen(passwd);
	sha1_initial(&c);
	sha1_process(&c, (byte*) passwd, keylen);
	sha1_final(&c, p->seed + 1);
	p->count = 1 + HW;
}

void prng_set_time(prng * p) {
	p->seed[1 + 3 * HW] = (word32) time(0);
	p->count = 2 + 3 * HW;
}

word32 prng_next(prng * p) {
	word32 tmp[HW];
	byte buffer[(3 * HW + 2) * 4];
	word32 i, j;
	sha1_ctx c;

	p->seed[0] += 1;
	for (i = 0; i < p->count; i += 1) {
		for (j = 0; j < 4; j += 1) {
			buffer[i * 4 + j] = (byte) (p->seed[i] >> (j * 8));
		}
	}

	sha1_initial(&c);
	sha1_process(&c, buffer, p->count * 4);
	sha1_final(&c, tmp);
	memset(buffer, 0, sizeof(buffer));
	return tmp[0];
}

void prng_to_vlong(prng * p, vlPoint V) {
	unsigned i;
	V[0] = VL_UNITS - 2;
	for (i = 1; i < VL_UNITS - 1; i += 1)
		V[i] = (word16) prng_next(p);
}

byte* getPubKey(const char* password, vlPoint publicKey) {
	vlPoint secret;
	prng p;
	int i;
	byte* result;
	ByteArrayStream bas;
	byteStreamInit(&bas, (VL_SIZE) << 1);
	prng_init(&p);
	prng_set_secret_str(&p, password); //生成SHA-1的Password串
	prng_to_vlong(&p, secret); //将160bit的SHA-1转换成vlPoint
	gfInit();
	cpMakePublicKey(publicKey, secret);
	for (i = 0; i < VL_SIZE; i++) {
		bas.writeShort(&bas, publicKey[i]); //输入之后一定是BIG_ENDIAN
	}
	result = bas.toArray(&bas);
	bas.dispose(&bas);
	gfQuit();
	prng_init(&p);
	vlClear(secret);
	return result;
}

byte* getRc4Key(const char* seed, vlPoint rc4Encode, vlPoint publicKey, vlPoint msg) {
	assert(rc4Encode != NULL);
	prng p;
	int i;
	byte* result;
	ByteArrayStream bas;
	byteStreamInit(&bas, (VL_SIZE) << 1);

	prng_init(&p);
	prng_set_secret_str(&p, seed); // seed 生成Rc4秘钥
	prng_set_time(&p);
	prng_to_vlong(&p, rc4Encode); //生成 写入
	gfInit();
	cpEncodeSecret(publicKey, msg, rc4Encode);
	gfQuit();
	for (i = 0; i < VL_SIZE; i++) {
		bas.writeShort(&bas, rc4Encode[i]);
	}
	result = bas.toArray(&bas);
	bas.dispose(&bas);
	prng_init(&p);
	vlClear(rc4Encode);
	return result;
}

byte* ecc_encode(vlPoint session, vlPoint publicKey, vlPoint msg) {
	assert(session != NULL && publicKey != NULL);
	prng p;
	int i;
	byte* result;
	ByteArrayStream bas;
	byteStreamInit(&bas, (VL_SIZE) << 1);
	prng_init(&p);
	gfInit();
	cpEncodeSecret(publicKey, msg, session);
	for (i = 0; i < VL_SIZE; i++) {
		bas.writeShort(&bas, msg[i]);
	}
	gfQuit();
	result = bas.toArray(&bas);
	bas.dispose(&bas);
	prng_init(&p);
	vlClear(publicKey);
	vlClear(session);
	return result;
}

byte* ecc_decode(vlPoint toDecode, const char* passwd, vlPoint msg) {
	assert(toDecode != NULL);
	prng p;
	int i;
	vlPoint secret;
	byte* result;
	ByteArrayStream bas;
	byteStreamInit(&bas, (VL_SIZE) << 1);
	prng_init(&p);
	prng_set_secret_str(&p, passwd);
	prng_to_vlong(&p, secret);
	gfInit();
	cpDecodeSecret(secret, toDecode, msg);
	for (i = 0; i < VL_SIZE; i++) {
		bas.writeShort(&bas, msg[i]);
	}
	gfQuit();
	result = bas.toArray(&bas);
	bas.dispose(&bas);
	prng_init(&p);
	vlClear(secret);
	return result;
}

#ifdef TEST_SELF
int main(void) {
	return EXIT_SUCCESS;
}
#endif
