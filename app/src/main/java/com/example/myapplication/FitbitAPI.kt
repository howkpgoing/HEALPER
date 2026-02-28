package com.example.myapplication

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class FitBitAPI {
    @RequiresApi(Build.VERSION_CODES.O)
    private var accessToken: String = ""
    private val requestInterval: Long = 1000L  // 2 seconds delay between requests

    // Create a Channel to hold the requests
    private val requestChannel = Channel<() -> Unit>(Channel.UNLIMITED)

    init {
        runRequestQueue()
    }

    private fun runRequestQueue() {
        GlobalScope.launch {
            for (request in requestChannel) {
                request()
                delay(requestInterval)
            }
        }
    }




    //Keeper專區
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserData(callBack : OnCallback) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/profile.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getHeartRateByPeriod(callBack : OnCallback,startDate : LocalDate, endDate : LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/heart/date/$startDate/$endDate.json"

        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getHeartRateByDate(callBack : OnCallback,date : LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/heart/$date/today/1d.json"

        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getVO2ByPeriod(callBack : OnCallback,startDate : LocalDate, endDate : LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/cardioscore/date/$startDate/$endDate.json"

        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getVO2ByDate( callBack : OnCallback,date : LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/cardioscore/date/$date.json"

        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getBreathingRateByPeriod(callBack: OnCallback,startDate: LocalDate, endDate: LocalDate ): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/br/date/$startDate/$endDate.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getBreathingRateByDate( callBack: OnCallback,date: LocalDate): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/br/date/$date.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSPO2ByPeriod(callBack : OnCallback,startDate : LocalDate, endDate : LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/spo2/date/$startDate/$endDate.json"

        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getSPO2ByDate(callBack : OnCallback,date : LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/spo2/date/$date.json"

        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getSleepByPeriod( callBack: OnCallback,startDate: LocalDate, endDate: LocalDate): String {
        val reqUrl = " https://api.fitbit.com/1.2/user/-/sleep/date/$startDate/$endDate.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getSleepByDate(callBack: OnCallback,date: LocalDate ): String {
        val reqUrl = "https://api.fitbit.com/1.2/user/-/sleep/date/$date.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getstepByPeriod( callBack: OnCallback,startDate: LocalDate, endDate: LocalDate): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/steps/date/$startDate/$endDate.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getstepByDate(callBack: OnCallback,date: LocalDate ): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/steps/date/$date/1d.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getcarByPeriod( callBack: OnCallback,startDate: LocalDate, endDate: LocalDate): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/caloriesOut/date/$startDate/$endDate.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getcarByDate(callBack: OnCallback,date: LocalDate ): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/caloriesOut/date/$date/1d.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getsitByPeriod( callBack: OnCallback,startDate: LocalDate, endDate: LocalDate): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/sedentaryMinutes/date/$startDate/$endDate.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getsitByDate(callBack: OnCallback,date: LocalDate ): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/sedentaryMinutes/date/$date/1d.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

//HSD跟HBD





    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserSleep(callBack : OnCallback, date:LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1.2/user/-/sleep/date/$date.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSummaryData(callBack: OnCallback,date: LocalDate): String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/date/$date.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserHeart(callBack : OnCallback, date:LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/activities/heart/date/$date/1d.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserVO2(callBack : OnCallback, date:LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/cardioscore/date/$date.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserBreath(callBack : OnCallback, date:LocalDate,date2:LocalDate) : String {
        val reqUrl = "https://api.fitbit.com/1/user/-/br/date/$date/$date2.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserSpo2(callBack : OnCallback, date:LocalDate) : String {
        val reqUrl = " https://api.fitbit.com/1/user/-/spo2/date/$date.json"
        var resData = ""
        sendPOST(reqUrl, callBack)
        return resData
    }

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendPOST(url: String, callback: OnCallback) {
        // Offer the request to the Channel instead of executing it directly
        requestChannel.trySend {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("DEBUG", e.message.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    val res = response.body?.string()
                    callback.onOKCall(res)
                }
            })
        }.isSuccess
    }

    interface OnCallback {
        fun onOKCall(respond: String?)
        fun onFailCall(error: String?)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setAccessToken(token : String){
        accessToken = token
        Log.e("inside fit bit api para", token)
        Log.e("inside fit bit api accesstoken", accessToken)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getAccessToken(): String {
        return accessToken
    }

}

