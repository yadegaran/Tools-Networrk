package com.tools.net

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tools.net.ui.components.GlassCard
import com.tools.net.ui.theme.ErrorRed
import com.tools.net.ui.theme.SuccessGreen
import com.tools.net.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerApp(vm: ScannerViewModel) {
    // استفاده از remember برای حفظ وضعیت ورودی‌ها
    var threads by remember { mutableStateOf("100") }
    var timeout by remember { mutableStateOf("1000") }
    var maxResults by remember { mutableStateOf("20") }
    val selectedRanges = remember { mutableStateListOf<String>() }

    val portOptions = listOf(
        "443", "80", "2052", "2053", "2082", "2083",
        "2086", "2087", "2095", "2096", "8080", "8443"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedPort by remember { mutableStateOf(portOptions[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ۱. بخش تنظیمات عددی (ترد، تایم‌اوت، تعداد)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = threads,
                onValueChange = { threads = it },
                label = { Text(stringResource(R.string.scanner_threads_label), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = timeout,
                onValueChange = { timeout = it },
                label = { Text(stringResource(R.string.scanner_timeout_label), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = maxResults,
                onValueChange = { maxResults = it },
                label = { Text(stringResource(R.string.scanner_count_label), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ۲. انتخاب پورت هدف
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = stringResource(R.string.scanner_port_label, selectedPort),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(12.dp))

        // ۳. باکس انتخاب رنج‌های IP
        Text(
            stringResource(R.string.scanner_ranges_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(4.dp)) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedRanges.size == vm.ipRanges.size) selectedRanges.clear()
                                else {
                                    selectedRanges.clear(); selectedRanges.addAll(vm.ipRanges)
                                }
                            }
                    ) {
                        Checkbox(
                            checked = selectedRanges.size == vm.ipRanges.size,
                            onCheckedChange = null
                        )
                        Text(stringResource(R.string.scanner_select_all), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

        // ۴. دکمه شروع/توقف عملیات
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
                .padding(vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (vm.isScanning.value) ErrorRed else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (vm.isScanning.value) Icons.Default.Refresh else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (vm.isScanning.value) stringResource(R.string.scanner_stop_action) else stringResource(R.string.scanner_start_action),
                fontWeight = FontWeight.Bold
            )
        }

        // ۵. نمایش نتایج
        Text(
            stringResource(R.string.scanner_results_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
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
    val successLabel = stringResource(R.string.scanner_exchange_success)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("IP", res.ip))

            // ذخیره در ViewModel برای صفحه مبدل
            vm.selectedIpForConverter.value = res.ip

            Toast.makeText(context, context.getString(R.string.scanner_ip_copied, res.ip), Toast.LENGTH_SHORT).show()
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${res.ip}:${res.port}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.scanner_latency_ms, res.latency),
                        color = if (res.latency < 500) SuccessGreen else WarningOrange,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        stringResource(R.string.scanner_loss_label, res.packetLoss),
                        color = if (res.packetLoss > 0) ErrorRed else SuccessGreen,
                        fontSize = 10.sp
                    )
                }
            }
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.scanner_location_label, countryCodeToFlag(res.countryCode), res.colo),
                        fontSize = 13.sp
                    )
                    Text(
                        stringResource(R.string.scanner_mtu_label, res.mtu.toString()),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    res.exchangeStatus,
                    color = if (res.exchangeStatus == successLabel) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

fun countryCodeToFlag(code: String): String {

    if (code.length != 2) return "🌐"

    return code.uppercase().map { char ->

        Character.codePointAt(char.toString(), 0) - 0x41 + 0x1F1E6

    }.joinToString("") { codePoint ->

        String(Character.toChars(codePoint))

    }

}
