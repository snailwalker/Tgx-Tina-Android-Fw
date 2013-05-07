/*
 * AndroidEnv.h
 *
 *  Created on: Oct 18, 2011
 *      Author: Zhangzhuo
 */
#ifndef ANDROIDENV_H
#define ANDROIDENV_H
#include <android/log.h>

#define LOG_TAG    "TGX-SEARCHPY"
#define LOG_ENABLE 1
#define LOG_P_ULONG %llu
#define LOG_P_SLONG %lld
#define LOG_P_UINT %lu
#define LOG_P_INT %d
#ifdef LOG_ENABLE
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG,__VA_ARGS__)
#else
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)
#define LOGE(...)
#define LOGF(...)
#endif

#endif /* ANDROIDENV_H_ */
