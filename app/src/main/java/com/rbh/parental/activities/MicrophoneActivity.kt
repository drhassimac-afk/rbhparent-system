package com.rbh.parental.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.rbh.parental.R
import com.rbh.parental.services.AudioRecordService
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MicrophoneActivity : AppCompatActivity() {

    private var isRecording = false
    private var timerHandler: Handler? = null
    private var elapsedSeconds = 0

    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var ivIndicator: ImageView
    private lateinit var btnRecord: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var tvEmpty: TextView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.RECORD_AUDIO] == true) {
            startRecording()
        } else {
            Toast.makeText(this, "إذن الميكروفون مطلوب!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_microphone)

        tvStatus = findViewById(R.id.tvRecStatus)
        tvTimer = findViewById(R.id.tvRecTimer)
        ivIndicator = findViewById(R.id.ivRecIndicator)
        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        tvEmpty = findViewById(R.id.tvEmptyRecordings)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnRecord.setOnClickListener { requestAndRecord() }
        btnStop.setOnClickListener { stopRecording() }

        findViewById<MaterialSwitch>(R.id.switchContinuous).setOnCheckedChangeListener { _, checked ->
            if (checked) {
                stopService(Intent(this, AudioRecordService::class.java))
                startContinuousRecording()
            } else {
                stopRecording()
            }
        }

        val seek = findViewById<SeekBar>(R.id.seekDuration)
        val tvMax = findViewById<TextView>(R.id.tvMaxDuration)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { tvMax.text = "$p" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        findViewById<MaterialButton>(R.id.btnOpenGallery).setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        loadRecordings()
    }

    private fun requestAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun startRecording() {
        val maxMin = findViewById<SeekBar>(R.id.seekDuration).progress
        val maxMs = maxMin.toLong() * 60000L

        val intent = Intent(this, AudioRecordService::class.java).apply {
            putExtra("action", "single")
            putExtra("max_duration", maxMs)
        }
        startForegroundService(intent)

        isRecording = true
        elapsedSeconds = 0
        tvStatus.text = "🔴 جاري التسجيل"
        btnRecord.isEnabled = false
        btnStop.isEnabled = true
        ivIndicator.alpha = 1f

        ObjectAnimator.ofFloat(ivIndicator, "alpha", 1f, 0.3f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            duration = 600
            start()
        }

        startTimer()
    }

    private fun startContinuousRecording() {
        val intent = Intent(this, AudioRecordService::class.java).apply {
            putExtra("action", "continuous")
            putExtra("max_duration", 300000L)
        }
        startForegroundService(intent)

        isRecording = true
        tvStatus.text = "🔴 تسجيل مستمر"
        btnRecord.isEnabled = false
        btnStop.isEnabled = true
    }

    private fun stopRecording() {
        stopService(Intent(this, AudioRecordService::class.java))
        isRecording = false
        elapsedSeconds = 0
        timerHandler?.removeCallbacksAndMessages(null)

        tvStatus.text = "⏸ توقف"
        tvTimer.text = "00:00"
        btnRecord.isEnabled = true
        btnStop.isEnabled = false

        ivIndicator.animate().cancel()
        ivIndicator.alpha = 0.5f

        Handler(Looper.getMainLooper()).postDelayed({ loadRecordings() }, 1000)
    }

    private fun startTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    elapsedSeconds++
                    val min = elapsedSeconds / 60
                    val sec = elapsedSeconds % 60
                    tvTimer.text = String.format("%02d:%02d", min, sec)
                    timerHandler?.postDelayed(this, 1000)
                }
            }
        }
        timerHandler?.postDelayed(runnable, 1000)
    }

    private fun loadRecordings() {
        val dir = File(getExternalFilesDir(null), "RBH_Audio")
        val files: Array<File> = dir.listFiles() ?: arrayOf()

        if (files.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            val sorted = files.sortedByDescending { it.lastModified() }
            val sb = StringBuilder()
            sorted.take(10).forEach { f ->
                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                val date = sdf.format(Date(f.lastModified()))
                sb.append("🎤 ${f.name} (${f.length() / 1024}KB) - $date\n")
            }
        }
    }
}
