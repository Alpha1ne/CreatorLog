package com.voiceofmelody.songdailytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentStatus { PENDING, PARTIALLY_PAID, PAID }

@Entity(tableName = "promotions")
data class Promotion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val promotionTitle: String,
    val amount: Double,
    val paymentStatus: PaymentStatus,
    val client: String? = null,
    val contentLink: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val paymentDate: Long? = null
)
