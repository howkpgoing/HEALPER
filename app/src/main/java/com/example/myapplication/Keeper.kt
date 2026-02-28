package com.example.myapplication
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.util.*


class Keeper : AppCompatActivity(){
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var binding: ActivityMainBinding
    private var bmi: Float? = null
    private var height: Int? = null
    private var weight: Int? = null
    private var check: String = ""
    private var againcheck: String = ""
    private var accessToken = ""
    private val FitBitAPITool = FitBitAPI()
    private var totalApiRequests = 0
    private var completedApiRequests = 0

    companion object {
        const val YOUR_KEY = ""
        const val URL = "https://api.openai.com/v1/completions"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
//        if(MainActivity().getSharedPreferences().getString("TOKEN", null) != null)
        accessToken = intent.getStringExtra("accessToken").toString()
        FitBitAPITool.setAccessToken(accessToken)
        Log.e("access token test", FitBitAPITool.getAccessToken())

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setContentView(R.layout.keeper)
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
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("accessToken", accessToken)
                    startActivity(intent)
                    true
                }
                R.id.keeper -> {
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

        val tvAnswer = findViewById<TextView>(R.id.textView_Answer)
        val editText = findViewById<EditText>(R.id.edittext_Input)
        findViewById<Button>(R.id.button_Send).setOnClickListener { view ->
            val question =
                findViewById<EditText>(R.id.edittext_Input).text.toString()
            if (question.isEmpty()) return@setOnClickListener
            findViewById<TextView>(R.id.textView_Question).text = question

            tvAnswer.text = "小管家為你量身訂做答案中...請稍等..."
            //設置Header中的Content-Type
            val mediaType = "application/json".toMediaTypeOrNull()
            //寫入body
            editText.text.clear()
            var content = Gson().toJson(makeRequest(question))
            var requestBody = content.toRequestBody(mediaType)

            //發送請求
            Log.e("first gpt post", "first gpt post")
            HttpRequest().sendPOST(URL, requestBody, object : HttpRequest.OnCallback {
                override fun onOKCall(respond: String?) {
                    Log.d("TAG", "onOKCall: $respond")
                    val chatGPTRespond =
                        Gson().fromJson(respond, ChatGPTRespond::class.java)
                    runOnUiThread {

                        check = chatGPTRespond.choices?.get(0)?.text.toString()
                      //tvAnswer.text = check
                        into(check)

                    }}
                override fun onFailCall(error: String?) {
                    Log.e("TAG", "onFailCall: $error")
                    tvAnswer.text = error

                }
            })
        }
    }

    //寫入body
    private fun makeRequest(input: String): WeakHashMap<String, Any> {
        val weakHashMap: WeakHashMap<String, Any> = WeakHashMap()
        weakHashMap["model"] = "davinci:ft-personal:davinci-tune-2023-08-20-09-34-54"
        weakHashMap["prompt"] = input
        weakHashMap["temperature"] = 0.1
        weakHashMap["max_tokens"] = 100
        weakHashMap["top_p"] = 0.9
        weakHashMap["frequency_penalty"] = 1.0
        weakHashMap["presence_penalty"] = 1.0
        val stopSequences = listOf<String>(
            "end","END"
        )
        weakHashMap["stop"] = stopSequences
        val bestOfValue = 3
        weakHashMap["best_of"] = bestOfValue
        return weakHashMap
    }
    private fun againRequest(input: String): WeakHashMap<String, Any> {
        val weakHashMap: WeakHashMap<String, Any> = WeakHashMap()
        weakHashMap["model"] = "text-davinci-003"
        weakHashMap["prompt"] = input+"請用繁體中文回答"
        weakHashMap["temperature"] = 0.5
        weakHashMap["max_tokens"] = 1000
        weakHashMap["top_p"] = 0.8
        weakHashMap["frequency_penalty"] = 1.0
        weakHashMap["presence_penalty"] = 1.0
        //val bestOfValue = 2
        //weakHashMap["best_of"] = bestOfValue
        return weakHashMap
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun into(check: String) {
        var secPost = false  // 初始化為 false
        againcheck = check
        Log.e("againcheck", againcheck)

        // 先判斷天數
        val days = extractDays(check) ?: 0
        if (days > 0) {
            secPost = true
        }

        if (check != null) {
            if (check.contains("身高")) {
                totalApiRequests++
                getUserHeight()
                //secPost = true
            } else if (check.contains("體重")) {
                totalApiRequests++
                getUserWeight()
                //secPost = true
            } else if (check.contains("BMI")) {
                totalApiRequests++
                getUserBMI()
               // secPost = true
            } else if (check.contains("心率")) {
                totalApiRequests++
                if (days == 0) {
                    getHeartRateByDate(LocalDate.now())
                    Log.e("Blood", "cool")
                } else {
                    getHeartRateByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
                //secPost = true
            } else if (check.contains("心跳")) {
                totalApiRequests++
                if (days == 0) {
                    getHeartRateByDate(LocalDate.now())
                } else {
                    getHeartRateByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
                //secPost = true
            } else if (check.contains("vo2max")) {
                totalApiRequests++
                if (days == 0) {
                    getVO2ByDate(LocalDate.now())
                } else {
                    getVO2ByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
                //secPost = true
            } else if (check.contains("呼吸頻率")) {
                totalApiRequests++
                if (days == 0) {
                    showBreathRate(LocalDate.now(),LocalDate.now())
                } else {
                    showBreathRate(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
                //secPost = true
            } else if (check.contains("血氧")) {
                totalApiRequests++
                if (days == 0) {
                    val cool = getSPO2ByDate(LocalDate.now())
                    Log.e("Blood", "$cool")
                } else {
                    getSPO2ByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
                //secPost = true
            } else if (check.contains("睡眠時數")) {
                totalApiRequests++
                if (days == 0) {
                    getSleepByDate(LocalDate.now())
                } else {
                    getSleepByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
                //secPost = true
            }
            else if (check.contains("運動數據")) {
                totalApiRequests++
                if (days == 0) {
                    getstepByDate(LocalDate.now())
                    getcarByDate(LocalDate.now())
                    getsitByDate(LocalDate.now())
                } else {
                    getstepByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                    getcarByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                    getsitByPeriod(LocalDate.now().minusDays(days.toLong()), LocalDate.now())
                }
               // secPost = true
            }else {
                if (completedApiRequests == totalApiRequests) {
                    sendPost()
                }
            }
        }
        //if (secPost) sendPost()
    }

    private fun extractDays(text: String): Int? {
        val regex = Regex("""(\\d+)天""")
        val matchResult = regex.find(text)
        return matchResult?.groups?.get(1)?.value?.toInt()
    }

    private fun sendPost(){

        val tvAnswer = findViewById<TextView>(R.id.textView_Answer)
        val mediaType = "application/json".toMediaTypeOrNull()
        var content = Gson().toJson(againRequest(againcheck))
        var requestBody = content.toRequestBody(mediaType)

        HttpRequest().sendPOST(Keeper.URL, requestBody, object : HttpRequest.OnCallback {
            override fun onOKCall(respond: String?) {
                Log.d("TAG",  "onOKCall: $respond")
                val chatGPTRespond =
                    Gson().fromJson(respond, ChatGPTRespond::class.java)
                runOnUiThread {
                    tvAnswer.text  = chatGPTRespond.choices?.get(0)?.text.toString()
                }}
            override fun onFailCall(error: String?) {
                Log.e("TAG", "onFailCall: $error")
                tvAnswer.text = error

            }

        })
    }

    // function template
    //各種function從這裡開始


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUserBMI(){
        var reqValue : Float
        Log.e("3", "3")
        FitBitAPITool.getUserData(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
//                Log.e("testtest",respond.toString())
                val obj = JSONObject(respond.toString())
                val w = obj.getJSONObject("user").getString("weight").toFloat()
                val h = obj.getJSONObject("user").getString("height").toFloat() / 100
                Log.e("bmi", h.toString())
                Log.e("bmi", w.toString())
                reqValue = (w / (h * h))
                runOnUiThread {
                    // do here
                    bmi = reqValue
                    againcheck = againcheck.replace("bmi", "我的BMI是$bmi").trim()
                    Log.e("bmi post start", againcheck)
                    //sendPost()
                }
            }
            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserBMI Fail")
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUserHeight(){
        var reqValue : String = ""
        FitBitAPITool.getUserData(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                reqValue = obj.getJSONObject("user").getString("height")
                runOnUiThread {
                    // do here
                    height = reqValue.toFloat().toInt()
                    againcheck = againcheck.replace("身高", "我的身高是$height 公分").trim()
                    Log.e("height post start", againcheck)
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()
                    }
                    //sendPost()
                }
            }
            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserHeight Fail")
            }
        })

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUserWeight(){
        var reqValue : String = ""
        Log.e("2", "2")
        FitBitAPITool.getUserData(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
//                Log.e("testtest",respond.toString())
                val obj = JSONObject(respond.toString())
                reqValue = obj.getJSONObject("user").getString("weight")
                runOnUiThread {
                    // do here
                    weight = reqValue.toFloat().toInt()
                    againcheck = againcheck.replace("體重", "我的體重是$weight"+"公斤").trim()
                    Log.e("weight post start", againcheck)
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()
                    }
                    //sendPost()
                }
            }
            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserWeight Fail")
            }
        })
    }

    // 其他很很很重要

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getHeartRateByDate(date : LocalDate){

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
                    againcheck = againcheck.replace("心率", "我的心跳是$averageHeartRate"+"bpm").trim()
                    Log.e("gotit", "really")
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()
                    }
                } catch (e: JSONException) {
                    Log.e("JSON_PARSING", "Error parsing JSON", e)
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserWeight Fail")
            }
        }, date)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getHeartRateByPeriod(date: LocalDate, date2: LocalDate) {
        FitBitAPITool.getHeartRateByPeriod(object : FitBitAPI.OnCallback {
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
                    againcheck = againcheck.replace("心率", "我的心跳是${averageHeartRate.toInt()} bpm").trim()
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()}
                } catch (e: JSONException) {
                    Log.e("JSON_PARSING", "Error parsing JSON", e)
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserWeight Fail")
            }
        }, date, date2)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getVO2ByDate(date : LocalDate){
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
                againcheck = againcheck.replace("vo2max", "我的最大攝氧量是$vo2Max").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserVO2 Fail")
            }
        }, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getVO2ByPeriod(startDate : LocalDate, endDate : LocalDate) {
        FitBitAPITool.getVO2ByPeriod(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val vo2Array = obj.getJSONArray("cardioScore")
                var totalVo2Max = 0f
                var count = 0

                for (i in 0 until vo2Array.length()) {
                    val vo2MaxString =
                        vo2Array.getJSONObject(i).getJSONObject("value").getString("vo2Max")
                    val vo2Max = if (vo2MaxString.contains("-")) {
                        val parts = vo2MaxString.split("-").map { it.trim().toFloat() }
                        (parts[0] + parts[1]) / 2
                    } else {
                        vo2MaxString.toFloat()
                    }
                    totalVo2Max += vo2Max
                    count++
                }

                val averageVo2Max = if (count > 0) totalVo2Max / count else 0f
                againcheck =
                    againcheck.replace("vo2max", "我的平均最大攝氧量是$averageVo2Max").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserVO2 Fail")
            }
        }, startDate, endDate)
    }
        @RequiresApi(Build.VERSION_CODES.O)
        private fun showBreathRate(date: LocalDate, date2: LocalDate) {
            FitBitAPITool.getUserBreath(object : FitBitAPI.OnCallback {
                override fun onOKCall(respond: String?) {
                    val obj = JSONObject(respond.toString())

                    if (obj.has("br")) {
                        val brArray = obj.getJSONArray("br")
                        for (i in 0 until brArray.length()) {
                            val record = brArray.getJSONObject(i)
                            val dateTimeFromRecord = record.getString("dateTime")
                            if (dateTimeFromRecord == date.toString()) {
                                val breathRate = record.getJSONObject("value").getString("breathingRate")
                                val breath = breathRate.toFloat()
                                againcheck = againcheck.replace("呼吸頻率", "我的呼吸頻率是$breath").trim()
                                completedApiRequests++

                                if (completedApiRequests == totalApiRequests) {
                                    sendPost()}
                                break
                            }
                        }
                    } else {
                        Log.e("API_RESPONSE", "br array not found in the response")
                    }
                }

                override fun onFailCall(error: String?) {
                    Log.e("FAILCALL", "getUserBreathRate Fail")
                }
            }, date, date2)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSPO2ByPeriod(startDate: LocalDate, endDate: LocalDate) {
        FitBitAPITool.getSPO2ByPeriod(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val spo2Array = obj.optJSONArray("spo2Data") ?: JSONArray(respond)  // <-- Added this to directly parse the provided JSON array
                var totalSpo2 = 0f
                var count = 0

                if (spo2Array != null) {
                    for (i in 0 until spo2Array.length()) {
                        val spo2Obj = spo2Array.getJSONObject(i)
                        if (spo2Obj.has("value") && spo2Obj.getJSONObject("value").has("avg")) {
                            val spo2Avg = spo2Obj.getJSONObject("value").getDouble("avg").toFloat()
                            totalSpo2 += spo2Avg
                            count++
                        }
                    }
                }

                val averageSpo2 = if (count > 0) totalSpo2 / count else 0f
                againcheck = againcheck.replace("血氧", "我的平均血氧濃度在這段時間是$averageSpo2").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getSPO2ByPeriod Fail")
            }
        }, startDate, endDate)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSPO2ByDate(date : LocalDate){
        FitBitAPITool.getUserSpo2(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())

                if (obj.has("dateTime") && obj.getString("dateTime") == date.toString()) {
                    val valueObj = obj.getJSONObject("value")
                    if (valueObj.has("avg")) {
                        val spo2Avg = valueObj.getDouble("avg").toFloat()
                        againcheck = againcheck.replace("血氧", "我的血氧是$spo2Avg").trim()
                        completedApiRequests++

                        if (completedApiRequests == totalApiRequests) {
                            sendPost()}
                    }
                }

            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserSpo2 Fail")
            }
        }, date)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSleepByDate(date: LocalDate){
        FitBitAPITool.getUserSleep(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val sleepArray = obj.optJSONArray("sleep")
                val sleepDuration = if (sleepArray != null && sleepArray.length() > 0) {
                    sleepArray.getJSONObject(0).getLong("duration").toFloat() / 3600000
                } else {
                    0f
                }
                againcheck = againcheck.replace("睡眠時數", "我的睡眠時數是$sleepDuration"+"hr").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserWeight Fail")
            }
        }, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSleepByPeriod(startDate: LocalDate, endDate: LocalDate) {
        FitBitAPITool.getSleepByPeriod(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val sleepArray = obj.optJSONArray("sleep")
                var totalSleepDuration = 0f
                var count = 0

                if (sleepArray != null) {
                    for (i in 0 until sleepArray.length()) {
                        val sleepObj = sleepArray.getJSONObject(i)
                        if (sleepObj.has("duration")) {
                            val sleepDuration = sleepObj.getLong("duration").toFloat() / 3600000
                            totalSleepDuration += sleepDuration
                            count++
                        }
                    }
                }

                val averageSleepDuration = if (count > 0) totalSleepDuration / count else 0f
                againcheck = againcheck.replace("睡眠時數", "我在這段時間的平均睡眠時數是$averageSleepDuration"+"hr").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getSleepByPeriod Fail")
            }
        }, startDate, endDate)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getstepByDate(date: LocalDate) {
        FitBitAPITool.getstepByDate(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val stepsArray = obj.optJSONArray("activities-steps")
                if (stepsArray != null && stepsArray.length() > 0) {
                    val latestStepRecord = stepsArray.getJSONObject(0) // 取得最近一天的資料
                    val steps = latestStepRecord.getString("value")
                    againcheck = againcheck.replace("步數", "我在$date"+"的步數是$steps"+"步").trim()
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()}
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserSteps Fail")
            }
        }, date)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getstepByPeriod(startDate: LocalDate, endDate: LocalDate) {
        FitBitAPITool.getstepByPeriod(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val stepsArray = obj.optJSONArray("activities-steps")
                var totalSteps = 0
                var count = 0

                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        val stepRecord = stepsArray.getJSONObject(i)
                        val steps = stepRecord.getString("value").toInt()
                        totalSteps += steps
                        count++
                    }
                }

                val averageSteps = if (count > 0) totalSteps / count else 0
                againcheck = againcheck.replace("步數", "從$startDate"+"到$endDate"+"的平均步數是$averageSteps"+"步").trim()

                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}}

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserSteps Fail")
            }
        }, startDate, endDate)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getcarByDate(date: LocalDate) {
        FitBitAPITool.getcarByDate(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val caloriesArray = obj.optJSONArray("activities-calories")
                if (caloriesArray != null && caloriesArray.length() > 0) {
                    val latestCaloriesRecord = caloriesArray.getJSONObject(0)
                    val calories = latestCaloriesRecord.getString("value")
                    againcheck = againcheck.replace("卡路里", "我在$date"+"的卡路里消耗是$calories"+"大卡").trim()
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()}
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserCalories Fail")
            }
        }, date)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getcarByPeriod(startDate: LocalDate, endDate: LocalDate) {
        FitBitAPITool.getcarByPeriod(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val caloriesArray = obj.optJSONArray("activities-calories")
                var totalCalories = 0
                var count = 0

                if (caloriesArray != null) {
                    for (i in 0 until caloriesArray.length()) {
                        val caloriesRecord = caloriesArray.getJSONObject(i)
                        val calories = caloriesRecord.getString("value").toInt()
                        totalCalories += calories
                        count++
                    }
                }

                val averageCalories = if (count > 0) totalCalories / count else 0
                againcheck = againcheck.replace("卡路里", "從$startDate"+"到$endDate"+"的平均卡路里消耗是$averageCalories"+"大卡").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserCalories Fail")
            }
        }, startDate, endDate)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getsitByDate(date: LocalDate) {
        FitBitAPITool.getsitByDate(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val sedentaryArray = obj.optJSONArray("activities-sedentary")
                if (sedentaryArray != null && sedentaryArray.length() > 0) {
                    val latestSedentaryRecord = sedentaryArray.getJSONObject(0)
                    val sedentaryTime = latestSedentaryRecord.getString("value")
                    againcheck = againcheck.replace("久坐時間", "我在$date"+"的久坐時間是$sedentaryTime"+"分鐘").trim()
                    completedApiRequests++

                    if (completedApiRequests == totalApiRequests) {
                        sendPost()}
                }
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserSedentaryTime Fail")
            }
        }, date)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getsitByPeriod(startDate: LocalDate, endDate: LocalDate) {
        FitBitAPITool.getsitByPeriod(object : FitBitAPI.OnCallback {
            override fun onOKCall(respond: String?) {
                val obj = JSONObject(respond.toString())
                val sedentaryArray = obj.optJSONArray("activities-sedentary")
                var totalSedentaryTime = 0
                var count = 0

                if (sedentaryArray != null) {
                    for (i in 0 until sedentaryArray.length()) {
                        val sedentaryRecord = sedentaryArray.getJSONObject(i)
                        val sedentaryTime = sedentaryRecord.getString("value").toInt()
                        totalSedentaryTime += sedentaryTime
                        count++
                    }
                }

                val averageSedentaryTime = if (count > 0) totalSedentaryTime / count else 0
                againcheck = againcheck.replace("久坐時間", "從$startDate"+"到$endDate"+"的平均久坐時間是$averageSedentaryTime"+"分鐘").trim()
                completedApiRequests++

                if (completedApiRequests == totalApiRequests) {
                    sendPost()}
            }

            override fun onFailCall(error: String?) {
                Log.e("FAILCALL", "getUserSedentaryTime Fail")
            }
        }, startDate, endDate)
    }






}

