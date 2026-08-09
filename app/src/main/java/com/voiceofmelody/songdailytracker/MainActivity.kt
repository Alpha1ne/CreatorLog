package com.voiceofmelody.songdailytracker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.TrackerViewModelFactory
import com.voiceofmelody.songdailytracker.ui.screens.DashboardScreen
import com.voiceofmelody.songdailytracker.ui.screens.DashboardShimmerSkeleton
import com.voiceofmelody.songdailytracker.ui.screens.IdeaVaultScreen
import com.voiceofmelody.songdailytracker.ui.screens.SettingsScreen
import com.voiceofmelody.songdailytracker.ui.screens.SongsScreen
import com.voiceofmelody.songdailytracker.ui.screens.AddEditSongScreen
import com.voiceofmelody.songdailytracker.ui.screens.PromotionsScreen
import com.voiceofmelody.songdailytracker.ui.screens.AddEditPromotionScreen
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem
import com.voiceofmelody.songdailytracker.ui.theme.MyApplicationTheme
import com.voiceofmelody.songdailytracker.ui.theme.PrimaryBlue
import com.voiceofmelody.songdailytracker.util.openInstagramApp

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        splashScreen.setKeepOnScreenCondition { 
            false 
        }
        
        super.onCreate(savedInstanceState)
        
        // FIX 1A: Move legacy Preferences cleanup off the Main thread
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("vof_settings", MODE_PRIVATE)
            prefs.edit().apply {
                remove("pin_enabled")
                remove("security_pin")
                apply()
            }
        }

        enableEdgeToEdge()
        setContent {
            
            SideEffect {
            }

            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("vof_settings", Context.MODE_PRIVATE) }
            
            var themeMode by remember { mutableIntStateOf(sharedPrefs.getInt("theme_mode", 2)) }

            val systemTheme = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                0 -> systemTheme
                1 -> false
                else -> true
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                MainAppContainer(
                    themeMode = themeMode,
                    onThemeModeChanged = { newMode ->
                        themeMode = newMode
                        sharedPrefs.edit { putInt("theme_mode", newMode) }
                    },
                    onCheckNotificationPermission = { checkNotificationPermission() }
                )
            }
        }
    }
}

sealed class AppDestination {
    object Main : AppDestination()
    object Settings : AppDestination()
    object Promotions : AppDestination()
    data class AddEditSong(val song: com.voiceofmelody.songdailytracker.data.model.SongPost? = null) : AppDestination()
    data class AddEditPromotion(val promotion: com.voiceofmelody.songdailytracker.data.model.Promotion? = null) : AppDestination()
}

enum class TrackerTab(
    val title: String,
    val selectedIconRes: Int,
    val unselectedIconRes: Int,
    val tag: String
) {
    DASHBOARD(
        title = "Dashboard",
        selectedIconRes = R.drawable.ic_nav_dashboard,
        unselectedIconRes = R.drawable.ic_nav_dashboard_outline,
        tag = "tab_dashboard"
    ),
    SONGS(
        title = "Content",
        selectedIconRes = R.drawable.ic_nav_content,
        unselectedIconRes = R.drawable.ic_nav_content,
        tag = "tab_songs"
    ),
    PLANNER(
        title = "Planner",
        selectedIconRes = R.drawable.ic_nav_planner,
        unselectedIconRes = R.drawable.ic_nav_planner_outline,
        tag = "tab_ideas"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    onCheckNotificationPermission: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext as android.app.Application
    val viewModel: TrackerViewModel = viewModel(
        factory = remember { TrackerViewModelFactory(appContext) }
    )

    LaunchedEffect(Unit) {
        onCheckNotificationPermission()
    }

    var currentDestination by rememberSaveable(saver = AppDestinationSaver) { mutableStateOf<AppDestination>(AppDestination.Main) }
    var currentTab by rememberSaveable { mutableStateOf(TrackerTab.DASHBOARD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Main Content (Retained tabs and Settings)
            MainAppScreen(
                viewModel = viewModel,
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                showSettings = showSettings,
                onNavigateToSettings = { showSettings = true },
                onCloseSettings = { showSettings = false },
                onNavigateToAddEditSong = { currentDestination = AppDestination.AddEditSong(it) },
                onNavigateToPromotions = { currentDestination = AppDestination.Promotions },
                onNavigateToAddEditPromotion = { currentDestination = AppDestination.AddEditPromotion(it) },
                snackbarHostState = snackbarHostState,
                themeMode = themeMode,
                onThemeModeChanged = onThemeModeChanged,
                isVisible = currentDestination is AppDestination.Main || currentDestination is AppDestination.Promotions
            )

            // Promotions Screen (Full retained layer)
            if (currentDestination is AppDestination.Promotions) {
                PromotionsScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = { currentDestination = AppDestination.AddEditPromotion(it) },
                    onBack = { currentDestination = AppDestination.Main },
                    snackbarHostState = snackbarHostState
                )
            }

            // Add/Edit Song (Temporary screen)
            if (currentDestination is AppDestination.AddEditSong) {
                AddEditSongScreen(
                    viewModel = viewModel,
                    editingSong = (currentDestination as AppDestination.AddEditSong).song,
                    onBack = { currentDestination = AppDestination.Main },
                    snackbarHostState = snackbarHostState
                )
            }

            // Add/Edit Promotion (Temporary screen)
            if (currentDestination is AppDestination.AddEditPromotion) {
                AddEditPromotionScreen(
                    viewModel = viewModel,
                    editingPromotion = (currentDestination as AppDestination.AddEditPromotion).promotion,
                    onBack = { currentDestination = AppDestination.Promotions },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: TrackerViewModel,
    currentTab: TrackerTab,
    onTabSelected: (TrackerTab) -> Unit,
    showSettings: Boolean,
    onNavigateToSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onNavigateToAddEditSong: (com.voiceofmelody.songdailytracker.data.model.SongPost?) -> Unit,
    onNavigateToPromotions: () -> Unit,
    onNavigateToAddEditPromotion: (com.voiceofmelody.songdailytracker.data.model.Promotion?) -> Unit,
    snackbarHostState: SnackbarHostState,
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    isVisible: Boolean
) {
    if (!isVisible) return
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State holder to preserve scroll and UI state of uncomposed screens
    val tabStateHolder = rememberSaveableStateHolder()
    
    // Track if background tabs have been warmed up to eliminate first-visit hitch
    var isWarmedUp by rememberSaveable { mutableStateOf(false) }

    // FIRST-FRAME BRIDGE STATE: Initially show a lightweight shimmer box
    var showFullApp by remember { mutableStateOf(false) }

    if (!showFullApp) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DashboardShimmerSkeleton()
        }
        LaunchedEffect(Unit) {
            // Signal First Bridge Frame complete and swap to full shell on the next frame
            withFrameNanos { }
            showFullApp = true
        }
        return
    }

    // Define movable content for each tab to preserve composition and avoid layout spikes during moves
    val dashboardTab = remember {
        movableContentOf {
            tabStateHolder.SaveableStateProvider(TrackerTab.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = onNavigateToAddEditSong,
                    onNavigateToPromotions = onNavigateToPromotions,
                    onNavigateToAddEditPromotion = onNavigateToAddEditPromotion,
                    onTabSelected = onTabSelected,
                    snackbarHostState = snackbarHostState,
                    onReady = {
                        if (!isWarmedUp) {
                            scope.launch {
                                // Wait for two frames to ensure Dashboard settlement and UI idle
                                withFrameNanos { }
                                withFrameNanos { }
                                isWarmedUp = true
                            }
                        }
                    }
                )
            }
        }
    }

    // FIX 2: Lazy Lambda Registration. Defer creation until warming phase to speed up first frame.
    val songsTab = if (isWarmedUp || currentTab == TrackerTab.SONGS) {
        // We use a separate remember block for the lambda to ensure it's truly lazy.
        // It will only be created when the condition is first met.
        val songsContent = remember {
            movableContentOf {
                tabStateHolder.SaveableStateProvider(TrackerTab.SONGS) {
                    SongsScreen(
                        viewModel = viewModel,
                        onNavigateToAddEdit = onNavigateToAddEditSong,
                        onTabSelected = onTabSelected,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
        songsContent
    } else null

    val plannerTab = if (isWarmedUp || currentTab == TrackerTab.PLANNER) {
        val plannerContent = remember {
            movableContentOf {
                tabStateHolder.SaveableStateProvider(TrackerTab.PLANNER) {
                    IdeaVaultScreen(
                        viewModel = viewModel,
                        onTabSelected = onTabSelected,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
        plannerContent
    } else null

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Tabs Layer
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (showSettings) 0f else 1f),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (currentTab == TrackerTab.SONGS) "Content Library" else "CreatorLog",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { 
                                openInstagramApp(context) 
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram),
                                contentDescription = "Open Instagram",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_button")) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                FloatingNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = onTabSelected
                )
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Box {
                    // 1. The Active Spot (Measured and Placed)
                    // This spot occupies the full screen and is responsible for the visible UI.
                    Box(Modifier.fillMaxSize()) {
                        when (currentTab) {
                            TrackerTab.DASHBOARD -> dashboardTab()
                            TrackerTab.SONGS -> songsTab?.invoke()
                            TrackerTab.PLANNER -> plannerTab?.invoke()
                        }
                    }
                    
                    // 2. The Warming/Storage Slot (Composed but NOT measured or placed)
                    // This ensures inactive screens stay in the composition tree (warmed)
                    // but contribute zero layout/measurement work to the active Dashboard.
                    if (isWarmedUp) {
                        Layout(
                            content = {
                                if (currentTab != TrackerTab.DASHBOARD) dashboardTab()
                                if (currentTab != TrackerTab.SONGS) songsTab?.invoke()
                                if (currentTab != TrackerTab.PLANNER) plannerTab?.invoke()
                            }
                        ) { _, _ ->
                            // Return 0 size and DO NOT measure children.
                            // This eliminates the 85ms transition spike from 0 to full size
                            // because hidden tabs have essentially no layout existence.
                            layout(0, 0) {}
                        }
                    }
                }
            }
        }

        // Settings Layer (Overlay/Retained)
        if (showSettings) {
            SettingsScreen(
                onBack = onCloseSettings,
                themeMode = themeMode,
                onThemeModeChanged = onThemeModeChanged,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun FloatingNavigationBar(
    currentTab: TrackerTab,
    onTabSelected: (TrackerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { TrackerTab.entries.toTypedArray() }
    val selectedIndex = tabs.indexOf(currentTab)
    
    // Animate the index to drive the sliding pill (preserving spring feel)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "pillIndex"
    )
    
    Box(
        modifier = modifier
            .padding(horizontal = DesignSystem.ScreenPadding)
            .padding(bottom = DesignSystem.ScreenPadding)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge + 4.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge + 4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // FIX 1: Persistent Sliding Indicator (Draw-phase only movement)
            // Using a custom Layout to avoid structural changes in the Box/Row tree.
            Layout(
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                    )
                }
            ) { measurables, constraints ->
                val totalWidth = constraints.maxWidth
                val tabCount = tabs.size
                val tabWidth = totalWidth / tabCount
                
                val pillPlaceable = measurables.first().measure(
                    constraints.copy(minWidth = tabWidth, maxWidth = tabWidth)
                )
                
                layout(totalWidth, constraints.maxHeight) {
                    // FIX 3: High-Precision Pill Alignment
                    // Use (animatedIndex * totalWidth) / tabCount to avoid cumulative rounding errors
                    // that occur when using animatedIndex * (totalWidth / tabCount).
                    val xPosition = ((animatedIndex * totalWidth) / tabCount).toInt()
                    pillPlaceable.placeWithLayer(x = xPosition, y = 0)
                }
            }

            // Foreground Items
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    FloatingNavItem(
                        tab = tab,
                        isSelected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingNavItem(
    tab: TrackerTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // FIX 2: Draw-phase Icon Scaling
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )

    // FIX 3: Layout-stable Label Visibility (Draw-phase alpha)
    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "labelAlpha"
    )

    // Animate vertical shift in draw-phase to match the previous visual centering behavior
    // without the cost of a layout measurement pass.
    // Shift up by 6dp when selected to reveal the label.
    val verticalShift by animateFloatAsState(
        targetValue = if (isSelected) with(density) { -6.dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "verticalShift"
    )

    // FIX 2: Icon Centering (Independent coordinate system)
    // Using a Box instead of a Column to ensure the Icon's base layout position 
    // is always at the true center of the navigation item.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(tab.tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (isSelected) tab.selectedIconRes else tab.unselectedIconRes),
            contentDescription = tab.title,
            tint = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.8f),
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { 
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = verticalShift
                }
        )
        
        Text(
            text = tab.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                // Place label below the centered icon. 
                // Unselected verticalShift = 0 makes it invisible via alpha.
                // Selected verticalShift < 0 moves the whole group up.
                .padding(top = 34.dp) 
                .graphicsLayer { 
                    alpha = labelAlpha
                    translationY = verticalShift
                }
        )
    }
}

val AppDestinationSaver = androidx.compose.runtime.saveable.Saver<MutableState<AppDestination>, String>(
    save = { 
        when (val dest = it.value) {
            is AppDestination.Main -> "main"
            is AppDestination.Settings -> "settings"
            is AppDestination.Promotions -> "promotions"
            is AppDestination.AddEditSong -> "add_edit|${dest.song?.id ?: -1}"
            is AppDestination.AddEditPromotion -> "add_edit_promo|${dest.promotion?.id ?: -1}"
        }
    },
    restore = { savedString ->
        mutableStateOf(
            if (savedString == "main") AppDestination.Main 
            else if (savedString == "settings") AppDestination.Settings 
            else if (savedString == "promotions") AppDestination.Promotions
            else if (savedString.startsWith("add_edit_promo")) AppDestination.Main 
            else if (savedString.startsWith("add_edit")) AppDestination.Main 
            else AppDestination.Main
        )
    }
)
