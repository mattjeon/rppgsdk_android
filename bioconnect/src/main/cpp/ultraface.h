#ifndef ULTRAFACE_H
#define ULTRAFACE_H

#include <algorithm>
#include <iostream>
#include <string>
#include <vector>
#include <opencv2/opencv.hpp>

#define num_featuremap 4
#define hard_nms 1
#define blending_nms 2 /* mix nms was been proposaled in paper blaze face, aims to minimize the temporal jitter*/

typedef struct FaceInfo {
    float x1;
    float y1;
    float x2;
    float y2;
    float score;
} FaceInfo;

class UltraFace {
public:
    UltraFace();

    ~UltraFace();

    void load(const std::string &model_path, int input_width, int input_length, float score_threshold_ = 0.7, float iou_threshold_ = 0.3);
    int detect(cv::Mat &img, std::vector<FaceInfo> &face_list);

private:
    void generateBBox(std::vector<FaceInfo> &bbox_collection, cv::Mat scores, cv::Mat boxes, float score_threshold, int num_anchors);

    void nms(std::vector<FaceInfo> &input, std::vector<FaceInfo> &output, int type = blending_nms);

private:
    cv::dnn::Net ultraface;

    int image_w;
    int image_h;

    int in_w;
    int in_h;
    int num_anchors;

    float score_threshold;
    float iou_threshold;

    std::vector<std::vector<float>> featuremap_size;
    std::vector<std::vector<float>> shrinkage_size;
    std::vector<int> w_h_list;

    const cv::Scalar mean_vals = cv::Scalar(127, 127, 127);
    const float norm_val = 1.0 / 128;

    const float center_variance = 0.1;
    const float size_variance = 0.2;
    const std::vector<std::vector<float>> min_boxes = {
            {10.0f,  16.0f,  24.0f},
            {32.0f,  48.0f},
            {64.0f,  96.0f},
            {128.0f, 192.0f, 256.0f} };
    const std::vector<float> strides = { 8.0, 16.0, 32.0, 64.0 };

    std::vector<std::vector<float>> priors = {};
    cv::Mat in;
};

#endif //ULTRAFACE_H
