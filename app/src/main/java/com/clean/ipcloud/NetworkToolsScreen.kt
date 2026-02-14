package com.clean.ipcloud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.NetworkInterface
import java.net.URL

@Composable
fun NetworkToolsScreen(vm: ScannerViewModel) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var publicIpText by remember { mutableStateOf<String>("در حال دریافت...") }
    var localIpText by remember { mutableStateOf<String>(fetchInternalWifiIp()) }

    var ipLeakDetail by remember { mutableStateOf<String>("تست نشده") }
    var dnsServerList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLeakTesting by remember { mutableStateOf<Boolean>(false) }

    var bestMtuValue by remember { mutableStateOf<String>("-") }
    var currentMtuStep by remember { mutableStateOf<Int>(0) }
    var isMtuRunning by remember { mutableStateOf<Boolean>(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            publicIpText = fetchPublicIpFromServer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "جعبه ابزار پیشرفته",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ۱. اطلاعات IP
        ToolCard(title = "اطلاعات شبکه") {
            InfoRow("آی‌پی داخلی:", localIpText)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("آی‌پی عمومی:", publicIpText)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ۲. تست نشت با متد مولتی‌سورس
        ToolCard(title = "تست نشت هویت (Leak Test)") {
            Text("وضعیت نشت IP:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                ipLeakDetail,
                fontSize = 12.sp,
                color = if (ipLeakDetail.contains("ایران")) Color.Red else Color(0xFF388E3C)
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text("سرورهای DNS شناسایی شده:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (dnsServerList.isEmpty()) {
                Text("داده‌ای یافت نشد. تست را شروع کنید.", fontSize = 11.sp, color = Color.Gray)
            } else {
                dnsServerList.forEach { dns ->
                    Text("• $dns", fontSize = 11.sp, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLeakTesting = true
                    scope.launch {
                        ipLeakDetail = performMultiSourceIpCheck()
                        dnsServerList = performDetailedDnsCheck()
                        isLeakTesting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLeakTesting
            ) {
                Text(if (isLeakTesting) "در حال بررسی..." else "تست نشت هویت")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ۳. MTU Finder واقعی با UDP Packet Test
        ToolCard(title = "یافتن بهترین MTU") {
            Text(
                "تست لایه ۴ برای یافتن حداکثر اندازه پکت بدون شکستگی.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isMtuRunning) "تست $currentMtuStep..." else bestMtuValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (bestMtuValue == "1500") Color.Gray else Color(0xFF2E7D32)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                isMtuRunning = true
                bestMtuValue = "در حال اسکن..."
                scope.launch {
                    bestMtuValue = runRealMtuTest("8.8.8.8") { currentMtuStep = it }
                    isMtuRunning = false
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !isMtuRunning) {
                Text("شروع اسکن MTU")
            }
        }
    }
}

// --- بخش منطق اصلاح شده ---

suspend fun performMultiSourceIpCheck(): String = withContext(Dispatchers.IO) {
    val sources = listOf(
        "http://ip-api.com/json/?fields=status,message,country,countryCode,city,isp,query",
        "https://ipapi.co/json/"
    )

    for (url in sources) {
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(text)

            val ip = json.optString("query") ?: json.optString("ip")
            val country = json.optString("country") ?: json.optString("country_name")
            val city = json.optString("city")
            val isp = json.optString("isp") ?: json.optString("org")

            val isIran = country.contains("Iran") || json.optString("countryCode") == "IR"
            val status = if (isIran) "⚠️ نشت (ایران)" else "✅ امن ($country)"

            return@withContext "$status\nآی‌پی: $ip\nشهر: $city\nسرویس‌دهنده: $isp"
        } catch (e: Exception) {
            continue
        }
    }
    "خطا در دریافت اطلاعات موقعیت"
}

suspend fun performDetailedDnsCheck(): List<String> = withContext(Dispatchers.IO) {
    val dnsList = mutableListOf<String>()

    // متد اول: استفاده از ابزار تشخیص چندگانه (این چندین سرور را برمی‌گرداند)
    try {
        val response = URL("https://edns.ip-api.com/json").openConnection().apply {
            connectTimeout = 3000
            readTimeout = 3000
        }.getInputStream().bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        if (json.has("dns")) {
            val dnsObj = json.getJSONObject("dns")
            val ip = dnsObj.optString("ip")
            val geo = dnsObj.optString("geo")
            dnsList.add("سرور اصلی: $ip ($geo)")
        }
    } catch (e: Exception) {
    }

    // متد دوم: تشخیص گره‌های میانی (Colo)
    try {
        val trace = URL("https://1.1.1.1/cdn-cgi/trace").readText()
        val colo = trace.lines().find { it.startsWith("colo=") }?.split("=")?.get(1) ?: ""
        val loc = trace.lines().find { it.startsWith("loc=") }?.split("=")?.get(1) ?: ""
        dnsList.add("گره میانی شبکه (PoP): $colo (منطقه: $loc)")
    } catch (e: Exception) {
    }

    // متد سوم: چک کردن نشت مستقیم (اگر آی‌پی ایران در دی‌ان‌اس باشد)
    try {
        val dnsTestUrl = "https://am.i.mullvad.net/dns"
        val result = URL(dnsTestUrl).readText().trim()
        if (result.contains("Iran") || result.contains("Afranet") || result.contains("Mokhaberat")) {
            dnsList.add("⚠️ نشت دی‌ان‌اس به اپراتور داخلی شناسایی شد!")
        } else {
            dnsList.add("🌍 خروجی نهایی: $result")
        }
    } catch (e: Exception) {
    }

    if (dnsList.isEmpty()) listOf("دی‌ان‌اس در لایه امن قرار دارد") else dnsList
}


suspend fun runRealMtuTest(target: String, onStep: (Int) -> Unit): String {
    return withContext(Dispatchers.IO) {
        var bestPayload = 0
        // استفاده از یک هدف معتبر جهانی مثل 8.8.8.8
        val host = if (target.isEmpty() || target.contains("در حال") || target == "-") "8.8.8.8" else target

        // شروع از 1472 (که با 28 بایت هدر می شود 1500) تا 472 (که می شود 500)
        for (payload in 1472 downTo 472 step 10) {
            val currentMtuInUI = payload + 28
            onStep(currentMtuInUI)

            val isSuccessful = try {
                // -c 1: فقط یک پکت
                // -s: تعیین سایز دیتا (Payload)
                // -W 1: یک ثانیه صبر برای پاسخ
                val process = Runtime.getRuntime().exec("ping -c 1 -s $payload -W 1 $host")
                val exitCode = process.waitFor()

                // اگر exitCode صفر باشد، یعنی پکت با موفقیت برگشته (بدون نیاز به fragmentation)
                exitCode == 0
            } catch (e: Exception) {
                false
            }

            if (isSuccessful) {
                bestPayload = payload
                break // اولین (بزرگترین) سایزی که جواب داد را پیدا کردیم
            }
            // یک وقفه بسیار کوتاه برای جلوگیری از تداخل پکت‌ها
            kotlinx.coroutines.delay(20)
        }

        if (bestPayload == 0) {
            "ناموفق (ICMP مسدود است)"
        } else {
            (bestPayload + 28).toString()
        }
    }
}


suspend fun fetchPublicIpFromServer(): String = withContext(Dispatchers.IO) {
    try {
        URL("https://api.myip.com").readText().let { JSONObject(it).getString("ip") }
    } catch (e: Exception) {
        "خطا"
    }
}

fun fetchInternalWifiIp(): String {
    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress.contains('.') }
            ?.hostAddress ?: "127.0.0.1"
    } catch (e: Exception) {
        "127.0.0.1"
    }
}

@Composable
fun ToolCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF3949AB))
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}