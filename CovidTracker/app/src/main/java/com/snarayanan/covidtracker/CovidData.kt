package com.snarayanan.covidtracker

import com.google.gson.annotations.SerializedName

data class CovidData (
    val dateChecked :String,
    val positiveIncrease :Int,
    val negativeIncrease :Int,
    val deathIncrease :Int,
    val state :String
)