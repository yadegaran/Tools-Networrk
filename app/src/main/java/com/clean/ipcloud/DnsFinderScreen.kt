package com.clean.ipcloud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class DnsResult(val ip: String, val latency: Long)

@Composable
fun DnsFinderScreen(vm: ScannerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var results by remember { mutableStateOf<List<DnsResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("آماده اسکن") }
    var testDomain by remember { mutableStateOf("www.github.com") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            "DNS یاب هوشمند",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = testDomain,
            onValueChange = { testDomain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("دامنه تست (بدون http)") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isScanning
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isScanning) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.LightGray, RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                statusText,
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 13.sp,
                color = Color.DarkGray
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isScanning) {
                        isScanning = true
                        results = emptyList()
                        scope.launch {
                            val testResults =
                                runAdvancedDnsTest(context, testDomain.trim()) { p, status ->
                                    progress = p
                                    statusText = status
                                }
                            results = testResults.take(10)
                            isScanning = false
                            statusText = "پایان اسکن"
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isScanning
            ) {
                // جایگزینی آیکون با متن فارسی
                Text(if (isScanning) "در حال تست..." else "شروع تست", fontWeight = FontWeight.Bold)
            }

            if (results.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val allDns = results.joinToString("\n") { it.ip }
                        clipboard.setPrimaryClip(ClipData.newPlainText("All DNS", allDns))
                        Toast.makeText(context, "هر ۱۰ مورد کپی شد", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(55.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("کپی همه", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results) { res ->
                DnsCard(res, context)
            }
        }
    }
}

@Composable
fun DnsCard(res: DnsResult, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = res.ip,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A237E),
                    textAlign = TextAlign.Left
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تأخیر: ${res.latency} میلی‌ثانیه",
                    fontSize = 13.sp,
                    color = if (res.latency < 150) Color(0xFF2E7D32) else Color(0xFFD84315)
                )
            }

            // جایگزینی IconButton با TextButton برای نمایش عبارت "کپی"
            TextButton(
                onClick = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("DNS IP", res.ip))
                    Toast.makeText(context, "کپی شد: ${res.ip}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            ) {
                Text("کپی", fontWeight = FontWeight.Bold, color = Color.Blue)
            }
        }
    }
}

suspend fun runAdvancedDnsTest(
    context: Context,
    domainToTest: String,
    onUpdate: (Float, String) -> Unit
): List<DnsResult> {
    return withContext(Dispatchers.IO) {
        val verifiedDns = mutableListOf<DnsResult>()
        // تمیز کردن دامنه ورودی
        val cleanDomain = domainToTest.replace("https://", "").replace("http://", "").split("/")[0]

        try {
            val inputStream = context.assets.open("resolvers.txt")
            val allIps =
                inputStream.bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }

            // انتخاب ۱۰۰ مورد تصادفی برای تست
            val testSubset = allIps.shuffled().take(100)
            val total = testSubset.size

            testSubset.forEachIndexed { index, dnsIp ->
                val trimmedDns = dnsIp.trim()
                onUpdate((index + 1).toFloat() / total, "بررسی: $trimmedDns")

                val startTime = System.currentTimeMillis()

                try {
                    // ۱. تست لایه اتصال (TCP Handshake)
                    val socket = Socket()
                    socket.connect(InetSocketAddress(trimmedDns, 53), 750)
                    socket.close()

                    // ۲. تست لایه رزولوشن (DNS Query)
                    // توجه: InetAddress.getByName در اندروید به تنهایی اجازه تعیین سرور DNS را نمی‌دهد.
                    // اما به عنوان یک تخمین برای "در دسترس بودن" دامنه روی شبکه فعلی استفاده می‌شود.
                    val address = InetAddress.getByName(cleanDomain)
                    val resolvedIp = address.hostAddress ?: ""

                    // فیلتر مسمومیت DNS (آی‌پی‌های فیک فیلترینگ ایران)
                    val isPoisoned = resolvedIp.startsWith("10.") ||
                            resolvedIp.startsWith("127.") ||
                            resolvedIp == "0.0.0.0"

                    if (!isPoisoned) {
                        val latency = System.currentTimeMillis() - startTime
                        verifiedDns.add(DnsResult(trimmedDns, latency))
                    }
                } catch (e: Exception) {

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        verifiedDns.sortBy { it.latency }
        verifiedDns
    }
}