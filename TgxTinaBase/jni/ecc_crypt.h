/*
 * ecc_crypt.h
 *
 *  Created on: 2012-8-9
 *      Author: Zhangzhuo
 */

#ifndef ECC_CRYPT_H_
#define ECC_CRYPT_H_

#include "ec_crypt.h"
byte* getPubKey(const char* password, vlPoint publicKey);
byte* ecc_decode(vlPoint toDecode, const char* passwd, vlPoint msg);
byte* ecc_encode(vlPoint session, vlPoint publicKey, vlPoint msg);
byte* getRc4Key(const char* seed, vlPoint rc4Encode, vlPoint publicKey, vlPoint msg);
#endif /* ECC_CRYPT_H_ */
