package com.rbh.child

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ChildMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_main)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnStart = findViewById<Button>(R.id.btnStart)

        updateStatus(tvStatus)

        btnStart.setOnClickListener {
            // بدء خدمة جمع البيانات
            val intent = Intent(this, DataCollectorService::class.java)
            ContextCompat.startForegroundService(this, intent)

            // فتح إعدادات الوصول
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

            updateStatus(tvStatus)
        }
    }

    private fun updateStatus(tv: TextView) {
        val isEnabled = isServiceEnabled()
        tv.text = if (isEnabled) "✅ الخدمة نشطة" else "⚠️ الخدمة غير مفعّلة"
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains("com.rbh.child")
    }
}
