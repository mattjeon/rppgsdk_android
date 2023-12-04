#ifndef DETECTOR_H
#define DETECTOR_H

#include <opencv2/opencv.hpp>
#include <fstream>

#include "ultraface.h"
#include "kcftracker.h"

#define MIN_FACE_SIZE 20     // 최소 얼굴 검출 사이즈 (단위: 픽셀)

void getSkinImg(const cv::Mat &img, std::vector<float> &result);

class FaceDetector {

public:
    enum Preprocess_State {OK, LIGHT, MOVEMENT};

    FaceDetector(const std::string &model_path, const std::string &model_weight_path, int input_width, int input_length, float score_threshold_ = 0.7, float iou_threshold_ = 0.3, bool useTracker=false);

    cv::Rect detectLargestFace(cv::Mat &img);
    void suppressMinorMovement(cv::Rect &bbox, cv::Rect &prevBbox, int xyTolerance, int widthHeightTolerance);
    static void checkValidity(cv::Rect &_bbox, cv::Rect &bbox, int imgWidth, int imgHeight);
//    void getSkinImg(const cv::Mat &img, cv::Mat &result);
//    Preprocess_State preprocess(const cv::Mat& img);
    bool movementOccurs;

private:
    int type;
    UltraFace ultra_detector = UltraFace();
    cv::Mat resizedImg;
    int inputWidth;
    int inputHeight;
    cv::Rect prevBbox;
    bool useTracker;
    bool isTracking = false;
    KCFTracker tracker;
    int track_tolerance = 0;

    cv::Mat prev_gray;

    float prev_y = 0.f;


};

#endif //DETECTOR_H
