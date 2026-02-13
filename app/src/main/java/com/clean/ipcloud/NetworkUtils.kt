package com.clean.ipcloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object NetworkUtils {


    suspend fun fetchCloudflareInfo(ip: String): Pair<String, String> =
        withContext(Dispatchers.IO) {
            try {
                // استفاده از آدرس مستقیم کلاودفلر برای تست سریع
                val url = URL("http://$ip/cdn-cgi/trace")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1000
                conn.readTimeout = 1000
                conn.setRequestProperty("Host", "browserleaks.com") // ترفند برای عبور از بلاک

                val text = conn.inputStream.bufferedReader().readText()
                val colo =
                    text.lineSequence().firstOrNull { it.startsWith("colo=") }?.split("=")?.get(1)
                        ?: "N/A"
                val loc =
                    text.lineSequence().firstOrNull { it.startsWith("loc=") }?.split("=")?.get(1)
                        ?: "??"

                Pair(colo, loc)
            } catch (e: Exception) {
                Pair("Timeout", "??")
            }
        }

    suspend fun checkDataExchange(ip: String, port: Int): String = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 1500)
            socket.soTimeout = 1500
            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            // ارسال یک درخواست بسیار سبک
            output.write("GET /cdn-cgi/trace HTTP/1.1\r\nHost: cloudflare.com\r\n\r\n".toByteArray())

            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)

            socket.close()
            if (bytesRead > 0) "تبادل موفق" else "بدون پاسخ"
        } catch (e: Exception) {
            "خطای تبادل"
        }
    }
}

