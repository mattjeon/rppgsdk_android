package com.example.bioconnect.camera.core

import android.app.Activity
import com.example.bioconnect.camera.wrapper.Detector
import com.example.bioconnect.utils.assetFilePath
import java.io.File

internal class FaceDetector {
    companion object{
        val instance = FaceDetector()
    }
    // 얼굴 검출 관련
    private lateinit var assetPath : String
    private lateinit var modelPath : String

    //don't edit this value ( in_height & in_width)
    private var inHeight = 320
    private var inWidth = 240
    private val scoreThr = 0.8f
    private val iouThr = 0.5f
    lateinit var faceDetector : Detector

    fun initFaceDetector(activity: Activity){
        assetPath = assetFilePath(activity, "slim_320_without_postprocessing.onnx")!!
        modelPath = File(assetPath).absolutePath

        faceDetector = Detector(modelPath, "", inWidth, inHeight, scoreThr, iouThr, true)
    }

}