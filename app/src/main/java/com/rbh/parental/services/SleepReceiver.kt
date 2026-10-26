package com.rbh.parental.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SleepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        Log.d("SleepReceiver", "Event: $type")

        val prefs = context.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (type == "sleep") {
            // حظر جميع التطبيقات
            val app = context.packageManager.getInstalledApplications(0)
            app.forEach {
                if (it.packageName != context.packageName) {
                    editor.putBoolean(it.packageName, true)
                }
            }
            editor.apply()
            Log.d("SleepReceiver", "Sleep mode ON - all apps blocked")
        } else if (type == "wake") {
            // إلغاء الحظر
            editor.clear()
            editor.apply()
            Log.d("SleepReceiver", "Wake mode - all apps unblocked")
        }
    }
}
