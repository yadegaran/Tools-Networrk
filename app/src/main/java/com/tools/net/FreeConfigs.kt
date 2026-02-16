package com.tools.net

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset

// تابع دریافت داده‌ها از اینترنت
suspend fun fetchConfigsList(url: String): List<String> {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val rawContent = response.body?.string() ?: ""

                val decoded = try {
                    val data = Base64.decode(rawContent, Base64.DEFAULT)
                    String(data, Charset.defaultCharset())
                } catch (e: Exception) {
                    rawContent
                }

                return@withContext decoded.lines().filter { it.trim().length > 10 }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}

@Composable
fun FreeConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configs = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(false) }

    val sources = listOf(
        "https://raw.githubusercontent.com/Kolandone/v2raycollector/main/vmess.txt",
        "https://raw.githubusercontent.com/Kolandone/v2raycollector/main/vless.txt",
        "https://raw.githubusercontent.com/Kolandone/v2raycollector/main/trojan.txt",
        "https://raw.githubusercontent.com/Kolandone/v2raycollector/main/ss.txt",
        "https://raw.githubusercontent.com/Kolandone/v2raycollector/main/hysteria.txt",
        "https://raw.githubusercontent.com/Kolandone/v2raycollector/main/config_lite.txt"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "دریافت کانفیگ رایگان",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // دکمه‌های انتخاب نوع کانفیگ
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sources) { url ->
                val label = when {
                    url.contains("vmess") -> "وی‌مس"
                    url.contains("vless") -> "وی‌لس"
                    url.contains("trojan") -> "تروجان"
                    url.contains("ss.txt") -> "شادوساکس"
                    url.contains("hysteria") -> "هستریا"
                    else -> "پیشنهادی"
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = fetchConfigsList(url)
                            if (result.isNotEmpty()) {
                                configs.clear()
                                configs.addAll(result)
                            } else {
                                // اگر لیست خالی بود (خطا در شبکه یا فیلترینگ)
                                Toast.makeText(context, "خطا در اتصال! باز کردن در مرورگر...", Toast.LENGTH_LONG).show()
                                openInBrowser(context, url)
                            }
                            isLoading = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(label, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // دکمه کپی همگانی
        Button(
            onClick = {
                if (configs.isNotEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val fullText = configs.joinToString("\n")
                    val clip = ClipData.newPlainText("configs", fullText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "تعداد ${configs.size} مورد کپی شد", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = configs.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("کپی همه موارد در حافظه")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // باکس نمایش لیست کانفیگ‌ها (ضد هنگ)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFEF5350), shape = RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (configs.isEmpty()) {
                Text(
                    "لیست خالی است. یک گزینه را از بالا انتخاب کنید.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(configs) { item ->
                        ConfigListItem(item)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigListItem(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Black,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = Color.Red.copy(alpha = 0.1f))
    }
}

fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "مرورگری پیدا نشد!", Toast.LENGTH_SHORT).show()
    }
}