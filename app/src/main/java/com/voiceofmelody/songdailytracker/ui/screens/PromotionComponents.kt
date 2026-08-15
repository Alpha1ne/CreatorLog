package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceofmelody.songdailytracker.data.model.PaymentStatus
import com.voiceofmelody.songdailytracker.data.model.Promotion
import com.voiceofmelody.songdailytracker.ui.PromotionStats
import com.voiceofmelody.songdailytracker.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PromotionStatsCard(
    stats: PromotionStats,
    hasPromotions: Boolean,
    onViewAll: () -> Unit,
    onAddPromotion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding)) {
            Text("Promotion Earnings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            if (!hasPromotions) {
                Spacer(modifier = Modifier.height(DesignSystem.SpacingMedium))
                Text("No promotions yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Start tracking your promotion income.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(DesignSystem.SpacingLarge))
                Button(
                    onClick = onAddPromotion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                    Text("Add Promotion")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title already above, but we can put "View All" here
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onViewAll) {
                        Text("View All")
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                Text("Total Earnings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatCurrency(stats.totalEarnings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(DesignSystem.SpacingLarge))

                // Monthly Chart
                if (stats.monthlyEarnings.isNotEmpty()) {
                    MonthlyEarningsChart(earnings = stats.monthlyEarnings)
                    Spacer(modifier = Modifier.height(DesignSystem.SpacingLarge))
                }

                // Summaries
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingNormal)) {
                    EarningSummaryItem(
                        label = "Paid",
                        amount = stats.paidEarnings,
                        color = StatusPosted,
                        modifier = Modifier.weight(1f)
                    )
                    EarningSummaryItem(
                        label = "Pending",
                        amount = stats.pendingEarnings,
                        color = StatusScheduled,
                        modifier = Modifier.weight(1f)
                    )
                    EarningSummaryItem(
                        label = "Partial",
                        amount = stats.partiallyPaidEarnings,
                        color = StatusScheduled,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(DesignSystem.SpacingLarge))

                Button(
                    onClick = onAddPromotion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                    Text("Add Promotion")
                }
            }
        }
    }
}

@Composable
fun MonthlyEarningsChart(earnings: List<com.voiceofmelody.songdailytracker.ui.MonthlyEarning>) {
    val maxVal = earnings.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    
    val accessibilityDescription = remember(earnings) {
        val valuesDescription = earnings.asReversed().joinToString(", ") { 
            "${it.monthYear} ${formatCurrency(it.amount)}" 
        }
        "Monthly promotion earnings for the last ${earnings.size} months. $valuesDescription"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp)
            .semantics { contentDescription = accessibilityDescription }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            earnings.forEach { earning ->
                val barHeightFraction = (earning.amount / maxVal).toFloat()
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(barHeightFraction.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(SecondaryPurple, SecondaryPurple.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = earning.monthYear,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EarningSummaryItem(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (amount == 0.0) "₹0" else formatCurrency(amount), 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PromotionGridItem(
    promotion: Promotion,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .workspacePressAnimation(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            DesignSystem.BorderThickness,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(DesignSystem.SpacingMedium).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PaymentStatusBadge(status = promotion.paymentStatus)
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))

            Text(
                text = promotion.promotionTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!promotion.client.isNullOrBlank()) {
                Text(
                    text = promotion.client,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (promotion.amount == 0.0) "Amount TBD" else formatCurrency(promotion.amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )

            if (promotion.paymentDate != null) {
                Text(
                    text = "Paid on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(promotion.paymentDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!promotion.contentLink.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            com.voiceofmelody.songdailytracker.util.openContentLink(context, promotion.contentLink)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionItem(
    promotion: Promotion,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().workspacePressAnimation(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(DesignSystem.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(promotion.promotionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!promotion.client.isNullOrBlank()) {
                    Text(promotion.client, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (promotion.paymentDate != null) {
                    Text(
                        text = "Paid on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(promotion.paymentDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (promotion.amount == 0.0) "Amount TBD" else formatCurrency(promotion.amount), 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Black
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                PaymentStatusBadge(status = promotion.paymentStatus)
                
                Row {
                    if (!promotion.contentLink.isNullOrBlank()) {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            com.voiceofmelody.songdailytracker.util.openContentLink(context, promotion.contentLink) 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Link", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    val (color, label) = when (status) {
        PaymentStatus.PAID -> StatusPosted to "Paid"
        PaymentStatus.PENDING -> StatusScheduled to "Pending"
        PaymentStatus.PARTIALLY_PAID -> StatusScheduled to "Partial"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusBadge),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun formatCurrency(amount: Double): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.format(amount)
    } catch (_: Exception) {
        "₹${String.format("%.2f", amount)}"
    }
}
