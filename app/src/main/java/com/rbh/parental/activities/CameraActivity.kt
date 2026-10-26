package com.rbh.parental.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.view.Surface
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rbh.parental.R
import com.rbh.parental.services.CameraCaptureService
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {

    private var useFront = true
    private var isAutoCapture = false
    private var photoCount = 0

    private lateinit var tvStatus: TextView
    private lateinit var tvCount: TextView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.CAMERA] == true) {
            startPreview()
        } else {
            Toast.makeText(this, "كاميرا مطلوبة!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        tvStatus = findViewById(R.id.tvStatus)
        tvCount = findViewById(R.id.tvCount)

        countExistingPhotos()

        // رجوع
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // تبديل الكاميرا
        findViewById<Switch>(R.id.switchCamera).setOnCheckedChangeListener { _, checked ->
            useFront = checked
            tvStatus.text = if (useFront) "📷 كاميرا أمامية" else "📷 كاميرا خلفية"
            startPreview()
        }

        // التقاط
        findViewById<FloatingActionButton>(R.id.fabCapture).setOnClickListener {
            capturePhoto()
        }

        // التقاط تلقائي
        findViewById<MaterialButton>(R.id.btnAutoCapture).setOnClickListener {
            isAutoCapture = !isAutoCapture
            if (isAutoCapture) {
                startAutoCaptureService()
                Toast.makeText(this, "⏱ التقاط تلقائي بدأ", Toast.LENGTH_SHORT).show()
            } else {
                stopService(Intent(this, CameraCaptureService::class.java))
                Toast.makeText(this, "⏱ توقف التقاط التلقائي", Toast.LENGTH_SHORT).show()
            }
        }

        // المعرض
        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        // فحص الصلاحية
        checkPermissionAndPreview()
    }

    private fun checkPermissionAndPreview() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startPreview()
        } else {
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    // ── معاينة الكاميرا (Camera2) ──
    private fun startPreview() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = selectCamera(manager, useFront)

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        try {
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val surfaceView = findViewById<android.view.SurfaceView>(R.id.surfaceCamera)
                    val texture = SurfaceTexture(0)

                    val handler = Handler(Looper.getMainLooper())

                    texture.setOnFrameAvailableListener { } // noop
                    texture.setDefaultBufferSize(
                        1920, 1080
                    )

                    val surface = Surface(texture)
                    val request = camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW
                    )
                    request.addTarget(surface)

                    camera.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(request.build(), null, handler)
                                runOnUiThread {
                                    tvStatus.text =
                                        if (useFront) "📷 كاميرا أمامية (معاينة)"
                                        else "📷 كاميرا خلفية (معاينة)"
                                }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) {
                                runOnUiThread {
                                    tvStatus.text = "⚠️ فشل المعاينة"
                                }
                            }
                        }, handler
                    )
                }
                override fun onDisconnected(c: CameraDevice) { c.close() }
                override fun onError(c: CameraDevice, e: Int) {
                    runOnUiThread {
                        tvStatus.text = "❌ خطأ الكاميرا: $e"
                    }
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            tvStatus.text = "❌ لا يوجد إذن كاميرا"
        }
    }

    // ── التقاط صورة ──
    private fun capturePhoto() {
        val intent = Intent(this, CameraCaptureService::class.java).apply {
            putExtra(CameraCaptureService.EXTRA_USE_FRONT, useFront)
            putExtra("action", "single")
        }
        startForegroundService(intent)

        // تحديث العداد بعد تأخير
        Handler(Looper.getMainLooper()).postDelayed({
            countExistingPhotos()
            Toast.makeText(this, "📷 تم التقاط الصورة!", Toast.LENGTH_SHORT).show()
        }, 2000)
    }

    private fun startAutoCaptureService() {
        val intent = Intent(this, CameraCaptureService::class.java).apply {
            putExtra(CameraCaptureService.EXTRA_USE_FRONT, useFront)
            putExtra("action", "auto")
        }
        startForegroundService(intent)
    }

    // ── عدّ الصور ──
    private fun countExistingPhotos() {
        val dir = File(getExternalFilesDir(null), "RBH_Photos")
        photoCount = if (dir.exists()) dir.listFiles()?.size ?: 0 else 0
        tvCount.text = "📷 $photoCount صورة"
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
}
