package com.tools.net

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tools.net.ui.components.GlassCard
import com.tools.net.ui.theme.ErrorRed
import com.tools.net.ui.theme.SuccessGreen
import com.tools.net.ui.theme.WarningOrange

@Composable
fun SupportScreen(vm: ScannerViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = stringResource(R.string.support_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.support_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        item {
            SmartDiagnosticsCard(vm)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // استفاده از آیکون Info به جای QuestionAnswer (برای پایداری بیشتر)
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "FAQ",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.support_faq_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        items(getFaqList()) { item ->
            FAQCard(item)
        }
    }
}

@Composable
fun SmartDiagnosticsCard(vm: ScannerViewModel) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.support_doctor_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                stringResource(R.string.support_doctor_subtitle),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            vm.analysisResults.forEach { step ->
                AnalysisResultRow(step)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.runFullDiagnostics() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.isAnalyzing,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (vm.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.support_checking))
                } else {
                    Text(stringResource(R.string.support_start_analysis))
                }
            }

            AnimatedVisibility(
                visible = !vm.isAnalyzing && vm.analysisResults.any { it.status == AnalysisStatus.ERROR },
                enter = expandVertically() + fadeIn()
            ) {
                FinalSolutionBox(vm.analysisResults)
            }
        }
    }
}

@Composable
fun AnalysisResultRow(step: AnalysisStep) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (step.status) {
            AnalysisStatus.LOADING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )

            AnalysisStatus.SUCCESS -> Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "OK",
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp)
            )
            // استفاده از Warning به جای Error (چون Error در پکیج پایه نیست)
            AnalysisStatus.ERROR -> Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
            )

            AnalysisStatus.WARNING -> Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = WarningOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(step.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(step.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FinalSolutionBox(results: List<AnalysisStep>) {
    val errorStep = results.firstOrNull { it.status == AnalysisStatus.ERROR }
    GlassCard(modifier = Modifier.padding(top = 16.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // استفاده از آیکon Info به جای Lightbulb
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Solution",
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    stringResource(R.string.support_solution_title),
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    fontSize = 13.sp
                )
                val solution = when (errorStep?.title) {
                    "ساعت سیستم" -> stringResource(R.string.support_solution_clock)
                    "اتصال اینترنت" -> stringResource(R.string.support_solution_internet)
                    "وضعیت سرور" -> stringResource(R.string.support_solution_server)
                    else -> stringResource(R.string.support_solution_default)
                }
                Text(solution, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun FAQCard(item: FAQItem) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.question,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                // استفاده از KeyboardArrowDown/Up به جای ExpandMore/Less
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (expanded) {
                Text(
                    text = item.answer,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}


fun getFaqList() = listOf(
    FAQItem(
        "چرا پینگ دارم ولی تلگرام باز نمی‌شود؟",
        "این یعنی فیلترینگ روی لایه پروتکل اعمال شده. حتماً از قابلیت Fragment در بخش اسکنر استفاده کنید."
    ),
    FAQItem(
        "بهترین فرگمنت برای همراه اول چیست؟",
        "معمولاً برای همراه اول طول‌های کوچک مثل 1-1 یا 2-5 با اینتروال بالای 10 میلی‌ثانیه بهتر عمل می‌کنند."
    ),
    FAQItem(
        "آیا این برنامه حجم مصرفی را زیاد می‌کند؟",
        "خیر، فرگمنت فقط پکت‌های اولیه را تکه می‌کند و تاثیری روی حجم کلی مصرفی شما ندارد."
    )
)