package com.snarayanan.covidtracker

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import com.snarayanan.covidtracker.R.color
import com.snarayanan.covidtracker.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES = "All (Nationwide)"
class MainActivity : AppCompatActivity() {


    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.app_description)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
        val covidService = retrofit.create(CovidService::class.java)
        //Fetch the national data
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }

                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })
        //Fetch the state data
        covidService.getStatesData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner with states names")

                updateSpinnerWithStateData(perStateDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        //Add state list as a data source
        binding.spinnerSelect.attachDataSource(stateAbbreviationList)
        binding.spinnerSelect.setOnSpinnerItemSelectedListener {
                parent, _, position, _-> val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
        updateDisplayWithData(selectedData)}
    }

    private fun setupEventListeners() {
        binding.tickerView.setCharacterLists(TickerUtils.provideNumberList())
        // Add a listener for user scrubbing on the chart
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener {
            itemData -> if (itemData is CovidData) {
                updateInfoForDate(itemData)
        }
        }
        // Respond to radio button selected events
        binding.radioGroupTimeSelection.setOnCheckedChangeListener{ _, checkedId ->
            adapter.daysAgo = when(checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }

            adapter.notifyDataSetChanged()
        }
        binding.radioGroupMetricSelection.setOnCheckedChangeListener{ _,checkedId ->
            when (checkedId) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update the color of the chart
        val colorRes = when (metric){
            Metric.NEGATIVE -> color.colorNegative
            Metric.POSITIVE -> color.colorPositive
            Metric.DEATH -> color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        binding.sparkView.lineColor = colorInt
        // Update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()
        
        // Reset number and date shown in the bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        // Create a spark adapter
        adapter = CovidSparkAdapter(dailyData)
        // Update radio buttons to select the positive vases and max time by default
        binding.sparkView.adapter = adapter
        binding.radioButtonPositive.isChecked = true
        binding.radioButtonMax.isChecked = true
        // Display metric for the most recent date
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric){
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}