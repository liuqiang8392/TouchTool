#ifndef NATIVE_LIB
#define NATIVE_LIB

#include <jni.h>
#include <string>
#include <algorithm>

#include "native-lib.h"
#include "logging.h"

#if defined(__ARM_NEON) || defined(__ARM_NEON__)

#include <arm_neon.h>

#define USE_NEON 1
#else
#define USE_NEON 0
#endif

using namespace std;
using namespace cv;


static jobject createMatchResult(JNIEnv *env, jdouble value, jint x, jint y, jint width, jint height) {
    auto resultClass = (jclass) env->FindClass("top/bogey/touch_tool/utils/MatchResult");
    jmethodID mid = env->GetMethodID(resultClass, "<init>", "(DIIII)V");
    jobject result = env->NewObject(resultClass, mid, value, x, y, width, height);
    return result;
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

static float calculateBoxScore(const vector<Point> &contour, const Mat &probMap, const Mat &binary) {
    float min_x = contour[0].x, max_x = contour[0].x;
    float min_y = contour[0].y, max_y = contour[0].y;
    for (const auto &pt: contour) {
        min_x = min(min_x, static_cast<float>(pt.x));
        max_x = max(max_x, static_cast<float>(pt.x));
        min_y = min(min_y, static_cast<float>(pt.y));
        max_y = max(max_y, static_cast<float>(pt.y));
    }

    int x_start = max(0, static_cast<int>(min_x));
    int x_end = min(probMap.cols - 1, static_cast<int>(max_x));
    int y_start = max(0, static_cast<int>(min_y));
    int y_end = min(probMap.rows - 1, static_cast<int>(max_y));

    float box_score = 0.0f;
    int count = 0;
    for (int py = y_start; py <= y_end; ++py) {
        const float *prob_ptr = probMap.ptr<float>(py);
        const uchar *bin_ptr = binary.ptr<uchar>(py);
        for (int px = x_start; px <= x_end; ++px) {
            if (bin_ptr[px] > 0) {
                box_score += prob_ptr[px];
                ++count;
            }
        }
    }

    return (count > 0) ? (box_score / count) : 0.0f;
}

#ifdef USE_NEON

static void ArgmaxNeon8(const float *__restrict__ data, int size, int &max_idx, float &max_val) {
    if (size < 16) {
        max_idx = 0;
        max_val = data[0];
        for (int i = 1; i < size; ++i) {
            if (data[i] > max_val) {
                max_val = data[i];
                max_idx = i;
            }
        }
        return;
    }

    float32x4_t v_max = vld1q_f32(data);
    int32x4_t v_idx = {0, 1, 2, 3};
    int32x4_t v_max_idx = v_idx;
    const int32x4_t v_four = vdupq_n_s32(4);

    int i = 4;
    for (; i + 4 <= size; i += 4) {
        float32x4_t v_curr = vld1q_f32(data + i);
        v_idx = vaddq_s32(v_idx, v_four);

        uint32x4_t cmp = vcgtq_f32(v_curr, v_max);
        v_max = vbslq_f32(cmp, v_curr, v_max);
        v_max_idx = vbslq_s32(cmp, v_idx, v_max_idx);
    }

    float max_vals[4];
    int32_t max_idxs[4];
    vst1q_f32(max_vals, v_max);
    vst1q_s32(max_idxs, v_max_idx);

    max_val = max_vals[0];
    max_idx = max_idxs[0];
    for (int j = 1; j < 4; ++j) {
        if (max_vals[j] > max_val) {
            max_val = max_vals[j];
            max_idx = max_idxs[j];
        }
    }

    for (; i < size; ++i) {
        if (data[i] > max_val) {
            max_val = data[i];
            max_idx = i;
        }
    }
}

#endif

struct OcrRecInput {
    vector<float> input;
    int index;
    float x;
    float y;
    int left;
    int top;
    int right;
    int bottom;
};

extern "C" JNIEXPORT jobject JNICALL
Java_top_bogey_touch_1tool_utils_DisplayUtil_nativeMatchTemplate(JNIEnv *env, jclass clazz, jobject bitmap, jobject temp, jint speed, jboolean canny) {
    int scale = speed;

    Mat src = bitmap_to_cv_mat(env, bitmap);
    Mat tmp = bitmap_to_cv_mat(env, temp);
    if (src.empty() || tmp.empty() || tmp.cols > src.cols || tmp.rows > src.rows) {
        return createMatchResult(env, 0, 0, 0, 0, 0);
    }

    downsampleIfNeeded(src, tmp, scale);

    if (canny) {
        cvtColor(src, src, COLOR_BGR2GRAY);
        cvtColor(tmp, tmp, COLOR_BGR2GRAY);
        Canny(src, src, 50, 150);
        Canny(tmp, tmp, 50, 150);
    }

    Mat result;
    matchTemplate(src, tmp, result, TM_CCOEFF_NORMED);

    double minValue, maxValue;
    Point minLoc, maxLoc;
    minMaxLoc(result, &minValue, &maxValue, &minLoc, &maxLoc);

    jobject matchResult = createMatchResult(env, maxValue, maxLoc.x * scale, maxLoc.y * scale, tmp.cols * scale, tmp.rows * scale);
    return matchResult;
}

extern "C" JNIEXPORT jobject JNICALL
Java_top_bogey_touch_1tool_utils_DisplayUtil_nativeMatchAllTemplate(JNIEnv *env, jclass clazz, jobject bitmap, jobject temp, jint similarity, jint speed, jboolean canny) {
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

    if (canny) {
        cvtColor(src, src, COLOR_BGR2GRAY);
        cvtColor(tmp, tmp, COLOR_BGR2GRAY);
        Canny(src, src, 50, 150);
        Canny(tmp, tmp, 50, 150);
    }

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

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_top_bogey_touch_1tool_bean_save_model_OcrModel_detPostProcess(JNIEnv *env, jclass clazz, jobject bitmap, jfloatArray prob_map, jint width, jint height, jintArray widths, jfloatArray letter_box_info) {

    Mat src = bitmap_to_cv_mat(env, bitmap, COLOR_RGBA2RGB);

    vector<int> padWidths = jintarray_to_int_vector(env, widths);
    vector<float> letterBoxInfo = jfloatarray_to_float_vector(env, letter_box_info);

    jfloat *probMapPtr = env->GetFloatArrayElements(prob_map, JNI_FALSE);
    Mat probMap(height, width, CV_32FC1, probMapPtr);

    // 检测结果归一化
    double minVal, maxVal;
    minMaxLoc(probMap, &minVal, &maxVal);
    if (minVal < -0.5 || maxVal > 1.5) {
        exp(-probMap, probMap);
        probMap = 1.0 / (1.0 + probMap);
    }

    // 二值化
    Mat binary;
    threshold(probMap, binary, 0.1, 255, THRESH_BINARY);
    binary.convertTo(binary, CV_8U);

    // 获取外接矩形
    vector<vector<Point>> contours;
    findContours(binary, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    vector<OcrRecInput> inputs;
    // 筛选
    for (auto &contour: contours) {
        if (contour.size() < 4 || contourArea(contour) < 4) continue;

        RotatedRect box = minAreaRect(contour);
        if (box.size.width < 1 || box.size.height < 1) continue;

        float score = calculateBoxScore(contour, probMap, binary);
        if (score < 0.3) continue;

        // unclip
        float area = box.size.width * box.size.height;
        float perimeter = 2 * (box.size.width + box.size.height);
        if (perimeter > 1e-6f) {
            float distance = area * 1.5f / perimeter;
            box.size.width += distance * 2.f;
            box.size.height += distance * 2.f;
        }

        box.center.x = (box.center.x - letterBoxInfo[1]) / letterBoxInfo[0];
        box.center.y = (box.center.y - letterBoxInfo[2]) / letterBoxInfo[0];
        box.size.width = box.size.width / letterBoxInfo[0];
        box.size.height = box.size.height / letterBoxInfo[0];
        if (box.size.width * box.size.height < 50) continue;
        LOGD("detPostProcess", "box: %.2f %.2f %.2f %.2f", box.center.x, box.center.y, box.size.width, box.size.height);

        Rect boundRect = box.boundingRect();
        int left = boundRect.x;
        int top = boundRect.y;
        int right = boundRect.x + boundRect.width;
        int bottom = boundRect.y + boundRect.height;
        LOGD("detPostProcess", "box: %d, %d, %d, %d", left, top, right, bottom);

        Point2f pts[4];
        box.points(pts);
        Point2f tl, tr, br, bl;

        sort(pts, pts + 4, [](const Point2f &a, const Point2f &b) {
            return a.y < b.y;
        });
        if (pts[0].x < pts[1].x) {
            tl = pts[0];
            tr = pts[1];
        } else {
            tl = pts[1];
            tr = pts[0];
        }

        if (pts[2].x < pts[3].x) {
            bl = pts[2];
            br = pts[3];
        } else {
            bl = pts[3];
            br = pts[2];
        }
        pts[0] = tl;
        pts[1] = tr;
        pts[2] = br;
        pts[3] = bl;

        float w = (norm(tr - tl) + norm(br - bl)) * 0.5f;
        float h = (norm(bl - tl) + norm(br - tr)) * 0.5f;

        int maxWidth = padWidths[padWidths.size() - 1];

        int cropHeight = 48;
        int cropWidth = clamp(static_cast<int>(w * cropHeight / h), 1, maxWidth);
        LOGD("detPostProcess", "cropWidth: %d, cropHeight: %d", cropWidth, cropHeight);

        Point2f dst[4] = {
                Point2f(0, 0),
                Point2f(cropWidth - 1, 0),
                Point2f(cropWidth - 1, cropHeight - 1),
                Point2f(0, cropHeight - 1)
        };
        Mat M = getPerspectiveTransform(pts, dst);

        Mat crop;
        warpPerspective(src, crop, M, Size(cropWidth, cropHeight), INTER_LINEAR, BORDER_REPLICATE);

        int index = padWidths.size() - 1;
        for (int i = 0; i < padWidths.size(); ++i) {
            if (cropWidth <= padWidths[i]) {
                index = i;
                break;
            }
        }
        int realWidth = padWidths[index];
        if (realWidth < cropWidth) continue;

        Mat pad(cropHeight, realWidth, CV_32FC3, Scalar(0, 0, 0));
        crop.convertTo(pad(Rect(0, 0, crop.cols, crop.rows)), CV_32FC3, 1.0 / 255);

        vector<float> input(realWidth * cropHeight * 3);
        memcpy(input.data(), pad.data, input.size() * sizeof(float));
        inputs.push_back({std::move(input), index, box.center.x, box.center.y, left, top, right, bottom});
    }

    jclass inputCls = env->FindClass("top/bogey/touch_tool/bean/save/model/OcrRecInput");
    jmethodID ctor = env->GetMethodID(inputCls, "<init>", "([FIFFIIII)V");

    jobjectArray result = env->NewObjectArray(inputs.size(), inputCls, nullptr);

    for (int i = 0; i < inputs.size(); ++i) {
        OcrRecInput &input = inputs[i];

        jfloatArray inputData = cpp_array_to_jfloatarray(env, input.input.data(), input.input.size());
        jobject item = env->NewObject(inputCls, ctor, inputData, input.index, input.x, input.y, input.left, input.top, input.right, input.bottom);
        env->SetObjectArrayElement(result, i, item);

        env->DeleteLocalRef(item);
        env->DeleteLocalRef(inputData);
    }

    env->ReleaseFloatArrayElements(prob_map, probMapPtr, JNI_ABORT);
    return result;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_top_bogey_touch_1tool_bean_save_model_OcrModel_recPostProcess(JNIEnv *env, jclass clazz, jfloatArray output, jint num_classes, jint time_step) {
    vector<float> logits = jfloatarray_to_float_vector(env, output);
    float *logits_ptr = logits.data();

    vector<int> indices;

    float total_confidence = 0.0f;
    int char_count = 0;
    int prev_index = 0;

    for (int t = 0; t < time_step; ++t) {
        const float *step_logits = logits_ptr + t * num_classes;

        if (t + 1 < time_step) {
            __builtin_prefetch(logits_ptr + (t + 1) * num_classes, 0, 0);
        }

        int max_index = 0;
        float max_value = 0.0f;

#if USE_NEON
        ArgmaxNeon8(step_logits, num_classes, max_index, max_value);
#else
        max_value = step_logits[0];
        for (int c = 1; c < num_classes; ++c) {
            if (step_logits[c] > max_value) {
                max_value = step_logits[c];
                max_index = c;
            }
        }
#endif

        if (max_index == 0 || max_index == prev_index) {
            prev_index = max_index;
            continue;
        }
        prev_index = max_index;

        const int dict_idx = max_index - 1;
        if (dict_idx >= 0 && dict_idx < num_classes) {
            indices.push_back(dict_idx);
            total_confidence += max_value;
            ++char_count;
        }
    }

    float confidence = (char_count > 0) ? (total_confidence / char_count) : 0.0f;

    jclass outputCls = env->FindClass("top/bogey/touch_tool/bean/save/model/OcrRecOutput");
    jmethodID ctor = env->GetMethodID(outputCls, "<init>", "([IF)V");
    jintArray indicesArray = cpp_array_to_jintarray(env, indices.data(), indices.size());
    jobject result = env->NewObject(outputCls, ctor, indicesArray, confidence);
    env->DeleteLocalRef(indicesArray);
    return result;
}

#endif