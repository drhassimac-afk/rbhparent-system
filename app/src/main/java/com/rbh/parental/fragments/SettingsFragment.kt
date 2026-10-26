package com.rbh.parental.fragments

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.rbh.parental.R
import com.rbh.parental.services.RBHAccessibilityService

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissions(view)
        setupClicks(view)
    }

    private fun checkPermissions(v: View) {
        setStatus(v, R.id.tvUsageStatsStatus, hasUsageStats())
        setStatus(v, R.id.tvAccessibilityStatus, isAccessibilityOn())
        setStatus(v, R.id.tvNotificationStatus, isNotifListenerOn())
        setStatus(v, R.id.tvLocationPermStatus, ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    private fun setStatus(v: View, id: Int, granted: Boolean) {
        val tv = v.findViewById<TextView>(id) ?: return
        if (granted) {
            tv.text = getString(R.string.permission_granted)
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            tv.text = getString(R.string.permission_denied)
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
        }
    }

    private fun setupClicks(v: View) {
        v.findViewById<View>(R.id.layoutUsageStats)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        v.findViewById<View>(R.id.layoutAccessibility)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        v.findViewById<View>(R.id.layoutNotification)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        v.findViewById<View>(R.id.layoutLocation)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", requireContext().packageName, null)
            })
        }
    }

    private fun hasUsageStats(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), requireContext().packageName)
        else @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), requireContext().packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityOn(): Boolean {
        val comp = ComponentName(requireContext(), RBHAccessibilityService::class.java)
        return Settings.Secure.getString(requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(comp.flattenToString()) == true
    }

    private fun isNotifListenerOn(): Boolean {
        return Settings.Secure.getString(requireContext().contentResolver,
            "enabled_notification_listeners")?.contains(requireContext().packageName) == true
    }
}
