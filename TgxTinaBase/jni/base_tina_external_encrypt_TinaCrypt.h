#ifndef BASE_TINA_EXTERNAL_CRYPT_TINACRYPT_H_
#define BASE_TINA_EXTERNAL_CRYPT_TINACRYPT_H_
#undef __cplusplus
#include <jni.h>
#include <stdlib.h>
#include "AndroidEnv.h"
#include "sha1.h"
#include "sha256.h"
#include "ecc_crypt.h"
JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_pubKey(JNIEnv *, jobject, jstring);
JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_sha1(JNIEnv *, jobject, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_sha256(JNIEnv *, jobject, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_getRc4Key(JNIEnv *, jobject, jstring, jbyteArray, jbyteArray);
JNIEXPORT jint JNICALL Java_base_tina_external_encrypt_TinaCrypt_getVlsize(JNIEnv *, jobject);
JNIEXPORT void JNICALL Java_base_tina_external_encrypt_TinaCrypt_test(JNIEnv *, jobject);
#endif /*BASE_TINA_EXTERNAL_CRYPT_TINACRYPT_H_*/
