package com.rbh.parental.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var isBlocked: Boolean = false,
    var usageMinutesToday: Long = 0,
    var notificationCount: Int = 0
) {
    fun getFormattedUsage(): String {
        val hours = usageMinutesToday / 60
        val minutes = usageMinutesToday % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
