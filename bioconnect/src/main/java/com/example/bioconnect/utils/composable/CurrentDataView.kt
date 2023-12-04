package com.example.bioconnect.utils.composable

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bioconnect.R
import com.example.bioconnect.data.HealthData
import com.example.bioconnect.utils.stressToLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrentDataView(
    openBottomSheet: MutableState<Boolean>,
    data: Pair<Boolean, HealthData?>,
    func: () -> Unit
){
    val skipPartiallyExpanded by remember { mutableStateOf(true) }

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    val coroutineScope = rememberCoroutineScope()

//    var multiplier by remember { mutableStateOf(1f) }

    fun Dismissfunc(){
        openBottomSheet.value = false
        func()
    }

    if(openBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                Dismissfunc()
            },
            sheetState = bottomSheetState,
            shape = customModalRoundCornerShape(),
            containerColor = colorResource(id = R.color.background_light_gray),
            dragHandle = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
//                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "측정 결과",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    overflow = TextOverflow.Visible,
//                    style = LocalTextStyle.current.copy(
//                        fontSize = LocalTextStyle.current.fontSize * multiplier
//                    ),
//                    onTextLayout = {
//                        if (it.hasVisualOverflow) {
//                            multiplier *= 0.99f
//                        }
//                    }
                )

                Spacer(modifier = Modifier.height(30.dp))

                if(data.first)
                    FaceHealthDataResultView(
                        data.second!!
                    )
                else
                    Text(
                        text = "일시적인 오류입니다.\n다시 측정해주세요.",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        overflow = TextOverflow.Visible,
//                        style = LocalTextStyle.current.copy(
//                            fontSize = LocalTextStyle.current.fontSize * multiplier
//                        ),
//                        onTextLayout = {
//                            if (it.hasVisualOverflow) {
//                                multiplier *= 0.99f
//                            }
//                        }
                    )

                Spacer(modifier = Modifier.height(30.dp))

                Row(
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Button(
                        onClick = {
                            coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                                if (!bottomSheetState.isVisible)
                                    Dismissfunc()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor= colorResource(id = R.color.disalbed_gray),
                            disabledContainerColor = colorResource(id = R.color.disalbed_gray)
                        ),

                        shape = customFieldRoundCornerShape(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "닫기",
                            color = Color.Gray,
                            overflow = TextOverflow.Visible,
//                            style = LocalTextStyle.current.copy(
//                                fontSize = LocalTextStyle.current.fontSize * multiplier
//                            ),
//                            onTextLayout = {
//                                if (it.hasVisualOverflow) {
//                                    multiplier *= 0.99f
//                                }
//                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun FaceHealthDataResultView(
    data: HealthData
){
    Column(modifier = Modifier.height(IntrinsicSize.Min)) {
        TableRow(
            backgroundColor = Color.LightGray,
            textColor = Color.Black,
            firstText = "지표",
            secondText = "값",
            isTitle = true
        )

        TableRow(
            backgroundColor = colorResource(id = R.color.background_light_gray),
            textColor = Color.Black,
            firstText = "심박수",
            secondText = data.bpm.toString(),
        )


        TableRow(
            backgroundColor = colorResource(id = R.color.background_light_gray),
            textColor = Color.Black,
            firstText = "호흡 수",
           secondText = data.rr.toString()
        )

        TableRow(
            backgroundColor = colorResource(id = R.color.background_light_gray),
            textColor = Color.Black,
            firstText = "혈압",
            secondText = data.bp.toString()
        )

        TableRow(
            backgroundColor = colorResource(id = R.color.background_light_gray),
            textColor = Color.Black,
            firstText = "산소포화도",
            secondText = data.spo2.toString()
        )

        TableRow(
            backgroundColor = colorResource(id = R.color.background_light_gray),
            textColor = Color.Black,
            firstText = "스트레스",
            secondText = stressToLevel(data.stress),
        )
    }
}

@Composable
fun TableRow(
    backgroundColor: Color,
    textColor: Color,
    firstText: String,
    secondText: String,
    isTitle: Boolean = false
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(customModalRoundCornerShape())
            .background(backgroundColor),
        horizontalArrangement = Arrangement.SpaceEvenly
    ){

        Text(
            text = firstText,
            textAlign = TextAlign.Center,
            fontWeight = if(isTitle) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(0.5f)
                .height(20.dp),
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = secondText,
            textAlign = TextAlign.Center,
            fontWeight = if(isTitle) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth()
                .height(20.dp),
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.LightGray)
    )
}