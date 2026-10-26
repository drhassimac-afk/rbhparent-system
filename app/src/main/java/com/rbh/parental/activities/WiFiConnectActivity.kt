package com.rbh.parental.activities

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rbh.parental.R
import com.rbh.parental.network.WiFiServer
import java.net.Inet4Address
import java.net.NetworkInterface

class WiFiConnectActivity : AppCompatActivity() {

    private var server: WiFiServer? = null
    private var isRunning = false
    private lateinit var tvStatus: TextView
    private lateinit var tvIP: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_connect)

        tvStatus = findViewById(R.id.tvServerStatus)
        tvIP = findViewById(R.id.tvIP)
        tvLog = findViewById(R.id.tvLog)
        btnToggle = findViewById(R.id.btnToggleServer)

        tvIP.text = "IP: ${getLocalIP()}"

        btnToggle.setOnClickListener { toggleServer() }
    }

    private fun toggleServer() {
        if (isRunning) {
            server?.stop()
            isRunning = false
            tvStatus.text = "Server: OFF"
            btnToggle.text = "▶ تشغيل السيرفر"
        } else {
            server = WiFiServer { json ->
                Handler(Looper.getMainLooper()).post {
                    val msg = "📥 ${json.optString("type", "?")}: ${json.toString()}\n\n"
                    tvLog.append(msg)
                    saveMessage(json)
                }
            }
            server?.start(8989)
            isRunning = true
            tvStatus.text = "Server: ON ✅"
            btnToggle.text = "⏹ إيقاف السيرفر"
            tvLog.append("✅ Server started on port 8989\n\n")
        }
    }

    private fun saveMessage(json: org.json.JSONObject) {
        val prefs = getSharedPreferences("wifi_messages", Context.MODE_PRIVATE)
        val key = "msg_${System.currentTimeMillis()}"
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun getLocalIP(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                val addresses = network.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) { }
        return "غير متاح"
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }
}
