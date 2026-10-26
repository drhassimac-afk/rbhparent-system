package com.rbh.parental.network

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class FirebaseSync(private val context: Context) {

    companion object {
        const val TAG = "FirebaseSync"
        // ضع مفتاح مشروع Firebase هنا بعد إنشائه
        const val FIREBASE_URL = "https://rbhparent-default-rtdb.firebaseio.com"
        const val API_KEY = "PUT_YOUR_API_KEY_HERE"
    }

    // ══════ إرسال بيانات ══════
    fun sendData(path: String, data: JSONObject, callback: ((Boolean) -> Unit)? = null) {
        Thread {
            try {
                val url = URL("$FIREBASE_URL/$path.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(data.toString())
                }

                val code = conn.responseCode
                val success = code in 200..299
                Log.d(TAG, "Send $path: $code")
                callback?.invoke(success)
                conn.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
                callback?.invoke(false)
            }
        }.start()
    }

    // ══════ استلام بيانات ══════
    fun getData(path: String, callback: (JSONObject?) -> Unit) {
        Thread {
            try {
                val url = URL("$FIREBASE_URL/$path.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                callback(json)
                conn.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Get error: ${e.message}")
                callback(null)
            }
        }.start()
    }

    // ══════ إرسال موقع ══════
    fun sendLocation(lat: Double, lng: Double) {
        val data = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            put("time", System.currentTimeMillis())
        }
        sendData("children/location", data)
    }

    // ══════ إرسال استخدام التطبيقات ══════
    fun sendUsage(apps: Map<String, Long>) {
        val appsObj = JSONObject()
        for ((key, value) in apps) {
            appsObj.put(key, value)
        }
        val data = JSONObject().apply {
            put("apps", appsObj)
            put("time", System.currentTimeMillis())
        }
        sendData("children/usage", data)
    }

    // ══════ إرسال صورة (base64) ══════
    fun sendPhoto(filePath: String) {
        Thread {
            try {
                val file = File(filePath)
                if (!file.exists()) return@Thread

                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(
                    bytes, android.util.Base64.NO_WRAP
                )

                val data = JSONObject().apply {
                    put("type", "photo")
                    put("filename", file.name)
                    put("data", base64)
                    put("time", System.currentTimeMillis())
                }
                sendData("children/photos/${file.nameWithoutExtension}", data)
            } catch (e: Exception) {
                Log.e(TAG, "Photo sync error: ${e.message}")
            }
        }.start()
    }

    // ══════ إرسال إشعار ══════
    fun sendNotification(title: String, body: String) {
        val data = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("time", System.currentTimeMillis())
        }
        sendData("children/notifications", data)
    }
}
