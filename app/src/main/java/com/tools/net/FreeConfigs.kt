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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
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
                    // بررسی اینکه آیا محتوا Base64 است یا متن عادی
                    val data = Base64.decode(rawContent, Base64.DEFAULT)
                    String(data, Charset.defaultCharset())
                } catch (e: Exception) {
                    rawContent
                }

                return@withContext decoded.lines()
                    .map { it.trim() }
                    .filter { it.length > 10 }
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

    // لیست آدرس‌های جدید شما
    val sources = listOf(
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt",
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt",
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt",
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS+All_RUS.txt",
        "https://raw.githubusercontent.com/4n0nymou3/multi-proxy-config-fetcher/refs/heads/main/configs/proxy_configs.txt",
        "https://raw.githubusercontent.com/AvenCores/goida-vpn-configs/refs/heads/main/githubmirror/1.txt",
        "https://raw.githubusercontent.com/sevcator/5ubscrpt10n/main/protocols/vl.txt",
        "https://raw.githubusercontent.com/yitong2333/proxy-minging/refs/heads/main/v2ray.txt",
        "https://raw.githubusercontent.com/miladtahanian/V2RayCFGDumper/refs/heads/main/config.txt",
        "https://github.com/Epodonios/v2ray-configs/raw/main/Splitted-By-Protocol/trojan.txt",
        "https://raw.githubusercontent.com/mohamadfg-dev/telegram-v2ray-configs-collector/refs/heads/main/category/vless.txt",
        "https://raw.githubusercontent.com/mheidari98/.proxy/refs/heads/main/all",
        "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/refs/heads/main/xray_final.txt",
        "https://github.com/MhdiTaheri/V2rayCollector_Py/raw/refs/heads/main/sub/Mix/mix.txt",
        "https://raw.githubusercontent.com/V2RayRoot/V2RayConfig/refs/heads/main/Config/vless.txt"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "دریافت کانفیگ رایگان ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // دکمه‌های انتخاب منبع
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sources) { url ->
                // تشخیص هوشمند نام دکمه بر اساس آدرس
                val label = when {
                    url.contains("vless", ignoreCase = true) -> "Vless"
                    url.contains("vmess", ignoreCase = true) -> "Vmess"
                    url.contains("trojan", ignoreCase = true) -> "Trojan"
                    url.contains("ss", ignoreCase = true) -> "Shadowsocks"
                    url.contains("mix", ignoreCase = true) -> "ترکیبی"
                    url.contains("all", ignoreCase = true) -> "همه"
                    else -> "منبع ${sources.indexOf(url) + 1}"
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
                                Toast.makeText(context, "خطا در دریافت! باز کردن در مرورگر...", Toast.LENGTH_SHORT).show()
                                openInBrowser(context, url)
                            }
                            isLoading = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (url.contains("RUS")) Color(0xFF607D8B) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(label, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // دکمه کپی کل لیست
        Button(
            onClick = {
                if (configs.isNotEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val fullText = configs.joinToString("\n")
                    val clip = ClipData.newPlainText("configs", fullText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "${configs.size} کانفیگ کپی شد", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = configs.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("کپی همه موارد در حافظه")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // نمایش لیست کانفیگ‌ها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (configs.isEmpty()) {
                Text(
                    "یک منبع را از لیست بالا انتخاب کنید",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 13.sp
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
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.DarkGray,
            lineHeight = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Divider(thickness = 0.5.dp, color = Color.LightGray)
    }
}

fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "مرورگری یافت نشد!", Toast.LENGTH_SHORT).show()
    }
}