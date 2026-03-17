//
// Created by Bogey on 2026/3/10.
//

#ifndef TOUCHTOOL_LOGGING_H
#define TOUCHTOOL_LOGGING_H

#include <android/log.h>

#define LOGD(TAG, ...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(TAG, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#endif //TOUCHTOOL_LOGGING_H
