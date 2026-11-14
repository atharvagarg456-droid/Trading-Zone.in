package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight local HTTP server that serves `assets/apple_webapp.html` over standard HTTP
 * so Google Chrome and Apple Safari on the device or same Wi-Fi/Hotspot network can open
 * the full standalone website without `file://` or 404 restrictions.
 */
object LocalWebAppServer {
    private const val DEFAULT_PORT = 8765
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var activePort: Int = DEFAULT_PORT

    fun isServerRunning(): Boolean = isRunning.get()

    fun getActivePort(): Int = activePort

    fun startServer(context: Context, port: Int = DEFAULT_PORT): String {
        if (isRunning.get()) {
            return getLocalServerUrl()
        }
        val appContext = context.applicationContext
        activePort = port
        executor.execute {
            try {
                val socket = ServerSocket(port)
                serverSocket = socket
                isRunning.set(true)
                while (isRunning.get() && !socket.isClosed) {
                    val client = try {
                        socket.accept()
                    } catch (_: Exception) {
                        break
                    }
                    executor.execute {
                        handleClient(appContext, client)
                    }
                }
            } catch (_: Exception) {
                isRunning.set(false)
            }
        }
        return getLocalServerUrl()
    }

    fun stopServer() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private fun handleClient(context: Context, client: Socket) {
        try {
            client.soTimeout = 5000
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val requestLine = reader.readLine() ?: ""
            while (true) {
                val header = reader.readLine()
                if (header.isNullOrEmpty()) break
            }

            val htmlBytes = context.assets.open("apple_webapp.html").use { it.readBytes() }
            val output: OutputStream = client.getOutputStream()
            val responseHeaders = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/html; charset=utf-8\r\n")
                append("Content-Length: ${htmlBytes.size}\r\n")
                append("Cache-Control: no-store, no-cache, must-revalidate\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            output.write(responseHeaders.toByteArray(Charsets.UTF_8))
            if (!requestLine.startsWith("HEAD")) {
                output.write(htmlBytes)
            }
            output.flush()
        } catch (_: Exception) {
        } finally {
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
    }

    fun getDeviceLanIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("169.254")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "127.0.0.1"
    }

    fun getLocalServerUrl(): String {
        val ip = getDeviceLanIpAddress()
        return "http://$ip:$activePort"
    }

    fun getLoopbackUrl(): String {
        return "http://127.0.0.1:$activePort"
    }

    fun openInChromeOrBrowser(context: Context, url: String) {
        try {
            val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.android.chrome")
            }
            context.startActivity(chromeIntent)
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {
            }
        }
    }
}
