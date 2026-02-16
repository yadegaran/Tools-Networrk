package com.tools.net

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ConverterScreen(vm: ScannerViewModel) {
    var rawConfigs by remember { mutableStateOf("") }
    var resultConfigs by remember { mutableStateOf("") }
    var targetIp by remember { mutableStateOf(vm.selectedIpForConverter.value) }
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val defaultUrls = listOf(
        "https://raw.githubusercontent.com/10ium/V2RayAggregator/master/sub/sub_merge.txt",
        "https://github.com/Epodonios/v2ray-configs/raw/main/All_Configs_Sub.txt"
    )

    LaunchedEffect(vm.selectedIpForConverter.value) {
        if (vm.selectedIpForConverter.value.isNotEmpty()) {
            targetIp = vm.selectedIpForConverter.value
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("دریافت و مبدل هوشمند", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            defaultUrls.forEachIndexed { index, url ->
                OutlinedButton(
                    onClick = { urlInput = url },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("منبع ${index + 1}", fontSize = 12.sp) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("آدرس منبع کانفیگ") },
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = {
                        if (urlInput.isBlank()) return@TextButton
                        isLoading = true
                        scope.launch {
                            val fetched = fetchConfigsFromUrl(urlInput)
                            if (fetched.isEmpty()) {
                                // متن درخواستی شما در صورت عدم موفقیت
                                Toast.makeText(context, "ارتباط برقرار نشد. لطفاً از اتصال اینترنت یا ابزار تغییر آی‌پی خود مطمئن شوید.", Toast.LENGTH_LONG).show()
                            } else {
                                val randomConfigs = fetched.shuffled().take(50)
                                rawConfigs = randomConfigs.joinToString("\n")
                                Toast.makeText(context, "تعداد ${randomConfigs.size} کانفیگ با موفقیت دریافت شد", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }
                    }) { Text("دریافت", fontWeight = FontWeight.Bold) }
                }
            }
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = Color.LightGray)

        OutlinedTextField(
            value = targetIp,
            onValueChange = { targetIp = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("آی‌پی هدف (تمیز)") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = rawConfigs,
            onValueChange = { rawConfigs = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
            label = { Text("لیست ورودی") },
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                TextButton(onClick = {
                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    rawConfigs = clip
                }) { Text("چسباندن") }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (targetIp.isBlank()) {
                    Toast.makeText(context, "ابتدا آی‌پی هدف را مشخص کنید", Toast.LENGTH_SHORT).show()
                } else {
                    val converted = processConfigsLogic(rawConfigs, targetIp)
                    if (converted.isBlank()) {
                        Toast.makeText(context, "کانفیگ معتبری در لیست ورودی پیدا نشد.", Toast.LENGTH_LONG).show()
                    } else {
                        resultConfigs = converted
                        Toast.makeText(context, "عملیات تبدیل با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("تبدیل و جایگزینی")
        }

        if (resultConfigs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = resultConfigs,
                onValueChange = { resultConfigs = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                label = { Text("نتیجه نهایی") },
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                leadingIcon = {
                    TextButton(onClick = {
                        val clip = android.content.ClipData.newPlainText("Clean", resultConfigs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
                    }) { Text("کپی کُل", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
                }
            )
        }
    }
}

// تابع واکشی بدون نیاز به کتابخانه خارجی
suspend fun fetchConfigsFromUrl(url: String): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val pattern = """(vless|vmess|trojan)://[^\s"<>]+""".toRegex()
                pattern.findAll(text).map { it.value }.toList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// تابع منطق تبدیل (بدون تغییر)
fun processConfigsLogic(input: String, newIp: String): String {
    if (input.isBlank() || newIp.isBlank()) return ""
    val lines = input.split("\n")
    return lines.map { line ->
        var cleanLine = line.trim().replace("\"", "").replace(",", "")
        if (cleanLine.isEmpty()) return@map ""
        when {
            cleanLine.startsWith("vless://") || cleanLine.startsWith("trojan://") -> {
                val atIndex = cleanLine.indexOf("@")
                if (atIndex != -1) {
                    val prefix = cleanLine.substring(0, atIndex + 1)
                    val rest = cleanLine.substring(atIndex + 1)
                    val separators = charArrayOf(':', '/', '?', '#')
                    val firstSep = rest.indexOfAny(separators)
                    if (firstSep != -1) prefix + newIp + rest.substring(firstSep) else prefix + newIp
                } else cleanLine
            }
            cleanLine.startsWith("vmess://") -> {
                try {
                    val base64Data = cleanLine.removePrefix("vmess://")
                    val jsonString = String(android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT))
                    val updatedJson = jsonString.replace(Regex("""("add"\s*:\s*")[^"]+(")"""), "$1$newIp$2")
                    "vmess://${android.util.Base64.encodeToString(updatedJson.toByteArray(), android.util.Base64.NO_WRAP)}"
                } catch (e: Exception) { "" }
            }
            else -> ""
        }
    }.filter { it.isNotBlank() }.joinToString("\n")
}