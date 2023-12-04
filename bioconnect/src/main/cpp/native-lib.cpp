#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include "logs.h"
#include "detector.h"

// 변수 선언을 한 후에 값을 비교해야함.
// * debug 모드는 괜찮은데, release 모드에서 에러
// AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) == ANDROID_BITMAP_RESULT_SUCCESS  -> err

bool BitmapToMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &mat_bmp) {
    /* https://jamssoft.tistory.com/113
     **/
    bool ret = JNI_FALSE;
//    void *pBmp = NULL;               // receive the pixel data
    uint8_t* pBmp = NULL;
    AndroidBitmapInfo bitmapInfo;    // receive the bitmap info

    auto getInfo = AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo);

    assert(obj_bitmap);
    assert( getInfo == ANDROID_BITMAP_RESULT_SUCCESS);  // Get bitmap info
//    assert( bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
//            bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565);

    auto lock = AndroidBitmap_lockPixels(env, obj_bitmap, (void**)&pBmp);

    assert( lock == ANDROID_BITMAP_RESULT_SUCCESS );     // Get pixel data (lock memory block)
    assert( pBmp );

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat _tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, pBmp);    // Establish temporary mat
        _tmp.copyTo(mat_bmp);                                                  // Copy to target matrix
        ret = JNI_TRUE;
    }

    if (pBmp) {
        AndroidBitmap_unlockPixels(env, obj_bitmap);            // Java에서 쓸 수 있도록 unlock
    }

    return ret;
}

jobject Mat2Bitmap(JNIEnv * env, cv::Mat & src, bool needPremultiplyAlpha, jobject bitmap_config){

    jclass java_bitmap_class = (jclass)env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetStaticMethodID(java_bitmap_class,
                                           "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jobject bitmap = env->CallStaticObjectMethod(java_bitmap_class,
                                                 mid, src.size().width, src.size().height, bitmap_config);
    AndroidBitmapInfo  info;
    void* pixels = 0;

    try {
        //validate
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);

        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);

        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);

        CV_Assert(pixels);

        //type mat
        if(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888){

            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(src.type() == CV_8UC1){

                cvtColor(src, tmp, cv::COLOR_GRAY2RGBA);
            } else if(src.type() == CV_8UC3){
//                cvtColor(src, tmp, cv::COLOR_RGB2RGBA);
                cvtColor(src, tmp, cv::COLOR_RGB2BGRA);
            } else if(src.type() == CV_8UC4){

                if(needPremultiplyAlpha){
                    cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                }else{
                    src.copyTo(tmp);
                }
            }

        } else{
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if(src.type() == CV_8UC1){
                cvtColor(src, tmp, cv::COLOR_GRAY2BGR565);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, cv::COLOR_RGB2BGR565);
            } else if(src.type() == CV_8UC4){
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }

        AndroidBitmap_unlockPixels(env, bitmap);

        return bitmap;
    }
//    catch(cv::Exception e){
//        AndroidBitmap_unlockPixels(env, bitmap);
//        jclass je = env->FindClass("org/opencv/core/CvException");
//        if(!je) je = env->FindClass("java/lang/Exception");
//        env->ThrowNew(je, e.what());
//        return bitmap;
//    }
    catch (...){
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return bitmap;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_bioconnect_camera_wrapper_Detector_newSelf(JNIEnv *env, jobject thiz,
                                                       jstring model_path,
                                                       jstring model_weight_path, jint in_width,
                                                       jint in_height, jfloat score_thresh,
                                                       jfloat iou_thresh, jboolean use_tracker) {
    // TODO: implement newSelf()
    // TODO: implement newSelf()
    const char *model_path_char = env->GetStringUTFChars(model_path, 0);
    const char *model_weight_path_char = env->GetStringUTFChars(model_weight_path, 0);

    auto *self = new FaceDetector(model_path_char, model_weight_path_char, in_width, in_height, score_thresh, iou_thresh, use_tracker);
    return (jlong) self;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_bioconnect_camera_wrapper_Detector_deleteSelf(JNIEnv *env, jobject thiz,
                                                          jlong self_addr) {
    // TODO: implement deleteSelf()
    if (self_addr != 0) {
        auto *self = (FaceDetector *) self_addr;
        delete self;
    }
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_bioconnect_camera_wrapper_Detector_run(JNIEnv *env, jobject thiz, jlong self_addr,
                                                   jobject input) {
    // TODO: implement run()
    if (self_addr != 0) {
        auto *self = (FaceDetector *) self_addr;

        cv::Mat pixel_data ;
        bool ret = BitmapToMatrix(env, input, pixel_data);

        if (ret != JNI_TRUE){
            return 0;
        }else{
            cv::Mat bgr;
            cv::cvtColor(pixel_data, bgr, cv::COLOR_RGBA2BGR);
            cv::Rect2i result = self->detectLargestFace(bgr);

            // 자바 자료형 및 메소드 생성
            jclass rectClass = env->FindClass("android/graphics/Rect");
            jmethodID rectCtorID = env->GetMethodID(rectClass, "<init>", "(IIII)V");
            jobject jRect = env->NewObject(rectClass, rectCtorID, result.x, result.y, result.x+result.width, result.y+result.height);
            return jRect;
        }

    }

    return 0;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_bioconnect_camera_wrapper_Detector_getSkinDataArray(JNIEnv *env, jobject thiz,
                                                                jlong self_addr, jobject input) {
    // TODO: implement getSkinDataArray()
    cv::Mat pixel_data;
    bool ret = BitmapToMatrix(env, input, pixel_data);

    if (ret != JNI_TRUE) return 0;
    else{
        cv::Mat bgr;
        std::vector<float> output(3);
        cv::cvtColor(pixel_data, bgr, cv::COLOR_RGBA2BGR);

        getSkinImg(bgr, output);

        // 자바 자료형 및 메소드 생성
        jclass vectorClass = env->FindClass("java/util/Vector");
        jclass floatClass = env->FindClass("java/lang/Float");
        jmethodID initMethodID = env->GetMethodID(vectorClass, "<init>", "()V");
        jmethodID addMethodID = env->GetMethodID(vectorClass, "add", "(Ljava/lang/Object;)Z");
        // 자바용 벡터 생성
        jobject jvec = env->NewObject(vectorClass, initMethodID);
        for (float f : output) {
            jmethodID floatConstructorID = env->GetMethodID(floatClass, "<init>", "(F)V");
            jobject floatValue = env->NewObject(floatClass, floatConstructorID, f);
            env->CallBooleanMethod(jvec, addMethodID, floatValue);
        }
        env->DeleteLocalRef(vectorClass);
        env->DeleteLocalRef(floatClass);

        return jvec;
    }
}