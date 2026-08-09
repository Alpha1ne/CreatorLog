package com.voiceofmelody.songdailytracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voiceofmelody.songdailytracker.data.model.PaymentStatus
import com.voiceofmelody.songdailytracker.data.model.Promotion
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPromotionScreen(
    viewModel: TrackerViewModel,
    editingPromotion: Promotion? = null,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var title by rememberSaveable { mutableStateOf(editingPromotion?.promotionTitle ?: "") }
    var amountText by rememberSaveable { mutableStateOf(editingPromotion?.amount?.toString() ?: "") }
    var status by rememberSaveable { mutableStateOf(editingPromotion?.paymentStatus ?: PaymentStatus.PENDING) }
    var client by rememberSaveable { mutableStateOf(editingPromotion?.client ?: "") }
    var contentLink by rememberSaveable { mutableStateOf(editingPromotion?.contentLink ?: "") }
    var notes by rememberSaveable { mutableStateOf(editingPromotion?.notes ?: "") }
    var paymentDate by rememberSaveable { mutableStateOf(editingPromotion?.paymentDate) }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = paymentDate ?: System.currentTimeMillis())

    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = amount > 0.0
    val isLinkValid = contentLink.isBlank() || android.util.Patterns.WEB_URL.matcher(contentLink).matches()
    val canSave = title.isNotBlank() && isAmountValid && isLinkValid

    val hasChanges = remember(title, amountText, status, client, contentLink, notes, paymentDate) {
        title != (editingPromotion?.promotionTitle ?: "") ||
        amountText != (editingPromotion?.amount?.toString() ?: "") ||
        status != (editingPromotion?.paymentStatus ?: PaymentStatus.PENDING) ||
        client != (editingPromotion?.client ?: "") ||
        contentLink != (editingPromotion?.contentLink ?: "") ||
        notes != (editingPromotion?.notes ?: "") ||
        paymentDate != editingPromotion?.paymentDate
    }

    BackHandler {
        if (hasChanges) showDiscardDialog = true else onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (editingPromotion == null) "Add Promotion" else "Edit Promotion", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { if (hasChanges) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (canSave) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            savePromotion(viewModel, editingPromotion, title, amount, status, client, contentLink, notes, paymentDate, onBack)
                            scope.launch { snackbarHostState.showSnackbar("Promotion saved successfully") }
                        }
                    }, enabled = canSave) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(DesignSystem.SpacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingNormal)
                ) {
                    OutlinedButton(
                        onClick = onBack, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            savePromotion(viewModel, editingPromotion, title, amount, status, client, contentLink, notes, paymentDate, onBack)
                            scope.launch { snackbarHostState.showSnackbar("Promotion saved successfully") }
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    ) {
                        Text("Save Promotion", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(DesignSystem.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
        ) {
            Card(
                shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(DesignSystem.CardPadding), verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
                    Text("Promotion Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Promotion Title *") }, placeholder = { Text("e.g., Summer Campaign") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
                    
                    OutlinedTextField(
                        value = amountText, 
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountText = it }, 
                        label = { Text("Amount *") }, 
                        placeholder = { Text("e.g., 5000") }, 
                        prefix = { Text("₹") },
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )

                    Text("Payment Status *", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                        PaymentStatus.entries.forEach { s ->
                            val isSelected = status == s
                            val color = when(s) {
                                PaymentStatus.PAID -> com.voiceofmelody.songdailytracker.ui.theme.StatusPosted
                                else -> com.voiceofmelody.songdailytracker.ui.theme.StatusScheduled
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { status = s },
                                label = { 
                                    Text(
                                        when(s) {
                                            PaymentStatus.PAID -> "Paid"
                                            PaymentStatus.PENDING -> "Pending"
                                            PaymentStatus.PARTIALLY_PAID -> "Partial"
                                        }
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }

                    OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Client") }, placeholder = { Text("e.g., Brand Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
                    
                    OutlinedTextField(
                        value = contentLink, 
                        onValueChange = { contentLink = it }, 
                        label = { Text("Content Link") }, 
                        placeholder = { Text("https://www.instagram.com/...") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                        isError = contentLink.isNotBlank() && !isLinkValid,
                        supportingText = if (contentLink.isNotBlank() && !isLinkValid) { { Text("Please enter a valid URL") } } else null
                    )

                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))

                    Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))
                    
                    Text("Payment Date (Optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (paymentDate != null) {
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(paymentDate!!))
                                    } else "Not Set",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (paymentDate != null) {
                                IconButton(
                                    onClick = { 
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        paymentDate = null 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Date", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("Are you sure you want to discard your changes?") },
            confirmButton = {
                TextButton(onClick = onBack) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { paymentDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}

private fun savePromotion(
    viewModel: TrackerViewModel,
    editingPromotion: Promotion?,
    title: String,
    amount: Double,
    status: PaymentStatus,
    client: String?,
    link: String?,
    notes: String?,
    paymentDate: Long?,
    onSuccess: () -> Unit
) {
    if (editingPromotion == null) {
        viewModel.addPromotion(title, amount, status, client, link, notes, paymentDate)
    } else {
        viewModel.updatePromotion(editingPromotion.id, title, amount, status, client, link, notes, editingPromotion.createdAt, paymentDate)
    }
    onSuccess()
}
