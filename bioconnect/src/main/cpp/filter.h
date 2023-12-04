#ifndef FILTER_H
#define FILTER_H

#include <opencv2/opencv.hpp>

void createSkinMask(cv::InputArray _src, cv::OutputArray _dst);
void detrend(cv::InputArray _a, cv::OutputArray _b, int winSize);
void bandpass(cv::InputArray _a, cv::OutputArray _b, double srate, int minBpm, int maxBpm);
void timeToFrequency(cv::InputArray _a, cv::OutputArray _b, bool magnitude);
void frequencyToTime(cv::InputArray _a, cv::OutputArray _b);
void applySlopSumFunction(cv::InputArray _a, cv::OutputArray _b, int winSize);

#endif //FILTER_H