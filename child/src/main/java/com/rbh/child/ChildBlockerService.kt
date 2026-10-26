package com.rbh.child

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ChildBlockerService : AccessibilityService() {

    companion object {
        var blockedApps: MutableSet<String> = mutableSetOf()
        var screenTimeLimitMinutes: Int = -1  // -1 = بدون حد
    }

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()

        // تحميل التطبيقات المحظورة من SharedPreferences
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        blockedApps = prefs.all.keys.toMutableSet()

        // تحميل حد الوقت
        screenTimeLimitMinutes = prefs.getInt("screen_limit", -1)

        startTime = System.currentTimeMillis()
        Log.d("ChildBlocker", "Active - ${blockedApps.size} blocked apps")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return

            // حظر التطبيقات المحظورة
            if (pkg in blockedApps && pkg != packageName) {
                Log.d("ChildBlocker", "Blocking: $pkg")
                handler.postDelayed({
                    startActivity(Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }, 300)
            }
        }
    }

    override fun onInterrupt() {}
}
