package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URLEncoder

class FitBitAuthorization : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_sport_data)

        fun String.utf8(): String = URLEncoder.encode(this, "UTF-8")

        val authParams = mapOf(
            "client_id" to Config.CLIENT_ID,
            "redirect_uri" to Config.REDIRECT_URI,
            "response_type" to "code",
            "code_challenge" to PKCEUtil.getCodeChallenge(), // Set the code challenge
            "code_challenge_method" to "S256",
            "scope" to Config.SCOPE
        ).map {(k,v) -> "${(k.utf8())}=${v.utf8()}"}.joinToString("&")
        // Initiate the OAuth 2.0 flow using CustomTabs<url>.
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse("${Config.AUTHORIZE_ENDPOINT}?$authParams"))
    }
}