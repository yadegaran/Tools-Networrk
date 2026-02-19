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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object ScannerHome : Screen("scanner", "اسکنر ای پی", Icons.Default.Search)
    data object Converter : Screen("converter", "مبدل کانفیگ", Icons.Default.Build)
    data object DnsFinder : Screen("dns", "DNS یاب", Icons.Default.Refresh) // تغییر آیکون
    data object NetworkTools : Screen("tools", "تست شبکه", Icons.Default.Settings) // تغییر آیکون
    data object SpeedTest : Screen("speed", "تست سرعت", Icons.Default.PlayArrow)
    data object FreeConfigs :
        Screen("free_configs", "کانفیگ رایگان", Icons.Default.Menu) // تغییر آیکون

    data object FragmentFinder : Screen("fragment_finder", "فرگمنت یاب", Icons.Default.Build)
    data object SupportScreen :
        Screen("support", "عیب یابی", Icons.Default.Info) // تغییر مسیر و آیکون
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ScannerViewModel()
        setContent {
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
    val currentVersionCode = remember { UpdateManager.updateManager.getCurrentVersionCode(context) }

    var showDialog by remember { mutableStateOf(false) }
    var updateData by remember { mutableStateOf<UpdateInfo?>(null) }


    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val info = UpdateManager.updateManager.fetchUpdateInfo()
        if (info != null && info.versionCode > currentVersionCode) {
            updateData = info
            showDialog = true
        }
    }

    if (showDialog && updateData != null) {
        UpdateDialog(updateData!!) { showDialog = false }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
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

                val menuItems = listOf(
                    Screen.ScannerHome,
                    Screen.Converter,
                    Screen.DnsFinder,
                    Screen.NetworkTools,
                    Screen.FreeConfigs,
                    Screen.SpeedTest,
                    Screen.FragmentFinder,
                    Screen.SupportScreen
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
                Divider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    UpdateMenuItem(currentVersionCode) { info ->
                        updateData = info
                        showDialog = true
                    }
                    GitHubMenuItem(context)
                }
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

@Composable
fun UpdateMenuItem(currentVersionCode: Int, onUpdateFound: (UpdateInfo) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var hasUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val currentVersionName = remember { UpdateManager.updateManager.getAppVersionName(context) }

    // بررسی اولیه موقع باز شدن منو
    LaunchedEffect(Unit) {
        val info = UpdateManager.updateManager.fetchUpdateInfo()
        if (info != null && info.versionCode > currentVersionCode) {
            updateInfo = info
            hasUpdate = true
            Toast.makeText(context, "سرور: ${info.versionCode} | شما: $currentVersionCode", Toast.LENGTH_LONG).show()
        }

    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecking) {
                scope.launch {
                    isChecking = true
                    // نمایش پیغام برای شروع بررسی دستی
                    Toast.makeText(context, "در حال بررسی نسخه جدید...", Toast.LENGTH_SHORT).show()

                    val info = UpdateManager.updateManager.fetchUpdateInfo()
                    isChecking = false

                    if (info != null && info.versionCode > currentVersionCode) {
                        updateInfo = info
                        hasUpdate = true
                        // باز کردن دیالوگ اصلی
                        onUpdateFound(info)
                    } else {
                        hasUpdate = false
                        Toast.makeText(context, "شما از آخرین نسخه استفاده می‌کنید.", Toast.LENGTH_SHORT).show()
                    }
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
            Text("نسخه $currentVersionName", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (isChecking) "در حال بررسی..." else if (hasUpdate) "نسخه جدید یافت شد!" else "بررسی به‌روزرسانی",
                fontSize = 11.sp,
                color = if (hasUpdate) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}

@Composable
fun UpdateDialog(updateInfo: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نسخه جدید در دسترس است!", fontWeight = FontWeight.Bold) },
        text = { Text(updateInfo.changeLog) },
        confirmButton = {
            Button(
                onClick = {
                    UpdateManager.updateManager.startDownload(context, updateInfo.downloadUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("دانلود مستقیم") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("فعلاً نه") }
        }
    )
}

@Composable
fun AppNavHost(navController: NavHostController, vm: ScannerViewModel) {
    NavHost(navController = navController, startDestination = Screen.ScannerHome.route) {
        composable(Screen.ScannerHome.route) { ScannerApp(vm) }
        composable(Screen.Converter.route) { ConverterScreen(vm) }
        composable(Screen.DnsFinder.route) { DnsFinderScreen(vm) }
        composable(Screen.NetworkTools.route) { NetworkToolsScreen(vm) }
        composable(Screen.FreeConfigs.route) { FreeConfigScreen() }
        composable(Screen.SpeedTest.route) { SpeedTestScreen(vm) }
        composable(Screen.FragmentFinder.route) { FragmentFinderScreen(vm) }
        composable(Screen.SupportScreen.route) { SupportScreen(vm) }
    }
}

@Composable
fun GitHubMenuItem(context: Context) {
    val githubUrl = "https://github.com/yadegaran/Tools-Networrk"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.Transparent, // یا MaterialTheme.colorScheme.surfaceVariant برای پس‌زمینه ملایم
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                    context.startActivity(intent)
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build, // آیکون ابزار یا ساخت
                contentDescription = null,
                tint = Color(0xFF607D8B), // رنگ خاکستری-آبی (Steel Grey)
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "مشاهده سورس در گیت‌هاب",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "github.com/yadegaran",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}