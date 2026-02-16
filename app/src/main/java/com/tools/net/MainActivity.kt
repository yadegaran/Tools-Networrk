package com.tools.net

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// تعریف مسیرها (اسکنر از منو حذف و به Home تبدیل شد)
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object ScannerHome : Screen("scanner", "اسکنر اصلی", Icons.Default.Search)
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
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        Screen.ScannerHome,
        Screen.Converter,
        Screen.DnsFinder,
        Screen.NetworkTools,
        Screen.FreeConfigs,
        Screen.SpeedTest
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // هدر منو
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

                // آیتم‌های منو
                Column(modifier = Modifier.weight(1f)) {
                    menuItems.forEach { screen ->
                        NavigationDrawerItem(
                            label = { Text(screen.title, fontWeight = FontWeight.Medium) },
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

                // بخش آپدیت در پایین منو
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.clickable { /* بعداً کد آپدیت اینجا قرار می‌گیرد */ },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("نسخه 1.0.0", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("بررسی آپدیت از گیت‌هاب", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // تولبار با رنگ متمایز (Primary)
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.app_name),
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.padding(innerPadding),
                color = MaterialTheme.colorScheme.surface
            ) {
                AppNavHost(navController, vm)
            }
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
fun SimplePlaceholder(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}