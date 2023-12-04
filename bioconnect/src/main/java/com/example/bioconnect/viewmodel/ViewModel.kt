package com.example.bioconnect.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bioconnect.data.HealthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Vector

class ViewModel: ViewModel(){
    private val _sharedPermissionState = mutableStateOf(false)
    internal val sharedPermissionState: State<Boolean> = _sharedPermissionState

    private val _sharedIsErr = MutableStateFlow(false)
    internal val sharedIsErr: StateFlow<Boolean> = _sharedIsErr

    private val _sharedFaceBoxState = MutableStateFlow(RectF(0f,0f,0f, 0f))
    internal val sharedFaceBoxState: StateFlow<RectF> = _sharedFaceBoxState

    private val _isEstimating = mutableStateOf(false)
    internal val isEstimating: State<Boolean> = _isEstimating

    private val _estimateTime = mutableStateOf(0L)
    internal val estimateTime: State<Long> = _estimateTime

    private val _startTime = mutableStateOf(0L)
    private val startTime = _startTime

    private val _lists = mutableListOf<Vector<Float>>()
    internal val lists: MutableList<Vector<Float>> = _lists

    private val _data = MutableStateFlow(
        Pair<Boolean, HealthData?>(
            false, null
        )
    )
    public val data: StateFlow<Pair<Boolean, HealthData?>> = _data

    internal fun reset(){
        viewModelScope.launch{
            _isEstimating.value = false
            _estimateTime.value = 0L
            _startTime.value = 0L
            _lists.clear()
            _sharedIsErr.emit(false)
            _data.emit(
                Pair<Boolean, HealthData?>(
                    false, null
                )
            )
        }
    }

    internal fun update(box: RectF){
        viewModelScope.launch {
            _sharedFaceBoxState.emit(
                box
            )
            if(box.left <= -100f) _sharedIsErr.emit(true)
            else  _sharedIsErr.emit(false)
        }
    }

    internal fun updateHealthData(result: Pair<Boolean, HealthData?>){
        viewModelScope.launch {
            _data.emit(
                result
            )
        }

    }

    internal fun updateEstimateState(state: Boolean){
        _isEstimating.value = state
    }
    internal fun updatePermissionState(state: Boolean){
        _sharedPermissionState.value = state
    }
    internal fun updateEstimateTime(time: Long){
        _estimateTime.value = time
    }

    internal fun updateStartTime(time: Long){
        _startTime.value = time
    }

    fun addFrameList(frames: Vector<Float>){
        _lists.add(frames)
    }
    fun getEstimateTime(time: Long) = time - startTime.value

}
