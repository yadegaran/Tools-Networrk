package com.clean.ipcloud

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConverterScreen(vm: ScannerViewModel) {
    var rawConfigs by remember { mutableStateOf("") }
    var resultConfigs by remember { mutableStateOf("") }
    var targetIp by remember { mutableStateOf(vm.selectedIpForConverter.value) }

    LaunchedEffect(vm.selectedIpForConverter.value) {
        if (vm.selectedIpForConverter.value.isNotEmpty()) {
            targetIp = vm.selectedIpForConverter.value
        }
    }

    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("مبدل هوشمند کانفیگ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        // فیلد آی‌پی
        OutlinedTextField(
            value = targetIp,
            onValueChange = { targetIp = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("آی‌پی هدف") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // باکس ورودی - دکمه چسباندن به سمت چپ منتقل شد
        OutlinedTextField(
            value = rawConfigs,
            onValueChange = { rawConfigs = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
            label = { Text("ورودی: کانفیگ‌های خام") },
            shape = RoundedCornerShape(12.dp),
            // استفاده از leadingIcon برای نمایش در سمت چپ در چیدمان RTL
            leadingIcon = {
                TextButton(onClick = {
                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    rawConfigs = clip
                }) {
                    Text("چسباندن", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (targetIp.isBlank()) {
                    Toast.makeText(context, "آی‌پی خالی است", Toast.LENGTH_SHORT).show()
                } else {
                    val converted = processConfigsLogic(rawConfigs, targetIp)
                    if (converted.isBlank()) {
                        Toast.makeText(context, "❌ کانفیگ معتبری یافت نشد", Toast.LENGTH_LONG).show()
                    } else {
                        resultConfigs = converted
                        Toast.makeText(context, "✅ تبدیل انجام شد", Toast.LENGTH_SHORT).show()
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

        // باکس خروجی - دکمه کپی به سمت چپ منتقل شد
        if (resultConfigs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = resultConfigs,
                onValueChange = { resultConfigs = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                label = { Text("خروجی: کانفیگ‌های تمیز") },
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                // استفاده از leadingIcon برای نمایش در سمت چپ
                leadingIcon = {
                    TextButton(onClick = {
                        val clip = android.content.ClipData.newPlainText("Clean", resultConfigs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("کپی همه", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

fun processConfigsLogic(input: String, newIp: String): String {
    if (input.isBlank() || newIp.isBlank()) return ""

    val lines = input.split("\n")
    val processedLines = lines.map { line ->
        // ۱. پاکسازی: حذف کوتیشن، کاما و فضاهای خالی ابتدا و انتها
        var cleanLine = line.trim().replace("\"", "").replace(",", "")

        if (cleanLine.isEmpty()) return@map ""

        when {
            // برای VLESS و Trojan
            cleanLine.startsWith("vless://") || cleanLine.startsWith("trojan://") -> {
                try {
                    val atIndex = cleanLine.indexOf("@")
                    if (atIndex != -1) {
                        val prefix = cleanLine.substring(0, atIndex + 1) // vless://uuid@
                        val rest = cleanLine.substring(atIndex + 1) // ip:port...

                        // پیدا کردن اولین جداکننده (دو نقطه برای پورت یا علامت سوال برای تنظیمات)
                        val separators = charArrayOf(':', '/', '?', '#')
                        val firstSep = rest.indexOfAny(separators)

                        if (firstSep != -1) {
                            prefix + newIp + rest.substring(firstSep)
                        } else {
                            prefix + newIp
                        }
                    } else cleanLine
                } catch (e: Exception) { cleanLine }
            }

            // برای VMess
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
    }.filter { it.isNotBlank() }

    return processedLines.joinToString("\n")
}