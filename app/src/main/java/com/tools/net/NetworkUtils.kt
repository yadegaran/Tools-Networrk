package com.tools.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit

object NetworkUtils {

    /**
     * تمام درخواست‌های یک هاست را به IP مشخص‌شده هدایت می‌کند تا بتوانیم
     * با همان نام میزبان (و SNI صحیح) مستقیماً به آی‌پی مدنظر کانکت شویم.
     */
    private class FixedIpDns(private val ip: String) : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            listOf(InetAddress.getByName(ip))
    }

    private fun speedTestClient(ip: String, timeoutSec: Long): OkHttpClient =
        OkHttpClient.Builder()
            .dns(FixedIpDns(ip))
            .connectTimeout(timeoutSec, TimeUnit.SECONDS)
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
            .writeTimeout(timeoutSec, TimeUnit.SECONDS)
            .build()

    /** سرعت دانلود تقریبی (Mbps) از طریق آی‌پی مشخص با اسپوفینگ Host روی سرویس کلودفلر. */
    suspend fun measureDownloadMbps(ip: String, maxDurationMs: Long = 3500): Double =
        withContext(Dispatchers.IO) {
            try {
                val client = speedTestClient(ip, 4)
                val request = Request.Builder()
                    .url("https://speed.cloudflare.com/__down?bytes=10000000")
                    .build()
                val start = System.currentTimeMillis()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext 0.0
                    var total = 0L
                    response.body?.byteStream()?.use { stream ->
                        val buffer = ByteArray(16384)
                        while (true) {
                            val read = stream.read(buffer)
                            if (read == -1) break
                            total += read
                            if (System.currentTimeMillis() - start > maxDurationMs) break
                        }
                    }
                    val sec = (System.currentTimeMillis() - start) / 1000.0
                    if (sec <= 0 || total <= 0) 0.0 else (total * 8.0) / (sec * 1024 * 1024)
                }
            } catch (e: Exception) {
                0.0
            }
        }

    /** سرعت آپلود تقریبی (Mbps) از طریق آی‌پی مشخص با اسپوفینگ Host روی سرویس کلودفلر. */
    suspend fun measureUploadMbps(ip: String, payloadBytes: Int = 3_000_000): Double =
        withContext(Dispatchers.IO) {
            try {
                val client = speedTestClient(ip, 4)
                val data = ByteArray(payloadBytes)
                val body = data.toRequestBody("application/octet-stream".toMediaType())
                val request = Request.Builder()
                    .url("https://speed.cloudflare.com/__up")
                    .post(body)
                    .build()
                val start = System.currentTimeMillis()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext 0.0
                    val sec = (System.currentTimeMillis() - start) / 1000.0
                    if (sec <= 0) 0.0 else (payloadBytes * 8.0) / (sec * 1024 * 1024)
                }
            } catch (e: Exception) {
                0.0
            }
        }


    suspend fun fetchCloudflareInfo(ip: String): Pair<String, String> =
        withContext(Dispatchers.IO) {
            try {
                // استفاده از آدرس مستقیم کلاودفلر برای تست سریع
                val url = URL("http://$ip/cdn-cgi/trace")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1000
                conn.readTimeout = 1000
                conn.setRequestProperty("Host", "browserleaks.com") // ترفند برای عبور از بلاک

                val text = try {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
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
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 1500)
                socket.soTimeout = 1500
                val output = socket.getOutputStream()
                val input = socket.getInputStream()

                // ارسال یک درخواست بسیار سبک
                output.write("GET /cdn-cgi/trace HTTP/1.1\r\nHost: cloudflare.com\r\n\r\n".toByteArray())

                val buffer = ByteArray(1024)
                val bytesRead = input.read(buffer)

                if (bytesRead > 0) "تبادل موفق" else "بدون پاسخ"
            }
        } catch (e: Exception) {
            "خطای تبادل"
        }
    }
}

