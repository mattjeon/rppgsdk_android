package com.example.smuprlab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.smuprlab.ui.theme.SMUPrlabTheme

import com.example.bioconnect.MeasureView
import com.example.bioconnect.data.HealthData

// option
import com.example.bioconnect.utils.ESTIMATE_TIME
import com.example.bioconnect.utils.GET_LOG
import com.example.bioconnect.utils.stressToLevel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SMUPrlabTheme {
                // 측정 결과 변수
//                val resultOk = remember{ mutableStateOf( false ) }
//                val result = remember{ mutableStateOf<HealthData?>( null ) }
                val result = remember{
                    MutableStateFlow(
                        Pair<Boolean, HealthData?>(false, null)
                    )
                }
                // setting estimate time : Int (default : 15)
                ESTIMATE_TIME = 15

                // setting communication log flag : Boolean
                // true -> can see communication log in logcat (tag = Bioconnect)
                // false -> no log (default)
                GET_LOG = true

                // 측정 결과 리스너
//                LaunchedEffect(key1 = resultOk){
                LaunchedEffect(key1 = result.collectAsState().value){
                    // resultOk: Boolean
                    // false -> not estimate state & fail estimate (server err)
                    // true -> success estimate

                    // result: HealthData?
                    // null -> init state
                    // HealthData(bpm=0, rr=0, stress=0, spo2=0) -> err
                    // else -> estimate result


                    val resultOk = result.value.first
                    Log.e("Test>>", "resultOk: ${resultOk}")

                    if (resultOk){
                        Log.e("Test>>", "result: ${result.value.second}")
                        // stress index to stress level
                        Log.e("Test>>", stressToLevel(result.value.second!!.stress))
                    }
                }

                MeasureView(
                    activity = this@MainActivity,
                    baseUrl = BuildConfig.BASE_URL,
                    showResultTable = true,
//                    resultOk = resultOk,
                    result = result
                )
            }
        }
    }
}