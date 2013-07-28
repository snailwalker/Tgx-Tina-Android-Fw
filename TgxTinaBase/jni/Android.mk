# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := tina_crypt
APP_ABI 		:= armeabi armeabi-v7a x86
LOCAL_SRC_FILES := \
	byte_stream.c \
    sha1.c \
    sha256.c \
    ec_param.c \
    ec_vlong.c \
    ec_field.c \
    ec_curve.c \
    ec_crypt.c \
    ecc_crypt.c \
    base_tina_external_encrypt_TinaCrypt.c \
     
LOCAL_LDLIBS := -llog 

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := searchpy
APP_ABI 		:= armeabi armeabi-v7a x86
LOCAL_SRC_FILES := \
	Collection.c \
    SearchCore.c \
    SearchTools.c \
    base_tina_external_contacts_serach_SearchPy.c \
    
LOCAL_LDLIBS := -llog 

include $(BUILD_SHARED_LIBRARY)
