package com.rbh.parental.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.rbh.parental.R

class EmergencyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlertDialog.Builder(this)
            .setTitle("🚨 طوارئ")
            .setMessage("تم تفعيل زر الطوارئ!\n\nسيتم إرسال موقعك للوالد")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()

        Toast.makeText(this, "SOS مُفعّل!", Toast.LENGTH_LONG).show()
    }
}
