package com.rbh.child.network

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class WiFiClient(private val serverIP: String, private val serverPort: Int = 8989) {

    companion object {
        const val TAG = "WiFiClient"
        var isConnected = false
        var lastServerIP: String = ""
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    fun connect(callback: ((Boolean) -> Unit)? = null) {
        Thread {
            try {
                socket = Socket(serverIP, serverPort)
                socket?.soTimeout = 10000

                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                isConnected = true
                lastServerIP = serverIP
                Log.d(TAG, "Connected to $serverIP:$serverPort")
                callback?.invoke(true)

                // بدء استقبال الأوامر
                listenForCommands()

            } catch (e: Exception) {
                isConnected = false
                Log.e(TAG, "Connect error: ${e.message}")
                callback?.invoke(false)
            }
        }.start()
    }

    private fun listenForCommands() {
        Thread {
            try {
                while (isConnected) {
                    val line = reader?.readLine() ?: break
                    val json = JSONObject(line)
                    val command = json.optString("command", "")

                    Log.d(TAG, "Command: $command")
                    // معالجة الأوامر تلقائياً
                }
            } catch (e: Exception) {
                isConnected = false
                Log.e(TAG, "Listen error: ${e.message}")
            }
        }.start()
    }

    // إرسال بيانات
    fun send(data: JSONObject, callback: ((String) -> Unit)? = null) {
        Thread {
            try {
                if (socket == null || socket!!.isClosed) {
                    callback?.invoke("disconnected")
                    return@Thread
                }

                data.put("timestamp", System.currentTimeMillis())
                writer?.println(data.toString())

                val response = reader?.readLine()
                callback?.invoke(response ?: "ok")

                Log.d(TAG, "Sent: ${data.optString("type")}")
            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
                callback?.invoke("error")
            }
        }.start()
    }

    // إرسال صورة كـ Base64
    fun sendPhoto(filePath: String, callback: ((String) -> Unit)? = null) {
        Thread {
            try {
                val file = java.io.File(filePath)
                if (!file.exists()) { callback?.invoke("not_found"); return@Thread }

                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                val json = JSONObject().apply {
                    put("type", "photo")
                    put("data", base64)
                    put("filename", file.name)
                }
                send(json, callback)
            } catch (e: Exception) {
                callback?.invoke("error: ${e.message}")
            }
        }.start()
    }

    // إرسال صوت كـ Base64
    fun sendAudio(filePath: String, callback: ((String) -> Unit)? = null) {
        Thread {
            try {
                val file = java.io.File(filePath)
                if (!file.exists()) { callback?.invoke("not_found"); return@Thread }

                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                val json = JSONObject().apply {
                    put("type", "audio")
                    put("data", base64)
                    put("filename", file.name)
                }
                send(json, callback)
            } catch (e: Exception) {
                callback?.invoke("error: ${e.message}")
            }
        }.start()
    }

    fun sendLocation(lat: Double, lng: Double) {
        val json = JSONObject().apply {
            put("type", "location")
            put("lat", lat)
            put("lng", lng)
        }
        send(json)
    }

    fun sendUsage(map: Map<String, Long>) {
        val json = JSONObject().apply {
            put("type", "usage")
            val apps = JSONObject()
            for ((key, value) in map) {
                apps.put(key, value)
            }
            put("apps", apps)
        }
        send(json)
    }

    fun disconnect() {
        isConnected = false
        try { socket?.close() } catch (e: Exception) {}
        socket = null
    }
}
