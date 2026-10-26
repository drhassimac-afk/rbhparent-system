package com.rbh.parental.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rbh.parental.R
import com.rbh.parental.activities.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraCaptureService : Service() {

    companion object {
        const val TAG = "CameraCapture"
        const val EXTRA_USE_FRONT = "use_front"
        var isCapturing = false
    }

    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var captureRequest: CaptureRequest? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var useFrontCamera = true
    private var autoCaptureHandler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        useFrontCamera = intent?.getBooleanExtra(EXTRA_USE_FRONT, true) ?: true
        val action = intent?.getStringExtra("action") ?: "single"

        startForeground(3001, buildNotification())

        if (action == "auto") {
            startAutoCapture()
        } else {
            captureSinglePhoto()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── التقاط صورة واحدة ──
    private fun captureSinglePhoto() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            stopSelf()
            return
        }

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = selectCamera(manager, useFrontCamera)

        val size = getPreviewSize(manager, cameraId)

        imageReader = ImageReader.newInstance(
            size.width, size.height,
            ImageFormat.JPEG, 1
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()

            savePhoto(bytes)
        }, Handler(Looper.getMainLooper()))

        try {
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera

                    val texture = SurfaceTexture(0)
                    texture.setDefaultBufferSize(size.width, size.height)
                    val surface = android.view.Surface(texture)

                    val surfaceList = listOf(surface, imageReader!!.surface)

                    camera.createCaptureSession(surfaceList,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraCaptureSession = session
                                val request = camera.createCaptureRequest(
                                    CameraDevice.TEMPLATE_STILL_CAPTURE
                                )
                                request.addTarget(imageReader!!.surface)
                                // جودة أعلى
                                request.set(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON
                                )
                                captureRequest = request.build()
                                session.capture(captureRequest!!, null, null)
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) {
                                Log.e(TAG, "Session failed")
                                stopSelf()
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    stopSelf()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    stopSelf()
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            Log.e(TAG, "Security: ${e.message}")
            stopSelf()
        }
    }

    // ── التقاط تلقائي كل دقيقة ──
    private fun startAutoCapture() {
        isCapturing = true
        autoCaptureHandler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                if (isCapturing) {
                    captureSinglePhoto()
                    autoCaptureHandler?.postDelayed(this, 60000L) // كل دقيقة
                }
            }
        }
        autoCaptureHandler?.postDelayed(runnable, 1000)

        // إيقاف بعد 10 دقائق
        autoCaptureHandler?.postDelayed({ stopSelf() }, 600000L)
    }

    // ── حفظ الصورة ──
    private fun savePhoto(bytes: ByteArray) {
        try {
            val dir = File(getExternalFilesDir(null), "RBH_Photos")
            if (!dir.exists()) dir.mkdirs()

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "IMG_${sdf.format(Date())}.jpg"
            val file = File(dir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            // إشعار
            notifyPhoto(file.name, file.length())

            // حفظ المسار
            val prefs = getSharedPreferences("rbh_photos", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_${System.currentTimeMillis()}", file.absolutePath)
                .apply()

            Log.d(TAG, "Photo saved: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Save error: ${e.message}")
        } finally {
            // إعادة إغلاق الكاميرا
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun notifyPhoto(name: String, size: Long) {
        val nm = getSystemService(NotificationManager::class.java)
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "rbh_camera")
            .setContentTitle("📷 صورة محفوظة")
            .setContentText("$name (${size / 1024}KB)")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

        nm.notify(301, notification)
    }

    private fun selectCamera(manager: CameraManager, front: Boolean): String {
        val cameras = manager.cameraIdList
        val target = if (front) CameraCharacteristics.LENS_FACING_FRONT
                     else CameraCharacteristics.LENS_FACING_BACK

        for (id in cameras) {
            val facing = manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING)
            if (facing == target) return id
        }
        return cameras[0]
    }

    private fun getPreviewSize(manager: CameraManager, cameraId: String)
            : android.util.Size {
        val map = manager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map?.getOutputSizes(ImageFormat.JPEG) ?: arrayOf()
        // أكبر حجم متاح
        return sizes.maxByOrNull { it.width * it.height } ?: android.util.Size(1920, 1080)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("rbh_camera",
                "Camera", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "rbh_camera")
            .setContentTitle("📷 RBH - الكاميرا")
            .setContentText("جاري التصوير...")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isCapturing = false
        autoCaptureHandler?.removeCallbacksAndMessages(null)
        cameraDevice?.close()
        imageReader?.close()
        super.onDestroy()
    }
}
