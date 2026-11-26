package com.mtrack.installer.data.api

import okhttp3.Interceptor
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

    @Volatile
    private var globalAuthToken: String? = null

    fun setBaseUrl(url: String) {
        customBaseUrl = if (url.endsWith("/")) url else "$url/"
    }

    fun setAuthToken(token: String?) {
        globalAuthToken = token
    }

    fun getBaseUrl(): String = customBaseUrl ?: DEFAULT_BASE_URL

    private val authHeadersInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // Ensure Accept: application/json
        if (original.header("Accept") == null) {
            builder.addHeader("Accept", "application/json")
        }

        // Attach Authorization and X-API-KEY headers for Apache compatibility
        val token = globalAuthToken
        if (!token.isNullOrBlank()) {
            val clean = if (token.startsWith("Bearer ")) token.substring(7).trim() else token.trim()
            if (original.header("Authorization") == null) {
                builder.addHeader("Authorization", "Bearer $clean")
            }
            if (original.header("X-API-KEY") == null) {
                builder.addHeader("X-API-KEY", clean)
            }
            if (original.header("X-Auth-Token") == null) {
                builder.addHeader("X-Auth-Token", clean)
            }
        }

        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authHeadersInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = com.google.gson.GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    val apiService: MTrackApiService by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MTrackApiService::class.java)
    }
}
