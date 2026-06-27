package com.tools.net

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tools.net.ui.components.GlassCard
import com.tools.net.ui.theme.ErrorRed
import com.tools.net.ui.theme.SuccessGreen
import com.tools.net.ui.theme.WarningOrange

@Composable
fun FragmentFinderScreen(vm: ScannerViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- هدر صفحه ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.fragment_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.fragment_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- بخش دکمه‌های کنترل (اسکن و توقف) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.startDeepFragmentScan() },
                modifier = Modifier.weight(1f),
                enabled = !vm.isScanningg, // غیرفعال شدن دکمه هنگام اسکن
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (vm.isScanningg) stringResource(R.string.fragment_scanning) else stringResource(R.string.fragment_start_action))
            }

            // نمایش دکمه توقف فقط در زمان اسکن
            AnimatedVisibility(visible = vm.isScanningg) {
                Button(
                    onClick = { vm.stopScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.fragment_stop_action), color = Color.White)
                }
            }
        }

        // --- وضعیت پیشرفت و اطلاعات تست ---
        if (vm.isScanningg || vm.currentProgress > 0) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                LinearProgressIndicator(
                    progress = vm.currentProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (vm.isScanningg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vm.currentTestInfo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (vm.isScanningg) MaterialTheme.colorScheme.primary else ErrorRed
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- لیست نتایج ---
        Text(stringResource(R.string.fragment_results_title), fontSize = 14.sp, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(vm.scanResults) { result ->
                val copiedLabel = stringResource(R.string.fragment_copied)
                FragmentResultItem(result) {
                    // کپی کردن به فرمت V2Ray
                    val copyText = context.getString(R.string.fragment_copy_format, result.lengthRange, result.intervalRange)
                    clipboardManager.setText(AnnotatedString(copyText))
                    Toast.makeText(context, copiedLabel, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun FragmentResultItem(result: FragmentResult, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // دایره نمایش پایداری
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(
                        if (result.stability > 80) SuccessGreen else WarningOrange,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("${result.stability}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.fragment_size_label, result.lengthRange), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.fragment_interval_label, result.intervalRange),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${result.latency}ms", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.fragment_ping_label),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}