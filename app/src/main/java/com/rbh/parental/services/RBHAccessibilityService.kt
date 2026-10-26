package com.rbh.parental.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RBHAccessibilityService : AccessibilityService() {

    companion object {
        var blockedApps: MutableSet<String> = mutableSetOf()
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        blockedApps = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            .all.keys.toMutableSet()
        Log.d("RBH_Access", "خدمة الوصول نشطة - ${blockedApps.size} محظور")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg in blockedApps && pkg != packageName) {
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
