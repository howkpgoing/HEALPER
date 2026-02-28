package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

class HistoryBodyDataWeek : Fragment() {
    private var accessToken: String? = null
    private val FitBitAPITool = FitBitAPI()
    private val allBarEntries = ArrayList<BarEntry>()

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        accessToken = arguments?.getString("accessToken")
        accessToken?.let { FitBitAPITool.setAccessToken(it) }
        val view = inflater.inflate(R.layout.history_body_data_week, container, false)
        val currentDate = LocalDate.now()
        val barChart: BarChart = view.findViewById(R.id.chart2)
        barChart.setNoDataText("讀取中...")
        fetchDataForDate(currentDate)

        val sport1 = view.findViewById<Button>(R.id.spb1)
        sport1.text = currentDate.toString()
        sport1.setOnClickListener {
            fetchDataForDate(currentDate)
        }

        val sport2 = view.findViewById<Button>(R.id.spb2)
        val previousDate1 = currentDate.minusDays(7)
        sport2.text = previousDate1.toString()
        sport2.setOnClickListener {
            fetchDataForDate(previousDate1)
        }

        val sport3 = view.findViewById<Button>(R.id.spb3)
        val previousDate2 = currentDate.minusDays(14)
        sport3.text = previousDate2.toString()
        sport3.setOnClickListener {
            fetchDataForDate(previousDate2)
        }

        val sport4 = view.findViewById<Button>(R.id.spb4)
        val previousDate3 = currentDate.minusDays(21)
        sport4.text = previousDate3.toString()
        sport4.setOnClickListener {
            fetchDataForDate(previousDate3)
        }

        val sport5 = view.findViewById<Button>(R.id.spb5)
        val previousDate4 = currentDate.minusDays(28)
        sport5.text = previousDate4.toString()
        sport5.setOnClickListener {
            fetchDataForDate(previousDate4)
        }
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDataForDate(date: LocalDate) {
        allBarEntries.clear()
        var completedApiCalls = 0
        val totalApiCalls = 5

        fun checkAllApiCallsCompleted() {
            completedApiCalls++
            if (completedApiCalls == totalApiCalls) {
                updateBarChart()
            }
        }

        showsleep(date) { checkAllApiCallsCompleted() }
        showheart(date) { checkAllApiCallsCompleted() }
        showVO2(date) { checkAllApiCallsCompleted() }
        showSpo2(date) { checkAllApiCallsCompleted() }
        showBreathRate(date, date) { checkAllApiCallsCompleted() }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showsleep(date: LocalDate, onComplete: () -> Unit) {
        FitBitAPITool.getUserSleep(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val sleepArray = obj.optJSONArray("sleep")
                val sleepDuration = if (sleepArray != null && sleepArray.length() > 0) {
                    sleepArray.getJSONObject(0).getLong("duration").toFloat() / 3600000
                } else {
                    0f
                }
                allBarEntries.add(BarEntry(0f, sleepDuration))
                onComplete()
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserWeight Fail")
                onComplete()
            }
        }, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showheart(date: LocalDate, onComplete: () -> Unit) {
        FitBitAPITool.getUserHeart(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                try {
                    val obj = JSONObject(respond ?: "")
                    val heartArray = obj.optJSONArray("activities-heart")
                    var averageHeartRate = 0f

                    if (heartArray != null && heartArray.length() > 0) {
                        val firstHeartRecord = heartArray.optJSONObject(0)
                        val heartRateZones = firstHeartRecord?.optJSONObject("value")?.optJSONArray("heartRateZones")

                        if (heartRateZones != null) {
                            var totalMinutes = 0
                            var weightedHeartRateSum = 0f

                            for (i in 0 until heartRateZones.length()) {
                                val zone = heartRateZones.optJSONObject(i)
                                val min = zone?.optInt("min", 0) ?: 0
                                val max = zone?.optInt("max", 0) ?: 0
                                val minutes = zone?.optInt("minutes", 0) ?: 0

                                totalMinutes += minutes
                                weightedHeartRateSum += ((min + max) / 2f) * minutes
                            }

                            averageHeartRate = if (totalMinutes > 0) weightedHeartRateSum / totalMinutes else 0f
                        }
                    }
                    allBarEntries.add(BarEntry(1f, averageHeartRate))
                } catch (e: JSONException) {
                    Log.e("JSON_PARSING", "Error parsing JSON", e)
                }
                onComplete()
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserWeight Fail")
                onComplete()
            }
        }, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showVO2(date: LocalDate, onComplete: () -> Unit) {
        FitBitAPITool.getUserVO2(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val vo2Array = obj.getJSONArray("cardioScore")
                var vo2Max = 0f
                if (vo2Array.length() > 0) {
                    val vo2MaxString = vo2Array.getJSONObject(0).getJSONObject("value").getString("vo2Max")

                    vo2Max = if (vo2MaxString.contains("-")) {
                        val parts = vo2MaxString.split("-").map { it.trim().toFloat() }
                        (parts[0] + parts[1]) / 2
                    } else {
                        vo2MaxString.toFloat()
                    }
                }
                allBarEntries.add(BarEntry(2f, vo2Max))
                onComplete()
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserVO2 Fail")
                onComplete()
            }
        }, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSpo2(date: LocalDate, onComplete: () -> Unit) {
        FitBitAPITool.getUserSpo2(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                var dataFound = false  // To track if data for the date was found

                if (obj.has("dateTime") && obj.getString("dateTime") == date.toString()) {
                    val valueObj = obj.getJSONObject("value")
                    if (valueObj.has("avg")) {
                        val spo2Avg = valueObj.getDouble("avg").toFloat()
                        allBarEntries.add(BarEntry(4f, spo2Avg))  // Assuming index 4 for spo2 avg
                        dataFound = true
                    }
                }

                // If no data was found for the date, add an entry with value 0
                if (!dataFound) {
                    allBarEntries.add(BarEntry(4f, 0f))  // Index 4 for spo2 avg with value 0
                }

                onComplete()
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserSpo2 Fail")
                onComplete()
            }
        }, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBreathRate(date: LocalDate, date2: LocalDate, onComplete: () -> Unit) {
        FitBitAPITool.getUserBreath(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                var dataFound = false  // To track if data for the date was found

                if (obj.has("br")) {
                    val brArray = obj.getJSONArray("br")
                    for (i in 0 until brArray.length()) {
                        val record = brArray.getJSONObject(i)
                        val dateTimeFromRecord = record.getString("dateTime")
                        if (dateTimeFromRecord == date.toString()) {
                            val breathRate = record.getJSONObject("value").getString("breathingRate")
                            val breath = breathRate.toFloat()
                            allBarEntries.add(BarEntry(3f, breath))  // Index 3 for breath rate
                            dataFound = true
                            break
                        }
                    }
                } else {
                    Log.e("API_RESPONSE", "br array not found in the response")
                }

                // If no data was found for the date, add an entry with value 0
                if (!dataFound) {
                    allBarEntries.add(BarEntry(3f, 0f))  // Index 3 for breath rate with value 0
                }

                onComplete()
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserBreathRate Fail")
                onComplete()
            }
        }, date, date2)
    }

    private fun updateBarChart() {
        val barChart = view?.findViewById<BarChart>(R.id.chart2)
        val barDataSet = BarDataSet(allBarEntries, "Label")
        setBarChart(barChart!!, barDataSet)
    }


    private fun setBarChart(barChart: BarChart, barDataSet: BarDataSet) {
        barDataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
        val xAxis = barChart.xAxis
        val yAxisLeft = barChart.axisLeft
        val yAxisRight = barChart.axisRight
        val formatter = CustomValueFormatter()

        // Set custom labels for the X axis
        xAxis.valueFormatter = formatter
        xAxis.setDrawLabels(true)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f

        yAxisLeft.setDrawLabels(false)
        yAxisRight.setDrawLabels(false)

        // Display values on top of bars rounded to two decimal places
        barDataSet.setDrawValues(true)
        barDataSet.valueTextSize = 12f
        barDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.2f", value)
            }
        }

        val barData = BarData(barDataSet)
        barChart.data = barData

        // Disable the legend
        barChart.legend.isEnabled = false

        // Remove description label
        barChart.description.isEnabled = false

        barChart.invalidate()
    }

    class CustomValueFormatter : ValueFormatter() {
        private val labels = arrayOf("睡眠時數(Hr)", "平均心率(min)", "VO2 Max", "呼吸率(min)","血氧濃度")

        override fun getFormattedValue(value: Float): String {
            return if (value.toInt() in labels.indices) labels[value.toInt()] else "未知"
        }
    }

}
