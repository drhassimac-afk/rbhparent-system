package com.rbh.parental.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rbh.parental.R

class PinLockActivity : AppCompatActivity() {

    private var enteredPin = ""
    private lateinit var dotsContainer: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvError: TextView
    private var isNewPin = false
    private var firstPinEntry = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_lock)

        dotsContainer = findViewById(R.id.pinDots)
        tvTitle = findViewById(R.id.tvPinTitle)
        tvError = findViewById(R.id.tvPinError)

        isNewPin = intent.getBooleanExtra("new_pin", false)

        tvTitle.text = if (isNewPin) "أنشئ كلمة مرور جديدة (4-6 أرقام)" else "أدخل كلمة المرور"

        createPinDots(4)
        createKeypad()

        findViewById<TextView>(R.id.tvForgotPin).setOnClickListener {
            // إعادة تعيين
            getSharedPreferences("rbh_lock", Context.MODE_PRIVATE)
                .edit().remove("pin").apply()
            finish()
        }
    }

    private fun createPinDots(count: Int) {
        dotsContainer.removeAllViews()
        for (i in 0 until count) {
            val dot = TextView(this).apply {
                width = 40
                height = 40
                gravity = Gravity.CENTER
                textSize = 24f
                text = "○"
                setTextColor(Color.parseColor("#B3FFFFFF"))
                setPadding(8, 0, 8, 0)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots() {
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as TextView
            dot.text = if (i < enteredPin.length) "●" else "○"
            dot.setTextColor(if (i < enteredPin.length) Color.WHITE else Color.parseColor("#B3FFFFFF"))
        }
    }

    private fun createKeypad() {
        val keypad = findViewById<GridLayout>(R.id.keypad)
        val keys = arrayOf("1","2","3","4","5","6","7","8","9","","0","⌫")

        for (key in keys) {
            val tv = TextView(this).apply {
                text = if (key == "") "" else key
                textSize = 28f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 24)
                if (key.isNotEmpty()) {
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                    setOnClickListener { onKeyClick(key) }
                    isClickable = true
                } else {
                    isClickable = false
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 80
                height = 80
                setMargins(8, 8, 8, 8)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            tv.layoutParams = params
            keypad.addView(tv)
        }
    }

    private fun onKeyClick(key: String) {
        if (key == "⌫") {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.substring(0, enteredPin.length - 1)
                updateDots()
            }
            return
        }

        if (enteredPin.length >= 6) return
        enteredPin += key
        updateDots()

        val requiredLength = if (isNewPin) 4 else getStoredPin().length
        if (enteredPin.length >= requiredLength) {
            if (isNewPin) {
                if (firstPinEntry.isEmpty()) {
                    firstPinEntry = enteredPin
                    enteredPin = ""
                    updateDots()
                    tvTitle.text = "أعد إدخال كلمة المرور"
                } else if (enteredPin == firstPinEntry) {
                    getSharedPreferences("rbh_lock", Context.MODE_PRIVATE)
                        .edit().putString("pin", enteredPin).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showError("كلمتا المرور غير متطابقتين")
                    enteredPin = ""
                    firstPinEntry = ""
                    updateDots()
                    tvTitle.text = "أنشئ كلمة مرور جديدة"
                }
            } else {
                if (enteredPin == getStoredPin()) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showError("كلمة مرور خاطئة")
                    enteredPin = ""
                    updateDots()
                }
            }
        }
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
        tvError.postDelayed({ tvError.visibility = View.GONE }, 2000)
    }

    private fun getStoredPin(): String {
        return getSharedPreferences("rbh_lock", Context.MODE_PRIVATE)
            .getString("pin", "") ?: ""
    }
}
