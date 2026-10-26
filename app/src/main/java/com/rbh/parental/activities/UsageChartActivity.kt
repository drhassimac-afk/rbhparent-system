package com.rbh.parental.activities

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.rbh.parental.R
import com.rbh.parental.adapters.UsageBarAdapter
import com.rbh.parental.models.AppInfo
import java.util.Calendar

class UsageChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_chart)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        loadUsageData()
        loadWeeklyChart()
    }

    private fun loadUsageData() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis()
        ).filter { it.totalTimeInForeground > 0 }
         .sortedByDescending { it.totalTimeInForeground }

        val pm = packageManager
        val apps = stats.map { stat ->
            val name = try {
                pm.getApplicationLabel(pm.getApplicationInfo(stat.packageName, 0)).toString()
            } catch (e: Exception) { stat.packageName }

            val icon: Drawable? = try {
                pm.getApplicationIcon(stat.packageName)
            } catch (e: Exception) { null }

            AppInfo(
                packageName = stat.packageName,
                appName = name,
                icon = icon,
                usageMinutesToday = stat.totalTimeInForeground / 60000
            )
        }

        // ملخص
        val totalMin = apps.sumOf { it.usageMinutesToday }
        val hours = totalMin / 60
        val minutes = totalMin % 60
        findViewById<TextView>(R.id.tvTotalToday).text = "${hours}h ${minutes}m"
        findViewById<TextView>(R.id.tvAppCount).text = "${apps.size} تطبيق مستخدم"

        // مخطط دائري
        setupPieChart(apps, totalMin)

        // قائمة
        val rv = findViewById<RecyclerView>(R.id.rvUsageList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = UsageBarAdapter(apps, totalMin)
    }

    private fun setupPieChart(apps: List<AppInfo>, total: Long) {
        val pie = findViewById<PieChart>(R.id.pieChart)
        pie.setUsePercentValues(true)
        pie.description.isEnabled = false
        pie.setDrawHoleEnabled(true)
        pie.holeRadius = 40f
        pie.transparentCircleRadius = 45f
        pie.setEntryLabelTextSize(12f)
        pie.setEntryLabelColor(Color.BLACK)
        pie.isRotationEnabled = true

        val entries = mutableListOf<PieEntry>()
        val topApps = apps.take(6)
        topApps.forEach { app ->
            val percent = if (total > 0) (app.usageMinutesToday * 100f) / total else 0f
            entries.add(PieEntry(percent, app.appName))
        }

        if (entries.isEmpty()) {
            entries.add(PieEntry(100f, "لا توجد بيانات"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#2D6AE8"),
                Color.parseColor("#00C853"),
                Color.parseColor("#FFA726"),
                Color.parseColor("#E53935"),
                Color.parseColor("#7B1FA2"),
                Color.parseColor("#00BCD4")
            ) + ColorTemplate.COLORFUL_COLORS.toList()
            sliceSpace = 3f
        }

        pie.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pie))
            setValueTextSize(12f)
            setValueTextColor(Color.BLACK)
        }

        pie.legend.apply {
            form = Legend.LegendForm.CIRCLE
            textSize = 12f
        }

        pie.invalidate()
    }

    private fun loadWeeklyChart() {
        val bar = findViewById<BarChart>(R.id.barChart)
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val dayNames = arrayOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val end = cal.timeInMillis + 86400000L

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                end
            )

            val totalMin = stats.sumOf { it.totalTimeInForeground } / 60000f
            entries.add(BarEntry(6f - i, totalMin))

            val dayIdx = cal.get(Calendar.DAY_OF_WEEK)
            labels.add(dayNames[(dayIdx + 1) % 7])
        }

        val dataSet = BarDataSet(entries, "الدقائق").apply {
            setColors(
                Color.parseColor("#2D6AE8"),
                Color.parseColor("#00C853"),
                Color.parseColor("#FFA726"),
                Color.parseColor("#E53935"),
                Color.parseColor("#7B1FA2"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#FF5722")
            )
        }

        bar.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            setFitBars(true)
            axisRight.isEnabled = false
            legend.textSize = 12f
            xAxis.apply {
                setDrawGridLines(false)
                textSize = 10f
            }
            invalidate()
        }
    }
}
