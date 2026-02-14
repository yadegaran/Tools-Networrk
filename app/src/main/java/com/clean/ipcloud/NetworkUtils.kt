package com.clean.ipcloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.Collections

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


// دریافت آی‌پی داخلی گوشی
fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in Collections.list(interfaces)) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr.hostAddress.contains(".")) {
                    return addr.hostAddress
                }
            }
        }
    } catch (ex: Exception) {
    }
    return "نامشخص"
}

// دریافت آی‌پی عمومی (از طریق سرویس icanhazip)
suspend fun getPublicIp(): String {
    return withContext(Dispatchers.IO) {
        try {
            URL("https://icanhazip.com").readText().trim()
        } catch (e: Exception) {
            "عدم اتصال"
        }
    }
}

// تست هوشمند MTU با استفاده از دستور پینگ سیستم
suspend fun runMtuTest(target: String, onStep: (Int) -> Unit): String {
    return withContext(Dispatchers.IO) {
        var resultMtu = 500
        // تست از ۱۵۰۰ به پایین با گام‌های ۱۰تایی
        for (mtu in 1500 downTo 500 step 10) {
            onStep(mtu)
            try {
                // محاسبه سایز واقعی پکت (بدون هدر)
                val payloadSize = mtu - 28
                if (payloadSize < 0) continue

                // حذف -M do برای سازگاری بیشتر و استفاده از -W برای تایم‌اوت
                val process = Runtime.getRuntime().exec("ping -c 1 -s $payloadSize -W 1 $target")
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    resultMtu = mtu
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (resultMtu == 500) "1420 (Auto)" else resultMtu.toString()
    }
}

suspend fun calculateDownloadSpeed(url: String, onProgress: (Float) -> Unit): Double {
    return withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val connection = URL(url).openConnection()
            connection.connect()

            val fileSize = connection.contentLength
            val inputStream = connection.getInputStream()
            val buffer = ByteArray(1024)
            var bytesRead = 0
            var totalBytesRead = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                if (fileSize > 0) {
                    onProgress(totalBytesRead.toFloat() / fileSize)
                }
            }

            val endTime = System.currentTimeMillis()
            val durationInSeconds = (endTime - startTime) / 1000.0
            val speedMbps = (totalBytesRead * 8.0) / (durationInSeconds * 1024 * 1024)
            speedMbps
        } catch (e: Exception) {
            0.0
        }
    }
}

// تابع تست پکت لاس و جیتر
suspend fun runAdvancedPingTest(host: String): Triple<String, String, String> {
    return withContext(Dispatchers.IO) {
        val pings = mutableListOf<Long>()
        var lostPackets = 0
        val count = 10 // تست با ۱۰ پکت برای دقت بالا

        for (i in 1..count) {
            val start = System.currentTimeMillis()
            val isReached = try {
                val address = InetAddress.getByName(host)
                address.isReachable(800)
            } catch (e: Exception) {
                false
            }

            if (isReached) {
                pings.add(System.currentTimeMillis() - start)
            } else {
                lostPackets++
            }
        }

        val avgPing = if (pings.isNotEmpty()) "${pings.average().toInt()} ms" else "Timeout"
        val packetLoss = "${(lostPackets.toFloat() / count * 100).toInt()}%"

        // محاسبه جیتر (تغییرات پینگ)
        val jitter = if (pings.size > 1) {
            val diffs = mutableListOf<Long>()
            for (i in 0 until pings.size - 1) {
                diffs.add(Math.abs(pings[i] - pings[i + 1]))
            }
            "${diffs.average().toInt()} ms"
        } else "0 ms"

        Triple(avgPing, packetLoss, jitter)
    }
}