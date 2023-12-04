package com.example.bioconnect.retrofit

import com.example.bioconnect.data.MeasurementDTO
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface RetrofitInterface {
    // measurement
    @Headers("Content-Type:application/json")
    @POST("/measurements")
    fun measurement(
        @Body body: MeasurementDTO
    ):Call<JsonElement>
}