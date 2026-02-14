package com.clean.ipcloud

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ScannerViewModel()
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFFF8F9FA)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        MainNavigationScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationScreen(vm: ScannerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("اسکنر") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("مبدل") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("DNS یاب") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    label = { Text("ابزار شبکه") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    label = { Text("تست سرعت") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ScannerApp(vm)
                1 -> ConverterScreen(vm)
                2 -> DnsFinderScreen(vm)
                3 -> NetworkToolsScreen(vm)
                4 -> SpeedTestScreen(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerApp(vm: ScannerViewModel) {
    var threads by remember { mutableStateOf("100") }
    var timeout by remember { mutableStateOf("1000") }
    var maxResults by remember { mutableStateOf("20") }
    val selectedRanges = remember { mutableStateListOf<String>() }

    val portOptions = listOf("443", "80", "2052", "2053", "2082", "2083", "2086", "2087", "2095", "2096", "8080", "8443")
    var expanded by remember { mutableStateOf(false) }
    var selectedPort by remember { mutableStateOf(portOptions[0]) }

    // استفاده از Column اصلی بدون اسکرول برای جلوگیری از تداخل لمس
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("اسکنر آی‌پی تمیز کلاودفلر", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        )

        // ۱. بخش تنظیمات عددی
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = threads,
                onValueChange = { threads = it },
                label = { Text("ترد", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = timeout,
                onValueChange = { timeout = it },
                label = { Text("تایم‌اوت", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = maxResults,
                onValueChange = { maxResults = it },
                label = { Text("تعداد", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ۲. انتخاب پورت
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "پورت هدف: $selectedPort",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                portOptions.forEach { port ->
                    DropdownMenuItem(
                        text = { Text(port) },
                        onClick = { selectedPort = port; expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ۳. باکس انتخاب رنج‌ها با ارتفاع ثابت
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(modifier = Modifier.padding(4.dp)) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedRanges.size == vm.ipRanges.size) selectedRanges.clear()
                                else { selectedRanges.clear(); selectedRanges.addAll(vm.ipRanges) }
                            }
                    ) {
                        Checkbox(checked = selectedRanges.size == vm.ipRanges.size, onCheckedChange = null)
                        Text("انتخاب همه رنج‌ها", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items(vm.ipRanges) { range ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedRanges.contains(range)) selectedRanges.remove(range)
                                else selectedRanges.add(range)
                            }
                    ) {
                        Checkbox(checked = selectedRanges.contains(range), onCheckedChange = null)
                        Text(range, fontSize = 13.sp)
                    }
                }
            }
        }

        // ۴. دکمه شروع/توقف
        Button(
            onClick = {
                if (vm.isScanning.value) {
                    vm.isScanning.value = false
                } else {
                    vm.startScan(
                        selectedRanges.toList(),
                        threads.toIntOrNull() ?: 100,
                        timeout.toIntOrNull() ?: 1000,
                        maxResults.toIntOrNull() ?: 20,
                        selectedPort.toInt()
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (vm.isScanning.value) Color(0xFFD32F2F) else Color(0xFF1976D2)
            )
        ) {
            Icon(
                imageVector = if (vm.isScanning.value) Icons.Default.Refresh else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (vm.isScanning.value) "توقف اسکن" else "شروع عملیات اسکن")
        }

        // ۵. لیست نتایج (بسیار مهم: وزن ۱ برای اسکرول مستقل و حل مشکل لمس)
        Text(
            "نتایج اسکن:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
            color = Color.Gray
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f) // تمام فضای باقی‌مانده را مدیریت می‌کند
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(vm.foundIps) { res ->
                ScannerResultItem(res, vm)
            }
        }
    }
}

@Composable
fun ScannerResultItem(res: IpScanResult, vm: ScannerViewModel) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // کپی در حافظه
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("IP", res.ip))

                // ذخیره در ViewModel برای صفحه مبدل
                vm.selectedIpForConverter.value = res.ip

                Toast.makeText(context, "آی‌پی ${res.ip} کپی و انتخاب شد", Toast.LENGTH_SHORT).show()
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${res.ip}:${res.port}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${res.latency}ms", color = if (res.latency < 500) Color(0xFF2E7D32) else Color(0xFFE65100), fontWeight = FontWeight.Black)
                    Text("Loss: ${res.packetLoss}%", color = if (res.packetLoss > 0) Color.Red else Color(0xFF2E7D32), fontSize = 10.sp)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${countryCodeToFlag(res.countryCode)} لوکیشن: ${res.colo}", fontSize = 13.sp)
                    Text("MTU: ${res.mtu}", fontSize = 11.sp, color = Color.Gray)
                }
                Text(res.exchangeStatus, color = if (res.exchangeStatus == "تبادل موفق") Color(0xFF2E7D32) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// تابع پرچم کشورها (بدون تغییر)
fun countryCodeToFlag(code: String): String {
    if (code.length != 2) return "🌐"
    return code.uppercase().map { char ->
        Character.codePointAt(char.toString(), 0) - 0x41 + 0x1F1E6
    }.joinToString("") { codePoint ->
        String(Character.toChars(codePoint))
    }
}