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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tools.net.ui.components.GlassCard
import com.tools.net.ui.components.HelpCard
import com.tools.net.ui.theme.BrandPurple
import com.tools.net.ui.theme.InfoBlue
import com.tools.net.ui.theme.SuccessGreen
import com.tools.net.ui.theme.WarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset

/** پروتکل کانفیگ را از روی پیشوند آن تشخیص می‌دهد. */
private fun configProtocol(config: String): String = when {
    config.startsWith("vless://", true) -> "vless"
    config.startsWith("vmess://", true) -> "vmess"
    config.startsWith("trojan://", true) -> "trojan"
    config.startsWith("ss://", true) -> "ss"
    else -> "other"
}

private val protocolOrder = listOf("vless", "vmess", "trojan", "ss", "other")

/**
 * اسم/Remark کانفیگ را استخراج می‌کند: برای vless/trojan/ss معمولاً بعد از "#" در انتهای لینک
 * قرار دارد (URL-encoded)؛ برای vmess که خودش Base64 از یک JSON است، فیلد "ps" استخراج می‌شود.
 * اگر اسمی پیدا نشود، خود IP/دامنه به‌عنوان نام نمایش داده می‌شود.
 */
fun extractConfigName(config: String): String {
    return try {
        if (config.startsWith("vmess://", true)) {
            val base64Part = config.removePrefix("vmess://")
            val json = String(Base64.decode(base64Part, Base64.DEFAULT))
            val nameMatch = Regex(""""ps"\s*:\s*"([^"]*)"""").find(json)
            val name = nameMatch?.groupValues?.get(1)?.trim()
            if (!name.isNullOrBlank()) name else fallbackHostName(config)
        } else {
            val hashIndex = config.indexOf('#')
            val rawName = if (hashIndex != -1) config.substring(hashIndex + 1).trim() else ""
            val decoded = if (rawName.isNotBlank()) {
                try {
                    java.net.URLDecoder.decode(rawName, "UTF-8")
                } catch (e: Exception) {
                    rawName
                }
            } else ""
            if (decoded.isNotBlank()) decoded else fallbackHostName(config)
        }
    } catch (e: Exception) {
        fallbackHostName(config)
    }
}

/** اگر اسمی در کانفیگ پیدا نشد، بخش میزبان (بعد از @ تا اولین جداکننده) به‌عنوان اسم استفاده می‌شود. */
private fun fallbackHostName(config: String): String {
    val atIndex = config.indexOf('@')
    if (atIndex == -1) return config.take(24)
    val rest = config.substring(atIndex + 1)
    val end = rest.indexOfFirst { it == ':' || it == '/' || it == '?' || it == '#' }
    return if (end != -1) rest.substring(0, end) else rest.take(24)
}

/** حذف موارد تکراری و مرتب‌سازی بر اساس نوع پروتکل و سپس بر اساس اسم/Remark کانفیگ. */
private fun sortAndDedupeConfigs(list: List<String>): List<String> =
    list.distinct().sortedWith(
        compareBy(
            { protocolOrder.indexOf(configProtocol(it)).let { i -> if (i == -1) protocolOrder.size else i } },
            { extractConfigName(it).lowercase() }
        )
    )

private fun protocolColor(protocol: String) = when (protocol) {
    "vless" -> SuccessGreen
    "vmess" -> InfoBlue
    "trojan" -> WarningOrange
    "ss" -> BrandPurple
    else -> InfoBlue
}

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
            text = stringResource(R.string.free_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))
        HelpCard(stringResource(R.string.help_free_configs))
        Spacer(modifier = Modifier.height(12.dp))

        val mixedLabel = stringResource(R.string.free_source_mixed)
        val allLabel = stringResource(R.string.free_source_all)
        val fetchErrorLabel = stringResource(R.string.free_fetch_error)
        val copiedFormat = stringResource(R.string.free_copy_all_action)

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
                    url.contains("mix", ignoreCase = true) -> mixedLabel
                    url.contains("all", ignoreCase = true) -> allLabel
                    else -> stringResource(R.string.free_source_n, sources.indexOf(url) + 1)
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = fetchConfigsList(url)
                            if (result.isNotEmpty()) {
                                configs.clear()
                                configs.addAll(sortAndDedupeConfigs(result))
                            } else {
                                Toast.makeText(context, fetchErrorLabel, Toast.LENGTH_SHORT).show()
                                openInBrowser(context, url)
                            }
                            isLoading = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (url.contains("RUS")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
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
                    Toast.makeText(context, context.getString(R.string.free_copied_count, configs.size), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = configs.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(copiedFormat)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (configs.isNotEmpty()) {
            Text(
                stringResource(R.string.free_count_label, configs.size),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // نمایش لیست کانفیگ‌ها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (configs.isEmpty()) {
                Text(
                    stringResource(R.string.free_empty_hint),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    items(configs) { item ->
                        ConfigListItem(item, context)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigListItem(text: String, context: Context) {
    val protocol = configProtocol(text)
    val name = remember(text) { extractConfigName(text) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(protocolColor(protocol).copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    protocol.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = protocolColor(protocol)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = text,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("config", text))
                    Toast.makeText(context, context.getString(R.string.free_item_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.free_no_browser), Toast.LENGTH_SHORT).show()
    }
}