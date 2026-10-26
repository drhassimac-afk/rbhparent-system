package com.rbh.parental.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rbh.parental.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val tvName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)

        ivLogo.alpha = 0f
        ivLogo.translationY = 100f
        tvName.alpha = 0f
        tvTagline.alpha = 0f

        ivLogo.animate()
            .alpha(1f).translationY(0f)
            .setDuration(800).start()

        tvName.animate()
            .alpha(1f).setStartDelay(400).setDuration(600).start()

        tvTagline.animate()
            .alpha(1f).setStartDelay(700).setDuration(600).start()

        Handler(Looper.getMainLooper()).postDelayed({
            val isFirst = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("first_launch", true)
            if (isFirst) {
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit().putBoolean("first_launch", false).apply()
                startActivity(Intent(this, PermissionActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 2500)
    }
}
