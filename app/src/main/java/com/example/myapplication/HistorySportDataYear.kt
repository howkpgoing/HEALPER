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
import org.json.JSONObject
import java.time.LocalDate

class HistorySportDataYear : Fragment() {
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
        val view = inflater.inflate(R.layout.history_sport_data_year, container, false)
        val currentDate = LocalDate.now()
        val barChart: BarChart = view.findViewById(R.id.chart2)
        barChart.setNoDataText("讀取中...")
        fetchDataForDate(currentDate)
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDataForDate(date: LocalDate) {
        allBarEntries.clear()

        FitBitAPITool.getSummaryData(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                try {
                    val obj = JSONObject(respond.toString())

                    // Extracting data from the summary
                    val summary = obj.optJSONObject("summary") ?: return
                    val caloriesOut = summary.optDouble("caloriesOut", 0.0).toFloat()
                    val caloriesBMR = summary.optDouble("caloriesBMR", 0.0).toFloat() // 使用 caloriesBMR 替換 activeScore
                    val sedentaryMinutes = summary.optInt("sedentaryMinutes", 0)
                    val steps = summary.optInt("steps", 0)

                    // Extracting distances
                    val distancesArray = summary.optJSONArray("distances")
                    val totalDistance = distancesArray?.optJSONObject(0)?.optDouble("distance", 0.0)?.toFloat() ?: 0f

                    // Add these values to your BarEntries
                    allBarEntries.add(BarEntry(0f, caloriesOut))
                    allBarEntries.add(BarEntry(1f, caloriesBMR)) // 使用 caloriesBMR 替換 activeScore
                    allBarEntries.add(BarEntry(2f, totalDistance*1000))
                    allBarEntries.add(BarEntry(3f, sedentaryMinutes.toFloat()))
                    allBarEntries.add(BarEntry(4f, steps.toFloat()))

                    updateBarChart()
                } catch (e: Exception) {
                    Log.e("JSON_PARSING", "Error parsing JSON", e)
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getSummaryData Fail")
            }
        }, date)
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
        xAxis.textSize = 12f

        yAxisLeft.setDrawLabels(false)
        yAxisRight.setDrawLabels(false)

        // Display values on top of bars rounded to two decimal places
        barDataSet.setDrawValues(true)
        barDataSet.valueTextSize = 12f
        barDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.0f", value)
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
        private val labels = arrayOf("總卡路里(kcal)", "基代(kcal)", "距離(m)", "久坐(min)","步數")

        override fun getFormattedValue(value: Float): String {
            return if (value.toInt() in labels.indices) labels[value.toInt()] else "未知"
        }
    }

}
