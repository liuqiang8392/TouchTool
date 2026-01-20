#ifndef NATIVE_LIB
#define NATIVE_LIB

#include <jni.h>
#include <string>
#include <algorithm>

#include "native-lib.h"

using namespace std;
using namespace cv;


static jobject createMatchResult(JNIEnv *env, jdouble value, jint x, jint y, jint width, jint height) {
    auto resultClass = (jclass) env->FindClass("top/bogey/touch_tool/utils/MatchResult");
    jmethodID mid = env->GetMethodID(resultClass, "<init>", "(DIIII)V");
    jobject result = env->NewObject(resultClass, mid, value, x, y, width, height);
    return result;
}

static int clamp(int up, int low, int value) {
    return max(low, min(up, value));
}

static void downsampleIfNeeded(Mat &src, Mat &tmp, int scale) {
    if (scale > 1) {
        double factor = 1.0 / scale;
        resize(src, src, Size(), factor, factor, INTER_AREA);
        resize(tmp, tmp, Size(), factor, factor, INTER_AREA);
    }
}

static float calculateIoU(const Rect &a, const Rect &b) {
    int inner_x1 = max(a.x, b.x);
    int inner_y1 = max(a.y, b.y);
    int inner_x2 = min(a.x + a.width, b.x + b.width);
    int inner_y2 = min(a.y + a.height, b.y + b.height);

    int inner_w = max(0, inner_x2 - inner_x1);
    int inner_h = max(0, inner_y2 - inner_y1);

    int inner_area = inner_w * inner_h;
    int all_area = a.area() + b.area() - inner_area;
    return inner_area * 1.0f / all_area;
}

extern "C" JNIEXPORT jobject JNICALL
Java_top_bogey_touch_1tool_utils_DisplayUtil_nativeMatchTemplate(JNIEnv *env, jclass clazz, jobject bitmap, jobject temp, jint speed) {
    int scale = speed;

    Mat src = bitmap_to_cv_mat(env, bitmap);
    Mat tmp = bitmap_to_cv_mat(env, temp);
    if (src.empty() || tmp.empty() || tmp.cols > src.cols || tmp.rows > src.rows) {
        return createMatchResult(env, 0, 0, 0, 0, 0);
    }

    downsampleIfNeeded(src, tmp, scale);

    Mat result;
    matchTemplate(src, tmp, result, TM_CCOEFF_NORMED);

    double minValue, maxValue;
    Point minLoc, maxLoc;
    minMaxLoc(result, &minValue, &maxValue, &minLoc, &maxLoc);

    jobject matchResult = createMatchResult(env, maxValue, maxLoc.x * scale, maxLoc.y * scale, tmp.cols * scale, tmp.rows * scale);
    src.release();
    tmp.release();
    result.release();
    return matchResult;
}

extern "C" JNIEXPORT jobject JNICALL
Java_top_bogey_touch_1tool_utils_DisplayUtil_nativeMatchAllTemplate(JNIEnv *env, jclass clazz, jobject bitmap, jobject temp, jint similarity, jint speed) {
    int scale = speed;

    Mat src = bitmap_to_cv_mat(env, bitmap);
    Mat tmp = bitmap_to_cv_mat(env, temp);

    jclass listCls = env->FindClass("java/util/ArrayList");
    jmethodID listInit = env->GetMethodID(listCls, "<init>", "()V");
    jobject listObj = env->NewObject(listCls, listInit);

    if (src.empty() || tmp.empty() || tmp.cols > src.cols || tmp.rows > src.rows) {
        return listObj;
    }

    downsampleIfNeeded(src, tmp, scale);

    Mat result;
    matchTemplate(src, tmp, result, TM_CCOEFF_NORMED);

    jmethodID listAdd = env->GetMethodID(listCls, "add", "(Ljava/lang/Object;)Z");

    double threshold = clamp(100, 1, similarity) / 100.0;


    struct Candidate {
        Rect rect;
        float score;
    };

    vector<Candidate> candidates;
    for (int i = 0; i < result.rows; ++i) {
        auto *rowPtr = result.ptr<float>(i);
        for (int j = 0; j < result.cols; ++j) {
            if (rowPtr[j] >= threshold) {
                candidates.push_back({Rect(j, i, tmp.cols, tmp.rows), rowPtr[j]});
            }
        }
    }

    sort(candidates.begin(), candidates.end(), [](const Candidate &a, const Candidate &b) {
        return a.score > b.score;
    });

    vector<bool> suppressed(candidates.size(), false);
    const float iouThreshold = 0.5;
    const int maxCandidates = 100;
    int count = 0;

    for (int i = 0; i < candidates.size(); ++i) {
        if (suppressed[i]) continue;
        if (count >= maxCandidates) break;

        const auto &best = candidates[i];
        jobject item = createMatchResult(env, best.score, best.rect.x * scale, best.rect.y * scale, best.rect.width * scale, best.rect.height * scale);
        env->CallBooleanMethod(listObj, listAdd, item);
        env->DeleteLocalRef(item);

        count++;

        for (int j = i + 1; j < candidates.size(); ++j) {
            if (suppressed[j]) continue;

            const auto &curr = candidates[j];
            float iou = calculateIoU(best.rect, curr.rect);
            if (iou > iouThreshold) {
                suppressed[j] = true;
            }
        }
    }

    src.release();
    tmp.release();
    result.release();
    return listObj;
}

extern "C" JNIEXPORT jobject JNICALL
Java_top_bogey_touch_1tool_utils_DisplayUtil_nativeMatchColor(JNIEnv *env, jclass clazz, jobject bitmap, jintArray rgb, jint similarity) {
    Mat src = bitmap_to_cv_mat(env, bitmap);

    jclass listCls = env->FindClass("java/util/ArrayList");
    jmethodID listInit = env->GetMethodID(listCls, "<init>", "()V");
    jobject listObj = env->NewObject(listCls, listInit);

    if (src.empty()) return listObj;

    cvtColor(src, src, COLOR_BGR2HSV);
    GaussianBlur(src, src, Size(5, 5), 0);

    jint *rgbColor = env->GetIntArrayElements(rgb, JNI_FALSE);
    Mat bgr = Mat(1, 1, CV_8UC3, Vec3b(rgbColor[2], rgbColor[1], rgbColor[0]));
    Mat hsv;
    cvtColor(bgr, hsv, COLOR_BGR2HSV);
    Vec3b hsvColor = hsv.at<Vec3b>(0, 0);

    int similar = clamp(100, 1, similarity);
    double diff = 1 - (similar / 100.0);

    double tolH = 180 * diff;
    double tolSV = 255 * diff;

    double lowH = hsvColor[0] - tolH;
    double highH = hsvColor[0] + tolH;
    double lowS = max(0.0, hsvColor[1] - tolSV);
    double highS = min(255.0, hsvColor[1] + tolSV);
    double lowV = max(0.0, hsvColor[2] - tolSV);
    double highV = min(255.0, hsvColor[2] + tolSV);

    Mat mask;
    if (lowH >= 0 && highH <= 180) {
        inRange(src, Scalar(lowH, lowS, lowV), Scalar(highH, highS, highV), mask);
    } else {
        mask = Mat::zeros(src.size(), CV_8UC1);

        // 非环绕部分
        double nLowH = max(0.0, lowH);
        double nHighH = min(180.0, highH);
        if (nLowH < nHighH) {
            Mat temp;
            inRange(src, Scalar(nLowH, lowS, lowV), Scalar(nHighH, highS, highV), temp);
            mask |= temp;
        }

        // 环绕部分
        if (lowH < 0) {
            Mat temp;
            inRange(src, Scalar(180 + lowH, lowS, lowV), Scalar(180, highS, highV), temp);
            mask |= temp;
        }
        if (highH > 180) {
            Mat temp;
            inRange(src, Scalar(0, lowS, lowV), Scalar(highH - 180, highS, highV), temp);
            mask |= temp;
        }
    }

    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    morphologyEx(mask, mask, MORPH_OPEN, kernel);

    vector<vector<Point> > contours;
    findContours(mask, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    jmethodID listAdd = env->GetMethodID(listCls, "add", "(Ljava/lang/Object;)Z");

    for (auto &contour: contours) {
        double area = contourArea(contour);
        if (area > 100) {
            Rect r = boundingRect(contour);
            jobject item = createMatchResult(env, area, r.x, r.y, r.width, r.height);
            env->CallBooleanMethod(listObj, listAdd, item);
            env->DeleteLocalRef(item);
        }
    }

    src.release();
    env->ReleaseIntArrayElements(rgb, rgbColor, 0);
    return listObj;
}

#endif