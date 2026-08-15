package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voiceofmelody.songdailytracker.data.model.PaymentStatus
import com.voiceofmelody.songdailytracker.data.model.Promotion
import com.voiceofmelody.songdailytracker.ui.PromotionFilter
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.ViewMode
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(
    viewModel: TrackerViewModel,
    onNavigateToAddEdit: (Promotion?) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val promotions by viewModel.searchedPromotions.collectAsState()
    val searchQuery by viewModel.promotionSearchQuery.collectAsState()
    val viewMode by viewModel.promotionViewMode.collectAsState()
    val statusFilter by viewModel.promotionStatusFilter.collectAsState()
    
    val scope = rememberCoroutineScope()
    var showFiltersPanel by remember { mutableStateOf(false) }
    var promotionToDelete by remember { mutableStateOf<Promotion?>(null) }

    val filteredPromotions by remember(promotions, statusFilter) {
        derivedStateOf {
            when (statusFilter) {
                PromotionFilter.ALL -> promotions
                PromotionFilter.PAID -> promotions.filter { it.paymentStatus == PaymentStatus.PAID }
                PromotionFilter.PENDING -> promotions.filter { it.paymentStatus == PaymentStatus.PENDING }
                PromotionFilter.PARTIAL -> promotions.filter { it.paymentStatus == PaymentStatus.PARTIALLY_PAID }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Promotions", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                }
            )
        },
        floatingActionButton = {
            CreatorLogFAB(
                onClick = { onNavigateToAddEdit(null) },
                contentDescription = "Add Promotion",
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 116.dp)
                    .testTag("add_promotion_fab")
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            // Search Bar
            Box(modifier = Modifier.padding(DesignSystem.ScreenPadding)) {
                UnifiedSearchToolbar(
                    query = searchQuery,
                    onQueryChange = { viewModel.promotionSearchQuery.value = it },
                    placeholder = "Search promotions...",
                    showFiltersPanel = showFiltersPanel,
                    onFilterToggle = { showFiltersPanel = !showFiltersPanel },
                    viewMode = viewMode,
                    onViewModeToggle = { viewModel.promotionViewMode.value = it },
                    testTag = "promotion_search"
                )
            }

            // Filters Panel
            AnimatedVisibility(
                visible = showFiltersPanel,
                enter = expandVertically(animationSpec = tween(DesignSystem.AnimationDurationShort, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(DesignSystem.AnimationDurationShort, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignSystem.ScreenPadding, vertical = DesignSystem.SpacingTiny)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)
                ) {
                    PromotionFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = statusFilter == filter,
                            onClick = { viewModel.promotionStatusFilter.value = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        PromotionFilter.ALL -> "All"
                                        PromotionFilter.PAID -> "Paid"
                                        PromotionFilter.PENDING -> "Pending"
                                        PromotionFilter.PARTIAL -> "Partial"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (filteredPromotions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank() && statusFilter == PromotionFilter.ALL) "No promotions recorded yet" else "No matches found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = DesignSystem.ScreenPadding, 
                            top = 0.dp, 
                            end = DesignSystem.ScreenPadding, 
                            bottom = 116.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
                    ) {
                        items(filteredPromotions, key = { it.id }, contentType = { "promotion_item" }) { promo ->
                            PromotionItem(
                                promotion = promo,
                                onClick = { onNavigateToAddEdit(promo) },
                                onDelete = { promotionToDelete = promo }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = DesignSystem.ScreenPadding, 
                            top = 0.dp, 
                            end = DesignSystem.ScreenPadding, 
                            bottom = 116.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing),
                        verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
                    ) {
                        items(filteredPromotions, key = { it.id }, contentType = { "promotion_grid_item" }) { promo ->
                            PromotionGridItem(
                                promotion = promo,
                                onClick = { onNavigateToAddEdit(promo) },
                                onDelete = { promotionToDelete = promo }
                            )
                        }
                    }
                }
            }
        }
    }

    if (promotionToDelete != null) {
        AlertDialog(
            onDismissRequest = { promotionToDelete = null },
            title = { Text("Delete Promotion?") },
            text = { Text("Are you sure you want to permanently delete this promotion record?") },
            confirmButton = {
                Button(
                    onClick = {
                        val promo = promotionToDelete!!
                        viewModel.deletePromotion(promo)
                        promotionToDelete = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Promotion deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeletePromotion()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { promotionToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
