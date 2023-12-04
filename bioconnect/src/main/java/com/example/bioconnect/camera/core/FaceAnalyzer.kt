package com.example.bioconnect.camera.core

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRectF
import com.example.bioconnect.viewmodel.ViewModel
import java.util.Vector

internal class FaceAnalyzer(
    private val faceBoxViewModel: ViewModel,
    private val width: Float,
    private val height: Float
) : ImageAnalysis.Analyzer {
    // 얼굴 검출
    private val translation = 60f // faceDetection 결과로 얼굴 box가 너무 딱 맞게 나와서 마진을 줌

    override fun analyze(imageProxy: ImageProxy) {
        val rotation = imageProxy.imageInfo.rotationDegrees

        val bitmap = Bitmap.createBitmap(
            imageProxy.toBitmap(),
            0,
            0,
            imageProxy.width,
            imageProxy.height,
            getRotation(rotation, imageProxy.width),
            false
        )

        val ratioX = width / bitmap.width.toFloat()
        val ratioY = height / bitmap.height.toFloat()

        // 얼굴 검출
        var _box = FaceDetector.instance.faceDetector.run(bitmap)

        if (_box == null) _box = Rect(0, 0, 0, 0)

        var box = RectF()

        Matrix().apply {
            setScale(ratioX, ratioY)
            mapRect(box, _box.toRectF())
        }

        box = if (box.left == 0f && box.top == 0f) {
            RectF(
                -100f,
                -100f,
                width + 100f,
                height + 100f
            )
        } else {
            RectF(
                box.left - translation,
                box.top - translation,
                box.right + translation,
                box.bottom + translation
            ) // adjust face box - end
        }

        faceBoxViewModel.update(box)

        if (_box.width() > 0 && _box.height() > 0) {
            try {
                if (faceBoxViewModel.isEstimating.value) {
                    val faceBitmap = Bitmap.createBitmap(
                        bitmap,
                        _box.left,
                        _box.top,
                        _box.width(),
                        _box.height()
                    )

                    var segmentation = FaceDetector.instance.faceDetector.getSkin(faceBitmap)
                    for (i in segmentation) {
                        if (i.isNaN() || i < 0f) {
                            segmentation = Vector<Float>(3)
                            break
                        }
                    }
                    faceBoxViewModel.addFrameList(segmentation)

                    faceBitmap.recycle()
                }

            } catch (e: Exception) {
                Log.e("Face>>", e.stackTrace.toString())
            }
        }
        bitmap.recycle()
        imageProxy.close()
    }
}
private fun getRotation(rotation: Int, width : Int)
        = Matrix().apply {
    setScale(-1f,1f)
    postTranslate(width.toFloat(),0f)
    postRotate(360 - rotation.toFloat())
}
