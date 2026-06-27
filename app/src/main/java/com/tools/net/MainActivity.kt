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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tools.net.ui.components.GlassAppBackground
import com.tools.net.ui.components.GlassCard
import com.tools.net.ui.theme.AppThemeMode
import com.tools.net.ui.theme.CleanIpCloudTheme
import com.tools.net.ui.theme.ThemePreferences
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object ScannerHome : Screen("scanner", R.string.nav_scanner, Icons.Default.Search)
    data object Converter : Screen("converter", R.string.nav_converter, Icons.Default.Build)
    data object DnsFinder : Screen("dns", R.string.nav_dns_finder, Icons.Default.Refresh)
    data object NetworkTools : Screen("tools", R.string.nav_network_tools, Icons.Default.Settings)
    data object FreeConfigs : Screen("free_configs", R.string.nav_free_configs, Icons.Default.Menu)
    data object SpeedTest : Screen("speed", R.string.nav_speed_test, Icons.Default.PlayArrow)
    data object FragmentFinder : Screen("fragment_finder", R.string.nav_fragment_finder, Icons.Default.Build)
    data object SupportScreen : Screen("support", R.string.nav_support, Icons.Default.Info)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ScannerViewModel = viewModel()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val themeMode by ThemePreferences.themeModeFlow(context)
                .collectAsState(initial = AppThemeMode.SYSTEM)

            CleanIpCloudTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    GlassAppBackground {
                        MainNavigationApp(
                            vm = vm,
                            themeMode = themeMode,
                            onThemeModeChange = { mode ->
                                scope.launch { ThemePreferences.setThemeMode(context, mode) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationApp(
    vm: ScannerViewModel,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
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
                            stringResource(R.string.app_name),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.app_slogan),
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
                            label = { Text(stringResource(screen.titleRes)) },
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

                ThemeModeSelector(themeMode = themeMode, onThemeModeChange = onThemeModeChange)

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
            containerColor = Color.Transparent,
            topBar = {
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
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.menu_content_description),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding), color = Color.Transparent) {
                AppNavHost(navController, vm)
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.theme_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            ThemeOptionChip(
                icon = Icons.Default.PhoneAndroid,
                label = stringResource(R.string.theme_system),
                selected = themeMode == AppThemeMode.SYSTEM,
                onClick = { onThemeModeChange(AppThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionChip(
                icon = Icons.Default.LightMode,
                label = stringResource(R.string.theme_light),
                selected = themeMode == AppThemeMode.LIGHT,
                onClick = { onThemeModeChange(AppThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionChip(
                icon = Icons.Default.DarkMode,
                label = stringResource(R.string.theme_dark),
                selected = themeMode == AppThemeMode.DARK,
                onClick = { onThemeModeChange(AppThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeOptionChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(containerColor, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = contentColor)
    }
}

@Composable
fun UpdateMenuItem(currentVersionCode: Int, onUpdateFound: (UpdateInfo) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var hasUpdate by remember { mutableStateOf(false) }
    val currentVersionName = remember { UpdateManager.updateManager.getAppVersionName(context) }

    LaunchedEffect(Unit) {
        val info = UpdateManager.updateManager.fetchUpdateInfo()
        if (info != null && info.versionCode > currentVersionCode) {
            hasUpdate = true
            Toast.makeText(
                context,
                context.getString(R.string.update_server_vs_local, info.versionCode, currentVersionCode),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecking) {
                scope.launch {
                    isChecking = true
                    Toast.makeText(context, context.getString(R.string.update_checking_toast), Toast.LENGTH_SHORT).show()

                    val info = UpdateManager.updateManager.fetchUpdateInfo()
                    isChecking = false

                    if (info != null && info.versionCode > currentVersionCode) {
                        hasUpdate = true
                        onUpdateFound(info)
                    } else {
                        hasUpdate = false
                        Toast.makeText(context, context.getString(R.string.update_latest_toast), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = null,
            tint = if (hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                stringResource(R.string.update_version_label, currentVersionName),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    isChecking -> stringResource(R.string.update_checking)
                    hasUpdate -> stringResource(R.string.update_found)
                    else -> stringResource(R.string.update_check_action)
                },
                fontSize = 11.sp,
                color = if (hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpdateDialog(updateInfo: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(updateInfo.changeLog) },
        confirmButton = {
            Button(
                onClick = {
                    UpdateManager.updateManager.startDownload(context, updateInfo.downloadUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(stringResource(R.string.update_dialog_download)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_dialog_dismiss)) }
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

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.github_source_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(R.string.github_source_subtitle),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
