package com.snarayanan.covidtracker

import com.google.gson.annotations.SerializedName
import java.util.*

data class CovidData (
    @SerializedName("dateChecked") val dateChecked: Date,
    @SerializedName("postiveIncrease") val positiveIncrease: Int,
    @SerializedName("negativeIncrease") val negativeIncrease: Int,
    @SerializedName("deathIncrease") val deathIncrease: Int,
    @SerializedName("state") val state: String
        )