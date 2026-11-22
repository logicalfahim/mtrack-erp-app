package com.mtrack.installer.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * Default Server Base URL.
     * Note:
     * - Live Production: "https://erp.mtrack.com.bd/" or "https://install.mtrack.com.bd/"
     * - Android Emulator Local Test: "http://10.0.2.2/"
     * - Physical Device on Wi-Fi: "http://<YOUR_PC_IP>/"
     */
    private const val DEFAULT_BASE_URL = "https://erp.mtrack.com.bd/"

    private var customBaseUrl: String? = null

    fun setBaseUrl(url: String) {
        customBaseUrl = if (url.endsWith("/")) url else "$url/"
    }

    fun getBaseUrl(): String = customBaseUrl ?: DEFAULT_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: MTrackApiService by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MTrackApiService::class.java)
    }
}
