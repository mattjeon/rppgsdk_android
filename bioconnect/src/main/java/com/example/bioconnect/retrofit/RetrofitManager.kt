package com.example.bioconnect.retrofit

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.bioconnect.data.HealthData
import com.example.bioconnect.data.MeasurementDTO
import com.example.bioconnect.utils.GET_LOG
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Vector
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "Bioconnect"

internal class RetrofitManager {
    companion object{
        val instance = RetrofitManager()
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        if(GET_LOG)
            Log.e(
                TAG,
                message
            )
    }.setLevel(HttpLoggingInterceptor.Level.BODY)

    lateinit var retrofitInterface: RetrofitInterface

    private val builder = OkHttpClient().newBuilder().addInterceptor(loggingInterceptor)
    private val client = builder
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()


    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun initRetrofit(baseUrl:String):RetrofitInterface{
        val gson = GsonBuilder()
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(RetrofitInterface::class.java)
    }

    // get measurement result
    suspend fun callMeasurement(frames: MutableList<Vector<Float>>): Pair<Boolean, HealthData?> =
        suspendCoroutine{ continuation ->
            _isLoading.value = true
            val result = HealthData()

            val call = retrofitInterface.measurement(
                MeasurementDTO(frames)
            )

            call.enqueue(object : Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) =
                    if(response.isSuccessful){
                        val jsonObj = JSONObject(response.body().toString())

                        jsonObj.apply {
                            result.bpm = this["bpm"].toString().toInt()
                            result.spo2 = this["SpO2"].toString().toInt()
                            result.rr = this["RR"].toString().toInt()
                            result.stress = this["stressIndex"].toString().toInt()
                            result.bp = this["BP"].toString()
                        }

                        resumeWithLoadingState(
                            continuation,
                            Pair(true, result)
                        )
                    }
                    else{
                        resumeWithLoadingState(
                            continuation,
                            Pair(false, result)
                        )
                    }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    resumeWithLoadingState(
                        continuation,
                        Pair(false, result)
                    )
                }
            })
    }

    private fun resumeWithLoadingState(
        continuation : Continuation<Pair<Boolean, HealthData>>,
        stateValue: Pair<Boolean, HealthData>
    ){
        _isLoading.value = false
        continuation.resume(stateValue)
    }
}