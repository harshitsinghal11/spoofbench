package com.spoofbench.attacker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log

class AttackService : Service() {

    private val tag = "SB_ATTACK_T0"
    private val channelId = "spoofbench_attack"
    private val notifId = 1

    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private var pushRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("SpoofBench T0 attack running")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val lat = intent?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN
        val lon = intent?.getDoubleExtra("lon", Double.NaN) ?: Double.NaN
        val intervalMs = intent?.getLongExtra("intervalMs", 1000L) ?: 1000L
        val durationSec = intent?.getIntExtra("durationSec", 30) ?: 30

        if (lat.isNaN() || lon.isNaN()) {
            Log.e(tag, "Missing lat/lon extras — stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(tag, "Starting T0 attack: lat=$lat lon=$lon intervalMs=$intervalMs durationSec=$durationSec")

        providers.forEach { setupMockProvider(it) }

        val endAt = SystemClock.elapsedRealtime() + durationSec * 1000L
        pushRunnable = object : Runnable {
            override fun run() {
                if (SystemClock.elapsedRealtime() >= endAt) {
                    Log.d(tag, "Attack duration finished — stopping.")
                    stopSelf()
                    return
                }
                providers.forEach { pushLocation(it, lat, lon) }
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(pushRunnable!!)

        return START_NOT_STICKY
    }

    private fun setupMockProvider(provider: String) {
        try {
            locationManager.addTestProvider(
                provider,
                false, false, false, false, true, true, true,
                android.location.provider.ProviderProperties.POWER_USAGE_LOW,
                android.location.provider.ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(provider, true)
        } catch (e: SecurityException) {
            Log.e(tag, "No mock-location permission for provider=$provider — run the appops command first.", e)
        } catch (e: Exception) {
            Log.e(tag, "Could not set up test provider=$provider", e)
        }
    }

    private fun pushLocation(provider: String, lat: Double, lon: Double) {
        val location = Location(provider).apply {
            latitude = lat
            longitude = lon
            accuracy = 5.0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        try {
            locationManager.setTestProviderLocation(provider, location)
            Log.d(tag, "Pushed to provider=$provider lat=$lat lon=$lon")
        } catch (e: Exception) {
            Log.e(tag, "Failed to push location for provider=$provider", e)
        }
    }

    override fun onDestroy() {
        pushRunnable?.let { handler.removeCallbacks(it) }
        providers.forEach {
            try { locationManager.removeTestProvider(it) } catch (_: Exception) {}
        }
        super.onDestroy()
    }
}