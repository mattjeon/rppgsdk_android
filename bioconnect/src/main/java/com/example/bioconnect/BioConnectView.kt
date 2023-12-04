package com.example.bioconnect

import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bioconnect.camera.CameraView
import com.example.bioconnect.camera.core.FaceDetector
import com.example.bioconnect.data.HealthData
import com.example.bioconnect.retrofit.RetrofitManager
import com.example.bioconnect.ui.theme.BioconnectTheme
import com.example.bioconnect.utils.composable.CurrentDataView
import com.example.bioconnect.utils.composable.LoadingView
import com.example.bioconnect.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MeasureView(
    activity: ComponentActivity,
    baseUrl: String,
    showResultTable: Boolean = true,
//    resultOk: MutableState<Boolean>,
//    result: MutableState<HealthData?>
    result: MutableStateFlow<Pair<Boolean, HealthData?>>
){
    // 통신 모듈 초기화
    RetrofitManager.instance.retrofitInterface = RetrofitManager().initRetrofit(baseUrl)

    // 얼굴 감지기 초기화
    FaceDetector.instance.initFaceDetector(activity)

    // 데이터 및 로직 처리를 위한 ViewModel 초기화
    val viewModel by activity.viewModels<ViewModel>()

    // 뒤로가기 두번시 종료를 위한 변수
    var backPressedTime by remember{ mutableStateOf(0L) }

    // 측정 결과 화면 trigger
    val openBottomSheet = remember{ mutableStateOf(false) }

    // 측정 결과를 받는 리스너
    val _result = viewModel.data.collectAsState().value

    BioconnectTheme {
        // 측정중이 아닐때 뒤로가기 두번 입력시 안내메세지 출력 및 종료
        BackHandler(enabled = !viewModel.isEstimating.value) {
            if (System.currentTimeMillis() > backPressedTime + 2000){
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(
                    activity,
                    "\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }else {
                activity.finish() // 해당 액티비티만 종료하고 싶다면,
//                        killApp(this) // 앱 자체를 종료하고 싶다면,
            }
        }

        // 측정중일때 뒤로가기가 한번이라도 입력시 측정 종료 및 내부 변수 초기화
        BackHandler(enabled = viewModel.isEstimating.value) {
            viewModel.reset()
        }

        // 측정중 얼굴 미감지시 안내메시지 출력 후, 초기화
        LaunchedEffect(key1 = viewModel.sharedIsErr.collectAsState().value){
            if(viewModel.sharedIsErr.value && viewModel.isEstimating.value) {
                val text = "측정 중에 얼굴이 감지 되지 않아 취소되었습니다."

                Toast.makeText(activity, text, Toast.LENGTH_LONG).show()

                viewModel.reset()
            }
        }

        // 측정 결과 리스너
        LaunchedEffect(key1 = _result){
//            Log.e("Test>>", "result change: ${_result}")
            if(_result.second != null ) {
                openBottomSheet.value = showResultTable
//                result = viewModel.data
            }

            result.emit(_result)
//            resultOk.value = _result.first
//            result.value = _result.second
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ){
            // Camera 화면 및 UI View
            CameraView(
                viewModel = viewModel,
//                func = {  }
            )

            // 측정 결과 출력 View
            CurrentDataView(
                openBottomSheet,
                _result
            ) { viewModel.reset() }

            // 통신 로딩 처리 애니매이션
            if(RetrofitManager.instance.isLoading.value){
                LoadingView(modifier = Modifier.align(Alignment.Center))
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }else{
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }
}