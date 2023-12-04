package com.example.bioconnect.camera.core

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bioconnect.R
import com.example.bioconnect.retrofit.RetrofitManager
import com.example.bioconnect.utils.composable.customButtonColor
import com.example.bioconnect.utils.composable.customFieldRoundCornerShape
import com.example.bioconnect.viewmodel.ViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.bioconnect.utils.ESTIMATE_TIME

@Composable
fun MeasureView(
    modifier: Modifier,
    viewModel : ViewModel,
//    func: () -> Unit = {}
){
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier){
        // bottom button for measurement
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 20.dp)
                ,
                colors = customButtonColor(),
                shape = customFieldRoundCornerShape(),
                enabled = (
                    !viewModel.isEstimating.value &&
                    viewModel.estimateTime.value == 0L &&
                    !viewModel.sharedIsErr.collectAsState().value
                ),
                onClick = {

//                    viewModel.lists.clear()

                    viewModel.reset()

                    viewModel.updateEstimateState(true)
                    viewModel.updateStartTime(System.currentTimeMillis())
                    // 측정 로직
                    coroutineScope.launch {
                        val job =coroutineScope.async {
                            // 1초마다 state 체크
                            repeat(ESTIMATE_TIME){
                                if(!viewModel.isEstimating.value || viewModel.sharedIsErr.value)
                                    this.cancel()

                                delay(1000L)

                                viewModel.updateEstimateTime(
                                    if(viewModel.isEstimating.value)
                                        viewModel.getEstimateTime(System.currentTimeMillis())
                                    else
                                        0L
                                )
                            }
                            Pair(0L, false)
                        }
                        val result = job.await()

                        viewModel.updateEstimateTime(result.first)
                        viewModel.updateEstimateState(result.second)

                        // rgb 배열 서버 전송
                        viewModel.updateHealthData(
                            RetrofitManager.instance.callMeasurement(
                                viewModel.lists
                            )
                        )

//                        Log.e("Test>>", "measure end: estimate state = ${viewModel.isEstimating.value}")
                    }
                }
            ){
                if(!viewModel.isEstimating.value) {
//                    var multiplier by remember { mutableStateOf(1f) }

                    Text(
                        text = "측정",
                        fontWeight = FontWeight.SemiBold,
//                        fontSize = 20.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                    )
                }
                else
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxHeight(),
                        color = Color.White,
                        trackColor = colorResource(id = R.color.disalbed_gray),
                        strokeCap = StrokeCap.Round,
                        strokeWidth = 5.dp
                    )
            }

            Spacer(modifier = Modifier.height(50.dp))
        } // bottom button - end
    }
}