#include "detector.h"
#include "logs.h"
#include "filter.h"

using namespace std;
using namespace cv;

#define TRACK_TOLERANCE_RATIO 0.08
#define CUT_ASPECT_RATIO 2.5


FaceDetector::FaceDetector(const std::string &model_path, const std::string &model_weight_path,
                           int input_width, int input_height, float score_threshold_,
                           float iou_threshold_, bool useTracker) {
    this->ultra_detector.load(model_path, input_width, input_height, score_threshold_,
                              iou_threshold_);
    this->useTracker = useTracker;
    this->inputWidth = input_width;
    this->inputHeight = input_width;
    if (useTracker)
        this->tracker = KCFTracker();
}

void getSkinImg(const cv::Mat &img, std::vector<float> &result) {
    // 피부색 필터링
    Mat mask, _result;
    Mat planes[3];
    createSkinMask(img, mask);

    int nPixels;
    float th = 0.5f;
    int imgSize = img.cols * img.rows;

    if(!img.empty()){
//        cvtColor(img, yCrCb, COLOR_BGR2YCrCb);

        if (mask.empty()) {
            nPixels = imgSize;

        } else {
            nPixels = (int)cv::sum(mask)[0];
            if (nPixels < (int)(imgSize * th)) {
                nPixels = imgSize;
            } else {
//                cv::cvtColor(yCrCb, _result, cv::COLOR_YCrCb2BGR);

                img.copyTo(_result);

                _result.setTo(0, mask == 0);

                split(_result, planes);

                result[2] = (float)(sum(planes[0])[0] / nPixels); // b
                result[1] = (float)(sum(planes[1])[0] / nPixels); // g
                result[0] = (float)(sum(planes[2])[0] / nPixels); // r
            }
        }
    }

}

Rect FaceDetector::detectLargestFace(cv::Mat &img) {
    Rect bbox(0, 0, 1, 1);
    Rect _bbox;

    // 처리 속도 향상을 위한 다운샘플링된 이미지 생성
    float xScaleRatio = (float) inputWidth / img.cols;
    float yScaleRatio = (float) inputHeight / img.rows;
    cv::resize(img, resizedImg, cv::Size(), xScaleRatio, yScaleRatio);
    cv::Mat gray;

    if(!img.empty()){
        cv::cvtColor(img, gray, cv::COLOR_BGR2GRAY);

        // 추적 중인 객체가 존재할 경우, 추적결과 가져오기
        if (isTracking) {
            _bbox = tracker.update(resizedImg, 0.4);
            if (_bbox.width == 0)
                isTracking = false;

            // 추적 중인 객체가 존재하지 않을 경우, 새로 검출
        } else {
            vector<FaceInfo> faceInfos;
            ultra_detector.detect(resizedImg, faceInfos);

            if (!faceInfos.empty() > 0) {
                // 가장 큰 얼굴 하나만 남김
                std::sort(faceInfos.begin(), faceInfos.end(), [](const FaceInfo &a, const FaceInfo &b) {
                    return (a.x2 - a.x1) * (a.y2 - a.y1) - (b.x2 - b.x1) * (b.y2 - b.y1);
                });
                FaceInfo largest = faceInfos[0];

                _bbox.x = largest.x1;
                _bbox.y = largest.y1;
                _bbox.width = largest.x2 - largest.x1;
                _bbox.height = largest.y2 - largest.y1;

                if (useTracker) {
                    isTracking = true;
                    tracker.init(_bbox, resizedImg);
                }
            }
        }

        if (_bbox.width > 0) {
            // 얼굴 위치 원본 이미지 사이즈로 복원
            _bbox.x = (int) (_bbox.x / xScaleRatio);
            _bbox.y = (int) (_bbox.y / yScaleRatio);
            _bbox.width = (int) (_bbox.width / xScaleRatio);
            _bbox.height = (int) (_bbox.height / yScaleRatio);

            // 작은 움직임 억제
            track_tolerance = (int) (TRACK_TOLERANCE_RATIO * _bbox.width);
            suppressMinorMovement(_bbox, prevBbox, track_tolerance, track_tolerance);

            // 좌표 유효성 검증
            checkValidity(_bbox, bbox, img.cols, img.rows);

            prevBbox = bbox;
        }
        prev_gray = gray;
        return bbox;
    }
    else
        return bbox;
}

void FaceDetector::suppressMinorMovement(Rect &bbox, Rect &prevBbox, int xyTolerance,
                                         int widthHeightTolerance) {
    movementOccurs = false;

    // 영역 안정화 작업(이전 얼굴 대비 TRACK_TOLERANCE 보다 작은 변화값은 무시함)
    if (abs(bbox.x - prevBbox.x) <= xyTolerance)
        bbox.x = prevBbox.x;
    else
        movementOccurs = true;

    if (abs(bbox.y - prevBbox.y) <= xyTolerance)
        bbox.y = prevBbox.y;
    else
        movementOccurs = true;

    if (abs(bbox.width - prevBbox.width) <= widthHeightTolerance)
        bbox.width = prevBbox.width;
    else
        movementOccurs = true;

    if (abs(bbox.height - prevBbox.height) <= widthHeightTolerance)
        bbox.height = prevBbox.height;
    else
        movementOccurs = true;
}

void FaceDetector::checkValidity(Rect &_bbox, Rect &bbox, int imgWidth, int imgHeight) {
    // 좌표 유효성 검증
    if (_bbox.x >= 0 && _bbox.y >= 0
        && _bbox.x + _bbox.width < imgWidth - 1 &&
        _bbox.y + _bbox.height < imgHeight - 1  // 이미지 경계 체크
        && _bbox.width >= MIN_FACE_SIZE &&
        _bbox.height >= MIN_FACE_SIZE   // 얼굴 사이즈 체크
        &&
        _bbox.height / _bbox.width < CUT_ASPECT_RATIO) { // 얼굴 종횡비 체크
        bbox.x = _bbox.x;
        bbox.y = _bbox.y;
        bbox.width = _bbox.width;
        bbox.height = _bbox.height;
    }
}