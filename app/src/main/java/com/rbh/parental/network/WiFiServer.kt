package com.rbh.parental.network

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class WiFiServer(private val onMessage: (JSONObject) -> Unit) {

    companion object {
        const val TAG = "WiFiServer"
        var isRunning = false
    }

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<Socket>()
    private var port = 8989

    fun start(port: Int = 8989) {
        this.port = port
        isRunning = true

        Thread {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Listening on port $port")

                while (isRunning) {
                    val client: Socket = serverSocket!!.accept()
                    clients.add(client)
                    Log.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")
                    handleClient(client)
                }
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "Error: ${e.message}")
            }
        }.start()
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                var line: String? = reader.readLine()
                while (line != null && isRunning) {
                    val json = JSONObject(line)

                    // استقبال الصور/الصوتيات (base64)
                    val type = json.optString("type", "")
                    if (type == "photo" || type == "audio") {
                        handleBinaryMessage(json)
                    }

                    onMessage(json)
                    writer.println("""{"status":"ok"}""")

                    line = reader.readLine()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client error: ${e.message}")
            } finally {
                clients.remove(socket)
                try { socket.close() } catch (e: Exception) {}
            }
        }.start()
    }

    private fun handleBinaryMessage(json: JSONObject) {
        val type = json.optString("type")
        val data = json.optString("data", "")
        val timestamp = json.optLong("timestamp", System.currentTimeMillis())

        if (data.isNotEmpty()) {
            val bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
            saveFile(type, bytes, timestamp)
        }
    }

    private fun saveFile(type: String, bytes: ByteArray, timestamp: Long) {
        try {
            val dirName = if (type == "photo") "RBH_Received_Photos" else "RBH_Received_Audio"
            val ext = if (type == "photo") ".jpg" else ".m4a"
            val dir = android.content.ContextWrapper(null) // placeholder
        } catch (e: Exception) { }
    }

    // إرسال أمر للطفل
    fun sendCommand(command: String): Boolean {
        for (client in clients) {
            try {
                if (!client.isClosed) {
                    val writer = PrintWriter(client.getOutputStream(), true)
                    writer.println(command)
                    return true
                }
            } catch (e: Exception) { }
        }
        return false
    }

    fun stop() {
        isRunning = false
        clients.forEach { try { it.close() } catch (e: Exception) {} }
        clients.clear()
        try { serverSocket?.close() } catch (e: Exception) {}
    }

    fun getClientCount(): Int = clients.size
}
