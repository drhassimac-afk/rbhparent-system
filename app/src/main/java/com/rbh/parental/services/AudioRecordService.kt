package com.rbh.parental.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rbh.parental.R
import com.rbh.parental.activities.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecordService : Service() {

    companion object {
        const val TAG = "AudioRecord"
        var isRecording = false
        var currentRecording: MediaRecorder? = null
    }

    private var recorder: MediaRecorder? = null
    private var handler: Handler? = null
    private var maxDurationMs = 300000L // 5 دقائق
    private var recordingFile: File? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action") ?: "single"
        maxDurationMs = intent?.getLongExtra("max_duration", 300000L) ?: 300000L

        startForeground(3002, buildNotification())

        if (action == "continuous") {
            startContinuousRecording()
        } else {
            startRecording()
            // إيقاف تلقائي
            Handler(Looper.getMainLooper()).postDelayed({
                stopRecording()
                stopSelf()
            }, maxDurationMs)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── تسجيل واحد ──
    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission not granted")
            stopSelf()
            return
        }

        try {
            val dir = File(getExternalFilesDir(null), "RBH_Audio")
            if (!dir.exists()) dir.mkdirs()

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "AUD_${sdf.format(Date())}.m4a"
            recordingFile = File(dir, fileName)

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(recordingFile!!.absolutePath)
                setMaxDuration(maxDurationMs.toInt())
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Recorder error: $what/$extra")
                    stopRecording()
                }
                prepare()
                start()
            }

            isRecording = true
            updateNotification()
            Log.d(TAG, "Recording started: ${recordingFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Start error: ${e.message}")
            stopSelf()
        }
    }

    // ── تسجيل مستمر كل 5 دقائق ──
    private fun startContinuousRecording() {
        handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                if (isRecording) return
                startRecording()

                Handler(Looper.getMainLooper()).postDelayed({
                    stopRecording()
                    saveRecording()

                    // التسجيل التالي بعد 30 ثانية
                    handler?.postDelayed(this, 30000L)
                }, maxDurationMs)
            }
        }
        handler?.postDelayed(runnable, 1000)

        // إيقاف بعد 30 دقيقة
        handler?.postDelayed({
            stopSelf()
        }, 1800000L)
    }

    // ── إيقاف التسجيل ──
    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            saveRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Stop error: ${e.message}")
        }
    }

    // ── حفظ التسجيل ──
    private fun saveRecording() {
        val file = recordingFile ?: return
        if (!file.exists() || file.length() == 0L) return

        val size = file.length()
        val mins = size / (1024 * 1024)

        // إشعار
        val nm = getSystemService(NotificationManager::class.java)
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        nm.notify(302, NotificationCompat.Builder(this, "rbh_audio")
            .setContentTitle("🎤 تسجيل محفوظ")
            .setContentText("${file.name} (${size / 1024}KB)")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build())

        // حفظ المسار
        val prefs = getSharedPreferences("rbh_recordings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_${System.currentTimeMillis()}", file.absolutePath)
            .apply()

        Log.d(TAG, "Recording saved: ${file.absolutePath} (${size} bytes)")
        recordingFile = null
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        nm.notify(3002, NotificationCompat.Builder(this, "rbh_audio")
            .setContentTitle("🎤 تسجيل صوتي")
            .setContentText("جاري التسجيل...")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(intent)
            .setOngoing(true)
            .build())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("rbh_audio",
                "Audio", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "rbh_audio")
            .setContentTitle("🎤 RBH - الميكروفون")
            .setContentText("جاري التسجيل...")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        if (isRecording) stopRecording()
        super.onDestroy()
    }
}
