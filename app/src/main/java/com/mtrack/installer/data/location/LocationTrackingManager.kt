package com.mtrack.installer.data.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.mtrack.installer.data.api.RetrofitClient
import com.mtrack.installer.data.local.SessionManager
import com.mtrack.installer.data.model.LocationUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Enterprise GPS Telematics Engine (Play Store Compliant)
 * Handles high-precision staff location tracking, battery diagnostics,
 * mock location detection, and battery-optimized background updates.
 */
class LocationTrackingManager(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sessionManager = SessionManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private val _batteryPercentage = MutableStateFlow(100)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private var lastUploadedTimestamp: Long = 0L

    companion object {
        private const val TAG = "MTrackLocation"
        private const val MIN_TIME_INTERVAL_MS = 30000L // 30 seconds
        private const val MIN_DISTANCE_METERS = 0.0f   // 0 meters to ensure steady periodic updates even when stationary
    }

    /**
     * Verify location runtime permissions
     */
    fun hasLocationPermissions(): Boolean {
        val finePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return finePerm && coarsePerm
    }

    /**
     * Start Real-Time Staff Location Tracking
     */
    fun startTracking() {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions missing. Request permissions from user first.")
            return
        }

        try {
            updateBatteryDiagnostics()

            // 1. Register GPS Provider
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_INTERVAL_MS,
                    MIN_DISTANCE_METERS,
                    this
                )
            }

            // 2. Register Network Provider fallback
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_INTERVAL_MS,
                    MIN_DISTANCE_METERS,
                    this
                )
            }

            // Read last known location immediately
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGps ?: lastNet

            if (bestLocation != null) {
                _lastLocation.value = bestLocation
                uploadLocationToServer(bestLocation)
            }

            _isTrackingActive.value = true
            Log.i(TAG, "Staff GPS tracking initialized successfully.")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while requesting location updates", e)
            _isTrackingActive.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates", e)
            _isTrackingActive.value = false
        }
    }

    /**
     * Stop Location Tracking
     */
    fun stopTracking() {
        try {
            locationManager?.removeUpdates(this)
            _isTrackingActive.value = false
            Log.i(TAG, "Staff GPS tracking paused.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        _lastLocation.value = location
        updateBatteryDiagnostics()

        val currentTime = System.currentTimeMillis()
        // Rate-limit upload to server: minimum 15 seconds apart to preserve device battery
        if (currentTime - lastUploadedTimestamp < 15000L) {
            return
        }

        lastUploadedTimestamp = currentTime
        uploadLocationToServer(location)
    }

    private fun uploadLocationToServer(location: Location) {
        val token = sessionManager.getAuthToken() ?: return
        val bearerToken = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
        val currentUser = sessionManager.getUser()
        val userId = currentUser?.userId

        val isMocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

        val request = LocationUpdateRequest(
            userId = userId,
            staffId = userId,
            latitude = location.latitude,
            longitude = location.longitude,
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy,
            speed = if (location.hasSpeed()) location.speed else 0.0f,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            batteryLevel = _batteryPercentage.value,
            battery = _batteryPercentage.value,
            isCharging = isDeviceCharging(),
            isMocked = isMocked,
            provider = location.provider ?: "fused"
        )

        scope.launch {
            try {
                // Primary: Try ERP staff-tracking endpoint
                val response = RetrofitClient.apiService.recordStaffTracking(bearerToken, request)
                if (response.isSuccessful) {
                    Log.d(TAG, "Location synced to ERP staff-tracking successfully (code: ${response.code()}).")
                    return@launch
                }
                Log.w(TAG, "staff-tracking returned ${response.code()}, trying staff_tracking fallback...")

                // Fallback 1: Try staff_tracking (underscore)
                val response2 = RetrofitClient.apiService.recordStaffTrackingUnderscore(bearerToken, request)
                if (response2.isSuccessful) {
                    Log.d(TAG, "Location synced to ERP staff_tracking successfully (code: ${response2.code()}).")
                    return@launch
                }
                Log.w(TAG, "staff_tracking returned ${response2.code()}, trying update-location fallback...")

                // Fallback 2: Try update-location
                val response3 = RetrofitClient.apiService.updateStaffLocation(bearerToken, request)
                if (response3.isSuccessful) {
                    Log.d(TAG, "Location synced to ERP update-location successfully (code: ${response3.code()}).")
                } else {
                    Log.w(TAG, "All location endpoints failed. Last code: ${response3.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network failure syncing location to server", e)
            }
        }
    }

    /**
     * Read Battery Percentage & Status
     */
    private fun updateBatteryDiagnostics() {
        try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                _batteryPercentage.value = (level * 100 / scale)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read battery status", e)
        }
    }

    private fun isDeviceCharging(): Boolean {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
