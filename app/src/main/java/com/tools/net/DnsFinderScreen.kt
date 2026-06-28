package com.tools.net

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tools.net.ui.components.GlassCard
import com.tools.net.ui.theme.ErrorRed
import com.tools.net.ui.theme.SuccessGreen
import com.tools.net.ui.components.HelpCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

data class DnsResult(val ip: String, val latency: Long)

@Composable
fun DnsFinderScreen(vm: ScannerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var results by remember { mutableStateOf<List<DnsResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf(context.getString(R.string.dns_status_ready)) }
    var testDomain by remember { mutableStateOf("www.github.com") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            stringResource(R.string.dns_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))
        HelpCard(stringResource(R.string.help_dns))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = testDomain,
            onValueChange = { testDomain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dns_domain_label)) },
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
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                statusText,
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            statusText = context.getString(R.string.dns_status_done)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isScanning
            ) {
                Text(
                    if (isScanning) stringResource(R.string.dns_action_running) else stringResource(R.string.dns_action_start),
                    fontWeight = FontWeight.Bold
                )
            }

            if (results.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val allDns = results.joinToString("\n") { it.ip }
                        clipboard.setPrimaryClip(ClipData.newPlainText("All DNS", allDns))
                        Toast.makeText(context, context.getString(R.string.dns_copy_all_done), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(55.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.dns_copy_all_action), fontWeight = FontWeight.Bold)
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

    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dns_latency_label, res.latency),
                    fontSize = 13.sp,
                    color = if (res.latency < 150) SuccessGreen else ErrorRed
                )
            }

            TextButton(
                onClick = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("DNS IP", res.ip))
                    Toast.makeText(context, context.getString(R.string.dns_copy_done, res.ip), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            ) {
                Text(stringResource(R.string.dns_copy_action), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                onUpdate((index + 1).toFloat() / total, context.getString(R.string.dns_status_checking, trimmedDns))

                // کوئری DNS واقعی (UDP) مستقیماً از همین سرور - نه از resolver سیستم
                val latency = queryDnsServer(trimmedDns, cleanDomain, timeoutMs = 1200)
                if (latency != null) {
                    verifiedDns.add(DnsResult(trimmedDns, latency))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        verifiedDns.sortBy { it.latency }
        verifiedDns
    }
}

/** یک پکت کوئری DNS استاندارد (نوع A) برای دامنه مشخص می‌سازد. */
private fun buildDnsQuery(domain: String, transactionId: Int): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(transactionId ushr 8); out.write(transactionId and 0xFF) // Transaction ID
    out.write(0x01); out.write(0x00) // Flags: کوئری استاندارد + recursion desired
    out.write(0x00); out.write(0x01) // QDCOUNT = 1
    out.write(0x00); out.write(0x00) // ANCOUNT = 0
    out.write(0x00); out.write(0x00) // NSCOUNT = 0
    out.write(0x00); out.write(0x00) // ARCOUNT = 0
    domain.split(".").forEach { label ->
        if (label.isNotEmpty()) {
            out.write(label.length)
            out.write(label.toByteArray(Charsets.US_ASCII))
        }
    }
    out.write(0x00) // پایان QNAME
    out.write(0x00); out.write(0x01) // QTYPE = A
    out.write(0x00); out.write(0x01) // QCLASS = IN
    return out.toByteArray()
}

/**
 * یک کوئری DNS واقعی روی UDP پورت ۵۳ به سرور مشخص می‌فرستد و در صورت پاسخ معتبر
 * (همان Transaction ID، بدون خطا و حداقل یک رکورد پاسخ)، تاخیر رفت‌وبرگشت را برمی‌گرداند.
 */
private suspend fun queryDnsServer(dnsIp: String, domain: String, timeoutMs: Int): Long? =
    withContext(Dispatchers.IO) {
        try {
            val transactionId = Random.nextInt(0, 65535)
            val query = buildDnsQuery(domain, transactionId)
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val address = InetAddress.getByName(dnsIp)
                val start = System.currentTimeMillis()
                socket.send(DatagramPacket(query, query.size, address, 53))

                val buffer = ByteArray(512)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(responsePacket)
                val elapsed = System.currentTimeMillis() - start

                val respId = ((buffer[0].toInt() and 0xFF) shl 8) or (buffer[1].toInt() and 0xFF)
                val rcode = buffer[3].toInt() and 0x0F
                val ancount = ((buffer[6].toInt() and 0xFF) shl 8) or (buffer[7].toInt() and 0xFF)

                if (respId == transactionId && rcode == 0 && ancount > 0) elapsed else null
            }
        } catch (e: Exception) {
            null
        }
    }