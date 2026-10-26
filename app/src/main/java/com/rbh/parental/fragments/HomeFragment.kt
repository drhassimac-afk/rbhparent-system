package com.rbh.parental.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import com.rbh.parental.activities.*
import com.rbh.parental.adapters.ToolsAdapter
import com.rbh.parental.models.ToolAction
import com.rbh.parental.models.ToolItem

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDashboard(view)
        setupToolsGrid(view)
        setupClicks(view)
    }

    private fun setupDashboard(view: View) {
        val blocked = requireContext()
            .getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        view.findViewById<TextView>(R.id.tvBlockedCount)?.text = "${blocked.all.size}"

        val notif = requireContext()
            .getSharedPreferences("notifications", Context.MODE_PRIVATE)
        view.findViewById<TextView>(R.id.tvNotifCount)?.text = "${notif.all.size}"
    }

    private fun setupToolsGrid(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvTools)
        val tools = listOf(
            // ═══ الكاميرا ═══
            ToolItem("📷 الكاميرا", "التقاط صور", R.drawable.ic_shield,
                0xFF2D6AE8.toInt(), ToolAction.REPORT),

            // ═══ الميكروفون ═══
            ToolItem("🎤 الميكروفون", "تسجيل صوتي", R.drawable.ic_emergency,
                0xFFE53935.toInt(), ToolAction.EMERGENCY),

            // ═══ حظر التطبيقات ═══
            ToolItem("حظر التطبيقات", "تحكم كامل", R.drawable.ic_block,
                0xFF9C27B0.toInt(), ToolAction.BLOCK_APPS),

            // ═══ تقرير بياني ═══
            ToolItem("📊 تقرير", "رسم بياني", R.drawable.ic_time,
                0xFF6A5ACD.toInt(), ToolAction.REPORT),

            // ═══ وقت النوم ═══
            ToolItem("⏰ وقت النوم", "جدولة", R.drawable.ic_schedule,
                0xFFFFA726.toInt(), ToolAction.SCHEDULE),

            // ═══ اتصال WiFi ═══
            ToolItem("🔗 اتصال WiFi", "سيرفر", R.drawable.ic_apps,
                0xFF00BCD4.toInt(), ToolAction.CALLS_LOG),
        )

        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        rv.adapter = ToolsAdapter(tools) { tool ->
            val intent: Intent? = when (tool.title) {
                "📷 الكاميرا" -> Intent(requireContext(), CameraActivity::class.java)
                "🎤 الميكروفون" -> Intent(requireContext(), MicrophoneActivity::class.java)
                "حظر التطبيقات" -> Intent(requireContext(), BlockAppsActivity::class.java)
                "📊 تقرير" -> Intent(requireContext(), UsageChartActivity::class.java)
                "⏰ وقت النوم" -> Intent(requireContext(), SleepScheduleActivity::class.java)
                "🔗 اتصال WiFi" -> Intent(requireContext(), WiFiConnectActivity::class.java)
                else -> null
            }
            intent?.let { startActivity(it) }
        }
    }

    private fun setupClicks(view: View) {
        view.findViewById<View>(R.id.cardEmergency)?.setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyActivity::class.java))
        }
    }
}
