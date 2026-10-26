package com.rbh.child

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.Timer
import java.util.TimerTask

class DataCollectorService : Service(), LocationListener {

    companion object {
        const val TAG = "ChildService"
    }

    private var timer: Timer? = null
    private var locationManager: LocationManager? = null

    // الكاميرا
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null

    // الميكروفون
    private var recorder: MediaRecorder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(2001, buildNotification("جاري جمع البيانات..."))

        // كل 30 ثانية: جمع استخدام التطبيقات
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() { collectUsageData() }
        }, 0, 30000L)

        // كل دقيقة: التقاط صورة
        timer?.schedule(object : TimerTask() {
            override fun run() { capturePhoto() }
        }, 5000, 60000L)

        // كل 5 دقائق: تسجيل صوت
        timer?.schedule(object : TimerTask() {
            override fun run() { recordAudio() }
        }, 10000, 300000L)

        // كل دقيقة: موقع
        timer?.schedule(object : TimerTask() {
            override fun run() { updateLocation() }
        }, 0, 60000L)

        startLocation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    // ════════ 1. جمع استخدام التطبيقات ════════
    private fun collectUsageData() {
        try {
            val prefs = getSharedPreferences("child_data", Context.MODE_PRIVATE)
            val usm = getSystemService(Context.USAGE_STATS_SERVICE)
                as android.app.usage.UsageStatsManager

            val stats = usm.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                System.currentTimeMillis() - 86400000L,
                System.currentTimeMillis()
            )

            val editor = prefs.edit()
            for (stat in stats) {
                if (stat.totalTimeInForeground > 0) {
                    editor.putLong("usage_${stat.packageName}", stat.totalTimeInForeground)
                }
            }
            editor.putLong("last_update", System.currentTimeMillis())
            editor.apply()

            Log.d(TAG, "Usage data collected")
        } catch (e: Exception) {
            Log.e(TAG, "Usage error: ${e.message}")
        }
    }

    // ════════ 2. التقاط صورة ════════
    private fun capturePhoto() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = selectCamera(manager, front = true)

            val map = manager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG) ?: arrayOf()
            val size = sizes.maxByOrNull { it.width * it.height }
                ?: android.util.Size(640, 480)

            imageReader = ImageReader.newInstance(
                size.width, size.height, ImageFormat.JPEG, 1
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                savePhoto(bytes)
                cameraDevice?.close()
                cameraDevice = null
            }, Handler(Looper.getMainLooper()))

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    val texture = SurfaceTexture(0)
                    texture.setDefaultBufferSize(size.width, size.height)
                    val surface = android.view.Surface(texture)

                    camera.createCaptureSession(
                        listOf(surface, imageReader!!.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                val req = camera.createCaptureRequest(
                                    CameraDevice.TEMPLATE_STILL_CAPTURE
                                )
                                req.addTarget(imageReader!!.surface)
                                session.capture(req.build(), null, null)
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) {}
                        }, Handler(Looper.getMainLooper())
                    )
                }
                override fun onDisconnected(c: CameraDevice) { c.close() }
                override fun onError(c: CameraDevice, e: Int) {}
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e(TAG, "Camera error: ${e.message}")
        }
    }

    private fun savePhoto(bytes: ByteArray) {
        try {
            val dir = File(getExternalFilesDir(null), "Child_Photos")
            if (!dir.exists()) dir.mkdirs()

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val file = File(dir, "IMG_${sdf.format(Date())}.jpg")

            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            // حفظ المسار وإرسال
            val prefs = getSharedPreferences("child_data", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_photo", file.absolutePath)
                .putLong("last_photo_time", System.currentTimeMillis())
                .apply()

            Log.d(TAG, "Photo saved: ${file.name} (${bytes.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Save photo error: ${e.message}")
        }
    }

    // ════════ 3. تسجيل صوت ════════
    private fun recordAudio() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            // إيقاف التسجيل السابق
            try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
            recorder = null

            val dir = File(getExternalFilesDir(null), "Child_Audio")
            if (!dir.exists()) dir.mkdirs()

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val file = File(dir, "AUD_${sdf.format(Date())}.m4a")

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setOutputFile(file.absolutePath)
                setMaxDuration(30000) // 30 ثانية فقط
                prepare()
                start()
            }

            // إيقاف بعد 30 ثانية
            Handler(Looper.getMainLooper()).postDelayed({
                try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
                recorder = null

                if (file.exists() && file.length() > 0) {
                    val prefs = getSharedPreferences("child_data", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("last_audio", file.absolutePath)
                        .putLong("last_audio_time", System.currentTimeMillis())
                        .apply()
                    Log.d(TAG, "Audio saved: ${file.name} (${file.length()} bytes)")
                }
            }, 30000)

            Log.d(TAG, "Audio recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Audio error: ${e.message}")
            try { recorder?.release() } catch (ex: Exception) {}
            recorder = null
        }
    }

    // ════════ 4. الموقع ════════
    private fun startLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 60000L, 50f, this,
                    Looper.getMainLooper()
                )
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 30000L, 100f, this,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) { }
    }

    private fun updateLocation() {
        try {
            val loc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            loc?.let { saveLocation(it) }
        } catch (e: SecurityException) { }
    }

    override fun onLocationChanged(location: Location) {
        saveLocation(location)
    }

    private fun saveLocation(loc: Location) {
        getSharedPreferences("child_data", Context.MODE_PRIVATE).edit()
            .putFloat("lat", loc.latitude.toFloat())
            .putFloat("lng", loc.longitude.toFloat())
            .putFloat("acc", loc.accuracy)
            .putLong("location_time", System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Location: ${loc.latitude}, ${loc.longitude}")
    }

    // ════════ 5. مساعدة الكاميرا ════════
    private fun selectCamera(manager: CameraManager, front: Boolean): String {
        val target = if (front) CameraCharacteristics.LENS_FACING_FRONT
                     else CameraCharacteristics.LENS_FACING_BACK
        for (id in manager.cameraIdList) {
            val facing = manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING)
            if (facing == target) return id
        }
        return manager.cameraIdList[0]
    }

    // ════════ إشعارات ════════
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("child_service",
                "RBH Child", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pending = PendingIntent.getActivity(this, 0,
            Intent(this, ChildMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "child_service")
            .setContentTitle("RBH Child Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        timer?.cancel()
        locationManager?.removeUpdates(this)
        try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
        cameraDevice?.close()
        imageReader?.close()
        super.onDestroy()
    }
}
