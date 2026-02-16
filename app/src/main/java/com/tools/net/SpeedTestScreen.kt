package com.tools.net

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

@Composable
fun SpeedTestScreen(vm: ScannerViewModel) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // اطلاعات هویتی
    var ipInfoText by remember { mutableStateOf("در حال دریافت...") }
    var locationText by remember { mutableStateOf("در حال شناسایی...") }

    // مقادیر تست
    var isTesting by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf(0.0) }
    var uploadSpeed by remember { mutableStateOf(0.0) }
    var pingValue by remember { mutableStateOf("-") }
    var jitterValue by remember { mutableStateOf("-") }
    var lossValue by remember { mutableStateOf("-") }

    // متد v2ray برای دریافت سریع اطلاعات شبکه
    fun refreshNetworkInfo() {
        scope.launch(Dispatchers.IO) {
            ipInfoText = "در حال بروزرسانی..."
            val providers =
                listOf("https://ipapi.co/json/", "https://api.myip.com", "https://ip-api.com/json")
            providers.forEach { url ->
                launch {
                    try {
                        val conn = URL(url).openConnection() as HttpURLConnection
                        conn.connectTimeout = 3000
                        val res = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(res)
                        val ip =
                            if (json.has("ip")) json.getString("ip") else json.getString("query")
                        val city = if (json.has("city")) json.getString("city") else ""
                        val country =
                            if (json.has("country")) json.getString("country") else json.optString("country_name")

                        ipInfoText = ip
                        locationText = "$city, $country".trimStart(',', ' ')
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { refreshNetworkInfo() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("آنالیز پیشرفته شبکه", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { refreshNetworkInfo() }) { Icon(Icons.Default.Refresh, null) }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F4))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("آی‌پی: $ipInfoText", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("موقعیت: $locationText", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SpeedGauge(downloadSpeed.toFloat())
        Spacer(modifier = Modifier.height(20.dp))

        // بخش پینگ، جیتر و پکت‌لاست (مشابه v2ray)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusItem("Ping", pingValue, "ms")
                StatusItem("Jitter", jitterValue, "ms")
                StatusItem("Loss", lossValue, "%")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // نتایج دانلود و آپلود
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultCard("Download", downloadSpeed, Color(0xFF4CAF50), Modifier.weight(1f))
            ResultCard("Upload", uploadSpeed, Color(0xFF2196F3), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isTesting = true
                scope.launch {
                    // ۱. تست دقیق پینگ، جیتر و پکت لاست
                    val netStats = runDetailedNetworkStats("1.1.1.1")
                    pingValue = netStats.first
                    jitterValue = netStats.second
                    lossValue = netStats.third

                    // ۲. تست دانلود موازی (Cloudflare Style)
                    runParallelDownload { downloadSpeed = it }

                    // ۳. تست آپلود موازی
                    runParallelUpload { uploadSpeed = it }

                    isTesting = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isTesting,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isTesting) "در حال آنالیز..." else "شروع تست سرعت")
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text("$value $unit", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ResultCard(label: String, value: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = color)
            Text(
                String.format("%.1f", value),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text("Mbps", fontSize = 10.sp, color = color)
        }
    }
}

@Composable
fun SpeedGauge(speed: Float) {
    val animatedSpeed by animateFloatAsState(targetValue = speed, animationSpec = tween(500))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(modifier = Modifier.size(180.dp)) {
            drawArc(
                Color.LightGray.copy(0.2f),
                135f,
                270f,
                false,
                style = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                Color(0xFF6200EE),
                135f,
                (animatedSpeed.coerceIn(0f, 100f) / 100f) * 270f,
                false,
                style = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text("${animatedSpeed.toInt()}", fontSize = 44.sp, fontWeight = FontWeight.Black)
    }
}

suspend fun runDetailedNetworkStats(host: String): Triple<String, String, String> =
    withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Long>()
        var lost = 0
        val samples = 6 // تعداد نمونه برای سرعت بیشتر

        for (i in 1..samples) {
            val start = System.currentTimeMillis()
            val success = try {
                // استفاده از درخواست واقعی برای دور زدن کش فیلترشکن
                val conn =
                    URL("https://1.1.1.1/cdn-cgi/trace").openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.inputStream.read()
                conn.disconnect()
                true
            } catch (e: Exception) {
                false
            }

            if (success) {
                val diff = System.currentTimeMillis() - start
                latencies.add(diff)
            } else {
                lost++
            }
            delay(100) // وقفه بین تست‌ها برای دقت جیتر
        }

        val avgPing = if (latencies.isNotEmpty()) latencies.average().toInt().toString() else "-"

        // محاسبه جیتر (تغییرات پینگ)
        val jitter = if (latencies.size > 1) {
            var sumDiff = 0L
            for (i in 0 until latencies.size - 1) {
                sumDiff += kotlin.math.abs(latencies[i] - latencies[i + 1])
            }
            (sumDiff / (latencies.size - 1)).toString()
        } else "0"

        val loss = ((lost * 100) / samples).toString()

        Triple(avgPing, jitter, loss)
    }

// متد دانلود موازی برای سرعت واقعی
suspend fun runParallelDownload(onUpdate: (Double) -> Unit) = withContext(Dispatchers.IO) {
    val totalBytes = AtomicLong(0)
    val start = System.currentTimeMillis()
    val jobs = List(6) {
        launch {
            try {
                val conn =
                    URL("https://speed.cloudflare.com/__down?bytes=20000000").openConnection()
                val inputStream = conn.inputStream
                val buffer = ByteArray(16384)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    totalBytes.addAndGet(read.toLong())
                    if (System.currentTimeMillis() - start > 8000) break
                }
                inputStream.close()
            } catch (e: Exception) {
            }
        }
    }
    while (System.currentTimeMillis() - start < 8000) {
        val sec = (System.currentTimeMillis() - start) / 1000.0
        if (sec > 0) withContext(Dispatchers.Main) { onUpdate((totalBytes.get() * 8.0) / (sec * 1024 * 1024)) }
        delay(300)
    }
    jobs.forEach { it.cancel() }
}

// متد آپلود موازی
suspend fun runParallelUpload(onUpdate: (Double) -> Unit) = withContext(Dispatchers.IO) {
    val totalBytes = AtomicLong(0)
    val start = System.currentTimeMillis()
    val testDuration = 7000L

    // استفاده از سرورهای مختلف برای دور زدن محدودیت‌های اپراتور
    val uploadTargets = listOf(
        "https://storage.googleapis.com",
        "https://www.google.com/upload",
        "https://httpbin.org/post"
    )

    val jobs = List(3) { index ->
        launch {
            try {
                // ایجاد یک آرایه بایت واقعی‌تر (نه فقط صفر)
                val data = ByteArray(1024 * 64)
                java.util.Random().nextBytes(data)

                while (System.currentTimeMillis() - start < testDuration) {
                    val url = URL(uploadTargets[index % uploadTargets.size])
                    val conn = url.openConnection() as HttpURLConnection
                    conn.doOutput = true
                    conn.requestMethod = "POST"
                    conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    conn.setConnectTimeout(3000)

                    // استفاده از FixedLength برای اینکه اندروید مجبور شود دیتا را بفرستد
                    conn.setFixedLengthStreamingMode(data.size)

                    try {
                        conn.outputStream.use { os ->
                            os.write(data)
                            os.flush()
                        }
                        // حتی اگر سرور خطا بدهد (مثل 404 یا 405)، دیتای ما از گوشی خارج شده است
                        // پس پهنای باند مصرف شده و باید حساب شود
                        totalBytes.addAndGet(data.size.toLong())

                        // یک وقفه بسیار کوتاه برای جلوگیری از بلاک شدن توسط فیلترشکن
                        delay(10)
                    } catch (e: Exception) {
                        delay(100)
                    } finally {
                        conn.disconnect()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    // مانیتورینگ زنده
    while (System.currentTimeMillis() - start < testDuration) {
        val sec = (System.currentTimeMillis() - start) / 1000.0
        if (sec > 0) {
            val currentMbps = (totalBytes.get() * 8.0) / (sec * 1024 * 1024)
            withContext(Dispatchers.Main) { onUpdate(currentMbps) }
        }
        delay(200)
    }
    jobs.forEach { it.cancel() }
}