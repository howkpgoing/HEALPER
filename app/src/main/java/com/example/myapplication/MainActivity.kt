package com.example.myapplication

import PKCEUtil
import PKCEUtil.getCodeVerifier
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myapplication.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate


class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityMainBinding
    private val fitBitTool = FitBitAPI()
    private var accessToken : String = ""
    private val allBarEntries = ArrayList<BarEntry>()
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        //FitBit req for access token
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val barChart: BarChart = findViewById(R.id.chart1)
        barChart.setNoDataText("讀取中...")
        val barChart1: BarChart = findViewById(R.id.chart2)
        barChart1.setNoDataText("讀取中...")
        fun String.utf8(): String = URLEncoder.encode(this, "UTF-8")
        val authParams = mapOf(
            "client_id" to Config.CLIENT_ID,
            "redirect_uri" to Config.REDIRECT_URI,
            "response_type" to "code",
            "code_challenge" to PKCEUtil.getCodeChallenge(), // Set the code challenge
            "code_challenge_method" to "S256",
            "scope" to Config.SCOPE
        ).map {(k,v) -> "${(k.utf8())}=${v.utf8()}"}.joinToString("&")

//        if(accessToken == "none")
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse("${Config.AUTHORIZE_ENDPOINT}?$authParams"))

        // req end
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerlayout)
        navigationView = findViewById(R.id.navigationView)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigration_open,
            R.string.navigration_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.home -> {
                    true
                }
                R.id.keeper -> {
                    val intent = Intent(this, Keeper::class.java)
                    intent.putExtra("accessToken", accessToken)
                    Log.e("666", accessToken)
                    startActivity(intent)
                    true
                }

                R.id.logout -> {
                    Firebase.auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    true
                }

               R.id.hsd -> {
                    val intent = Intent(this, HistorySportData::class.java)
                   intent.putExtra("accessToken", accessToken)
//                    Log.e("777777777", accessToken)
                    startActivity(intent)
                    true
               }

                R.id.hbd -> {
//                    val intent = Intent(this, HistoryBodyDataDay::class.java)
                    val intent = Intent(this, HistoryBodyData::class.java)
                    intent.putExtra("accessToken", accessToken)
                    startActivity(intent)
                    true
                }
                else -> false
            }

        }

        val user = Firebase.auth.currentUser
        if (user != null) {
            // User is signed in
        } else {
            // No user is signed in
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }
    fun keeper (view: View) {
        val intent = Intent(this, Keeper::class.java)
        intent.putExtra("accessToken", accessToken)
        startActivity(intent)
    }
    // avoid multiple req for token
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getToken(code: String) {
        val client = OkHttpClient()

        val formBody: RequestBody = FormBody.Builder()
            .add("client_id", Config.CLIENT_ID)
            .add("redirect_uri", Config.REDIRECT_URI)
            .add("grant_type", Config.GRANT_TYPE)
            .add("code_verifier", getCodeVerifier()) // Send the code verifier
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url(Config.TOKEN_ENDPOINT)
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        Thread {
            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val gson = Gson()
                val tokenResponse = gson.fromJson(response.body?.string(), TokenResponse::class.java)
                if (tokenResponse.accessToken.isNotEmpty()) {
                    accessToken = tokenResponse.accessToken
                    Log.e("TOKEN", "Got token")
                    // Existing code
                    runOnUiThread {
                        fitBitTool.setAccessToken(accessToken)
                        initializeAfterTokenReceived()
                    }
                } else {
                    Log.e("TOKEN_ERROR", "Failed to retrieve access token")
                }
            } else {
                Log.e("TOKEN_ERROR", "Token request failed with code: " + response.code)
            }
        }.start()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeAfterTokenReceived() {
        fetchDataForDate(LocalDate.now())
        fetchDataForDateSecondChart(LocalDate.now())
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            val callbackURI = Uri.parse(intent.data.toString())
            handleCallback(callbackURI)
        }
    }

    // func require by getToken
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleCallback(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code?.isEmpty() == false) {
            getToken(code)
        }
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
        fitBitTool.getUserSleep(object : FitBitAPI.OnCallback {
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
        fitBitTool.getUserHeart(object : FitBitAPI.OnCallback {
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
        fitBitTool.getUserVO2(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val vo2Array = obj.optJSONArray("cardioScore") // 使用optJSONArray而不是getJSONArray
                var vo2Max = 0f
                if (vo2Array != null && vo2Array.length() > 0) {
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
        fitBitTool.getUserSpo2(object : FitBitAPI.OnCallback {
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
        fitBitTool.getUserBreath(object : FitBitAPI.OnCallback {
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
        val barChart = findViewById<BarChart>(R.id.chart1)
        val barDataSet = BarDataSet(allBarEntries, "Label")
        setBarChart(barChart, barDataSet)
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDataForDateSecondChart(date: LocalDate) {
        allBarEntries.clear()

        fitBitTool.getSummaryData(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                try {
                    val obj = JSONObject(respond.toString())

                    // Extracting data from the summary
                    val summary = obj.optJSONObject("summary") ?: return
                    val caloriesOut = summary.optDouble("caloriesOut", 0.0).toFloat()
                    val caloriesBMR = summary.optDouble("caloriesBMR", 0.0).toFloat()
                    val sedentaryMinutes = summary.optInt("sedentaryMinutes", 0)
                    val steps = summary.optInt("steps", 0)

                    // Extracting distances
                    val distancesArray = summary.optJSONArray("distances")
                    val totalDistance = distancesArray?.optJSONObject(0)?.optDouble("distance", 0.0)?.toFloat() ?: 0f

                    // Add these values to your BarEntries
                    allBarEntries.add(BarEntry(0f, caloriesOut))
                    allBarEntries.add(BarEntry(1f, caloriesBMR))
                    allBarEntries.add(BarEntry(2f, totalDistance*1000))
                    allBarEntries.add(BarEntry(3f, sedentaryMinutes.toFloat()))
                    allBarEntries.add(BarEntry(4f, steps.toFloat()))

                    updateBarChartSecondChart()
                } catch (e: Exception) {
                    Log.e("JSON_PARSING", "Error parsing JSON", e)
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getSummaryData Fail")
            }
        }, date)
    }

    private fun updateBarChartSecondChart() {
        val barChart = findViewById<BarChart>(R.id.chart2) // Change this ID to the ID of the second BarChart in MainActivity
        val barDataSet = BarDataSet(allBarEntries, "Label")
        setSecBarChart(barChart, barDataSet)
    }

    private fun setSecBarChart(barChart: BarChart, barDataSet: BarDataSet) {
        barDataSet.colors = ColorTemplate.JOYFUL_COLORS.toList()
        val xAxis = barChart.xAxis
        val yAxisLeft = barChart.axisLeft
        val yAxisRight = barChart.axisRight
        val formatter = SecCustomValueFormatter()

        // Set custom labels for the X axis
        xAxis.valueFormatter = formatter
        xAxis.setDrawLabels(true)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textSize = 10f

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
    class SecCustomValueFormatter : ValueFormatter() {
        private val labels = arrayOf("總卡路里(kcal)", "基代(kcal)", "距離(m)", "久坐(min)","步數")

        override fun getFormattedValue(value: Float): String {
            return if (value.toInt() in labels.indices) labels[value.toInt()] else "未知"
        }
    }
}



