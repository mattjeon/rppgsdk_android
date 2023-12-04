package com.example.bioconnect.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bioconnect.R
import com.example.bioconnect.camera.core.FaceAnalyzer
import com.example.bioconnect.camera.core.FaceRecognitionScreenContent
import com.example.bioconnect.camera.core.MeasureView
import com.example.bioconnect.utils.ESTIMATE_TIME
import com.example.bioconnect.utils.HandleRequest
import com.example.bioconnect.utils.PermissionDeniedContent
import com.example.bioconnect.utils.composable.progressbar.LinearProgress
import com.example.bioconnect.utils.createCornersPath
import com.example.bioconnect.viewmodel.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun CameraView(
    viewModel: ViewModel,
//    func: () -> Unit = {}
){
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current

    val screenWidthPx = (configuration.screenWidthDp.dp * density).value
    val screeHeightPx = (configuration.screenHeightDp.dp * density).value

    val faceAnalyzer = remember{
        FaceAnalyzer(
            viewModel,
            screenWidthPx,
            screeHeightPx
        )
    }

    // setting for face box animation
    val box = viewModel.sharedFaceBoxState.collectAsState().value

    val animateSpec: TweenSpec<Float> = tween(durationMillis = 300, delayMillis = 0)
    var animationTrigger by remember { mutableStateOf(false) }
    val animateBox = getAnimationBox(box, animateSpec, animationTrigger)

    LaunchedEffect(key1 = true){
        animationTrigger = true
    }

    Box(modifier = Modifier.fillMaxSize()){
        // for camera permission
        RequestPermissions(
            context = LocalContext.current,
            permissions = listOf(Manifest.permission.CAMERA),
            func = {
                FaceRecognitionScreenContent(faceAnalyzer)
            },
            viewModel = viewModel
        )

        if(viewModel.sharedPermissionState.value){
            LinearProgress(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 125.dp)
                    .align(Alignment.BottomCenter),
                indicatorValue = if(viewModel.isEstimating.value) viewModel.estimateTime.value.toFloat() else 0f,
                foregroundIndicatorColor = colorResource(id = R.color.main_blue),
                backgroundIndicatorColor = colorResource(id = R.color.tab_backgroud_blue),
                maxIndicatorValue = (ESTIMATE_TIME * 1000).toFloat()
            )
            
            MeasureView(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
//                func = { func() }
            )

            Canvas(modifier = Modifier.fillMaxSize()){
                drawContext.canvas.nativeCanvas.drawPath(
                    createCornersPath(
                        animateBox.left,
                        animateBox.top,
                        animateBox.right,
                        animateBox.bottom,
                        50f,
                        50f
                    ),
                    Paint().apply {
                        style = Paint.Style.STROKE
                        color = Color.White.toArgb()
                        strokeWidth = 15f
                        strokeCap = Paint.Cap.ROUND
                    }
                )
            } // draw face box - end
        }
    } // box - end
}

@ExperimentalPermissionsApi
@Composable
private fun RequestPermissions(
    context: Context,
    permissions: List<String>,
    rationalMsg: String = stringResource(R.string.rational_msg),
    func: @Composable () -> Unit,
    viewModel: ViewModel
){
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    HandleRequest(
        multiplePermissionState = permissionState,

        deniedContent = { shouldShowRationale ->
            PermissionDeniedContent(
                context = LocalContext.current,
                permissionState = permissionState,
                rationaleMessage = rationalMsg,
                shouldShowRationale = shouldShowRationale,
                onRequestPermission = { permissionState.launchMultiplePermissionRequest() },
                deniedPermission = {
//                    killApp(context as Activity)
                    (context as Activity).finish()
               },
            )
        },

        content = { func() },
        viewModel = viewModel
    )
}

@Composable
private fun getAnimationBox(box: RectF, animateSpec: TweenSpec<Float>, animationTrigger: Boolean): RectF {

    val animatedLeft by animateFloatAsState(
        targetValue = if(animationTrigger) box.left else 0f,
        animationSpec = animateSpec
    )
    val animatedRight by animateFloatAsState(
        targetValue = if(animationTrigger) box.right else 0f,
        animationSpec = animateSpec
    )
    val animatedTop by animateFloatAsState(
        targetValue = if(animationTrigger) box.top else 0f,
        animationSpec = animateSpec
    )
    val animatedBottom by animateFloatAsState(
        targetValue = if(animationTrigger) box.bottom else 0f,
        animationSpec = animateSpec
    )

    return RectF(animatedLeft, animatedTop, animatedRight, animatedBottom)
}