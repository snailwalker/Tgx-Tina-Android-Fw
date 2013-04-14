#include "base_tina_external_encrypt_TinaCrypt.h"

JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_pubKey(JNIEnv * env, jobject thiz, jstring passwd) {
	const char* pwd = (*env)->GetStringUTFChars(env, passwd, 0);
	vlPoint publicKey;
	byte* result = getPubKey(pwd, publicKey);
	size_t arrayLength = VL_SIZE << 1;
	jbyteArray publicKeyArray = (*env)->NewByteArray(env, arrayLength);

	(*env)->SetByteArrayRegion(env, publicKeyArray, 0, arrayLength, result);
	free(result);
	return publicKeyArray; //BIG_ENDIAN
}
/*
 JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_eccEncrypt(JNIEnv * env, jobject thiz, jbyteArray publicKey, jbyteArray contents) {
 vlPoint vlPublicKey;

 jbyte* publicKeyIn = (*env)->GetByteArrayElements(env, publicKey, 0);
 byteArray2vlPoint((byte*) publicKeyIn, vlPublicKey);

 jbyte* contentsIn = (*env)->GetByteArrayElements(env, contents, 0);

 (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyIn, 0);
 (*env)->ReleaseByteArrayElements(env, contents, contentsIn, 0);

 }
 */

JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_eccDecrypt(JNIEnv * env, jobject thiz, jstring privateKey, jbyteArray cipher) {
	vlPoint vlMessage, vlSession;
	const char* privateKeyIn = (*env)->GetStringUTFChars(env, privateKey, 0);
	jbyte* cipherIn = (*env)->GetByteArrayElements(env, cipher, 0);
	jsize cipherLength = (*env)->GetArrayLength(env, cipher);
	size_t i = 0;

	for (i = 0; i < cipherLength; i += 2) {
		vlSession[i >> 1] = ((cipherIn[i] & 0xFF) << 8) | (cipherIn[i + 1] & 0xFF);
	}

	byte* result = ecc_decode(vlSession, privateKeyIn, vlMessage);

	(*env)->ReleaseStringUTFChars(env, privateKey, privateKeyIn);
	(*env)->ReleaseByteArrayElements(env, cipher, cipherIn, 0);

	jsize length = VL_SIZE << 1;
	jbyteArray decResult = (*env)->NewByteArray(env, length);

	(*env)->SetByteArrayRegion(env, decResult, 0, length, result);
	return decResult;
}

JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_sha1(JNIEnv * env, jobject thiz, jbyteArray contents) {
	sha1_ctx c;
	word32 result[5];

	jbyte* contentsIn = (*env)->GetByteArrayElements(env, contents, 0);
	size_t cLength = (*env)->GetArrayLength(env, contents);
	jbyteArray sha1Result = (*env)->NewByteArray(env, 20);

	sha1_initial(&c);
	sha1_process(&c, (byte*) contentsIn, cLength);
	sha1_final(&c, result);

#if _BYTE_ORDER == LITTLE_ENDIAN
	int i = 0;
	for (; i < 5; i++) {
		result[i] = htonl(result[i]);
	}
#endif

	(*env)->SetByteArrayRegion(env, sha1Result, 0, 20, (jbyte*) result);
	(*env)->ReleaseByteArrayElements(env, contents, contentsIn, 0);
	return sha1Result;
}

JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_sha256(JNIEnv * env, jobject thiz, jbyteArray contents) {
	sha256_ctx c;
	word32 result[8];
	jbyte* contentsIn = (*env)->GetByteArrayElements(env, contents, 0);
	size_t cLength = (*env)->GetArrayLength(env, contents);
	jbyteArray sha256Result = (*env)->NewByteArray(env, 32);

	sha256_initial(&c);
	sha256_process(&c, (byte*) contentsIn, cLength);
	sha256_final(&c, result);

	(*env)->SetByteArrayRegion(env, sha256Result, 0, 32, (jbyte*) result);
	(*env)->ReleaseByteArrayElements(env, contents, contentsIn, 0);
	return sha256Result;
}

JNIEXPORT jbyteArray JNICALL Java_base_tina_external_encrypt_TinaCrypt_getRc4Key(JNIEnv * env, jobject thiz, jstring seed, jbyteArray publicKey, jbyteArray contents) {
	vlPoint vlPublicKey, rc4Encode, msg;
	byte contentsOut[VL_SIZE << 1];
	const char* cseed = (*env)->GetStringUTFChars(env, seed, 0);
	jbyte* publicKeyIn = (*env)->GetByteArrayElements(env, publicKey, 0);
	jsize publicKeyLen = (*env)->GetArrayLength(env, publicKey);

	size_t i = 0, j = 0;

	for (i = 0, j = 0; i < VL_SIZE; i++, j += 2) {
		vlPublicKey[i] = ((publicKeyIn[j] & 0xFF) << 8) | (publicKeyIn[j + 1] & 0xFF);
	}

	byte *result = getRc4Key(cseed, rc4Encode, vlPublicKey, msg);

	for (i = 0, j = 0; i < VL_SIZE; i++, j += 2) {
		contentsOut[j] = msg[i] >> 8;
		contentsOut[j + 1] = msg[i] & 0xFF;
	}
	vlClear(msg);
	size_t arrayLength = VL_SIZE << 1;

	jbyteArray rc4KeyArray = (*env)->NewByteArray(env, arrayLength);
	(*env)->SetByteArrayRegion(env, rc4KeyArray, 0, arrayLength, result);
	(*env)->SetByteArrayRegion(env, contents, 0, arrayLength, contentsOut);

	(*env)->ReleaseStringUTFChars(env, seed, cseed);
	(*env)->ReleaseByteArrayElements(env, publicKey, publicKeyIn, 0);
	free(result);
	return rc4KeyArray;
}

JNIEXPORT jint JNICALL Java_base_tina_external_encrypt_TinaCrypt_getVlsize(JNIEnv * env, jobject thiz) {
	return VL_SIZE << 1;
}

JNIEXPORT void JNICALL Java_base_tina_external_encrypt_TinaCrypt_test(JNIEnv * env, jobject thiz) {
	vlPoint publicKey, msg, session, rc4Key;
	vlClear(publicKey);
	vlClear(msg);
	vlClear(session);
	vlClear(rc4Key);
	char* passwd = "watermoon";
	int i = 0;
	byte *pub, *rc4keyB, *out;
	pub = getPubKey(passwd, publicKey);

	rc4keyB = getRc4Key("0123456789", rc4Key, publicKey, msg);

	out = ecc_decode(msg, passwd, session);
	jboolean ok = 1;
	for (i = 0; i < 38; i++) {
		ok = rc4keyB[i] == out[i];
		if (!ok) break;
	}
}

