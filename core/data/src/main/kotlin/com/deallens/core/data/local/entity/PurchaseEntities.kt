package com.deallens.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PurchaseItemDto(
    val id: String,
    val name: String,
    val unitPriceMinorUnits: Long,
    val currency: String,
    val quantity: Int
)

@Entity(tableName = "confirmed_purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val timestamp: Instant,
    val itemsJson: String,
    val sellerOrPlatform: String,
    val payNowAmountMinorUnits: Long,
    val currency: String,
    val reportingCurrency: String,
    val immediateDiscountsMinorUnits: Long,
    val futureCashbackMinorUnits: Long,
    val receiptNote: String?,
    val receiptImageUri: String?,
    val snapshotId: String? = null
)
