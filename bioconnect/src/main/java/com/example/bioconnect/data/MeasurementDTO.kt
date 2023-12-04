package com.example.bioconnect.data

import androidx.annotation.Keep
import java.io.Serializable
import java.util.Vector

@Keep
internal data class MeasurementDTO(
    val rgbValues: MutableList<Vector<Float>>
): Serializable