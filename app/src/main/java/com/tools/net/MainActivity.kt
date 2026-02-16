package com.tools.net

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// تعریف مسیرها (اسکنر از منو حذف و به Home تبدیل شد)
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object ScannerHome : Screen("scanner", "اسکنر ای پی", Icons.Default.Search)
    object Converter : Screen("converter", "مبدل کانفیگ", Icons.Default.Build)
    object DnsFinder : Screen("dns", "DNS یاب", Icons.Default.Settings)
    object NetworkTools : Screen("tools", "تست شبکه", Icons.Default.Info)
    object SpeedTest : Screen("speed", "تست سرعت", Icons.Default.PlayArrow)
    object FreeConfigs : Screen("free_configs", "کانفیگ رایگان", Icons.Default.Refresh)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ScannerViewModel()
        setContent {


            // تعریف تم رنگی سفارشی متریال ۳
            val customColorScheme = lightColorScheme(
                primary = Color(0xFF1976D2),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE3F2FD),
                surface = Color(0xFFF8F9FA)
            )

            MaterialTheme(colorScheme = customColorScheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainNavigationApp(vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationApp(vm: ScannerViewModel) {
    val context = LocalContext.current
    val currentVersionCode = 1 // نسخه فعلی اپلیکیشن شما

    var showDialog by remember { mutableStateOf(false) }
    var updateData by remember { mutableStateOf<UpdateInfo?>(null) }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // چک کردن آپدیت به محض اجرای برنامه
    LaunchedEffect(Unit) {
        val info = fetchUpdateInfo()
        if (info != null && info.versionCode > currentVersionCode) {
            updateData = info
            showDialog = true
        }
    }

    // نمایش دیالوگ آپدیت
    if (showDialog && updateData != null) {
        UpdateDialog(updateData!!, { showDialog = false }, context)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // --- هدر منو ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            "Tools Net",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ابزار مدیریت شبکه",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- آیتم‌های ناوبری ---
                val menuItems = listOf(
                    Screen.ScannerHome,
                    Screen.Converter,
                    Screen.DnsFinder,
                    Screen.NetworkTools,
                    Screen.FreeConfigs,
                    Screen.SpeedTest
                )

                Column(modifier = Modifier.weight(1f)) {
                    menuItems.forEach { screen ->
                        NavigationDrawerItem(
                            label = { Text(screen.title) },
                            selected = false,
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.ScannerHome.route)
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }

                // --- بخش آپدیت اصلاح شده در منو ---
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                UpdateMenuItem(context, currentVersionCode)
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Tools Net",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "منو", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                AppNavHost(navController, vm)
            }
        }
    }
}

// تابع جدید برای گرفتن اطلاعات کامل آپدیت از فایل JSON
suspend fun fetchUpdateInfo(): UpdateInfo? {
    return withContext(Dispatchers.IO) {
        val client = okhttp3.OkHttpClient()
        // یک فایل update.json در گیت‌هاب بسازید
        val url =
            "https://raw.githubusercontent.com/yadegaran/Tools-Networrk/refs/heads/master/update.json"
        try {
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string() ?: return@withContext null
            val obj = JSONObject(jsonData)
            UpdateInfo(
                versionCode = obj.getInt("versionCode"),
                downloadUrl = obj.getString("downloadUrl"),
                mirrorUrl = obj.getString("mirrorUrl"),
                changeLog = obj.getString("changeLog")
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, vm: ScannerViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.ScannerHome.route
    ) {
        // صفحه اصلی مستقیماً به اسکنر متصل شد
        composable(Screen.ScannerHome.route) { ScannerApp(vm) }

        composable(Screen.Converter.route) { ConverterScreen(vm) }
        composable(Screen.DnsFinder.route) { DnsFinderScreen(vm) }
        composable(Screen.NetworkTools.route) { NetworkToolsScreen(vm) }
        composable(Screen.FreeConfigs.route) { FreeConfigScreen() }
        composable(Screen.SpeedTest.route) { SpeedTestScreen(vm) }
    }
}

@Composable
fun UpdateMenuItem(context: Context, currentVersionCode: Int) {
    var hasUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val currentVersionName = getAppVersion(context) // استفاده از تابع خودتان


    LaunchedEffect(Unit) {
        val info = fetchUpdateInfo()
        if (info != null && info.versionCode > currentVersionCode) {
            updateInfo = info
            hasUpdate = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (hasUpdate && updateInfo != null) {
                    startDownload(context, updateInfo!!.downloadUrl)
                } else {
                    Toast
                        .makeText(context, "در صورت وجود نسخه جدید اطلاع داده می شود.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = null,
            tint = if (hasUpdate) Color(0xFF4CAF50) else Color.Gray
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                "نسخه $currentVersionName",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ) // داینامیک شد
            Text(
                if (hasUpdate) "آپدیت جدید آماده دانلود است" else "بررسی آپدیت از گیت‌هاب",
                fontSize = 11.sp,
                color = if (hasUpdate) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}


// کلاس ساده برای مدل ورژن
data class UpdateInfo(
    val versionCode: Int,
    val downloadUrl: String,
    val mirrorUrl: String,
    val changeLog: String
)

// تابع دانلود فایل APK
fun startDownload(context: Context, url: String) {
    try {
        // باز کردن مستقیم لینک در مرورگر برای اطمینان ۱۰۰ درصدی از دانلود
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        Toast.makeText(context, "در حال انتقال به مرورگر برای دانلود ایمن...", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در باز کردن مرورگر!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    context: Context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نسخه جدید در دسترس است!", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(updateInfo.changeLog)
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    startDownload(context, updateInfo.downloadUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("دانلود مستقیم") }
        },

        )
}

// تابعی برای گرفتن نسخه فعلی اپلیکیشن
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        e.printStackTrace()
        "1.0.0"
    }
}

