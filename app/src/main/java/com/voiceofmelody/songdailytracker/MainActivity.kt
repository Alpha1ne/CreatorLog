package com.voiceofmelody.songdailytracker

import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.TrackerViewModelFactory
import com.voiceofmelody.songdailytracker.ui.screens.DashboardScreen
import com.voiceofmelody.songdailytracker.ui.screens.IdeaVaultScreen
import com.voiceofmelody.songdailytracker.ui.screens.SettingsScreen
import com.voiceofmelody.songdailytracker.ui.screens.SongsScreen
import com.voiceofmelody.songdailytracker.ui.screens.AddEditSongScreen
import com.voiceofmelody.songdailytracker.ui.theme.InstagramCoral
import com.voiceofmelody.songdailytracker.ui.theme.MyApplicationTheme
import com.voiceofmelody.songdailytracker.util.openInstagramApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Clean up legacy Security PIN data from SharedPreferences
        val prefs = getSharedPreferences("vof_settings", MODE_PRIVATE)
        prefs.edit().apply {
            remove("pin_enabled")
            remove("security_pin")
            apply()
        }

        enableEdgeToEdge()
        setContent {
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
                    }
                )
            }
        }
    }
}

sealed class AppDestination {
    object Main : AppDestination()
    object Settings : AppDestination()
    data class AddEditSong(val song: com.voiceofmelody.songdailytracker.data.model.SongPost? = null) : AppDestination()
}

enum class TrackerTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD(
        title = "Dashboard",
        selectedIcon = Icons.Default.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        tag = "tab_dashboard"
    ),
    SONGS(
        title = "Songs",
        selectedIcon = Icons.Default.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote,
        tag = "tab_songs"
    ),
    PLANNER(
        title = "Planner",
        selectedIcon = Icons.AutoMirrored.Filled.EventNote,
        unselectedIcon = Icons.AutoMirrored.Outlined.EventNote,
        tag = "tab_ideas"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit
) {
    val appContext = LocalContext.current.applicationContext as android.app.Application
    val viewModel: TrackerViewModel = viewModel(
        factory = TrackerViewModelFactory(appContext)
    )

    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }
    var currentTab by remember { mutableStateOf(TrackerTab.DASHBOARD) }

    when (val destination = currentDestination) {
        is AppDestination.Main -> {
            MainAppScreen(
                viewModel = viewModel,
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onNavigateToSettings = { currentDestination = AppDestination.Settings },
                onNavigateToAddEditSong = { currentDestination = AppDestination.AddEditSong(it) }
            )
        }
        is AppDestination.Settings -> {
            SettingsScreen(
                onBack = { currentDestination = AppDestination.Main },
                themeMode = themeMode,
                onThemeModeChanged = onThemeModeChanged,
                viewModel = viewModel
            )
        }
        is AppDestination.AddEditSong -> {
            AddEditSongScreen(
                viewModel = viewModel,
                editingSong = destination.song,
                onBack = { currentDestination = AppDestination.Main }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: TrackerViewModel,
    currentTab: TrackerTab,
    onTabSelected: (TrackerTab) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddEditSong: (com.voiceofmelody.songdailytracker.data.model.SongPost?) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "CreatorLog",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
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
            when (currentTab) {
                TrackerTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = { onNavigateToAddEditSong(null) }
                )
                TrackerTab.SONGS -> SongsScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = onNavigateToAddEditSong
                )
                TrackerTab.PLANNER -> IdeaVaultScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FloatingNavigationBar(
    currentTab: TrackerTab,
    onTabSelected: (TrackerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = TrackerTab.entries.toTypedArray()
    
    Box(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .background(
                color = Color(0xFF18141F),
                shape = RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Indicator
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentTab == tab) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(InstagramCoral, RoundedCornerShape(24.dp))
                            )
                        }
                    }
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
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(tab.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.title,
            tint = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.8f),
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
        )
        
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = tab.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
