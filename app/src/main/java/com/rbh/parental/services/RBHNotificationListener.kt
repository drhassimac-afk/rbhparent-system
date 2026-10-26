package com.rbh.parental.services

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class RBHNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        try {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            val prefs = getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val count = prefs.getInt(sbn.packageName, 0) + 1
            prefs.edit().putInt(sbn.packageName, count).apply()

            Log.d("NotifListener", "${sbn.packageName}: $title - $text")
        } catch (e: Exception) { }
    }
}
