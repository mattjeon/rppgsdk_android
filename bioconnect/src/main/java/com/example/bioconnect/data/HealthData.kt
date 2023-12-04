package com.example.bioconnect.data

data class HealthData(
    var bpm: Int = 0,
    var rr: Int = 0,
    var stress: Int = 0,
    var spo2: Int = 0,
    var bp: String = ""
)