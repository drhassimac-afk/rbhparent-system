package com.rbh.parental.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.rbh.parental.R
import com.rbh.parental.services.SleepReceiver

class SleepScheduleActivity : AppCompatActivity() {

    private var bedHour = 22
    private var bedMinute = 0
    private var wakeHour = 6
    private var wakeMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep_schedule)

        loadSavedSchedule()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // وقت النوم
        findViewById<android.view.View>(R.id.layoutBedtime).setOnClickListener {
            showTimePicker(bedHour, bedMinute) { h, m ->
                bedHour = h; bedMinute = m
                updatePreview()
            }
        }

        // وقت الاستيقاظ
        findViewById<android.view.View>(R.id.layoutWakeup).setOnClickListener {
            showTimePicker(wakeHour, wakeMinute) { h, m ->
                wakeHour = h; wakeMinute = m
                updatePreview()
            }
        }

        // تفعيل/إلغاء
        findViewById<MaterialSwitch>(R.id.switchSleep).setOnCheckedChangeListener { _, checked ->
            if (checked) scheduleAlarms()
            else cancelAlarms()
        }

        // حفظ
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveSchedule() }

        updatePreview()
    }

    private fun showTimePicker(h: Int, m: Int, onSet: (Int, Int) -> Unit) {
        TimePickerDialog(this, { _, hh, mm ->
            onSet(hh, mm)
        }, h, m, true).show()
    }

    private fun updatePreview() {
        val bed = String.format("%02d:%02d", bedHour, bedMinute)
        val wake = String.format("%02d:%02d", wakeHour, wakeMinute)

        val hours = if (wakeHour > bedHour) {
            wakeHour - bedHour
        } else {
            24 - bedHour + wakeHour
        }

        findViewById<TextView>(R.id.tvBedtime).text = bed
        findViewById<TextView>(R.id.tvWakeup).text = wake
        findViewById<TextView>(R.id.tvPreview).text =
            "⏰ النوم: $bed ← $wake\n$hours ساعات"
    }

    private fun scheduleAlarms() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // تنبيه وقت النوم
        val bedIntent = PendingIntent.getBroadcast(this, 100,
            Intent(this, SleepReceiver::class.java).apply {
                putExtra("type", "sleep")
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // تنبيه وقت الاستيقاظ
        val wakeIntent = PendingIntent.getBroadcast(this, 200,
            Intent(this, SleepReceiver::class.java).apply {
                putExtra("type", "wake")
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        Toast.makeText(this, "⏰ تم تفعيل الجدولة", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarms() {
        Toast.makeText(this, "⏰ تم إلغاء الجدولة", Toast.LENGTH_SHORT).show()
    }

    private fun saveSchedule() {
        getSharedPreferences("sleep_schedule", Context.MODE_PRIVATE).edit()
            .putInt("bed_hour", bedHour)
            .putInt("bed_minute", bedMinute)
            .putInt("wake_hour", wakeHour)
            .putInt("wake_minute", wakeMinute)
            .putBoolean("enabled",
                findViewById<MaterialSwitch>(R.id.switchSleep).isChecked)
            .apply()

        Toast.makeText(this, "💾 تم الحفظ", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedSchedule() {
        val prefs = getSharedPreferences("sleep_schedule", Context.MODE_PRIVATE)
        bedHour = prefs.getInt("bed_hour", 22)
        bedMinute = prefs.getInt("bed_minute", 0)
        wakeHour = prefs.getInt("wake_hour", 6)
        wakeMinute = prefs.getInt("wake_minute", 0)
        findViewById<MaterialSwitch>(R.id.switchSleep)
            .isChecked = prefs.getBoolean("enabled", false)
    }
}
