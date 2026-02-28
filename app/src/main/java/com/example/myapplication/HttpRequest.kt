package com.example.myapplication

import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpRequest {

    fun sendPOST(url: String, requestBody: RequestBody, callback: OnCallback) {
        /**建立連線*/
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()

        /**設置傳送需求*/
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + Keeper.YOUR_KEY)
            .post(requestBody)
            .build()

        /**設置回傳*/
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailCall(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val res = response.body?.string()
                callback.onOKCall(res)
            }
        })
    }

    interface OnCallback {
        fun onOKCall(respond: String?)
        fun onFailCall(error: String?)
    }
}
