package com.rbh.parental.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rbh.parental.R
import com.rbh.parental.activities.MainActivity

class LocationService : Service(), LocationListener {

    private var lm: LocationManager? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("loc_channel",
                "Location", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        startForeground(1002, NotificationCompat.Builder(this, "loc_channel")
            .setContentTitle("RBH - تتبع الموقع")
            .setContentText("جاري التتبع...")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true).build())

        startTracking()
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null

    private fun startTracking() {
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) return

        try {
            lm?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 15000L, 10f, this,
                Looper.getMainLooper()
            )
            lm?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 30000L, 50f, this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) { }
    }

    override fun onLocationChanged(location: Location) {
        Log.d("Location", "Update: ${location.latitude}, ${location.longitude}")
        getSharedPreferences("location", Context.MODE_PRIVATE).edit()
            .putFloat("lat", location.latitude.toFloat())
            .putFloat("lng", location.longitude.toFloat())
            .putFloat("acc", location.accuracy)
            .putLong("time", System.currentTimeMillis())
            .apply()
    }

    override fun onDestroy() {
        lm?.removeUpdates(this)
        super.onDestroy()
    }
}
