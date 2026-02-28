package com.example.myapplication

object Config {
    const val AUTHORIZE_ENDPOINT = "https://www.fitbit.com/oauth2/authorize"
    const val TOKEN_ENDPOINT = "https://api.fitbit.com/oauth2/token"
    const val CLIENT_ID = "23R42F"
    const val REDIRECT_URI = "oauth://healper.com" // if edited, update your Cloudentity client application redirect_uri
    const val GRANT_TYPE = "authorization_code"
    const val SCOPE = "activity cardio_fitness electrocardiogram heartrate location nutrition oxygen_saturation profile respiratory_rate settings sleep social temperature weight"
}