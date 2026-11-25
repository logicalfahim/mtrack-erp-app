package com.mtrack.installer.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mtrack.installer.data.model.UserProfile

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "mtrack_erp_session"
        private const val KEY_TOKEN = "auth_bearer_token"
        private const val KEY_USER_JSON = "user_profile_json"
    }

    fun saveAuth(token: String, user: UserProfile) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_JSON, gson.toJson(user))
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getAuthToken(): String? = getToken()

    fun getUser(): UserProfile? {
        val json = prefs.getString(KEY_USER_JSON, null) ?: return null
        return try {
            gson.fromJson(json, UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun logout() {
        prefs.edit().clear().apply()
    }
}
