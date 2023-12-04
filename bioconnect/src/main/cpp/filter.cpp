#include "filter.h"

#include <android/log.h>
#include "logs.h"

using namespace std;
using namespace cv;

#define SEC_PER_MIN 60


void createSkinMask(cv::InputArray _src, cv::OutputArray _dst) {
	/** Skin filtering based on YCbCr color space */
	cv::Mat rgb = _src.getMat();
	cv::Mat mask;
	cv::Mat ycrcb;

	cv::cvtColor(rgb, ycrcb, cv::COLOR_BGR2YCrCb);
	cv::inRange(ycrcb, cv::Scalar(0, 133, 77), cv::Scalar(235, 173, 127), mask);
	mask.setTo(1, mask == 255);
//	LOGD("aaaaa %f", cv::sum(mask)[0]);
	mask.copyTo(_dst);
}

void convolve_1d_same(cv::InputArray _a, cv::InputArray _b, cv::OutputArray _c) {

	Mat a = _a.getMat();
	Mat b = _b.getMat();

	Mat c = Mat::zeros(a.rows, a.cols, a.type());
	int kernelSize = b.rows;
	int xs, xe, ys;

	for(int i = 0; i < a.rows; i++) {
		int k = i - kernelSize / 2;
		if (k < 0) {
			xs = 0;
			ys = -k;
		} else {
			xs = k;
			ys = 0;
		}

		if (k + kernelSize - 1 < a.rows) {
			xe = k + kernelSize;
		} else {
			xe = a.rows;
		}

		float sum = 0.0;
		for(int j = 0; j < xe-xs; j++) {
			sum += a.at<float>(xs + j, 0) * b.at<float>(ys + j, 0);
		}
		c.at<float>(i, 0) = sum;
	}

	c.copyTo(_c);
}


void detrend(cv::InputArray _a, cv::OutputArray _b, int winSize) {
	/** Detrending filter */
	Mat a = _a.getMat();
	int rows = a.rows;

	if (rows < winSize) {
		a.copyTo(_b);

	} else {
		Mat norm, mean;
		convolve_1d_same(Mat::ones(a.rows, a.cols, a.type()), Mat::ones(winSize, a.cols, a.type()), norm);
		convolve_1d_same(a, Mat::ones(winSize, a.cols, a.type()), mean);
		mean /= norm;

		Mat b = (a - mean) / mean;
		b.copyTo(_b);
	}
}

void butterworth_lowpass_filter(cv::Mat &filter, double cutoff, int n) {
	if (cutoff > 0 && n > 0 && filter.rows % 2 == 0 && filter.cols % 2 == 0) {
		throw runtime_error("invalid_argument");
	}

	cv::Mat tmp = cv::Mat(filter.rows, filter.cols, CV_32F);
	double radius;

	for (int i = 0; i < filter.rows; i++) {
		for (int j = 0; j < filter.cols; j++) {
			radius = i;
			tmp.at<float>(i, j) = (float)(1 / (1 + pow(radius / cutoff, 2 * n)));
		}
	}

	cv::Mat toMerge[] = { tmp, tmp };
	cv::merge(toMerge, 2, filter);
}

void butterworth_bandpass_filter(cv::Mat &filter, double cutin, double cutoff, int n) {
	if (cutoff > 0 && cutin < cutoff && n > 0 && filter.rows % 2 == 0 && filter.cols % 2 == 0) {
		throw runtime_error("invalid_argument");
	}
	cv::Mat off = filter.clone();
	butterworth_lowpass_filter(off, cutoff, n);
	cv::Mat in = filter.clone();
	butterworth_lowpass_filter(in, cutin, n);
	filter = off - in;
}


void bandpass(cv::InputArray _a, cv::OutputArray _b, double srate, int minBpm, int maxBpm) {
	/** Bandpass filter */
	cv::Mat a = _a.getMat();
	if (a.total() < 3) {
		a.copyTo(_b);

	} else {
		// 관심 대역 계산
		int low = (int) (a.rows * minBpm / SEC_PER_MIN / srate);
		int high = (int) (a.rows * maxBpm / SEC_PER_MIN / srate) + 1;

		// Convert to frequency domain
		cv::Mat frequencySpectrum = cv::Mat(a.rows, a.cols, CV_32F);
		timeToFrequency(a, frequencySpectrum, false);

		// Make the filter
		cv::Mat filter = frequencySpectrum.clone();
		butterworth_bandpass_filter(filter, low, high, 8);

		// Apply the filter
		multiply(frequencySpectrum, filter, frequencySpectrum);

		// Convert to time domain
		frequencyToTime(frequencySpectrum, _b);
	}
}

//// This function calculates the normalized DFT
///** http://firsttimeprogrammer.blogspot.com/2018/03/calculating-dft-in-c.html
// */
//vector< complex<double> > calculate_dft(const vector< complex<double> > &signal)
//{
//	vector< complex<double> > dft(signal.size());   // DFT vector
//	double N = double(signal.size());               // Number of samples
//	complex<double> temp = {{0, 0}};                // Temporary loop variable
//
//	for(int k = 0; k < signal.size(); k++)
//	{
//		for(int n = 0; n < signal.size(); n++)
//		{
//			temp = {{double(-1*2*k*n) / N, 0}};
//			dft[k] += signal[n] * exp(dft_constants::IMAG_UNIT * dft_constants::PI * temp) / N;
//		}
//	}
//	return dft;
//}

void timeToFrequency(cv::InputArray _a, cv::OutputArray _b, bool magnitude) {
	// Prepare planes
	cv::Mat a = _a.getMat();
	cv::Mat planes[] = { cv::Mat_<float>(a), cv::Mat::zeros(a.size(), CV_32F) };
	cv::Mat powerSpectrum;
	cv::merge(planes, 2, powerSpectrum);

	// Fourier transform
	cv::dft(powerSpectrum, powerSpectrum, cv::DFT_COMPLEX_OUTPUT);

	if (magnitude) {
		split(powerSpectrum, planes);
		cv::magnitude(planes[0], planes[1], planes[0]);
		planes[0].copyTo(_b);

	} else {
		powerSpectrum.copyTo(_b);
	}
}

void frequencyToTime(cv::InputArray _a, cv::OutputArray _b) {
	cv::Mat a = _a.getMat();

	// Inverse fourier transform
	cv::idft(a, a);

	// Split into planes; plane 0 is output
	cv::Mat outputPlanes[2];
	cv::split(a, outputPlanes);
	cv::Mat output = cv::Mat(a.rows, 1, a.type());
	cv::normalize(outputPlanes[0], output, 0, 1, cv::NORM_MINMAX);
	output.copyTo(_b);
}


void applySlopSumFunction(cv::InputArray _a, cv::OutputArray _b, int winSize) {
	cv::Mat a = _a.getMat();
	cv::Mat b = Mat(a.size(), a.type());
	int len = a.rows;

	for (int i=0; i<len; i++) {
		double sum = 0.0;
		for(int j=max(0, i - winSize); j < i; j++) {
			sum += a.at<double>(j, 0);
		}
		b.at<double>(i) = sum > 0.0 ? sum : 0.0;
	}

	b.copyTo(_b);
}